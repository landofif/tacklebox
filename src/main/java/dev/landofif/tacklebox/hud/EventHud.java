package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The running event: points, thresholds with their rewards, placement and the missions you have progressed.
 * <p>
 * /event (luna's Halloween 2026 dump) gives the thresholds ({@code [<have> pts|<need> pts]} on the {@code Threshold n} heads, then
 * the reward lines), the points (the book and the leaderboard head) and the placement. Chat gives the live part (luna's log
 * 2026-10-24): every mission progress message ({@code <EVENT> EVENT MISSION [+50 pts] / 🎣 Murray Cod / ┗ (5/10) [250 pts|50 pts] Partial},
 * {@code ∞} for a repeatable mission, nothing for a one-time one, {@code ✔} once done) adds its points, and
 * {@code THRESHOLD tier up! [250 pts|220 pts]} sets them. Text with a shadow and no box, like the quests left hud. The state is
 * saved in the settings, so it survives a restart.
 */
public final class EventHud {
    private static final String NUM = "([0-9][0-9.,]*)\\s*([kmb]?)";
    private static final int CI = Pattern.CASE_INSENSITIVE;
    private static final Pattern THRESHOLD = Pattern.compile("^threshold (\\d+)(?: [^\\p{L}\\p{N}\\s]+)?$", CI);
    // [<none> pts|220 pts]   (<none> is the server's word for zero)
    private static final Pattern RANGE = Pattern.compile("^\\[\\s*(?:<?none>?|" + NUM + ")\\s*pts\\s*\\|\\s*" + NUM + "\\s*pts\\s*\\]", CI);
    // "Event Missions → <none> pts" on the book, "Event Points: <none> pts" on the leaderboard head
    private static final Pattern POINTS = Pattern.compile("^event (?:points:|missions \\W+)\\s*(?:<?none>?|" + NUM + ")\\s*pts", CI);
    private static final Pattern PLACEMENT = Pattern.compile("^your placement:\\s*(.+)$", CI);
    private static final Pattern WELCOME = Pattern.compile("^welcome to (.+?)!?$", CI);
    // HALLOWEEN 2026 EVENT MISSION [+50 pts] 🎣 Murray Cod ✔ ┗ (5/10) [250 pts|50 pts] Partial   or   ... ┗ (pts = Weight) 50.86 pts
    // (whitespace folded by Matcher.normalize; a player quoting the line has a "name >" in front and does not match)
    private static final Pattern MISSION = Pattern.compile("^([^>\\[\\]]+?) EVENT MISSION \\[\\+" + NUM + " pts\\] (?:[^\\p{L}\\p{N}\\s]+ )?(.+?)( ✔)? ┗ "
            + "(?:\\((\\d+)/(\\d+)\\) \\[" + NUM + " pts\\|" + NUM + " pts\\] ?(.*)|\\(pts = (.+?)\\) " + NUM + " pts)", CI);
    private static final Pattern TIER_UP = Pattern.compile("^([^>\\[\\]]+?) EVENT THRESHOLD tier up! \\[" + NUM + " pts\\|" + NUM + " pts\\]", CI);
    /** The mission message follows the tier up line of the catch that caused it, with that mission's points already counted in. */
    private static final long SAME_CATCH_MS = 1500;

    /** The River Monsters variants of the book: the tag is a pack glyph (U+F041..43, in the order of the menu), plain text drops it. */
    private static final Map<Character, String> TAGS = Map.of('\uF041', "albino", '\uF042', "melanistic", '\uF043', "trophy");
    private static final int GREEN = 0xFF55FF55;
    private static final Pattern MISSION_NAME = Pattern.compile("^- (?:[^\\p{L}\\p{N}\\s]+ )?(.+?)( ✔)?$");
    // ┗ (1/10) [24 pts|2 pts] ∞     or     ┗ (pts = Weight) 50.86 pts
    private static final Pattern MISSION_LINE = Pattern.compile("^┗ (?:\\((\\d+)/(\\d+)\\) \\[" + NUM + " pts\\|" + NUM + " pts\\] ?(.*)|\\(pts = (.+?)\\) " + NUM + " pts)", CI);

    /**
     * An item of the open menu as plain lowercase text ({@link Glyphs#plain}): name and lore lines. {@code colors} holds the menu's colour
     * (rgb) of each lore line's first word, -1 for none; it may be empty.
     */
    public record Item(String name, List<String> lines, List<Integer> colors) {
        public Item(String name, List<String> lines) {
            this(name, lines, List.of());
        }

        int color(int line) {
            return colorAt(colors, line);
        }
    }

