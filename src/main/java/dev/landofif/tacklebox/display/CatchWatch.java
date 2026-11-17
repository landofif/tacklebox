package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.hud.CommunityGoal;
import dev.landofif.tacklebox.hud.EventHud;
import dev.landofif.tacklebox.menu.Compendium;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Your catches, read by tacklebox itself (2026-10-30, it used foecollab's catch hook before): a title that is one glyph in
 * U+E000-E999 is the caught fish's icon, then within 3 seconds a fish stack turns up in your inventory whose {@code catcher}
 * is you and whose {@code id} was not there when the title came (research section 3). A catch with a full inventory (the
 * fish drops on the ground) is missed.
 */
public final class CatchWatch {
    /** A caught fish from its custom_data: species, variant, size and location ids as the server writes them. */
    public record Fish(UUID id, String fish, String variant, String size, String location) {}

    static final long WINDOW = 3000;
    /** No fish item this long after the title (full inventory): the catch effects go by the server's subtitle, like the catch title. */
    static final long FALLBACK_MS = CatchTitle.FALLBACK_MS;
    /** A subtitle this close to the catch title, before or after it, is that catch's. */
    static final long SUBTITLE_MS = CatchTitle.SUBTITLE_MS;

    private static Set<UUID> before;
    private static long until;
    /** The last catch title, whether its catch effects ran, and the last subtitle with a rarity badge. */
    private static long titleAt, subtitleAt;
    private static boolean effectsDone;
    private static String subtitle = "";

    private CatchWatch() {}

    public static boolean isCatchTitle(String title) {
        return title.length() == 1 && title.charAt(0) >= '' && title.charAt(0) <= '';
    }

    /** A fish of yours from an item's custom_data, or null for anything else. */
    public static Fish read(NbtCompound data, UUID me) {
        if (data == null || data.getString("fish").isEmpty()) {
            return null;
        }
        int[] id = data.getIntArray("id").orElse(new int[0]);
        int[] catcher = data.getIntArray("catcher").orElse(new int[0]);
        if (id.length != 4 || catcher.length != 4 || !Uuids.toUuid(catcher).equals(me)) {
            return null;
        }
        return new Fish(Uuids.toUuid(id), data.getString("fish").orElse(""), data.getString("variant").orElse("normal"),
                data.getString("size").orElse(""), data.getString("location").orElse(""));
    }

    /** The title came: remember which fish of yours are already in the inventory. */
    public static void onTitle(String title, List<NbtCompound> inventory, UUID me, long now) {
        if (!isCatchTitle(title)) {
            return;
        }
        before = new HashSet<>();
        for (NbtCompound data : inventory) {
            Fish f = read(data, me);
            if (f != null) {
                before.add(f.id());
            }
        }
        until = now + WINDOW;
        titleAt = now;
        effectsDone = false;
        BigCatch.clear();
    }

    /** The server's subtitle: one with a rarity badge is kept for a catch whose fish never reaches the inventory. */
    public static void onSubtitle(String text, long now) {
        if (text != null && !CatchTitle.rarityOf(text).isEmpty()) {
            subtitle = text;
            subtitleAt = now;
        }
    }

    /** What a catch of yours shows on the water and the screen, once per catch title: the catch ring and the big catch moment. */
    static void effects(String rarity, String variant, String size, long now) {
        effectsDone = true;
        WaterRings.Spot spot = WaterRings.onCatch(rarity, now);
        if (spot != null) {
            WaterRingRenderer.sparkles(spot, rarity, WaterRings.sparkles(rarity));
        }
        BigCatch.onCatch(rarity, variant, size, now);
    }

    /**
     * No fish item {@link #FALLBACK_MS} after the title (full inventory): the effects go by the subtitle's badges, so rarity,
     * mythical, fabled and trophy are known but gigantic is not. Gives up once no subtitle came within {@link #SUBTITLE_MS}.
     */
    static void fallback(long now) {
        if (effectsDone || titleAt == 0 || now - titleAt < FALLBACK_MS) {
            return;
        }
        if (subtitleAt != 0 && Math.abs(subtitleAt - titleAt) <= SUBTITLE_MS) {
            effects(CatchTitle.rarityOf(subtitle), CatchTitle.variantOf(subtitle), "", now);
        } else if (now - titleAt > SUBTITLE_MS) {
            effectsDone = true;
        }
    }

    /** The new fish of yours, once, while a catch title is fresh; null otherwise. */
    public static Fish check(List<NbtCompound> inventory, UUID me, long now) {
        if (before == null) {
            return null;
        }
        if (now > until) {
            before = null;
            return null;
        }
        for (NbtCompound data : inventory) {
            Fish f = read(data, me);
            if (f != null && !before.contains(f.id())) {
                before = null;
                return f;
            }
        }
        return null;
    }

    public static void onTitle(Text title) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null && title != null) {
            onTitle(title.getString(), inventory(mc), mc.player.getUuid(), System.currentTimeMillis());
        }
    }

    public static void tick(MinecraftClient mc) {
        if (before == null || mc.player == null) {
            return;
        }
        long now = System.currentTimeMillis();
        Fish f = check(inventory(mc), mc.player.getUuid(), now);
        if (f == null) {
            fallback(now);
        } else {
            onCatch(f, now);
            String name = f.fish().replace('_', ' '), rarity = "", group = "";
            for (ItemStack stack : mc.player.getInventory().getMainStacks()) { // the catch title wants the item: its name, weight and length
                NbtComponent c = stack.isEmpty() ? null : stack.get(DataComponentTypes.CUSTOM_DATA);
                Fish same = c == null ? null : read(c.copyNbt(), mc.player.getUuid());
                if (same != null && same.id().equals(f.id())) {
                    CatchTitle.onFish(c.copyNbt(), stack.getName().getString(), now);
                    name = CatchTitle.read(c.copyNbt(), stack.getName().getString()).name();
                    rarity = c.copyNbt().getString("rarity").orElse("");
                    group = c.copyNbt().getString("group").orElse("");
                    if (!effectsDone) {
                        effects(rarity, f.variant(), f.size(), now);
                    }
                    break;
                }
            }
            dev.landofif.tacklebox.chat.DryStreakChat.onCatch(name, f.variant(), f.size(), now);
            dev.landofif.tacklebox.hud.MiniEventHud.onCatch(new dev.landofif.tacklebox.hud.MiniEvent.Fish(f.location(), f.variant(), f.size(), rarity, group, name), now);
        }
    }

    /** Everything a catch of yours feeds: the community goal, the compendium lists, the event missions and the cryptid dry streaks. */
    static void onCatch(Fish f, long now) {
        CommunityGoal.onCatch(now);
        Compendium.onCatch(f.fish(), f.variant(), f.size(), f.location());
        boolean event = EventHud.onCatch(f.location(), f.fish(), now);
        if (dev.landofif.tacklebox.hud.CryptidHud.onCatch(f.location(), f.variant(), now) || event) {
            TackleboxSettings.save();
        }
    }

    private static List<NbtCompound> inventory(MinecraftClient mc) {
        List<NbtCompound> out = new ArrayList<>();
        for (ItemStack stack : mc.player.getInventory().getMainStacks()) {
            NbtComponent c = stack.isEmpty() ? null : stack.get(DataComponentTypes.CUSTOM_DATA);
            if (c != null) {
                out.add(c.copyNbt());
            }
        }
        return out;
    }
}
