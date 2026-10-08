package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.hud.XpEstimate;
import dev.landofif.dofisaddons.menu.Power;
import dev.landofif.dofisaddons.menu.Power.Row;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /dofi power: how much power you have and where it came from, then where the next power is cheapest or biggest.
 * Level power is exact (the same amounts at every location, menu/Power); fish power left is an upper bound, because
 * the compendium counts a fish once per location and power counts it once.
 */
public class PowerScreen extends DofiScreen {
    private static final int TOP = 34, PAD = 14, ROW = 16;
    /** One colour per source, always next to its label; tinted greys for text, never pure grey (the bait picker's). */
    private static final int LEVELING = 0xFF4FD1A1, COLLECTIONS = 0xFFE8B04A, FISH = 0xFF5B9DFF, COMPENDIUM = 0xFFB07CFF;
    private static final int MUTED = 0xFF9AA4B2, QUIET = 0xFF5F6877, RULE = 0x22FFFFFF, SELECTED = 0x2855FF55, HOVER = 0x18FFFFFF;
    private static final String[] SORTS = {"next level", "most power", "fish left"};

    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();
    private final long opened = Util.getMeasuringTimeMs();
    private final int[][] chips = new int[SORTS.length][];
    private int sort, scroll, contentH;
    private List<Text> tip;

    public PowerScreen(Screen parent) {
        super(Foe.text("dofis addons · power"));
        this.parent = parent;
    }

    private int cw() {
        return Math.min(width - 2 * (PAD + 12), 560);
    }

