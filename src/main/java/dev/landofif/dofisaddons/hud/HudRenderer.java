package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Draws custom HUDs as panels in the style of l's system24 Discord theme: a dark box with a thin border,
 * the title set into the top edge, roomy padding, and label / value rows with the values lined up on
 * the right. Lines are re-resolved 20 times a second, not every frame. A line with a value that
 * isn't available right now is left out, so a box only shows what makes sense at the moment.
 *
 * <p>Line rules: {@code %tab%} splits a line into a label (left) and a value (right-aligned). If the
 * first line is bold and has no values (e.g. {@code &6&lAll-time}), it becomes the title in the border.
 * A HUD with background 0 is drawn bare: no panel, border or title slot.
 */
public final class HudRenderer {
    private static final int PAD_X = 7;
    private static final int PAD_Y = 6;
    private static final int LINE_H = 12;
    private static final int TEXT_H = 9;
    private static final int COL_GAP = 12;
    /** Title: how far in from the left edge, the gap around it in the border, how far it rises above. */
    private static final int TITLE_X = 6;
    private static final int TITLE_GAP = 3;
    private static final int TITLE_RISE = 5;
    private static final int PANEL_RGB = 0x0E0F12;
    private static final int BORDER_RGB = 0x4E525C;
    /** Under 100ms so tenths of a second (the bobber timer) never skip a step. */
    private static final long REFRESH_MS = 50;

    private static final String TAB = "%tab%";
    /** Leading colour/format codes ending in &l, i.e. a bold first line. */
    private static final Pattern BOLD_HEAD = Pattern.compile("^(?:&(?:#[0-9a-fA-F]{6}|[0-9a-fk-orA-FK-OR]))*&[lL].*");

    /** One row: a label and, if the line had {@code %tab%}, a value drawn right-aligned. */
    public record Row(Text left, Text right) {}

    /** A resolved HUD: an optional border title and the rows to show. */
    public record Resolved(Text title, List<Row> rows) {
        public static final Resolved EMPTY = new Resolved(null, List.of());

        public static Resolved of(Text line) {
            return new Resolved(null, List.of(new Row(line, null)));
        }

        public boolean isEmpty() {
            return rows.isEmpty();
        }
    }

    private static final Map<CustomHud, Resolved> CACHE = new IdentityHashMap<>();
    private static long cachedAt;

    private HudRenderer() {}

    public static void invalidate() {
        cachedAt = 0;
    }

    /** Resolves a HUD's lines. Lines with missing values are dropped; a blank line is a spacer. */
    public static Resolved resolve(CustomHud hud) {
        List<String> templates = new ArrayList<>();
        for (String s : hud.lines) {
            if (s != null) templates.add(s);
        }
        Text title = null;
        if (hud.background > 0 && templates.size() > 1 && isTitle(templates.get(0))) {
            HudEngine.ParseResult parsed = HudEngine.parse(colorCodes(templates.remove(0)));
            title = mono(parsed.text());
        }

        List<Row> rows = new ArrayList<>();
        boolean any = false;
        for (String template : templates) {
            int split = template.toLowerCase(Locale.ROOT).indexOf(TAB);
            String leftT = split < 0 ? template : template.substring(0, split);
            String rightT = split < 0 ? null : template.substring(split + TAB.length());
            HudEngine.ParseResult left = HudEngine.parse(colorCodes(leftT));
            HudEngine.ParseResult right = rightT == null ? null : HudEngine.parse(colorCodes(rightT));
            if (!left.complete() || (right != null && !right.complete())) continue;
            rows.add(new Row(mono(left.text()), right == null ? null : mono(right.text())));
            if (!left.text().getString().isBlank() || (right != null && !right.text().getString().isBlank())) any = true;
        }
        return any ? new Resolved(title, rows) : Resolved.EMPTY;
    }

    private static boolean isTitle(String line) {
        return !line.contains("%") && BOLD_HEAD.matcher(line.trim()).matches();
    }

    private static MutableText mono(Text t) {
        return Text.empty().setStyle(Mono.style(Style.EMPTY)).append(t);
    }

    /** "&a" and "&#RRGGBB" to the § form the engine reads; "&&" stays a literal "&". */
    static String colorCodes(String s) {
        return s.replace("&&", "\u0000").replace('&', '§').replace('\u0000', '&');
    }

