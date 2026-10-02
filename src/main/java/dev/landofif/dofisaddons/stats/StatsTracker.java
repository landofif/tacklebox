package dev.landofif.dofisaddons.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.trigger.Matcher;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Counts catches and rare drops. Purely watches what the server already sends (titles, chat,
 * your inventory); it never clicks, casts, reels or sends anything.
 *
 * <p>How a catch is detected: FishOnMC shows the caught fish's icon as a one-character screen
 * title. When that title appears, a fish item that is new to your inventory (or lying on the
 * ground next to you when your inventory is full) and carries your UUID as its catcher is counted.
 * Every fish has a unique id, so the same fish is never counted twice.
 */
public final class StatsTracker {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final long CATCH_WINDOW_MS = 2500;
    private static final Pattern SHARD_COUNT = Pattern.compile("pulled an? (\\d+)x", Pattern.CASE_INSENSITIVE);
    private static final Pattern MONEY = Pattern.compile("\\$([0-9][0-9.,]*)\\s*([KMB]?)", Pattern.CASE_INSENSITIVE);

    private static StatBook session = new StatBook();
    private static StatBook allTime = new StatBook();
    private static UUID allTimeOwner;
    private static DofiSettings settings = new DofiSettings();

    /** Fish ids already accounted for (already held, already counted, or seen without a catch title). */
    private static final Set<UUID> known = new HashSet<>();
    /** Fish ids that just showed up, with the time they appeared; resolved within the catch window. */
    private static final Map<UUID, Long> fresh = new HashMap<>();
    private static final Map<UUID, FishInfo> pendingInfo = new HashMap<>();
    private static long catchTitleAt;
    private static boolean primed;
    private static long lastCatchAt;
    private static long lastTickAt;
    private static long lastSaveAt;
    private static boolean dirty;

    private StatsTracker() {}

    public static StatBook session() {
        return session;
    }

    public static StatBook allTime() {
        return allTime;
    }

    public static DofiSettings settings() {
        return settings;
    }

    // ------------------------------------------------------------------ lifecycle

    public static void onJoin() {
        known.clear();
        fresh.clear();
        pendingInfo.clear();
        primed = false;
        catchTitleAt = 0;
    }

    public static void onLeave() {
        saveAllTime();
        known.clear();
        fresh.clear();
        pendingInfo.clear();
        primed = false;
    }

    public static void resetSession() {
        session = new StatBook();
    }

    public static void resetAllTime() {
        allTime = new StatBook();
        dirty = true;
        saveAllTime();
    }

    // ------------------------------------------------------------------ inputs

    /** Called for every screen title the server sends (see InGameHudMixin). */
    public static void onTitle(Text title) {
        if (title == null) {
            return;
        }
        String s = title.getString();
        if (s.length() == 1 && s.charAt(0) > 0xE000 && s.charAt(0) < 0xE999 && TriggerEngine.isOnFishOnMc()) {
            catchTitleAt = System.currentTimeMillis();
        }
    }