    /** A reward step: the points the head says you have, the points it needs in total, and its reward lines (money, items, bait package) with their colours. */
    public record Threshold(int n, double have, double need, List<String> rewards, List<Integer> colors) {
        public Threshold(int n, double have, double need, List<String> rewards) {
            this(n, have, need, rewards, List.of());
        }
    }

    /**
     * A mission from chat. {@code kind}: "partial" (points per catch up to {@code max}, done at max), "repeat" (the infinity mark; {@code earned}
     * is all it gave so far), "once" (one catch, {@code max} 1) or "weight" (points are the best weight). {@code color} is the name's colour in
     * the menu (rgb), -1 when unknown.
     */
    public record Mission(String name, String kind, int cur, int max, double earned, double per, boolean done, int color) {}

    /** What is saved in the settings. */
    public static final class State {
        public String title = "";
        public double points = -1;
        public String placement = "";
        public List<Threshold> thresholds = new ArrayList<>();
        public Map<String, Mission> missions = new LinkedHashMap<>();
    }

    private static State state = new State();
    private static long tierUpAt;
    /** When the chat last said a repeatable mission filled its cycle (not saved): the catch of that line is not counted again. */
    private static final Map<String, Long> cycleAt = new java.util.HashMap<>();

    private EventHud() {}

    /** Called once the settings are loaded; the hud then reads and writes that state. */
    public static void bind(State saved) {
        state = saved;
    }

    /** Whether the screen is /event: its title is art only, {@code U+EEE4 U+D568}. */
    static boolean isEventMenu(Text title) {
        return title.getString().indexOf('함') >= 0;
    }

