package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.trigger.Matcher;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A sidebar styled hud (l asked for the look of a skyblock scoreboard): the server's own numbers, foecollab's tackle shop and
 * moon timers, the weather events and the moon that are active, and a faint picture behind the text (config/dofisaddons/hud.png).
 * Everything is read from chat, the scoreboard and foecollab's TimerHandler, nothing is sent.
 */
public final class ServerHud {
    public static final Path IMAGE = net.fabricmc.loader.api.FabricLoader.getInstance().getConfigDir().resolve("dofisaddons").resolve("hud.png");
    private static final Identifier TEXTURE = Identifier.of("dofisaddons", "hud_image");
    private static final Pattern EVENT = Pattern.compile("^[^A-Za-z0-9]*(RAINBOW|STORM|RAIN|GOLD RUSH|BLOOMING OASIS) » (.*)$");
    private static final Pattern MOON = Pattern.compile("^[^A-Za-z0-9]*(FULL|BLUE|SUPER|BLOOD) MOON » .* has (risen|set)");
    private static final Pattern EFFECT = Pattern.compile("\\(([^)]*)\\)");
    private static final Pattern ENDED = Pattern.compile("dissipated|faded|retracted|has ended");

    /** Active events by name with the time they started and the bonus the server named. */
    private static final Map<String, Object[]> EVENTS = new LinkedHashMap<>();
    private static String moon = "";

    /** Radius of the picture's rounded corners in gui pixels. */
    private static final int CORNER = 6;
    /** Room around the text, the line pitch and the least width, so it fills out like a sidebar and not a tight label. */
    private static final int PAD = 10, LINE = 12, MIN_WIDTH = 150;
    private static long imageStamp = -1;
    private static int imageW, imageH;
    private static long nextCheck;

    private ServerHud() {}

    public static void onChat(Text message) {
        String s = Matcher.normalize(message.getString());
        java.util.regex.Matcher m = MOON.matcher(s);
        if (m.find()) {
            moon = m.group(2).equals("risen") ? title(m.group(1)) + " Moon" : "";
            return;
        }
        m = EVENT.matcher(s);
        if (!m.find()) {
            return;
        }
        String name = title(m.group(1));
        String body = m.group(2);
        if (ENDED.matcher(body).find()) {
            EVENTS.remove(name);
        } else if (body.startsWith("A ") || body.startsWith("An ")) {
            java.util.regex.Matcher e = EFFECT.matcher(body);
            EVENTS.put(name, new Object[]{System.currentTimeMillis(), e.find() ? e.group(1) : ""});
        }
    }

    public static void reset() {
        EVENTS.clear();
        moon = "";
    }

    private static String title(String caps) {
        StringBuilder b = new StringBuilder();
        for (String w : caps.toLowerCase(java.util.Locale.ROOT).split(" ")) {
            b.append(b.isEmpty() ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
        }
        return b.toString();
    }

    public static void renderHud(DrawContext ctx) {
        if (DofiSettings.get().serverHud && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }

    public static int[] draw(DrawContext ctx, boolean preview) {
        DofiSettings s = DofiSettings.get();
        return draw(ctx, s.serverHudX, s.serverHudY);
    }

    public static int[] draw(DrawContext ctx, int hx, int hy) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return null;
        }
        DofiSettings s = DofiSettings.get();
        TextRenderer tr = mc.textRenderer;
        List<Text> lines = lines(mc);
        int w = 0;
        for (Text t : lines) {
            w = Math.max(w, tr.getWidth(t));
        }
        w = Math.max(w, MIN_WIDTH);
        float k = s.serverHudScale / 100f;
        int h = lines.size() * LINE;
        int[] rect = {hx, hy, Math.round((w + 2 * PAD) * k), Math.round((h + 2 * PAD) * k)};
        image(ctx, mc, rect, s.serverHudImage);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(hx, hy);
        m.scale(k, k);
        for (int i = 0; i < lines.size(); i++) {
            Text t = lines.get(i);
            int x = i == 0 && titleShown ? PAD + (w - tr.getWidth(t)) / 2 : PAD; // the title is centred
            ctx.drawTextWithShadow(tr, t, x, PAD + i * LINE, 0xFFFFFFFF);
        }
        m.popMatrix();
        return rect;
    }

    /** Every line the hud can show, by the key {@link DofiSettings#serverHudHide} switches it off with. */
    public static final String[][] LINES = {{"title", "title"}, {"clock", "time"}, {"weather", "weather"}, {"location", "location"}, {"players", "players"},
            {"level", "level"}, {"wallet", "wallet"}, {"credits", "credits"}, {"bite speed", "bite speed"}, {"reel speed", "reel speed"},
            {"luck", "luck"}, {"scale", "scale"}, {"prospect", "prospect value"}, {"prospect chance", "prospect chance"},
            {"shop", "tackle shop timer"}, {"moon", "moon"}, {"events", "active events"}};

