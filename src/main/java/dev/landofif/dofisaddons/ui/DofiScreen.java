package dev.landofif.dofisaddons.ui;

import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Base for the mod's screens: a flat system24 background instead of Minecraft's blur and dirt, and the
 * whole screen framed as one panel with the title set into its top border. While one of these is open,
 * the widget mixins draw buttons, sliders and text fields in the same style.
 */
public abstract class DofiScreen extends Screen {
    private static final int MARGIN = 4;

    protected DofiScreen(Text title) {
        super(title);
    }

    /** False for screens that must show the game behind them (e.g. moving HUDs). */
    protected boolean framed() {
        return true;
    }

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        if (!framed()) {
            return;
        }
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        ctx.fill(0, 0, w, h, 0xE6000000 | (S24.BG_3 & 0xFFFFFF));
        S24.panel(ctx, MARGIN, MARGIN + S24.LABEL_RISE, w - MARGIN * 2, h - MARGIN * 2 - S24.LABEL_RISE,
                Mono.text(title.getString().toLowerCase(java.util.Locale.ROOT)));
    }
}