    /** Called every half second for the open chest screen; true when the saved state changed. */
    public static boolean scan(HandledScreen<?> screen) {
        if (!isEventMenu(screen.getTitle())) {
            return false;
        }
        List<Item> items = new ArrayList<>();
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || slot.inventory instanceof PlayerInventory) {
                continue;
            }
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            List<Text> lines = lore == null ? List.<Text>of() : lore.lines();
            items.add(new Item(Glyphs.plain(stack.getName()), lines.stream().map(EventHud::tagged).toList(), lines.stream().map(EventHud::firstWordColor).toList()));
        }
        return read(items);
    }

    /** The line as plain text, with the pack glyph of a River Monsters variant spelled out. */
    private static String tagged(Text line) {
        StringBuilder out = new StringBuilder(Glyphs.plain(line));
        String raw = line.getString();
        TAGS.forEach((glyph, tag) -> {
            if (raw.indexOf(glyph) >= 0) {
                out.append(' ').append(tag);
            }
        });
        return out.toString();
    }

    /** The colour (rgb) of the first run with a latin letter in it, -1 for none. */
    static int firstWordColor(Text line) {
        // hangul syllables are letters too: the pack's art in front of every lore line (and its brown) must not count
        return line.visit((style, s) -> style.getColor() != null && s.codePoints().anyMatch(cp -> cp < 0x250 && Character.isLetter(cp))
                ? Optional.of(style.getColor().getRgb()) : Optional.empty(), Style.EMPTY).orElse(-1);
    }

    private static int colorAt(List<Integer> colors, int i) {
        return colors != null && i < colors.size() ? colors.get(i) : -1;
    }

    /** Takes what the menu items say; true when that changed the state (a menu without thresholds changes nothing). */
    static boolean read(List<Item> items) {
        List<Threshold> found = new ArrayList<>();
        String title = "", placement = "";
        double points = -1, haveMax = -1;
        Map<String, Mission> book = null;
        for (Item item : items) {
            if (item.name().startsWith("event missions")) {
                book = missions(item);
            }
            Matcher m = THRESHOLD.matcher(item.name());
            if (m.find()) {
                Threshold t = threshold(Integer.parseInt(m.group(1)), item);
                if (t != null) {
                    found.add(t);
                    haveMax = Math.max(haveMax, t.have());
                }
            }
            m = WELCOME.matcher(item.name());
            if (m.find()) {
                title = m.group(1);
            }
            List<String> all = new ArrayList<>(item.lines());
            all.add(0, item.name());
            for (String line : all) {
                m = POINTS.matcher(line);
                if (m.find()) {
                    points = m.group(1) == null ? 0 : num(m.group(1), m.group(2));
                }
                m = PLACEMENT.matcher(line);
                if (m.find()) {
                    placement = m.group(1);
                }
            }
        }
        boolean missionsChanged = book != null && !book.equals(state.missions);
        if (missionsChanged) {
            state.missions = book;
        }
        if (found.isEmpty()) {
            return missionsChanged;
        }
        found.sort((a, b) -> Integer.compare(a.n(), b.n()));
        double total = points >= 0 ? points : Math.max(0, haveMax);
        boolean changed = !found.equals(state.thresholds) || total != state.points || !placement.equals(state.placement)
                || !title.isEmpty() && !title.equals(state.title) || missionsChanged;
        state.thresholds = found;
        state.points = total;
        state.placement = placement;
        if (!title.isEmpty()) {
            state.title = title;
        }
        return changed;
    }

    /** The book's list: a {@code - name [✔]} line, then its {@code ┗ (cur/max) [earned|per] mark} line, for every mission of the event. */
    private static Map<String, Mission> missions(Item book) {
        Map<String, Mission> found = new LinkedHashMap<>();
        List<String> lines = book.lines();
        for (int i = 0; i + 1 < lines.size(); i++) {
            Matcher name = MISSION_NAME.matcher(lines.get(i));
            Matcher detail = MISSION_LINE.matcher(lines.get(i + 1));
            if (!name.find() || !detail.find()) {
                continue;
            }
            String title = name.group(1).trim();
            boolean done = name.group(2) != null;
            Mission mission;
            if (detail.group(1) != null) {
                int cur = Integer.parseInt(detail.group(1)), max = Integer.parseInt(detail.group(2));
                String mark = detail.group(7).trim();
                String kind = mark.equals("∞") ? "repeat" : mark.equalsIgnoreCase("partial") ? "partial" : "once";
                mission = new Mission(title, kind, cur, max, num(detail.group(3), detail.group(4)), num(detail.group(5), detail.group(6)),
                        done || !kind.equals("repeat") && cur >= max, book.color(i));
            } else {
                double weight = num(detail.group(9), detail.group(10));
                mission = new Mission(title, "weight", 0, 0, weight, weight, false, book.color(i));
            }
            found.put(title, mission);
        }
        return found;
    }

    private static Threshold threshold(int n, Item item) {
        List<String> lines = item.lines();
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = RANGE.matcher(lines.get(i));
            if (!m.find()) {
                continue;
            }
            double have = m.group(1) == null ? 0 : num(m.group(1), m.group(2));
            List<String> rewards = new ArrayList<>();
            List<Integer> colors = new ArrayList<>();
            for (int j = i + 1; j < lines.size(); j++) {
                String line = lines.get(j);
                if (!line.isEmpty() && !line.startsWith("┗")) { // the lines under the points; the bait package's biome line is dropped
                    rewards.add(line);
                    colors.add(item.color(j));
                }
            }
            return new Threshold(n, have, num(m.group(3), m.group(4)), rewards, colors);
        }
        return null;
    }

    /**
     * A game chat line, already through {@code Matcher.normalize}; true when the saved state changed. Mission lines add their points
     * to the total (unless they follow a tier up of the same catch, which already counted them) and a tier up line sets the total.
     */
    public static boolean onChat(String line, long now) {
        Matcher m = TIER_UP.matcher(line);
        if (m.find()) {
            state.points = num(m.group(2), m.group(3));
            tierUpAt = now;
            return true;
        }
        m = MISSION.matcher(line);
        if (!m.find()) {
            return false;
        }
        double gained = num(m.group(2), m.group(3));
        String name = Glyphs.fromSmallCaps(m.group(4).trim()); // a variant is in small caps here and plain in the book: one mission
        boolean done = m.group(5) != null;
        Mission mission;
        if (m.group(6) != null) {
            int cur = Integer.parseInt(m.group(6)), max = Integer.parseInt(m.group(7));
            String mark = m.group(12).trim();
            String kind = mark.equals("∞") ? "repeat" : mark.equalsIgnoreCase("partial") ? "partial" : "once";
            if (kind.equals("repeat")) {
                cycleAt.put(name.toLowerCase(Locale.ROOT), now); // sent when the cycle is full (10 River Monsters, 100 Freshwater): (0/100), a new one starts
            }
            mission = new Mission(name, kind, cur, max, num(m.group(8), m.group(9)), num(m.group(10), m.group(11)), done || !kind.equals("repeat") && cur >= max, -1);
        } else {
            mission = new Mission(name, "weight", 0, 0, num(m.group(14), m.group(15)), gained, false, -1);
        }
        String key = name.toLowerCase(Locale.ROOT);
        Mission old = state.missions.get(key);
        state.missions.put(key, old == null ? mission : new Mission(mission.name(), mission.kind(), mission.cur(), mission.max(), mission.earned(),
                mission.per(), mission.done(), old.color()));
        if (state.points >= 0 && now - tierUpAt > SAME_CATCH_MS) {
            state.points += gained;
        }
        return true;
    }

    /** Locations with fresh water (research doc section 7, foecollab's ids); the `Freshwater` mission counts catches there. */
    private static final java.util.Set<String> FRESH = java.util.Set.of("spawn", "cypress", "kenai", "biwa", "murray", "everglades",
            "toledobend", "greatlakes", "danube", "amazon", "nile");

    /** The River Monster species of the event (the species item of /event), letters only like {@link #letters}: any catch of one counts for `River Monsters`. */
    private static final java.util.Set<String> RIVER_MONSTERS = java.util.Set.of("piraiba", "alligatorgar", "welscatfish", "muskellunge", "murraycod",
            "goldendorado", "flatheadcatfish", "belugasturgeon", "tigermuskellunge", "arapaima", "electriceel");

    private static String letters(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    /**
     * A catch ({@code CatchWatch}: the fish's location and fish id): the server sends a chat line for a repeatable mission only when its cycle
     * is full, and none for the count of a location mission ({@code Amazon (40/1500)}), so these are counted here, +1 per catch: the
     * location's mission, {@code Freshwater} for fresh water and {@code River Monsters} for a species of the event. The book sets them
     * right when /event is opened. True when something changed.
     */
    public static boolean onCatch(String location, String fishId, long now) {
        String name = ServerInfo.LOCATIONS.get(location); // the book names a location mission like the boss bar does: amazon river
        boolean changed = bump(name == null ? null : name.toLowerCase(Locale.ROOT), now);
        if (FRESH.contains(location)) {
            changed |= bump("freshwater", now);
        }
        if (fishId != null && RIVER_MONSTERS.contains(letters(fishId))) {
            changed |= bump("river monsters", now);
        }
        return changed;
    }

    private static boolean bump(String key, long now) {
        Mission m = key == null ? null : state.missions.get(key);
        if (m == null || m.kind().equals("weight") || m.done() && !m.kind().equals("repeat")) {
            return false;
        }
        boolean repeat = m.kind().equals("repeat");
        if (repeat && now - cycleAt.getOrDefault(key, -SAME_CATCH_MS * 2) <= SAME_CATCH_MS) {
            return false; // the chat line came first: this catch is the one that filled the cycle, already 0
        }
        int cur = m.cur() + 1;
        if (repeat && cur >= m.max()) {
            cur = 0; // the cycle is full: it starts again (the chat line follows and sets the points)
        }
        state.missions.put(key, new Mission(m.name(), m.kind(), cur, m.max(), m.earned(), m.per(), !repeat && cur >= m.max(), m.color()));
        return true;
    }

    static double num(String digits, String suffix) {
        return XpEstimate.num(digits, suffix);
    }

    /** 840, 1.5K, 2.25M: the server's own short form. */
    static String fmt(double v) {
        String suffix = "";
        if (v >= 1_000_000) {
            v /= 1_000_000;
            suffix = "M";
        } else if (v >= 1_000) {
            v /= 1_000;
            suffix = "K";
        }
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.replaceAll("0+$", "").replaceAll("\\.$", "") + suffix;
    }

    /**
     * The hud rows {label, value} plus, for the coloured ones, {label colour, value colour} as rgb numbers; with preview and nothing read yet they
     * hold dashes. Missions: the ones not done yet and the repeatable ones, with the points earned so far and the points one catch gives.
     */
    static List<String[]> rows(boolean preview, boolean missions) {
        List<String[]> rows = new ArrayList<>();
        if (state.thresholds.isEmpty() && state.missions.isEmpty()) {
            if (preview) {
                rows.add(new String[]{"points", "-"});
                rows.add(new String[]{"next threshold", "-"});
                rows.add(new String[]{"placement", "-"});
            }
            return rows;
        }
        if (state.points >= 0) {
            rows.add(new String[]{"points", fmt(state.points)});
        }
        if (!state.placement.isEmpty()) {
            rows.add(new String[]{"placement", state.placement});
        }
        Mission fresh = state.missions.get("freshwater"); // every catch counts there: completed cycles of max catches plus the running one
        if (fresh != null && fresh.kind().equals("repeat") && fresh.per() > 0) {
            rows.add(new String[]{"catches", Long.toString(Math.round(fresh.earned() / fresh.per()) * fresh.max() + fresh.cur())});
        }
        if (missions) {
            for (Mission m : state.missions.values()) {
                if (m.done() && !m.kind().equals("repeat") || m.kind().equals("weight") || variant(m.name())) {
                    continue; // done, the heaviest arapaima and the albino, melanistic, trophy and gigantic River Monsters are left out
                }
                String value = switch (m.kind()) {
                    case "repeat" -> m.cur() + "/" + m.max() + " " + fmt(m.earned()) + "pts/" + fmt(m.per()) + "pts";
                    case "weight" -> fmt(m.earned()) + " pts";
                    case "partial" -> m.cur() + "/" + m.max() + " " + fmt(m.earned()) + "pts/" + fmt(m.per()) + "pts";
                    default -> m.cur() + "/" + m.max() + " " + fmt(m.per()) + "pts";
                };
                rows.add(colored(label(m.name().toLowerCase(Locale.ROOT)), value, m.color(), GREEN));
            }
        }
        return rows;
    }

    /** A River Monsters variant (the tag is spelled out at the end of its name). */
    private static boolean variant(String name) {
        String n = name.toLowerCase(Locale.ROOT);
        return TAGS.values().stream().anyMatch(n::endsWith) || n.endsWith("gigantic");
    }

    /** The hud's label of a mission: a location by its short name (as in the level bars), `river monsters` as monsters, `freshwater` as fresh. */
    private static String label(String name) {
        for (Map.Entry<String, String> e : ServerInfo.LOCATIONS.entrySet()) {
            if (e.getValue().equalsIgnoreCase(name)) {
                return LevelBars.SHORT.getOrDefault(e.getKey(), name);
            }
        }
        return name.equals("freshwater") ? "fresh" : name.startsWith("river monsters") ? name.substring("river ".length()) : name;
    }

    /** A name in the menu's gold or brown reads dark on the hud, so it is drawn in bright green. */
    private static boolean brown(int rgb) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        return r >= 0x80 && r > g && b < 0x40;
    }

    private static String[] colored(String label, String value, int labelRgb, int valueArgb) {
        return new String[]{label, value, Integer.toString(labelRgb < 0 ? Foe.TEXT : brown(labelRgb) ? GREEN : 0xFF000000 | labelRgb), Integer.toString(valueArgb == 0 ? Foe.TEXT : valueArgb)};
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}; with preview an empty hud still shows a line so it can be moved. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        TackleboxSettings s = TackleboxSettings.get();
        return s.eventPos.draw(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), (x, y) -> {
            s.eventX = x;
            s.eventY = y;
            return Foe.scaled(ctx, x, y, s.eventScale, c -> drawAt100(c, preview));
        });
    }

    private static int[] drawAt100(DrawContext ctx, boolean preview) {
        TackleboxSettings s = TackleboxSettings.get();
        List<String[]> rows = rows(preview, s.eventMissions);
        if (rows.isEmpty()) {
            return null;
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = 0;
        for (String[] r : rows) {
            w = Math.max(w, tr.getWidth(Foe.text(r[0])) + (r[1].isEmpty() ? 0 : 12 + tr.getWidth(Foe.text(r[1]))));
        }
        w += 16;
        int h = 25 + rows.size() * 11;
        Foe.title(ctx, s.eventX, s.eventY, w, s.eventAlign, Foe.text(state.title.isEmpty() ? "event" : state.title));
        for (int i = 0; i < rows.size(); i++) {
            int y = s.eventY + 20 + i * 11;
            Text l = Foe.text(rows.get(i)[0]), v = Foe.text(rows.get(i)[1]);
            int lx = s.eventX + 8, vx = s.eventX + w - 8 - tr.getWidth(v); // left keeps the values in a column on the right
            if (s.eventAlign != TackleboxSettings.HudAlign.LEFT) {
                lx = s.eventAlign.x(s.eventX + 8, w - 16, tr.getWidth(l) + (v.getString().isEmpty() ? 0 : 12 + tr.getWidth(v)));
                vx = lx + tr.getWidth(l) + 12;
            }
            String[] r = rows.get(i);
            ctx.drawTextWithShadow(tr, l, lx, y, r.length > 2 ? Integer.parseInt(r[2]) : Foe.DIM);
            ctx.drawTextWithShadow(tr, v, vx, y, r.length > 3 ? Integer.parseInt(r[3]) : Foe.TEXT);
        }
        return new int[]{s.eventX, s.eventY, w, h};
    }

    public static void renderHud(DrawContext ctx) {
        if (TackleboxSettings.get().eventHud && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }

    /** For tests: start from an empty state. */
    static void clear() {
        state = new State();
        tierUpAt = 0;
        cycleAt.clear();
    }
}
