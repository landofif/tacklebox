package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.menu.Locations;
import dev.landofif.dofisaddons.menu.Power;
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
    private static final Pattern EVENT = Pattern.compile("^[^A-Za-z0-9]*(RAINBOW|STORM|RAIN|GOLD RUSH|BLOOMING OASIS|FABLED) [>»] (.*)$");
    private static final Pattern MOON = Pattern.compile("^[^A-Za-z0-9]*(FULL|BLUE|SUPER|BLOOD) MOON [>»] .* has (risen|set)");
    private static final Pattern EFFECT = Pattern.compile("\\(([^)]*)\\)");
    private static final Pattern NOUN = Pattern.compile("^An? (.+?) has ");
    private static final Pattern AT = Pattern.compile(" at ([^.!(]+)");
    private static final Pattern SHARDS = Pattern.compile("Base Shard Chance.*?(\\d+)\\s*$");
    private static final Pattern ENDED = Pattern.compile("dissipated|faded|retracted|has ended|caught by");

    /** Active events by name with the time they started and the bonus the server named. */
    private static final Map<String, Object[]> EVENTS = new LinkedHashMap<>();
    private static String moon = "";
    private static String moonEffect = "";

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
            java.util.regex.Matcher bonus = EFFECT.matcher(s);
            moonEffect = !moon.isEmpty() && bonus.find() ? bonus.group(1) : "";
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
        } else if (body.startsWith("A ") || body.startsWith("An ") || name.equals("Fabled")) {
            java.util.regex.Matcher e = EFFECT.matcher(body);
            java.util.regex.Matcher noun = NOUN.matcher(body);
            java.util.regex.Matcher place = AT.matcher(body); // the fabled event says where
            EVENTS.put(name, new Object[]{System.currentTimeMillis(), e.find() ? shorten(e.group(1)) : place.find() ? "at " + place.group(1) : "",
                    name.equals("Fabled") ? "Fabled Event" : noun.find() ? title(noun.group(1)) : name});
        }
    }

    public static void reset() {
        EVENTS.clear();
        moon = "";
        moonEffect = "";
    }

    /** "Base Shard Chance → 1 in 50 25" (the gold rush says the old and the new chance) as "Shards → 1 in 25". */
    private static String shorten(String effect) {
        java.util.regex.Matcher m = SHARDS.matcher(effect);
        return m.find() ? "Shards → 1 in " + m.group(1) : effect;
    }

    /** For {@link BoostsHud}: the active events as {name, bonus}. */
    static List<String[]> activeEvents() {
        List<String[]> out = new ArrayList<>();
        for (Object[] e : EVENTS.values()) {
            out.add(new String[]{(String) e[2], (String) e[1]});
        }
        return out;
    }

    /** For {@link BoostsHud}: the moon as {name, bonus}, or null when none is up. */
    static String[] activeMoon() {
        return moon.isEmpty() ? null : new String[]{moon, moonEffect};
    }

    static String title(String caps) {
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
        List<Line> lines = lines(mc);
        int w = MIN_WIDTH;
        for (Line l : lines) {
            w = Math.max(w, tr.getWidth(l.text));
        }
        float k = s.serverHudScale / 100f;
        int h = lines.size() * LINE;
        int[] rect = {hx, hy, Math.round((w + 2 * PAD) * k), Math.round((h + 2 * PAD) * k)};
        image(ctx, mc, rect, s.serverHudImage);
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(hx, hy);
        m.scale(k, k);
        for (int i = 0; i < lines.size(); i++) {
            Line l = lines.get(i);
            int x = l.center ? PAD + (w - tr.getWidth(l.text)) / 2 : PAD;
            ctx.drawTextWithShadow(tr, l.text, x, PAD + i * LINE, 0xFFFFFFFF);
        }
        m.popMatrix();
        return rect;
    }

    /** One line of the hud; the title is centred. */
    private record Line(Text text, boolean center) {
        boolean blank() {
            return text.getString().isEmpty();
        }
    }

    private static final String DASH = "- ";

    /** Every line the hud can show as {key, name}, in the order they start in; a chip switches one off by its key ({@link DofiSettings#serverHudHide}). */
    private static final Map<String, String> LABEL = new LinkedHashMap<>();

    static {
        for (String[] l : new String[][]{{"title", "title"}, {"clock", "time"}, {"location", "location"}, {"players", "players"}, {"space 1", "blank line"},
                {"level", "level"}, {"wallet", "wallet"}, {"credits", "credits"}, {"space 2", "blank line"},
                {"bite speed", "bite speed"}, {"reel speed", "reel speed"}, {"luck", "luck"}, {"scale", "scale"},
                {"prospect", "prospect value"}, {"prospect chance", "prospect chance"}, {"space 3", "blank line"},
                {"dex location", "compendium location"}, {"dex level", "location level"}, {"dex catches", "catches"},
                {"dex runes", "runes"}, {"dex normal", "normal fish"}, {"dex gigantic", "gigantic fish"}, {"dex albino", "albino fish"},
                {"dex melanistic", "melanistic fish"}, {"dex trophy", "trophy fish"}, {"dex fabled", "fabled fish"}, {"space 4", "blank line"},
                {"shop", "tackle shop timer"}, {"moon", "moon"}, {"weather", "weather and events"}}) {
            LABEL.put(l[0], l[1]);
        }
    }

    public static String label(String key) {
        return LABEL.get(key);
    }

    /** The lines in the order the player arranged them: the saved order, then any line it does not know yet in its start order. */
    public static List<String> order() {
        List<String> out = new ArrayList<>();
        for (String key : DofiSettings.get().serverHudOrder) {
            if (LABEL.containsKey(key) && !out.contains(key)) {
                out.add(key);
            }
        }
        for (String key : LABEL.keySet()) {
            if (!out.contains(key)) {
                out.add(key);
            }
        }
        return out;
    }

    /** The colour of each event, by the tag the server puts in front of its chat line. */
    private static final Map<String, Integer> COLORS = Map.of("Rain", 0x3F9BFF, "Storm", 0xFFD83D, "Rainbow", 0xFF7BE0, "Gold Rush", 0xFFAA00, "Blooming Oasis", 0x4CD964, "Fabled", 0xE0413A);
    private static final Map<String, String> EFFECT_TEXT = Map.of("rain", "+50 Bite Speed", "thunderstorm", "+100 Bite Speed",
            "blooming oasis", "+100% Pet Luck", "gold rush", "Better shard chance");

    private static boolean on(String key) {
        return !DofiSettings.get().serverHudHide.contains(key);
    }

    private static Line plain(Text t) {
        return new Line(t, false);
    }

    /** The key of the line behind each row of the last hud that was built, for the preview's dragging. */
    private static List<String> rowKeys = List.of();

    public static List<String> rowKeys() {
        return rowKeys;
    }

    /** Where row {@code i} of the hud drawn into {@code rect} is on the screen, as {x, y, w, h}. */
    public static int[] rowBounds(int[] rect, int i) {
        float k = DofiSettings.get().serverHudScale / 100f;
        return new int[]{rect[0], rect[1] + Math.round((PAD + i * LINE) * k), rect[2], Math.round(LINE * k)};
    }

    /** The keys in their start order, for the chips. */
    public static List<String> keys() {
        return new ArrayList<>(LABEL.keySet());
    }

    /**
     * Moves the line of row {@code from} to just before row {@code gap} (the row count means after the last one), in the saved
     * order. A line of several rows (the weather) moves as one, and so do the two prospect chips.
     */
    public static void move(int from, int gap) {
        String key = rowKeys.get(from), before = gap < rowKeys.size() ? rowKeys.get(gap) : null;
        List<String> group = key.startsWith("prospect") ? List.of("prospect", "prospect chance") : List.of(key);
        if (before != null && group.contains(before)) {
            return;
        }
        List<String> order = order();
        List<String> moved = new ArrayList<>(order);
        moved.retainAll(group);
        order.removeAll(group);
        order.addAll(before == null ? order.size() : order.indexOf(before), moved);
        DofiSettings.get().serverHudOrder = order;
    }

    private static List<Line> lines(MinecraftClient mc) {
        List<Line> out = new ArrayList<>();
        List<String> keys = new ArrayList<>();
        boolean prospect = false;
        for (String key : order()) {
            if (key.startsWith("prospect")) { // two chips, one line, at the place of the first of them
                if (!prospect) {
                    prospect = true;
                    prospect(out);
                }
            } else if (on(key)) {
                add(out, key, mc);
            }
            while (keys.size() < out.size()) {
                keys.add(key);
            }
        }
        // switching lines off can leave gaps: no double blanks, none at the ends
        for (int k = out.size() - 1; k >= 0; k--) {
            if (out.get(k).blank() && (k == 0 || k == out.size() - 1 || out.get(k - 1).blank())) {
                out.remove(k);
                keys.remove(k);
            }
        }
        rowKeys = keys;
        return out;
    }

    private static void add(List<Line> out, String key, MinecraftClient mc) {
        var stats = DofiSettings.get().profileStats;
        switch (key) {
            case "title" -> out.add(new Line(Text.literal("FISHONMC").formatted(Formatting.GOLD, Formatting.BOLD), true));
            case "clock" -> {
                String clock = (ServerInfo.time + " " + ServerInfo.ampm + " " + ServerInfo.temperature).trim();
                if (!clock.isEmpty()) {
                    out.add(plain(Text.literal(clock).formatted(Formatting.GRAY)));
                }
            }
            case "location" -> {
                if (!ServerInfo.locationName.isEmpty()) {
                    out.add(plain(Text.literal("⏣ ").formatted(Formatting.GRAY).append(Text.literal(title(ServerInfo.locationName)).formatted(Formatting.GREEN))));
                }
            }
            case "players" -> {
                var tab = mc.getNetworkHandler();
                if (tab != null) {
                    out.add(plain(row("Players: ", String.valueOf(tab.getPlayerList().size()), Formatting.GREEN)));
                }
            }
            case "level" -> out.add(plain(row("Level: ", String.valueOf(mc.player.experienceLevel), Formatting.GREEN).append(percent(mc.player.experienceProgress))));
            case "wallet" -> {
                if (!ServerInfo.wallet.isEmpty()) {
                    out.add(plain(row("Wallet: ", "$" + ServerInfo.wallet, Formatting.GOLD)));
                }
            }
            case "credits" -> {
                if (!ServerInfo.credits.isEmpty()) {
                    out.add(plain(row("Credits: ", ServerInfo.credits, Formatting.AQUA)));
                }
            }
            case "bite speed", "reel speed", "luck", "scale" -> {
                String v = stats.get(key);
                if (v != null) {
                    out.add(plain(row(title(key) + ": ", v, Formatting.GREEN)));
                }
            }
            case "shop" -> {
                long shop = timer("baitShopTimer");
                if (shop >= 0) {
                    out.add(plain(row("Tackle Shop: ", clock(shop), Formatting.YELLOW)));
                }
            }
            case "moon" -> {
                if (!moon.isEmpty()) {
                    out.add(plain(row("Moon: ", moon, moon.startsWith("Blood") ? Formatting.RED : Formatting.LIGHT_PURPLE)));
                } else if (timer("moonTimer") >= 0) {
                    out.add(plain(row("Moon Cycle: ", clock(timer("moonTimer")), Formatting.YELLOW)));
                }
            }
            case "weather" -> { // every active event gets its own line, in its colour, with what it does
                for (Map.Entry<String, Object[]> e : EVENTS.entrySet()) {
                    out.add(plain(weatherLine((String) e.getValue()[2], (String) e.getValue()[1], COLORS.getOrDefault(e.getKey(), 0xFFFFFF))));
                }
            }
            default -> {
                if (key.startsWith("space")) {
                    out.add(plain(Text.empty()));
                } else {
                    compendium(out, key);
                }
            }
        }
    }

    private static void prospect(List<Line> out) {
        String prospect = DofiSettings.get().profileStats.get("prospect");
        if (prospect != null) { // "236 (1/42.06)": the number, the chance, or both
            int at = prospect.indexOf(" (");
            String value = at < 0 ? prospect : prospect.substring(0, at);
            String chance = at < 0 ? "" : prospect.substring(at + 2, prospect.length() - 1);
            boolean v = on("prospect"), c = on("prospect chance") && !chance.isEmpty();
            if (v || c) {
                String text = (v ? value : "") + (v && c ? " (" + chance + ")" : c ? chance : "");
                if (DofiSettings.get().serverHudRoundProspect) { // 1/34.96 as 1/35
                    text = Pattern.compile("/(\\d[\\d,]*(?:\\.\\d+)?)").matcher(text)
                            .replaceAll(r -> "/" + String.format("%,d", Math.round(Double.parseDouble(r.group(1).replace(",", "")))));
                }
                out.add(plain(row("Prospect: ", text, Formatting.GREEN)));
            }
        }
    }

    /**
     * One compendium line of the location you are at, as of the last time its compendium page was open (the level also moves with
     * the location xp in chat). The location name is the heading, the rest are list items with a "-" in front, and the level
     * has its progress in grey after it.
     */
    private static void compendium(List<Line> out, String key) {
        Locations.Location loc = Locations.byName(ServerInfo.locationName);
        if (loc == null) {
            return;
        }
        DofiSettings s = DofiSettings.get();
        double[] at = XpEstimate.toNext(loc.id());
        long[] d = s.dex.get(loc.id());
        int[] v = s.variants.get(loc.id());
        switch (key) {
            case "dex location" -> out.add(plain(Text.literal(title(loc.name())).formatted(Formatting.GREEN)));
            case "dex level" -> {
                if (at != null) {
                    int level = (int) at[0];
                    double next = XpEstimate.need(level + 1);
                    MutableText t = Text.literal(DASH).formatted(Formatting.GRAY).append(row("Location Level: ", level >= Power.MAX_LEVEL ? level + " (max)" : String.valueOf(level), Formatting.GREEN));
                    out.add(plain(level >= Power.MAX_LEVEL ? t : t.append(percent((next - at[1]) / next))));
                }
            }
            case "dex catches" -> {
                if (d != null) {
                    out.add(item("Catches: ", s.serverHudShortCatches ? abbrev(d[0]) : String.format("%,d", d[0]), Formatting.GREEN));
                }
            }
            case "dex runes" -> {
                if (d != null && d[1] >= 0) {
                    out.add(item("Runes: ", d[1] + "/" + d[2], d[1] >= d[2] ? Formatting.GREEN : Formatting.YELLOW));
                }
            }
            default -> { // "dex normal" ... "dex fabled"
                String name = key.substring(4);
                int i = java.util.Arrays.asList(Power.VARIANTS).indexOf(name);
                if (v != null && i >= 0 && v[i * 2] < v[i * 2 + 1]) { // a finished list hides itself here, and shows at the locations that are not
                    out.add(item(title(name) + ": ", v[i * 2] + "/" + v[i * 2 + 1], v[i * 2] == 0 ? Formatting.RED : Formatting.YELLOW));
                }
            }
        }
    }

    /** 4392 as "4.3K", 1234567 as "1.2M": one decimal, cut and not rounded, and no ".0". */
    private static String abbrev(long n) {
        if (n < 1000) {
            return String.valueOf(n);
        }
        boolean m = n >= 1_000_000;
        long tenths = n / (m ? 100_000 : 100);
        return (tenths % 10 == 0 ? String.valueOf(tenths / 10) : tenths / 10 + "." + tenths % 10) + (m ? "M" : "K");
    }

    /** " (89%)" in grey, cut and not rounded so it never says 100 before the level is up. */
    private static MutableText percent(double p) {
        return Text.literal(" (" + (int) (Math.max(0, Math.min(1, p)) * 100) + "%)").formatted(Formatting.GRAY);
    }

    /** A list item: a grey "- " then the row. */
    private static Line item(String label, String value, Formatting color) {
        return plain(Text.literal(DASH).formatted(Formatting.GRAY).append(row(label, value, color)));
    }

    private static MutableText weatherLine(String name, String effect, int color) {
        MutableText line = Text.literal(name.toUpperCase(java.util.Locale.ROOT)).styled(st -> st.withColor(color)); // like the chat tag: GOLD RUSH » Shards → 1 in 25
        return effect.isEmpty() ? line : line.append(Text.literal(" » " + effect).formatted(Formatting.GRAY));
    }

    private static MutableText row(String label, String value, Formatting color) {
        return Text.literal(label).formatted(Formatting.WHITE).append(Text.literal(value).formatted(color));
    }

    /** Above an hour only hours and minutes (it changes once a minute), below it minutes and seconds. */
    static String clock(long ms) {
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
                mc.getTextureManager().registerTexture(TEXTURE, dev.landofif.dofisaddons.display.MenuBackground.smooth(new NativeImageBackedTexture(() -> "dofisaddons hud image", img)));
            }
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            imageStamp = -1; // a bad file: no picture
            return false;
        }
    }
}
