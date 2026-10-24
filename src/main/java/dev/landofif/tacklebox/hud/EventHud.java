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
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The event points and thresholds of /event, read off its items while the menu is open (luna's Halloween 2026 dump): the
 * threshold heads carry {@code [<have> pts|<need> pts]} and their rewards, the book and the leaderboard head carry the points,
 * the leaderboard head the placement. Text with a shadow and no box, like the quests left hud. Not saved: it is blank after a
 * restart until /event was opened once.
 */
public final class EventHud {
    private static final String NUM = "([0-9][0-9.,]*)\\s*([kmb]?)";
    private static final Pattern THRESHOLD = Pattern.compile("^threshold (\\d+)$");
    // [<none> pts|220 pts]   (<none> is the server's word for zero)
    private static final Pattern RANGE = Pattern.compile("^\\[\\s*(?:<?none>?|" + NUM + ")\\s*pts\\s*\\|\\s*" + NUM + "\\s*pts\\s*\\]");
    // "Event Missions → <none> pts" on the book, "Event Points: <none> pts" on the leaderboard head
    private static final Pattern POINTS = Pattern.compile("^event (?:points:|missions \\W+)\\s*(?:<?none>?|" + NUM + ")\\s*pts");
    private static final Pattern PLACEMENT = Pattern.compile("^your placement:\\s*(.+)$");
    private static final Pattern WELCOME = Pattern.compile("^welcome to (.+?)!?$");

    /** An item of the open menu as plain lowercase text ({@link Glyphs#plain}), name and lore lines. */
    public record Item(String name, List<String> lines) {}

    /** A reward step: the points the head shows you have, the points needed in total, and the reward lines (money, items, bait package). */
    public record Threshold(int n, double have, double need, List<String> rewards) {}

    private static String title = "";
    private static double points = -1;
    private static String placement = "";
    private static List<Threshold> thresholds = List.of();

    private EventHud() {}

    /** Whether the screen is /event: its title is art only, {@code U+EEE4 U+D568}. */
    static boolean isEventMenu(Text title) {
        return title.getString().indexOf('함') >= 0;
    }

    /** Called every half second for the open chest screen. */
    public static void scan(HandledScreen<?> screen) {
        if (!isEventMenu(screen.getTitle())) {
            return;
        }
        List<Item> items = new ArrayList<>();
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || slot.inventory instanceof PlayerInventory) {
                continue;
            }
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            items.add(new Item(Glyphs.plain(stack.getName()), lore == null ? List.of() : lore.lines().stream().map(Glyphs::plain).toList()));
        }
        read(items);
    }

    /** Takes what the items say; returns whether any threshold was found (otherwise nothing is touched). */
    static boolean read(List<Item> items) {
        List<Threshold> found = new ArrayList<>();
        String newTitle = "";
        double newPoints = -1;
        String newPlacement = "";
        double haveMax = -1;
        for (Item item : items) {
            Matcher m = THRESHOLD.matcher(item.name());
            if (m.find()) {
                Threshold t = threshold(Integer.parseInt(m.group(1)), item.lines());
                if (t != null) {
                    found.add(t);
                    haveMax = Math.max(haveMax, t.have());
                }
            }
            m = WELCOME.matcher(item.name());
            if (m.find()) {
                newTitle = m.group(1);
            }
            for (String line : concat(item)) {
                m = POINTS.matcher(line);
                if (m.find()) {
                    newPoints = m.group(1) == null ? 0 : num(m.group(1), m.group(2));
                }
                m = PLACEMENT.matcher(line);
                if (m.find()) {
                    newPlacement = m.group(1);
                }
            }
        }
        if (found.isEmpty()) {
            return false;
        }
        found.sort((a, b) -> Integer.compare(a.n(), b.n()));
        thresholds = found;
        points = newPoints >= 0 ? newPoints : Math.max(0, haveMax);
        placement = newPlacement;
        if (!newTitle.isEmpty()) {
            title = newTitle;
        }
        return true;
    }

    private static List<String> concat(Item item) {
        List<String> all = new ArrayList<>();
        all.add(item.name());
        all.addAll(item.lines());
        return all;
    }

    private static Threshold threshold(int n, List<String> lines) {
        for (int i = 0; i < lines.size(); i++) {
            Matcher m = RANGE.matcher(lines.get(i));
            if (!m.find()) {
                continue;
            }
            double have = m.group(1) == null ? 0 : num(m.group(1), m.group(2));
            List<String> rewards = new ArrayList<>();
            for (String line : lines.subList(i + 1, lines.size())) {
                if (!line.isEmpty() && !line.startsWith("┗")) { // the lines under the points; the bait package's biome line is dropped
                    rewards.add(line);
                }
            }
            return new Threshold(n, have, num(m.group(3), m.group(4)), rewards);
        }
        return null;
    }

    static double num(String digits, String suffix) {
        return dev.landofif.tacklebox.hud.XpEstimate.num(digits, suffix);
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
        String s = String.format(java.util.Locale.ROOT, "%.2f", v);
        s = s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
        return s + suffix;
    }

    /** The hud rows {label, value}; with preview and nothing read yet they hold dashes. */
    static List<String[]> rows(boolean preview) {
        List<String[]> rows = new ArrayList<>();
        if (thresholds.isEmpty()) {
            if (preview) {
                rows.add(new String[]{"points", "-"});
                rows.add(new String[]{"next threshold", "-"});
                rows.add(new String[]{"placement", "-"});
            }
            return rows;
        }
        rows.add(new String[]{"points", fmt(points)});
        Threshold next = null;
        int reached = 0;
        for (Threshold t : thresholds) {
            if (points >= t.need()) {
                reached++;
            } else if (next == null) {
                next = t;
            }
        }
        if (next != null) {
            rows.add(new String[]{"threshold " + next.n(), fmt(next.need() - points) + " to go"});
            for (String reward : next.rewards()) {
                rows.add(new String[]{"  " + reward, ""});
            }
        } else {
            rows.add(new String[]{"all thresholds done", ""});
        }
        rows.add(new String[]{"thresholds", reached + "/" + thresholds.size()});
        if (!placement.isEmpty()) {
            rows.add(new String[]{"placement", placement});
        }
        return rows;
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
        List<String[]> rows = rows(preview);
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
        TackleboxSettings s = TackleboxSettings.get();
        Foe.title(ctx, s.eventX, s.eventY, w, s.eventAlign, Foe.text(title.isEmpty() ? "event" : title));
        for (int i = 0; i < rows.size(); i++) {
            int y = s.eventY + 20 + i * 11;
            Text l = Foe.text(rows.get(i)[0]), v = Foe.text(rows.get(i)[1]);
            int lx = s.eventX + 8, vx = s.eventX + w - 8 - tr.getWidth(v); // left keeps the values in a column on the right
            if (s.eventAlign != TackleboxSettings.HudAlign.LEFT) {
                lx = s.eventAlign.x(s.eventX + 8, w - 16, tr.getWidth(l) + (v.getString().isEmpty() ? 0 : 12 + tr.getWidth(v)));
                vx = lx + tr.getWidth(l) + 12;
            }
            ctx.drawTextWithShadow(tr, l, lx, y, Foe.DIM);
            ctx.drawTextWithShadow(tr, v, vx, y, Foe.TEXT);
        }
        return new int[]{s.eventX, s.eventY, w, h};
    }

    public static void renderHud(DrawContext ctx) {
        if (TackleboxSettings.get().eventHud && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }

    /** For tests: forget what was read. */
    static void clear() {
        title = "";
        points = -1;
        placement = "";
        thresholds = List.of();
    }
}
