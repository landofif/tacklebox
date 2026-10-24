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
    private static final Pattern THRESHOLD = Pattern.compile("^threshold (\\d+)$", CI);
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

    /** The colour (rgb) of the first run with a letter in it, -1 for none. */
    private static int firstWordColor(Text line) {
        return line.visit((style, s) -> s.codePoints().anyMatch(Character::isLetter) && style.getColor() != null
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
        String name = m.group(4).trim();
        boolean done = m.group(5) != null;
        Mission mission;
        if (m.group(6) != null) {
            int cur = Integer.parseInt(m.group(6)), max = Integer.parseInt(m.group(7));
            String mark = m.group(12).trim();
            String kind = mark.equals("∞") ? "repeat" : mark.equalsIgnoreCase("partial") ? "partial" : "once";
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
        Threshold next = null;
        int reached = 0;
        for (Threshold t : state.thresholds) {
            if (state.points >= t.need()) {
                reached++;
            } else if (next == null) {
                next = t;
            }
        }
        if (next != null) {
            rows.add(new String[]{"threshold " + next.n(), fmt(next.need() - state.points) + " to go"});
            for (int i = 0; i < next.rewards().size(); i++) {
                rows.add(colored("  " + next.rewards().get(i), "", colorAt(next.colors(), i), 0));
            }
        } else if (!state.thresholds.isEmpty()) {
            rows.add(new String[]{"all thresholds done", ""});
        }
        if (!state.thresholds.isEmpty()) {
            rows.add(new String[]{"thresholds", reached + "/" + state.thresholds.size()});
        }
        if (!state.placement.isEmpty()) {
            rows.add(new String[]{"placement", state.placement});
        }
        if (missions) {
            for (Mission m : state.missions.values()) {
                if (m.done() && !m.kind().equals("repeat")) {
                    continue;
                }
                String value = switch (m.kind()) {
                    case "repeat" -> fmt(m.earned()) + "pts/" + fmt(m.per()) + "pts";
                    case "weight" -> fmt(m.earned()) + " pts";
                    case "partial" -> m.cur() + "/" + m.max() + " " + fmt(m.earned()) + "pts/" + fmt(m.per()) + "pts";
                    default -> m.cur() + "/" + m.max() + " " + fmt(m.per()) + "pts";
                };
                rows.add(colored(m.name().toLowerCase(Locale.ROOT), value, m.color(), Foe.ACCENT));
            }
        }
        return rows;
    }

    private static String[] colored(String label, String value, int labelRgb, int valueArgb) {
        return new String[]{label, value, Integer.toString(labelRgb < 0 ? Foe.TEXT : 0xFF000000 | labelRgb), Integer.toString(valueArgb == 0 ? Foe.TEXT : valueArgb)};
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
    }
}
