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
    /** Opening: the contents fade in and rise this far over INTRO_MS (the frame stays put). */
    private static final int INTRO_RISE = 8;
    private static final long INTRO_MS = 220;

    private long introAt;
    private float introFrom;

    protected DofiScreen(Text title) {
        super(title);
    }

    @Override
    public void onDisplayed() {
        super.onDisplayed();
        introAt = Anim.now();
        introFrom = 0f;
    }

    /** A shorter version of the opening animation, for switching tabs. */
    protected void replayIntro() {
        introAt = Anim.now();
        introFrom = 0.45f;
    }

    /** How far the contents should be shifted down right now (0 when settled). */
    public float introOffset() {
        return framed() && !S24.vanilla() ? (1f - introEase()) * INTRO_RISE : 0f;
    }

    private float introEase() {
        float t = introFrom + (1f - introFrom) * Anim.progress(introAt, INTRO_MS);
        return Anim.easeOut(t);
    }

    /**
     * Called after the contents are drawn (see ScreenMixin): fades them in by laying the panel colour over
     * the inside of the frame and lifting it away. Tooltips are drawn after this, so they never fade.
     */
    public void drawIntroCover(DrawContext ctx) {
        if (!framed() || S24.vanilla()) {
            return;
        }
        float e = introEase();
        if (e >= 1f) {
            return;
        }
        int x0 = vx() + MARGIN, y0 = vy() + MARGIN, x1 = vx() + vw() - MARGIN, y1 = vy() + vh() - MARGIN;
        int top = y0 + S24.LABEL_RISE + 1;
        ctx.fill(x0 + 1, top, x1 - 1, y1 - 1, S24.alpha(S24.scale((int) ((1f - e) * 255), dev.landofif.dofisaddons.stats.StatsTracker.settings().screenOpacity), S24.BG_4));
    }

    /** How much of the screen the frame takes, in percent (custom guis let the player shrink it). */
    protected int sizePct() {
        return 100;
    }

    /** The frame area: the whole screen at 100%, otherwise centred. Contents lay out inside it. */
    protected int vw() {
        return width * sizePct() / 100;
    }

    protected int vh() {
        return height * sizePct() / 100;
    }

    protected int vx() {
        return (width - vw()) / 2;
    }

    protected int vy() {
        return (height - vh()) / 2;
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
        if (S24.vanilla()) {
            super.renderBackground(ctx, mouseX, mouseY, delta); // minecraft style: the normal dirt and blur
            return;
        }
        int w = ctx.getScaledWindowWidth();
        int h = ctx.getScaledWindowHeight();
        int pct = dev.landofif.dofisaddons.stats.StatsTracker.settings().screenOpacity;
        ctx.fill(0, 0, w, h, S24.alpha(S24.scale(0xE6, pct), S24.BG_3));
        S24.panelOpacity = pct;
        S24.panel(ctx, vx() + MARGIN, vy() + MARGIN + S24.LABEL_RISE, vw() - MARGIN * 2, vh() - MARGIN * 2 - S24.LABEL_RISE,
                Mono.text(title.getString().toLowerCase(java.util.Locale.ROOT)));
        S24.panelOpacity = 100;
    }
}
