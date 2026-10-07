package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.DofiSettings;
import net.minecraft.client.gui.DrawContext;

/** The box behind a group of slots: a see-through fill and/or a 1 pixel outline, per {@link DofiSettings.BoxLook}. */
public final class SlotBox {
    private SlotBox() {}

    public static void draw(DrawContext ctx, int x1, int y1, int x2, int y2, DofiSettings.BoxLook look) {
        DofiSettings s = DofiSettings.get();
        if (look == DofiSettings.BoxLook.FILL || look == DofiSettings.BoxLook.BOTH) {
            ctx.fill(x1, y1, x2, y2, (s.boxOpacity * 255 / 100) << 24);
        }
        if (look == DofiSettings.BoxLook.OUTLINE || look == DofiSettings.BoxLook.BOTH) {
            int c = 0xFF000000 | s.boxOutlineColor;
            ctx.fill(x1, y1, x2, y1 + 1, c);
            ctx.fill(x1, y2 - 1, x2, y2, c);
            ctx.fill(x1, y1 + 1, x1 + 1, y2 - 1, c);
            ctx.fill(x2 - 1, y1 + 1, x2, y2 - 1, c);
        }
    }
}
