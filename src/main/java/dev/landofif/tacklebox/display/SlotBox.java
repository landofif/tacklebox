package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Hypr;
import net.minecraft.client.gui.DrawContext;

/** The box behind a group of slots: a see-through fill and/or a 1 pixel outline, per {@link TackleboxSettings.BoxLook}. */
public final class SlotBox {
    private SlotBox() {}

    public static void draw(DrawContext ctx, int x1, int y1, int x2, int y2, TackleboxSettings.BoxLook look) {
        TackleboxSettings s = TackleboxSettings.get();
        if (look == TackleboxSettings.BoxLook.HYPR) { // a rounded pill filled with the accent gradient at the fill opacity, with the same gradient as its outline
            int w = x2 - x1, h = y2 - y1;
            Hypr.rrect(ctx, x1, y1, w, h, 5, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x1, w, s.boxOpacity / 100f), 8);
            Hypr.ring(ctx, x1, y1, w, h, 5, 1, Hypr.horizontal(Hypr.alpha(Hypr.acc1(), 0.55f), Hypr.alpha(Hypr.acc2(), 0.55f), x1, w, 1), 8);
            return;
        }
        if (look == TackleboxSettings.BoxLook.FILL || look == TackleboxSettings.BoxLook.BOTH) {
            ctx.fill(x1, y1, x2, y2, (s.boxOpacity * 255 / 100) << 24);
        }
        if (look == TackleboxSettings.BoxLook.OUTLINE || look == TackleboxSettings.BoxLook.BOTH) {
            int c = 0xFF000000 | s.boxOutlineColor;
            ctx.fill(x1, y1, x2, y1 + 1, c);
            ctx.fill(x1, y2 - 1, x2, y2, c);
            ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, c);
            ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, c);
        }
    }
}
