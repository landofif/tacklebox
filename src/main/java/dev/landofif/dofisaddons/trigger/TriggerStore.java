package dev.landofif.dofisaddons.trigger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.reflect.TypeToken;
import dev.landofif.dofisaddons.DofisAddons;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/** Loads and saves the trigger list at {@code config/dofisaddons/triggers.json}. */
public final class TriggerStore {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String SHARE_PREFIX = "dofi-trigger:";

    private static final List<Trigger> TRIGGERS = new ArrayList<>();

    private TriggerStore() {}

    public static List<Trigger> all() {
        return TRIGGERS;
    }

    private static Path file() {
        return FabricLoader.getInstance().getConfigDir().resolve("dofisaddons").resolve("triggers.json");
    }

    public static void load() {
        TRIGGERS.clear();
        Path path = file();
        if (!Files.isRegularFile(path)) {
            // First run: start with the quest preset switched on so there's a working example.
            TRIGGERS.add(TriggerPresets.questComplete());
            save();
            return;
        }
        try {
            String json = Files.readString(path, StandardCharsets.UTF_8);
            List<Trigger> loaded = GSON.fromJson(json, new TypeToken<List<Trigger>>() {}.getType());
            if (loaded != null) {
                for (Trigger t : loaded) {
                    if (t != null) {
                        t.sanitize();
                        TRIGGERS.add(t);
                    }
                }
            }
        } catch (IOException | JsonParseException e) {
            DofisAddons.LOGGER.error("Could not read {}; keeping a backup and starting fresh", path, e);
            try {
                Files.copy(path, path.resolveSibling("triggers.broken.json"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // nothing more we can do
            }
        }
        TriggerEngine.invalidate();
    }

    public static void save() {
        Path path = file();
        try {
            Files.createDirectories(path.getParent());
            Path tmp = path.resolveSibling("triggers.json.tmp");
            Files.writeString(tmp, GSON.toJson(TRIGGERS), StandardCharsets.UTF_8);
            Files.move(tmp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException e) {
            DofisAddons.LOGGER.error("Could not save {}", path, e);
        }
        TriggerEngine.invalidate();
    }

    public static void put(Trigger trigger) {
        for (int i = 0; i < TRIGGERS.size(); i++) {
            if (TRIGGERS.get(i).id.equals(trigger.id)) {
                TRIGGERS.set(i, trigger);
                save();
                return;
            }
        }
        TRIGGERS.add(trigger);
        save();
    }

    public static void remove(Trigger trigger) {
        TRIGGERS.removeIf(t -> t.id.equals(trigger.id));
        save();
    }

    public static void move(int index, int delta) {
        int to = index + delta;
        if (index < 0 || index >= TRIGGERS.size() || to < 0 || to >= TRIGGERS.size()) {
            return;
        }
        TRIGGERS.add(to, TRIGGERS.remove(index));
        save();
    }

    /** A short code to paste in Discord; {@link #importCode} turns it back into a trigger. */
    public static String exportCode(Trigger trigger) {
        String json = new Gson().toJson(trigger);
        return SHARE_PREFIX + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
    }

    /** Reads a code from {@link #exportCode} (surrounding text is ignored). Null if invalid. */
    public static Trigger importCode(String text) {
        if (text == null) {
            return null;
        }
        int start = text.indexOf(SHARE_PREFIX);
        if (start < 0) {
            return null;
        }
        int i = start + SHARE_PREFIX.length();
        int end = i;
        while (end < text.length() && "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=".indexOf(text.charAt(end)) >= 0) {
            end++;
        }
        try {
            String json = new String(Base64.getDecoder().decode(text.substring(i, end)), StandardCharsets.UTF_8);
            Trigger t = GSON.fromJson(json, Trigger.class);
            if (t == null) {
                return null;
            }
            t.sanitize();
            return t.copy(); // fresh id so importing twice doesn't overwrite
        } catch (IllegalArgumentException | JsonParseException e) {
            return null;
        }
    }
}
