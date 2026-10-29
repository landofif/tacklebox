package dev.landofif.tacklebox.chat;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.JsonOps;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.mixin.ChatHudAccessor;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Chat back after a reconnect or a restart (fishonmc only): when vanilla clears chat on disconnect
 * (ChatHudMixin, top of ChatHud.clear) the newest lines of vanilla chat and every box's lines are
 * written to config/tacklebox/chat-restore.json. On the next join, empty chat and empty boxes get
 * them back under a gray line with the date they were saved. Restored lines skip hide rules,
 * triggers, stacking and box routing: they go straight into the lists.
 */
public final class ChatRestore {
    private static final Logger LOG = LoggerFactory.getLogger("tacklebox");
    /** Vanilla chat lines kept; boxes keep all of theirs (200 at most). */
    static final int KEEP = 200;
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("d MMM HH:mm", Locale.ENGLISH);

    /** One line and when it arrived. */
    public record Saved(Text text, long at) {}

    /** What is written: when, vanilla chat (oldest first) and each box's lines by box name. */
    record Snapshot(long savedAt, List<Saved> chat, Map<String, List<Saved>> boxes) {}

    /** The text codec; tests swap in a plain one (the real one needs the game's registries). */
    static java.util.function.Supplier<com.mojang.serialization.Codec<Text>> codec = () -> TextCodecs.CODEC;

    private static boolean onFishOnMc;
    private static boolean saved;
    private static RegistryWrapper.WrapperLookup registries;

    private ChatRestore() {}

    private static Path file() {
        return TackleboxSettings.dir().resolve("chat-restore.json");
    }

    /** The gray line between restored chat and the new session's. */
    public static Text divider(long savedAt, ZoneId zone) {
        return Foe.text("── " + DATE.withZone(zone).format(Instant.ofEpochMilli(savedAt)).toLowerCase(Locale.ROOT) + " ──").formatted(Formatting.DARK_GRAY);
    }

    // ------------------------------------------------------------------ game side

    /** On join: remember the server, and put the saved chat back. */
    public static void join(RegistryWrapper.WrapperLookup lookup) {
        registries = lookup;
        onFishOnMc = TriggerEngine.isOnFishOnMc();
        saved = false;
        if (!onFishOnMc) {
            return;
        }
        Snapshot snap = read(file(), ops());
        if (snap == null) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHud hud = client.inGameHud.getChatHud();
        Text divider = divider(snap.savedAt(), ZoneId.systemDefault());
        List<ChatHudLine> messages = ((ChatHudAccessor) hud).tacklebox$messages();
        if (messages.isEmpty() && !snap.chat().isEmpty()) {
            int tick = client.inGameHud.getTicks();
            for (Saved s : snap.chat()) {
                messages.addFirst(new ChatHudLine(tick, s.text(), null, null));
                ChatTimes.record(s.text(), s.at());
            }
            messages.addFirst(new ChatHudLine(tick, divider, null, null));
            ((ChatHudAccessor) hud).tacklebox$refresh();
        }
        snap.boxes().forEach((name, lines) -> ChatBoxes.restore(name, lines, divider));
    }

    /** Vanilla is about to clear chat (a disconnect): save it while it is still there. */
    public static void beforeClear(ChatHud hud) {
        if (!onFishOnMc) {
            return;
        }
        List<Saved> chat = new ArrayList<>();
        List<ChatHudLine> messages = ((ChatHudAccessor) hud).tacklebox$messages();
        for (int i = Math.min(KEEP, messages.size()) - 1; i >= 0; i--) {
            Text t = messages.get(i).content();
            long at = ChatTimes.at(t);
            chat.add(new Saved(t, at < 0 ? System.currentTimeMillis() : at));
        }
        if (chat.isEmpty()) {
            return;
        }
        write(file(), new Snapshot(System.currentTimeMillis(), chat, ChatBoxes.saved()), ops());
        saved = true;
    }

    /** Closing the game while connected, in case it did not clear chat first. */
    public static void stopping(MinecraftClient client) {
        if (!saved && client.player != null) {
            beforeClear(client.inGameHud.getChatHud());
        }
    }

    private static DynamicOps<JsonElement> ops() {
        return registries == null ? JsonOps.INSTANCE : registries.getOps(JsonOps.INSTANCE);
    }

    // ------------------------------------------------------------------ file

    static void write(Path path, Snapshot snap, DynamicOps<JsonElement> ops) {
        JsonObject root = new JsonObject();
        root.addProperty("savedAt", snap.savedAt());
        root.add("chat", lines(snap.chat(), ops));
        JsonObject boxes = new JsonObject();
        snap.boxes().forEach((name, lines) -> boxes.add(name, lines(lines, ops)));
        root.add("boxes", boxes);
        try {
            Files.createDirectories(path.getParent());
            Files.writeString(path, new Gson().toJson(root), StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("could not write {}", path, e);
        }
    }

    /** Null when there is no file or it cannot be read. */
    static Snapshot read(Path path, DynamicOps<JsonElement> ops) {
        try {
            if (!Files.exists(path)) {
                return null;
            }
            JsonObject root = new Gson().fromJson(Files.readString(path, StandardCharsets.UTF_8), JsonObject.class);
            Map<String, List<Saved>> boxes = new LinkedHashMap<>();
            if (root.has("boxes")) {
                root.getAsJsonObject("boxes").entrySet().forEach(e -> boxes.put(e.getKey(), lines(e.getValue().getAsJsonArray(), ops)));
            }
            return new Snapshot(root.get("savedAt").getAsLong(), lines(root.getAsJsonArray("chat"), ops), boxes);
        } catch (IOException | RuntimeException e) {
            LOG.warn("could not read {}", path, e);
            return null;
        }
    }

    private static JsonArray lines(List<Saved> lines, DynamicOps<JsonElement> ops) {
        JsonArray out = new JsonArray();
        for (Saved s : lines) {
            JsonObject o = new JsonObject();
            // a line that does not encode (it needs something the codec cannot write) keeps its words
            o.add("text", codec.get().encodeStart(ops, s.text()).result()
                    .or(() -> codec.get().encodeStart(ops, Text.literal(s.text().getString())).result())
                    .orElseThrow());
            o.addProperty("at", s.at());
            out.add(o);
        }
        return out;
    }

    private static List<Saved> lines(JsonArray arr, DynamicOps<JsonElement> ops) {
        List<Saved> out = new ArrayList<>();
        if (arr == null) {
            return out;
        }
        for (JsonElement e : arr) {
            JsonObject o = e.getAsJsonObject();
            codec.get().parse(ops, o.get("text")).result()
                    .ifPresent(t -> out.add(new Saved(t, o.get("at").getAsLong())));
        }
        return out;
    }
}