    public static void renderAll(DrawContext ctx) {
        long now = System.currentTimeMillis();
        if (now - cachedAt >= REFRESH_MS) {
            CACHE.clear();
            for (CustomHud hud : HudStore.all()) {
                if (hud.enabled) CACHE.put(hud, resolve(hud));
            }
            cachedAt = now;
        }
        for (CustomHud hud : HudStore.all()) {
            Resolved r = CACHE.get(hud);
            if (hud.enabled && r != null && !r.isEmpty()) {
                draw(ctx, hud, r, false);
            }
        }
    }

    /** Draws one HUD and returns its on-screen rectangle {x, y, w, h} (title included). */
    public static int[] draw(DrawContext ctx, CustomHud hud, Resolved resolved, boolean highlight) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        float scale = (float) hud.scale;
        boolean panel = hud.background > 0;
        // A panel needs no shadow; text floating over the world gets a soft one.
        boolean shadow = !panel || hud.background < 25;

        Text title = resolved.title() == null ? null : Mono.prepare(resolved.title(), scale, shadow);
        List<Text> lefts = new ArrayList<>();
        List<Text> rights = new ArrayList<>();
        int contentW = 0;
        for (Row row : resolved.rows()) {
            Text l = Mono.prepare(row.left(), scale, shadow);
            Text r = row.right() == null ? null : Mono.prepare(row.right(), scale, shadow);
            lefts.add(l);
            rights.add(r);
            contentW = Math.max(contentW, tr.getWidth(l) + (r == null ? 0 : COL_GAP + tr.getWidth(r)));
        }
        int titleW = title == null ? 0 : tr.getWidth(title);
        int rise = title == null ? 0 : TITLE_RISE;
        int padX = panel ? PAD_X : 2;
        int padY = panel ? PAD_Y + (title == null ? 0 : 2) : 1;
        int boxW = Math.max(contentW + padX * 2, title == null ? 0 : titleW + (TITLE_X + TITLE_GAP) * 2);
        int boxH = padY * 2 + Math.max(1, lefts.size()) * LINE_H - (LINE_H - TEXT_H);

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int w = Math.round(boxW * scale);
        int h = Math.round((boxH + rise) * scale);
        int anchorX = (int) Math.round(hud.x / 100.0 * sw);
        int x = switch (hud.align) {
            case LEFT -> anchorX;
            case CENTER -> anchorX - w / 2;
            case RIGHT -> anchorX - w;
        };
        int y = (int) Math.round(hud.y / 100.0 * sh);
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        ctx.getMatrices().translate(0, rise);

        if (panel) {
            int alpha = Math.round(hud.background / 100f * 255);
            ctx.fill(0, 0, boxW, boxH, alpha << 24 | PANEL_RGB);
            int border = Math.min(255, Math.round(alpha * 1.4f)) << 24 | BORDER_RGB;
            if (title == null) {
                ctx.fill(0, 0, boxW, 1, border);
            } else { // leave a gap in the top edge for the title
                ctx.fill(0, 0, TITLE_X, 1, border);
                ctx.fill(TITLE_X + TITLE_GAP * 2 + titleW, 0, boxW, 1, border);
            }
            ctx.fill(0, boxH - 1, boxW, boxH, border);
            ctx.fill(0, 1, 1, boxH - 1, border);
            ctx.fill(boxW - 1, 1, boxW, boxH - 1, border);
            if (title != null) {
                ctx.drawTextWithShadow(tr, title, TITLE_X + TITLE_GAP, -TEXT_H / 2, 0xFFFFFFFF);
            }
        }
        if (highlight) {
            int c = 0xFF55FFFF;
            ctx.fill(-1, -1 - rise, boxW + 1, -rise, c);
            ctx.fill(-1, boxH, boxW + 1, boxH + 1, c);
            ctx.fill(-1, -rise, 0, boxH, c);
            ctx.fill(boxW, -rise, boxW + 1, boxH, c);
        }

        int ty = padY;
        for (int i = 0; i < lefts.size(); i++) {
            Text l = lefts.get(i);
            Text r = rights.get(i);
            if (r != null) { // label left, value right-aligned
                ctx.drawTextWithShadow(tr, l, padX, ty, 0xFFFFFFFF);
                ctx.drawTextWithShadow(tr, r, boxW - padX - tr.getWidth(r), ty, 0xFFFFFFFF);
            } else {
                int lw = tr.getWidth(l);
                int tx = switch (hud.align) {
                    case LEFT -> padX;
                    case CENTER -> padX + (contentW - lw) / 2;
                    case RIGHT -> boxW - padX - lw;
                };
                ctx.drawTextWithShadow(tr, l, tx, ty, 0xFFFFFFFF); // mono runs carry their own shadow setting
            }
            ty += LINE_H;
        }
        ctx.getMatrices().popMatrix();
        return new int[]{x, y, w, h};
    }
}