    /** The weather the boss bar names, in the colour it is, and what it does (the numbers the server announces). */
    private static final Map<String, int[]> WEATHER = Map.of("rain", new int[]{0x3F9BFF}, "thunderstorm", new int[]{0xFFD83D},
            "blooming oasis", new int[]{0x4CD964}, "gold rush", new int[]{0xFFAA00}, "fabled", new int[]{0xE0413A}, "clear", new int[]{0xFFE066});
    private static final Map<String, String> EFFECT_TEXT = Map.of("rain", "+50 Bite Speed", "thunderstorm", "+100 Bite Speed",
            "blooming oasis", "+100% Pet Luck", "gold rush", "Better shard chance");

    private static boolean titleShown;

    private static boolean on(String key) {
        return !DofiSettings.get().serverHudHide.contains(key);
    }

    private static List<Text> lines(MinecraftClient mc) {
        List<Text> out = new ArrayList<>();
        titleShown = on("title");
        if (titleShown) {
            out.add(Text.literal("FISHONMC").formatted(Formatting.GOLD, Formatting.BOLD));
        }
        String clock = (ServerInfo.time + " " + ServerInfo.ampm + " " + ServerInfo.temperature).trim();
        if (on("clock") && !clock.isEmpty()) {
            out.add(Text.literal(clock).formatted(Formatting.GRAY));
        }
        if (on("location") && !ServerInfo.locationName.isEmpty()) {
            out.add(Text.literal("⏣ ").formatted(Formatting.GRAY).append(Text.literal(title(ServerInfo.locationName)).formatted(Formatting.GREEN)));
        }
        var tab = mc.getNetworkHandler();
        if (on("players") && tab != null) {
            out.add(row("Players: ", String.valueOf(tab.getPlayerList().size()), Formatting.GREEN));
        }
        out.add(Text.empty());
        if (on("level")) {
            out.add(row("Level: ", String.valueOf(mc.player.experienceLevel), Formatting.GREEN));
        }
        if (on("wallet") && !ServerInfo.wallet.isEmpty()) {
            out.add(row("Wallet: ", "$" + ServerInfo.wallet, Formatting.GOLD));
        }
        if (on("credits") && !ServerInfo.credits.isEmpty()) {
            out.add(row("Credits: ", ServerInfo.credits, Formatting.AQUA));
        }
        out.add(Text.empty());
        var stats = DofiSettings.get().profileStats;
        for (String stat : new String[]{"Bite Speed", "Reel Speed", "Luck", "Scale"}) {
            String v = stats.get(stat.toLowerCase(java.util.Locale.ROOT));
            if (on(stat.toLowerCase(java.util.Locale.ROOT)) && v != null) {
                out.add(row(stat + ": ", v, Formatting.GREEN));
            }
        }
        String prospect = stats.get("prospect");
        if (prospect != null) { // "236 (1/42.06)": the number, the chance, or both
            int at = prospect.indexOf(" (");
            String value = at < 0 ? prospect : prospect.substring(0, at);
            String chance = at < 0 ? "" : prospect.substring(at + 2, prospect.length() - 1);
            boolean v = on("prospect"), c = on("prospect chance") && !chance.isEmpty();
            if (v || c) {
                out.add(row("Prospect: ", (v ? value : "") + (v && c ? " (" + chance + ")" : c ? chance : ""), Formatting.GREEN));
            }
        }
        out.add(Text.empty());
        long shop = timer("baitShopTimer");
        if (on("shop") && shop >= 0) {
            out.add(row("Tackle Shop: ", clock(shop), Formatting.YELLOW));
        }
        if (on("moon")) {
            if (!moon.isEmpty()) {
                out.add(row("Moon: ", moon, moon.startsWith("Blood") ? Formatting.RED : Formatting.LIGHT_PURPLE));
            } else if (timer("moonTimer") >= 0) {
                out.add(row("Moon Cycle: ", clock(timer("moonTimer")), Formatting.YELLOW));
            }
        }
        String weather = ServerInfo.weather;
        if (on("weather") && !weather.isEmpty() && !weather.equals("moon")) { // the moon has its own line
            int[] look = WEATHER.getOrDefault(weather, new int[]{0xFFFFFF});
            MutableText line = Text.literal(title(weather)).styled(st -> st.withColor(look[0]));
            String effect = EFFECT_TEXT.getOrDefault(weather, "");
            out.add(effect.isEmpty() ? line : line.append(Text.literal("  " + effect).formatted(Formatting.GRAY)));
        }
        if (on("events") && !EVENTS.isEmpty()) {
            out.add(Text.empty());
            for (Map.Entry<String, Object[]> e : EVENTS.entrySet()) {
                long up = System.currentTimeMillis() - (long) e.getValue()[0];
                MutableText t = Text.literal(e.getKey()).formatted(Formatting.LIGHT_PURPLE).append(Text.literal(" " + minutes(up)).formatted(Formatting.GRAY));
                String bonus = (String) e.getValue()[1];
                out.add(bonus.isEmpty() ? t : t.append(Text.literal("  " + bonus).formatted(Formatting.DARK_GRAY)));
            }
        }
        // switching lines off can leave gaps: no double blanks, none at the ends
        for (int k = out.size() - 1; k >= 0; k--) {
            if (out.get(k).getString().isEmpty() && (k == 0 || k == out.size() - 1 || out.get(k - 1).getString().isEmpty())) {
                out.remove(k);
            }
        }
        return out;
    }