    public static void onChat(Text message) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null || !TriggerEngine.isOnFishOnMc()) {
            return;
        }
        ensureAllTimeLoaded(player.getUuid());
        String line = Matcher.normalize(message.getString());
        String lower = line.toLowerCase(Locale.ROOT);

        if (lower.startsWith("pet drop!")) {
            both(b -> {
                b.pets++;
                b.mark("pet");
            });
        } else if (lower.startsWith("rare catch!")) {
            if (lower.contains("shard")) {
                long n = 1;
                java.util.regex.Matcher m = SHARD_COUNT.matcher(line);
                if (m.find()) {
                    n = Long.parseLong(m.group(1));
                }
                long count = n;
                both(b -> {
                    b.shards += count;
                    b.mark("shard");
                });
            } else if (lower.contains("lightning in a bottle")) {
                both(b -> {
                    b.lightningBottles++;
                    b.mark("lightning");
                });
            } else if (lower.contains("infusion capsule")) {
                both(b -> {
                    b.infusionCapsules++;
                    b.mark("capsule");
                });
            }
        } else if (lower.startsWith("quests >") && lower.contains("has been completed")) {
            both(b -> b.questsCompleted++);
        } else if (lower.startsWith("sell >") && lower.contains("earned")) {
            java.util.regex.Matcher m = MONEY.matcher(line);
            if (m.find()) {
                double amount = parseMoney(m.group(1), m.group(2));
                both(b -> b.moneyEarned += amount);
            }
        }
    }

    public static void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        long dt = lastTickAt == 0 ? 0 : Math.min(1000, now - lastTickAt);
        lastTickAt = now;

        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || !TriggerEngine.isOnFishOnMc()) {
            return;
        }
        ensureAllTimeLoaded(player.getUuid());

        // Active fishing time: keeps running while catches keep coming.
        if (lastCatchAt != 0 && now - lastCatchAt < settings.pauseAfterSeconds * 1000L) {
            session.activeMillis += dt;
            allTime.activeMillis += dt;
            dirty = true;
        }

        scanInventory(player, now);
        if (now - catchTitleAt < CATCH_WINDOW_MS) {
            scanGround(client, player, now);
        }
        resolveFresh(player.getUuid(), now);

        if (dirty && now - lastSaveAt > 30_000) {
            saveAllTime();
        }
    }

    private static void scanInventory(ClientPlayerEntity player, long now) {
        var stacks = player.getInventory().getMainStacks();
        boolean first = !primed;
        for (ItemStack stack : stacks) {
            FishInfo fish = FishInfo.from(stack);
            if (fish == null || known.contains(fish.id())) {
                continue;
            }
            if (first) {
                known.add(fish.id()); // already held when we joined
            } else {
                fresh.putIfAbsent(fish.id(), now);
                pendingInfo.putIfAbsent(fish.id(), fish);
            }
        }
        primed = true;
    }

    private static void scanGround(MinecraftClient client, ClientPlayerEntity player, long now) {
        for (var entity : client.world.getEntities()) {
            if (entity instanceof ItemEntity item && entity.squaredDistanceTo(player) < 64) {
                FishInfo fish = FishInfo.from(item.getStack());
                if (fish != null && !known.contains(fish.id())) {
                    fresh.putIfAbsent(fish.id(), now);
                    pendingInfo.putIfAbsent(fish.id(), fish);
                }
            }
        }
    }

    /** A fish that appears within the catch window of a catch title (either order) is a catch. */
    private static void resolveFresh(UUID me, long now) {
        Iterator<Map.Entry<UUID, Long>> it = fresh.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<UUID, Long> e = it.next();
            UUID id = e.getKey();
            long seenAt = e.getValue();
            FishInfo fish = pendingInfo.get(id);
            boolean nearTitle = catchTitleAt != 0 && Math.abs(seenAt - catchTitleAt) < CATCH_WINDOW_MS;
            if (fish != null && nearTitle && me.equals(fish.catcher())) {
                countCatch(fish, now);
                catchTitleAt = 0; // one title = one fish
            } else if (now - seenAt < CATCH_WINDOW_MS) {
                continue; // the title may still be on its way
            }
            known.add(id);
            pendingInfo.remove(id);
            it.remove();
        }
    }

    private static void countCatch(FishInfo fish, long now) {
        lastCatchAt = now;
        both(b -> b.addFish(fish, now));
        DofisAddons.LOGGER.debug("Counted catch: {} ({} {} {})", fish.species(), fish.rarity(), fish.size(), fish.variant());
    }

    private static void both(java.util.function.Consumer<StatBook> change) {
        change.accept(session);
        change.accept(allTime);
        dirty = true;
    }

    static double parseMoney(String number, String suffix) {
        double v;
        try {
            v = Double.parseDouble(number.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
        return switch (suffix.toUpperCase(Locale.ROOT)) {
            case "K" -> v * 1_000;
            case "M" -> v * 1_000_000;
            case "B" -> v * 1_000_000_000;
            default -> v;
        };
    }

    // ------------------------------------------------------------------ files

    public static Path dir() {
        return FabricLoader.getInstance().getConfigDir().resolve(DofisAddons.MOD_ID);
    }

    private static void ensureAllTimeLoaded(UUID player) {
        if (player.equals(allTimeOwner)) {
            return;
        }
        if (allTimeOwner != null) {
            saveAllTime();
        }
        allTimeOwner = player;
        allTime = read(dir().resolve("stats").resolve(player + ".json"), StatBook.class, new StatBook());
        allTime.sanitize();
    }

    public static void saveAllTime() {
        lastSaveAt = System.currentTimeMillis();
        if (allTimeOwner == null) {
            return;
        }
        write(dir().resolve("stats").resolve(allTimeOwner + ".json"), allTime);
        dirty = false;
    }

    public static void loadSettings() {
        settings = read(dir().resolve("settings.json"), DofiSettings.class, new DofiSettings());
        settings.sanitize();
        dev.landofif.dofisaddons.ui.S24.apply(settings.flavor);
    }

    public static void saveSettings() {
        settings.sanitize();
        write(dir().resolve("settings.json"), settings);
    }

    public static <T> T read(Path path, Class<T> type, T fallback) {
        if (!Files.isRegularFile(path)) {
            return fallback;
        }
        try {
            T value = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), type);
            return value == null ? fallback : value;
        } catch (IOException | JsonParseException e) {
            DofisAddons.LOGGER.error("Could not read {}; keeping a backup and starting fresh", path, e);
            try {
                Files.copy(path, path.resolveSibling(path.getFileName() + ".broken"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // nothing more we can do
            }
            return fallback;
        }
    }

    public static void write(Path path, Object value) {
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling(path.getFileName() + ".tmp");
            Files.writeString(tmp, GSON.toJson(value), StandardCharsets.UTF_8);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            DofisAddons.LOGGER.error("Could not save {}", path, e);
        }
    }
}
