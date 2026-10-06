package dev.landofif.dofisaddons.ui;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

/**
 * Base for the mod's screens: the normal game background with no frame around it, the title in the top
 * left corner. Contents lay out inside {@link #vx} .. {@link #vh}.
 */
public abstract class DofiScreen extends Screen {
    private static final int MARGIN = 4;

    protected DofiScreen(Text title) {
        super(title);
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

    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.renderBackground(ctx, mouseX, mouseY, delta);
        Foe.title(ctx, vx() + MARGIN, vy() + MARGIN, title);
    }
}
