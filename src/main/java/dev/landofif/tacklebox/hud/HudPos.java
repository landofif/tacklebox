package dev.landofif.tacklebox.hud;

import java.util.function.BiFunction;

/**
 * Where a hud or chat box sits, kept as the distance to the nearest sides of the window instead of a fixed
 * spot, so it stays against the same edges when the window changes size (F11, split screen). On each axis it
 * is stuck to the side it is closer to: the left or right edge, the top or bottom edge. A hud stuck to the
 * right or bottom keeps its own far edge at that distance, so it stays put when its width changes too.
 * All numbers are gui pixels, hud scale included. Whatever the window does, a hud is pushed back inside it.
 */
public final class HudPos {
    /** The gap to the left edge, or to the right edge while {@link #right} (the hud's right edge then). */
    public int x;
    /** The gap to the top edge, or to the bottom edge while {@link #bottom}. */
    public int y;
    public boolean right;
    public boolean bottom;
    /** False for a spot saved before this existed: x and y are then a plain top left corner and the first draw sticks it to the nearest sides. */
    public boolean pinned;
    /** The size it had when it was last drawn; the far edges need it. */
    public transient int w;
    public transient int h;

    public HudPos() {}

    /** A spot that is a plain top left corner, pinned to the nearest sides on its first draw. */
    public HudPos(int left, int top) {
        x = left;
        y = top;
    }

    /** The left edge to draw at in a window {@code sw} wide. */
    public int left(int sw) {
        return Math.max(0, Math.min(Math.max(0, sw - w), right ? sw - x - w : x));
    }

    /** The top edge to draw at in a window {@code sh} high. */
    public int top(int sh) {
        return Math.max(0, Math.min(Math.max(0, sh - h), bottom ? sh - y - h : y));
    }

    /** Remembers how big it is, and pins a spot saved the old way to the sides it is nearest to. */
    public void measure(int sw, int sh, int width, int height) {
        if (pinned) {
            w = width;
            h = height;
        } else {
            moveTo(sw, sh, x, y, width, height);
        }
    }

    /** Puts it with its top left corner at {@code left, top} and sticks it to the nearest sides from there. */
    public void moveTo(int sw, int sh, int left, int top, int width, int height) {
        w = width;
        h = height;
        left = Math.max(0, Math.min(Math.max(0, sw - w), left));
        top = Math.max(0, Math.min(Math.max(0, sh - h), top));
        right = left + w / 2.0 > sw / 2.0;
        bottom = top + h / 2.0 > sh / 2.0;
        x = Math.max(0, right ? sw - left - w : left);
        y = Math.max(0, bottom ? sh - top - h : top);
        pinned = true;
    }

    /** Draws a hud where this puts it: {@code draw} gets the top left corner and returns the hud's {x, y, w, h}, or null when nothing shows. */
    public int[] draw(int sw, int sh, BiFunction<Integer, Integer, int[]> draw) {
        return draw(null, sw, sh, draw);
    }

    /** Like {@link #draw(int, int, BiFunction)}, and with a context the parts under a chat window are cut off (see {@link HudCover}). */
    public int[] draw(net.minecraft.client.gui.DrawContext ctx, int sw, int sh, BiFunction<Integer, Integer, int[]> draw) {
        int[][] out = new int[1][];
        int x = left(sw), y = top(sh);
        if (ctx == null) {
            out[0] = draw.apply(x, y);
        } else {
            HudCover.clipped(ctx, x, y, w, h, () -> {
                int[] r = draw.apply(x, y);
                if (out[0] == null) {
                    out[0] = r;
                }
            });
        }
        int[] r = out[0];
        if (r != null) {
            measure(sw, sh, r[2], r[3]);
        }
        return r;
    }
}
