package dev.landofif.dofisaddons.hud;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;
import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.stats.StatsTracker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Collections;
import java.util.List;
import java.util.zip.Deflater;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

/**
 * Saves custom HUDs to {@code config/dofisaddons/huds.json} and handles share codes. Import also
 * understands codes copied from FOECollab and FishOnMC-Extras-R.
 */
public final class HudStore {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping().create();
    private static final String PREFIX = "dofi-hud:";
    private static final List<CustomHud> HUDS = new ArrayList<>();

    private HudStore() {}

    public static List<CustomHud> all() {
        return HUDS;
    }

    private static Path file() {
        return StatsTracker.dir().resolve("huds.json");
    }

    public static void load() {
        HUDS.clear();
        Path path = file();
        if (!Files.isRegularFile(path)) {
            HUDS.add(HudPresets.session());
            HUDS.add(HudPresets.bobber());
            save();
            return;
        }
        List<CustomHud> loaded = null;
        try {
            loaded = GSON.fromJson(Files.readString(path, StandardCharsets.UTF_8), new TypeToken<List<CustomHud>>() {}.getType());
        } catch (Exception e) {
            DofisAddons.LOGGER.error("Could not read {}; starting with no HUDs", path, e);
        }
        if (loaded != null) {
            for (CustomHud h : loaded) {
                if (h != null) {
                    h.sanitize();
                    HUDS.add(h);
                }
            }
        }
    }

    public static void save() {
        StatsTracker.write(file(), HUDS);
        HudRenderer.invalidate();
    }

    public static void move(int index, int delta) {
        int to = index + delta;
        if (index >= 0 && to >= 0 && index < HUDS.size() && to < HUDS.size()) {
            Collections.swap(HUDS, index, to);
            save();
        }
    }

    // ------------------------------------------------------------------ share codes

    public static String exportCode(CustomHud hud) {
        return PREFIX + Base64.getEncoder().encodeToString(deflate(GSON.toJson(hud)));
    }

    /** Accepts a Dofi code, or a FOECollab / FoE-R export (with or without the Discord ``` wrapper). */
    public static CustomHud importCode(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        try {
            if (text.startsWith(PREFIX)) {
                CustomHud hud = GSON.fromJson(inflate(Base64.getDecoder().decode(text.substring(PREFIX.length()).trim())), CustomHud.class);
                if (hud != null) hud.sanitize();
                return hud;
            }
            String b64 = longestBase64(text);
            if (b64.isEmpty()) {
                return null;
            }
            JsonObject root = JsonParser.parseString(inflate(Base64.getDecoder().decode(b64))).getAsJsonObject();
            CustomHud hud = root.has("value2") && !root.has("hud") ? fromFoeR(root) : fromFoeCollab(root);
            if (hud != null) hud.sanitize();
            return hud;
        } catch (Exception e) {
            DofisAddons.LOGGER.warn("Could not import HUD code: {}", e.getMessage());
            return null;
        }
    }

    private static CustomHud fromFoeCollab(JsonObject root) {
        JsonObject src = root.getAsJsonObject("hud");
        if (src == null) {
            return null;
        }
        CustomHud hud = new CustomHud();
        if (src.has("name")) hud.name = src.get("name").getAsString();
        hud.lines = new ArrayList<>();
        if (src.has("lines")) {
            for (JsonElement e : src.getAsJsonArray("lines")) hud.lines.add(e.getAsString());
        }
        if (src.has("hudX")) hud.x = src.get("hudX").getAsDouble();
        if (src.has("hudY")) hud.y = src.get("hudY").getAsDouble();
        if (src.has("fontSize")) hud.scale = src.get("fontSize").getAsDouble() / 10.0;
        if (src.has("backgroundOpacity")) hud.background = src.get("backgroundOpacity").getAsInt();
        if (src.has("enabled")) hud.enabled = src.get("enabled").getAsBoolean();
        if (src.has("alignment")) hud.align = alignOf(src.get("alignment").getAsString());
        return hud;
    }

    private static CustomHud fromFoeR(JsonObject root) {
        JsonObject src = root.getAsJsonObject("value2");
        if (src == null) {
            return null;
        }
        CustomHud hud = new CustomHud();
        if (root.has("value1") && !root.get("value1").isJsonNull()) hud.name = root.get("value1").getAsString();
        hud.lines = new ArrayList<>();
        if (src.has("stringLines")) {
            for (JsonElement e : src.getAsJsonArray("stringLines")) {
                if (e.isJsonObject() && e.getAsJsonObject().has("value1")) hud.lines.add(e.getAsJsonObject().get("value1").getAsString());
            }
        }
        if (src.has("xPos")) hud.x = src.get("xPos").getAsDouble();
        if (src.has("yPos")) hud.y = src.get("yPos").getAsDouble();
        if (src.has("scale")) hud.scale = src.get("scale").getAsDouble();
        if (src.has("showBackground")) hud.background = src.get("showBackground").getAsBoolean() ? 45 : 0;
        if (src.has("showElement")) hud.enabled = src.get("showElement").getAsBoolean();
        if (src.has("alignment")) hud.align = alignOf(src.get("alignment").getAsString());
        return hud;
    }

    private static CustomHud.Align alignOf(String s) {
        if (s == null) return CustomHud.Align.LEFT;
        if (s.contains("RIGHT")) return CustomHud.Align.RIGHT;
        if (s.equals("CENTER") || s.equals("TOP") || s.equals("BOTTOM")) return CustomHud.Align.CENTER;
        return CustomHud.Align.LEFT;
    }

    private static String longestBase64(String text) {
        String best = "";
        for (String line : text.replace("```", "\n").split("\\r?\\n")) {
            String t = line.trim();
            if (t.matches("[A-Za-z0-9+/=]+") && t.length() > best.length()) best = t;
        }
        return best;
    }

    private static byte[] deflate(String value) {
        Deflater d = new Deflater();
        d.setInput(value.getBytes(StandardCharsets.UTF_8));
        d.finish();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!d.finished()) out.write(buf, 0, d.deflate(buf));
        d.end();
        return out.toByteArray();
    }

    private static String inflate(byte[] data) throws Exception {
        if (data.length >= 2 && (data[0] & 0xFF) == 0x1F && (data[1] & 0xFF) == 0x8B) {
            try (GZIPInputStream in = new GZIPInputStream(new ByteArrayInputStream(data))) {
                return new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        }
        Inflater inf = new Inflater();
        inf.setInput(data);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[1024];
        while (!inf.finished()) {
            int n = inf.inflate(buf);
            if (n == 0 && inf.needsInput()) break;
            out.write(buf, 0, n);
        }
        inf.end();
        return out.toString(StandardCharsets.UTF_8);
    }
}