    private static MutableText row(String label, String value, Formatting color) {
        return Text.literal(label).formatted(Formatting.WHITE).append(Text.literal(value).formatted(color));
    }

    /** Above an hour only hours and minutes (it changes once a minute), below it minutes and seconds. */
    private static String clock(long ms) {
        long s = ms / 1000;
        return s >= 3600 ? (s / 3600) + "h " + String.format("%02d", s % 3600 / 60) + "m" : minutes(ms);
    }

    private static String minutes(long ms) {
        long s = ms / 1000;
        return String.format("%d:%02d", s / 60, s % 60);
    }

    private static Method instance;

    /** Foecollab's TimerHandler keeps both countdowns, in milliseconds (-1 when it is not there). */
    private static long timer(String field) {
        try {
            if (instance == null) {
                instance = Class.forName("io.github.foecollab.handler.TimerHandler").getMethod("instance");
            }
            Object handler = instance.invoke(null);
            return handler.getClass().getField(field).getLong(handler);
        } catch (ReflectiveOperationException | LinkageError e) {
            return -1;
        }
    }

    /** The picture, faint, cropped to cover the hud; it reloads when the file changes. */
    private static void image(DrawContext ctx, MinecraftClient mc, int[] r, int opacity) {
        if (opacity <= 0 || System.currentTimeMillis() > nextCheck && !load(mc) || imageStamp < 0) {
            return;
        }
        double k = Math.max((double) r[2] / imageW, (double) r[3] / imageH);
        int dw = (int) Math.round(imageW * k), dh = (int) Math.round(imageH * k);
        int x = r[0] + (r[2] - dw) / 2, y = r[1] + (r[3] - dh) / 2, color = (opacity * 255 / 100) << 24 | 0xFFFFFF;
        int radius = Math.min(CORNER, Math.min(r[2], r[3]) / 2);
        ctx.enableScissor(r[0], r[1] + radius, r[0] + r[2], r[1] + r[3] - radius); // the middle
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, dw, dh, imageW, imageH, imageW, imageH, color);
        ctx.disableScissor();
        for (int i = 0; i < radius; i++) { // the corners, a row at a time
            double up = radius - i - 0.5;
            int inset = radius - (int) Math.round(Math.sqrt(radius * radius - up * up));
            for (int row : new int[]{r[1] + i, r[1] + r[3] - 1 - i}) {
                ctx.enableScissor(r[0] + inset, row, r[0] + r[2] - inset, row + 1);
                ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x, y, 0f, 0f, dw, dh, imageW, imageH, imageW, imageH, color);
                ctx.disableScissor();
            }
        }
    }

    /** Returns false when there is no usable file. Checked once a second. */
    private static boolean load(MinecraftClient mc) {
        nextCheck = System.currentTimeMillis() + 1000;
        try {
            long stamp = Files.exists(IMAGE) ? Files.getLastModifiedTime(IMAGE).toMillis() : -1;
            if (stamp == imageStamp) {
                return stamp >= 0;
            }
            imageStamp = stamp;
            if (stamp < 0) {
                mc.getTextureManager().destroyTexture(TEXTURE);
                return false;
            }
            try (var in = Files.newInputStream(IMAGE)) {
                NativeImage img = NativeImage.read(in);
                imageW = img.getWidth();
                imageH = img.getHeight();
                mc.getTextureManager().registerTexture(TEXTURE, new NativeImageBackedTexture(() -> "dofisaddons hud image", img));
            }
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            imageStamp = -1; // a bad file: no picture
            return false;
        }
    }
}
