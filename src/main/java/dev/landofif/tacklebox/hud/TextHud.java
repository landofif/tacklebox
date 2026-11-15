package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;

/**
 * A hud of text rows in the look of the event hud: a title in the accent, then label and value rows with a shadow and no box.
 * The timers, warnings and mini event huds draw through this.
 */
public final class TextHud {
    /** A piece of a label or value in its own colour ({@code 0xFFrrggbb}); 0 is the row's colour. */
    public record Part(String text, int color) {}

    /**
     * A row; a colour of 0 is the default (dim label, white value). With parts, the label or value is drawn from them
     * (they spell the same text), each in its own colour: the mini event's missions in their chat colours.
     */
    public record Row(String label, String value, int labelColor, int valueColor, List<Part> labelParts, List<Part> valueParts) {
        public Row(String label, String value) {
            this(label, value, 0, 0);
        }

        public Row(String label, String value, int labelColor, int valueColor) {
            this(label, value, labelColor, valueColor, List.of(), List.of());
        }
    }

    private TextHud() {}

    /** Draws the rows at the hud's saved spot and returns {x, y, w, h}, or null when there are none. */
    public static int[] draw(DrawContext ctx, HudSpot spot, String title, List<Row> rows) {
        if (rows.isEmpty()) {
            return null;
        }
        return spot.pos.draw(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(),
                (x, y) -> Foe.scaled(ctx, x, y, spot.scale, c -> drawAt100(c, x, y, spot.align, title, rows)));
    }

    private static int[] drawAt100(DrawContext ctx, int x, int y, TackleboxSettings.HudAlign align, String title, List<Row> rows) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = tr.getWidth(Foe.text(title));
        for (Row r : rows) {
            w = Math.max(w, width(tr, r));
        }
        w += 16;
        Foe.title(ctx, x, y, w, align, Foe.text(title));
        for (int i = 0; i < rows.size(); i++) {
            Row r = rows.get(i);
            int ry = y + 20 + i * 11;
            Text l = text(r.label(), r.labelParts()), v = text(r.value(), r.valueParts());
            int lx = x + 8, vx = x + w - 8 - tr.getWidth(v); // left keeps the values in a column on the right
            if (align != TackleboxSettings.HudAlign.LEFT) {
                lx = align.x(x + 8, w - 16, width(tr, r));
                vx = lx + tr.getWidth(l) + 12;
            }
            ctx.drawTextWithShadow(tr, l, lx, ry, r.labelColor() != 0 ? r.labelColor() : Foe.DIM);
            ctx.drawTextWithShadow(tr, v, vx, ry, r.valueColor() != 0 ? r.valueColor() : Foe.TEXT);
        }
        return new int[]{x, y, w, 25 + rows.size() * 11};
    }

    private static int width(TextRenderer tr, Row r) {
        return tr.getWidth(text(r.label(), r.labelParts())) + (r.value().isEmpty() ? 0 : 12 + tr.getWidth(text(r.value(), r.valueParts())));
    }

    private static Text text(String plain, List<Part> parts) {
        if (parts.isEmpty()) {
            return Foe.text(plain);
        }
        MutableText out = Text.empty();
        for (Part p : parts) {
            MutableText t = Foe.text(p.text());
            out.append(p.color() != 0 ? t.withColor(p.color() & 0xFFFFFF) : t);
        }
        return out;
    }
}