    private int x0() {
        return (width - cw()) / 2;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(x0() - PAD, 24, x0() + cw() + PAD, height - 8, 0xC8131A22); // a semi solid panel so the numbers read over the world
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        for (int i = 0; i < chips.length; i++) {
            int[] c = chips[i];
            if (c != null && click.y() >= 26 && click.x() >= c[0] && click.x() < c[0] + c[2] && click.y() >= c[1] && click.y() < c[1] + c[3]) {
                sort = i;
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        scroll = Math.max(0, Math.min(Math.max(0, contentH - (height - 8 - TOP)), scroll - (int) (vertical * 24)));
        return true;
    }

    /** Next level first: the most power per xp left, maxed and unread locations last. */
    private static double ease(Row r) {
        return r.level() == 0 ? -2 : r.maxed() ? -1 : r.gain() / Math.max(1, r.xpLeft());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        tip = null;
        int x0 = x0(), cw = cw(), bottom = height - 8;
        float t = Math.min(1f, (Util.getMeasuringTimeMs() - opened) / 450f);
        float grow = 1 - (1 - t) * (1 - t) * (1 - t); // bars ease out, no bounce

        List<Row> rows = Power.rows();
        Row next = rows.stream().max(Comparator.comparingDouble(PowerScreen::ease)).filter(r -> r.level() > 0 && !r.maxed()).orElse(null);
        Row fish = rows.stream().filter(Row::known).max(Comparator.comparingInt(Row::fish)).filter(r -> r.fish() > 0).orElse(null);
        List<Row> sorted = new ArrayList<>(rows);
        sorted.sort(switch (sort) {
            case 0 -> Comparator.comparingDouble(PowerScreen::ease).reversed();
            case 1 -> Comparator.comparingInt((Row r) -> r.headroom() + r.fish()).reversed();
            default -> Comparator.comparingInt(Row::fish).reversed();
        });

        ctx.enableScissor(0, 26, width, bottom);
        int y = TOP - scroll;
        Map<String, Double> p = s.power;
        boolean have = p.containsKey("total");

        // the total, and how fresh it is
        big(ctx, have ? Power.fmt(p.get("total")) : "-", x0, y, 3, Foe.TEXT);
        int w = textRenderer.getWidth(have ? Power.fmt(p.get("total")) : "-") * 3;
        ctx.drawTextWithShadow(textRenderer, Foe.text("power"), x0 + w + 8, y + 16, MUTED);
        Text note = Foe.sentence(have ? "read from /stats " + ago(s.powerAt) : "open /stats once to read it");
        ctx.drawTextWithShadow(textRenderer, note, x0 + cw - textRenderer.getWidth(note), y + 16, QUIET);
        y += 38;

        // where it came from: one bar, then the four sources under it
        String[] names = {"location levels", "collections", "unique fish", "compendium"};
        double[] part = {p.getOrDefault("leveling", 0.0), p.getOrDefault("collections", 0.0), p.getOrDefault("unique", 0.0), p.getOrDefault("compendium", 0.0)};
        int[] color = {LEVELING, COLLECTIONS, FISH, COMPENDIUM};
        double sum = part[0] + part[1] + part[2] + part[3];
        ctx.fill(x0, y, x0 + cw, y + 6, RULE);
        double cum = 0;
        int from = x0;
        for (int i = 0; sum > 0 && i < 4; i++) {
            cum += part[i];
            int to = x0 + (int) Math.round(cw * grow * cum / sum);
            ctx.fill(from, y, Math.max(from, to - (i < 3 ? 1 : 0)), y + 6, color[i]);
            from = to;
        }
        y += 16;
        for (int i = 0; i < 4; i++) {
            int cx = x0 + i * (cw / 4);
            ctx.fill(cx, y + 2, cx + 4, y + 6, color[i]);
            ctx.drawTextWithShadow(textRenderer, Foe.text(names[i]), cx + 8, y, MUTED);
            big(ctx, have ? Power.fmt(part[i]) : "-", cx, y + 12, 1.5f, Foe.TEXT);
            ctx.drawTextWithShadow(textRenderer, Foe.sentence(have && sum > 0 ? pct(part[i] / sum) + " of your power" : ""), cx, y + 28, QUIET);
            if (i == 0 && mouseIn(mouseX, mouseY, cx, y, cw / 4, 38, bottom)) {
                tip = List.of(Foe.sentence("Every location gives the same power for each of its 20 levels, " + Power.fmt(Power.total(Power.MAX_LEVEL)) + " at level 20."),
                        Foe.sentence("All 17 at level 20 would be " + Power.fmt(Power.LEVEL_MAX) + "."));
            }
        }
        y += 52;
        ctx.fill(x0, y, x0 + cw, y + 1, RULE);
        y += 14;

        // the answer: the next level up that is cheapest for its power, and the place with the most fish left
        int half = cw / 2;
        ctx.drawTextWithShadow(textRenderer, Foe.text("easiest next level"), x0, y, Foe.TITLE);
        ctx.drawTextWithShadow(textRenderer, Foe.text("most fish left"), x0 + half + 10, y, Foe.TITLE);
        y += 14;
        int colW = half - 14; // each side trims to its column, so a long name never runs into the other one
        if (next != null) {
            big(ctx, textRenderer.trimToWidth(cap(next.loc().name()) + " to level " + (next.level() + 1), (int) (colW / 1.25f)), x0, y, 1.25f, Foe.ACCENT);
            double q = XpEstimate.questLxp();
            ctx.drawTextWithShadow(textRenderer, Foe.sentence(textRenderer.trimToWidth("+" + Power.fmt(next.gain()) + " power, " + Power.fmt(next.xpLeft()) + " xp left"
                    + (q > 0 ? ", about " + (long) Math.ceil(next.xpLeft() / q) + " quests" : ""), colW)), x0, y + 14, MUTED);
        } else {
            ctx.drawTextWithShadow(textRenderer, Foe.sentence(textRenderer.trimToWidth("Open the fish compendium to read your location levels.", colW)), x0, y + 2, MUTED);
        }
        if (fish != null) {
            big(ctx, textRenderer.trimToWidth(cap(fish.loc().name()), (int) (colW / 1.25f)), x0 + half + 10, y, 1.25f, Foe.ACCENT);
            ctx.drawTextWithShadow(textRenderer, Foe.sentence(textRenderer.trimToWidth("up to +" + Power.fmt(fish.fish()) + " power from " + java.util.Arrays.stream(fish.left()).sum() + " missing fish", colW)),
                    x0 + half + 10, y + 14, MUTED);
        } else {
            ctx.drawTextWithShadow(textRenderer, Foe.sentence(textRenderer.trimToWidth("Open the fish compendium to read what is missing.", colW)), x0 + half + 10, y + 2, MUTED);
        }
        y += 40;

        // the unique fish you have, per variant, from the breakdown
        if (p.containsKey("n.normal")) {
            ctx.drawTextWithShadow(textRenderer, Foe.text("unique fish caught"), x0, y, Foe.TITLE);
            y += 14;
            int col = cw / Power.VARIANTS.length;
            for (int i = 0; i < Power.VARIANTS.length; i++) {
                int cx = x0 + i * col;
                double n = p.getOrDefault("n." + Power.VARIANTS[i], 0.0);
                ctx.drawTextWithShadow(textRenderer, Foe.text(Power.VARIANTS[i]), cx, y, MUTED);
                big(ctx, String.valueOf((long) n), cx, y + 12, 1.25f, Foe.TEXT);
                ctx.drawTextWithShadow(textRenderer, Text.literal("+" + Power.fmt(n * Power.PER[i])), cx, y + 26, FISH);
                if (mouseIn(mouseX, mouseY, cx, y, col, 38, bottom)) {
                    tip = List.of(Foe.sentence("Each new unique " + Power.VARIANTS[i] + " fish gives " + Power.PER[i] + " power."));
                }
            }
            y += 52;
        }

        // every location: level, what the next one gives, what it costs, what fish are left
        ctx.fill(x0, y, x0 + cw, y + 1, RULE);
        y += 14;
        ctx.drawTextWithShadow(textRenderer, Foe.text("locations"), x0, y, Foe.TITLE);
        int cx = x0 + cw;
        for (int i = SORTS.length - 1; i >= 0; i--) {
            int chipW = textRenderer.getWidth(Foe.text(SORTS[i])) + 14;
            cx -= chipW;
            boolean hot = mouseIn(mouseX, mouseY, cx, y - 3, chipW, 14, bottom);
            chips[i] = new int[]{cx, y - 3, chipW, 14};
            if (i == sort) {
                ctx.fill(cx, y - 3, cx + chipW, y + 11, SELECTED);
            } else {
                Foe.button(ctx, cx, y - 3, chipW, 14, hot);
            }
            ctx.drawTextWithShadow(textRenderer, Foe.text(SORTS[i]), cx + 7, y, i == sort ? Foe.ACCENT : MUTED);
            cx -= 4;
        }
        y += 18;
        int cLevel = x0 + cw * 27 / 100, cNext = x0 + cw * 52 / 100, cXp = x0 + cw * 64 / 100;
        ctx.drawTextWithShadow(textRenderer, Foe.text("level"), cLevel, y, QUIET);
        ctx.drawTextWithShadow(textRenderer, Foe.text("next"), cNext, y, QUIET);
        ctx.drawTextWithShadow(textRenderer, Foe.text("xp left"), cXp, y, QUIET);
        Text fishHead = Foe.text("fish left");
        ctx.drawTextWithShadow(textRenderer, fishHead, x0 + cw - textRenderer.getWidth(fishHead), y, QUIET);
        y += 14;
        double q = XpEstimate.questLxp();
        int barW = Math.max(30, cw * 12 / 100);
        for (int i = 0; i < sorted.size(); i++) {
            Row r = sorted.get(i);
            int ry = y + i * ROW;
            if (ry + ROW < 26 || ry > bottom) {
                continue;
            }
            if (i > 0) {
                ctx.fill(x0, ry, x0 + cw, ry + 1, RULE);
            }
            boolean over = mouseIn(mouseX, mouseY, x0 - 6, ry, cw + 12, ROW, bottom);
            if (over) {
                ctx.fill(x0 - 6, ry, x0 + cw + 6, ry + ROW, HOVER);
                tip = tooltip(r, q);
            }
            int ty = ry + 4;
            boolean top = i == 0 && (sort == 0 ? r.level() > 0 && !r.maxed() : sort == 1 ? r.headroom() + r.fish() > 0 : r.fish() > 0);
            ctx.drawTextWithShadow(textRenderer, Text.literal(cap(r.loc().name())), x0, ty, top ? Foe.ACCENT : r.maxed() ? MUTED : Foe.TEXT);
            // level, with a bar for how far it is to the next one
            ctx.drawTextWithShadow(textRenderer, Text.literal(r.level() == 0 ? "-" : String.valueOf(r.level())), cLevel, ty, r.level() == 0 ? QUIET : Foe.TEXT);
            if (r.level() > 0) {
                int bx = cLevel + 18;
                double done = r.maxed() ? 1 : 1 - r.xpLeft() / XpEstimate.need(r.level() + 1);
                ctx.fill(bx, ty + 3, bx + barW, ty + 5, RULE);
                ctx.fill(bx, ty + 3, bx + (int) (barW * Math.max(0, Math.min(1, done)) * grow), ty + 5, LEVELING);
            }
            // next level: the power, then the xp and quests it takes
            if (r.maxed()) {
                ctx.drawTextWithShadow(textRenderer, Foe.text("max"), cNext, ty, QUIET);
            } else if (r.level() > 0) {
                ctx.drawTextWithShadow(textRenderer, Text.literal("+" + Power.fmt(r.gain())), cNext, ty, LEVELING);
                ctx.drawTextWithShadow(textRenderer, Text.literal(Power.fmt(r.xpLeft()) + (q > 0 ? " · ~" + (long) Math.ceil(r.xpLeft() / q) + "q" : "")), cXp, ty, MUTED);
            } else {
                ctx.drawTextWithShadow(textRenderer, Text.literal("-"), cNext, ty, QUIET);
            }
            Text f = Text.literal(r.known() ? "+" + Power.fmt(r.fish()) : "-");
            ctx.drawTextWithShadow(textRenderer, f, x0 + cw - textRenderer.getWidth(f), ty, r.known() ? FISH : QUIET);
        }
        y += sorted.size() * ROW + 10;
        if (rows.stream().anyMatch(r -> r.level() == 0 || !r.known())) {
            ctx.drawTextWithShadow(textRenderer, Foe.sentence("Open the fish compendium to read the rest."), x0, y, MUTED);
            y += 14;
        }
        for (OrderedText line : textRenderer.wrapLines(Foe.sentence("Level power is exact. Fish left is an upper bound: the compendium counts a fish once per location and power counts it once, so a fish that lives in two places is counted twice here. Compendium and collection power are shown as read, there is no breakdown for them yet."), cw)) {
            ctx.drawTextWithShadow(textRenderer, line, x0, y, QUIET);
            y += 11;
        }
        contentH = y + scroll - TOP + 6;
        ctx.disableScissor();
        scroll = Math.max(0, Math.min(Math.max(0, contentH - (bottom - TOP)), scroll));
        if (tip != null) {
            ctx.drawTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }

    private List<Text> tooltip(Row r, double q) {
        List<Text> out = new ArrayList<>();
        out.add(Text.literal(cap(r.loc().name())));
        if (r.level() == 0) {
            out.add(Foe.sentence("Level not read yet. Open the fish compendium."));
        } else if (r.maxed()) {
            out.add(Foe.sentence("Level 20, all level power collected: " + Power.fmt(Power.total(Power.MAX_LEVEL))));
        } else {
            out.add(Foe.sentence("Level " + r.level() + " of 20, next gives +" + Power.fmt(r.gain())));
            out.add(Foe.sentence(Power.fmt(r.xpLeft()) + " xp to go" + (q > 0 ? ", about " + (long) Math.ceil(r.xpLeft() / q) + " quests" : "")));
            out.add(Foe.sentence("Level power left to level 20: " + Power.fmt(r.headroom())));
        }
        if (r.known()) {
            out.add(Foe.sentence("Fish power left, at most: " + Power.fmt(r.fish())));
            for (int i = 0; i < r.left().length; i++) {
                if (r.left()[i] > 0) {
                    out.add(Foe.sentence(Power.VARIANTS[i] + ": " + r.left()[i] + " left, +" + Power.fmt(r.left()[i] * Power.PER[i])));
                }
            }
        }
        return out;
    }

    private void big(DrawContext ctx, String text, int x, int y, float scale, int color) {
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(scale, scale);
        ctx.drawTextWithShadow(textRenderer, Text.literal(text), 0, 0, color);
        m.popMatrix();
    }

    private static boolean mouseIn(double mx, double my, int x, int y, int w, int h, int bottom) {
        return mx >= x && mx < x + w && my >= Math.max(26, y) && my < Math.min(bottom, y + h);
    }

    private static String pct(double x) {
        return String.format(Locale.ROOT, "%.0f%%", x * 100);
    }

    private static String ago(long at) {
        long min = (System.currentTimeMillis() - at) / 60_000;
        return min < 1 ? "just now" : min < 60 ? min + " min ago" : min < 2880 ? min / 60 + " h ago" : min / 1440 + " days ago";
    }

    /** "key west" as "Key West". */
    private static String cap(String name) {
        StringBuilder b = new StringBuilder();
        for (String word : name.split(" ")) {
            b.append(b.isEmpty() ? "" : " ").append(Character.toUpperCase(word.charAt(0))).append(word.substring(1));
        }
        return b.toString();
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
