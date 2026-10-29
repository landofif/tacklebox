package dev.landofif.tacklebox.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.function.IntConsumer;

/**
 * A colour you pick by dragging: a saturation/brightness square on the left, a hue bar on the right.
 * Works in HSV internally so the hue doesn't jump when you drag into grey or black.
 */
public class ColorPickerWidget extends ClickableWidget {
    private static final int HUE_W = 14;
    private static final int GAP = 6;

    private enum Part { NONE, SQUARE, HUE }

    private final IntConsumer onChange;
    private float hue;
    private float sat;
    private float val;
    private Part dragging = Part.NONE;

    public ColorPickerWidget(int x, int y, int width, int height, int rgb, IntConsumer onChange) {
        super(x, y, width, height, Text.empty());
        this.onChange = onChange;
        setRgb(rgb);
    }

    public int getRgb() {
        return hsvToRgb(hue, sat, val);
    }

    /** Sets the colour from outside (hex field, palette) without firing {@code onChange}. */
    public void setRgb(int rgb) {
        float r = (rgb >> 16 & 0xFF) / 255f;
        float g = (rgb >> 8 & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float d = max - min;
        val = max;
        sat = max == 0 ? 0 : d / max;
        if (d != 0) { // keep the old hue for greys, so the square doesn't reset to red
            float h;
            if (max == r) {
                h = ((g - b) / d) % 6;
            } else if (max == g) {
                h = (b - r) / d + 2;
            } else {
                h = (r - g) / d + 4;
            }
            hue = ((h / 6f) % 1f + 1f) % 1f;
        }
    }

    private int squareW() {
        return width - HUE_W - GAP;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        int sw = squareW();
        int hx = x + sw + GAP;

        // Saturation left to right, brightness top to bottom: one vertical gradient per column.
        for (int i = 0; i < sw; i++) {
            int top = 0xFF000000 | hsvToRgb(hue, i / (float) (sw - 1), 1f);
            ctx.fillGradient(x + i, y, x + i + 1, y + height, top, 0xFF000000);
        }
        for (int j = 0; j < height; j++) {
            ctx.fill(hx, y + j, hx + HUE_W, y + j + 1, 0xFF000000 | hsvToRgb(j / (float) (height - 1), 1f, 1f));
        }
        outline(ctx, x - 1, y - 1, sw + 2, height + 2, 0xFF000000);
        outline(ctx, hx - 1, y - 1, HUE_W + 2, height + 2, 0xFF000000);

        // Square cursor: a small ring, dark on light colours and light on dark ones.
        int cx = x + Math.round(sat * (sw - 1));
        int cy = y + Math.round((1 - val) * (height - 1));
        int ring = val > 0.6f && sat < 0.5f ? 0xFF000000 : 0xFFFFFFFF;
        outline(ctx, cx - 3, cy - 3, 7, 7, ring);
        // Hue cursor: a bar across the hue strip.
        int hy = y + Math.round(hue * (height - 1));
        ctx.fill(hx - 2, hy - 1, hx + HUE_W + 2, hy + 2, 0xFF000000);
        ctx.fill(hx - 1, hy, hx + HUE_W + 1, hy + 1, 0xFFFFFFFF);
    }

    private static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y, x + 1, y + h, color);
        ctx.fill(x + w - 1, y, x + w, y + h, color);
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        dragging = click.x() < getX() + squareW() + GAP / 2.0 ? Part.SQUARE : Part.HUE;
        pick(click.x(), click.y());
    }

    @Override
    protected void onDrag(Click click, double dx, double dy) {
        pick(click.x(), click.y());
    }

    @Override
    public void onRelease(Click click) {
        dragging = Part.NONE;
    }

    private void pick(double mx, double my) {
        float fy = MathHelper.clamp((float) ((my - getY()) / (height - 1)), 0f, 1f);
        if (dragging == Part.SQUARE) {
            sat = MathHelper.clamp((float) ((mx - getX()) / (squareW() - 1)), 0f, 1f);
            val = 1f - fy;
        } else if (dragging == Part.HUE) {
            hue = Math.min(fy, 0.999f); // 1.0 would wrap back to red at the bottom
        }
        onChange.accept(getRgb());
    }

    static int hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float hp = (h % 1f) * 6f;
        float x = c * (1 - Math.abs(hp % 2 - 1));
        float r, g, b;
        if (hp < 1) { r = c; g = x; b = 0; }
        else if (hp < 2) { r = x; g = c; b = 0; }
        else if (hp < 3) { r = 0; g = c; b = x; }
        else if (hp < 4) { r = 0; g = x; b = c; }
        else if (hp < 5) { r = x; g = 0; b = c; }
        else { r = c; g = 0; b = x; }
        float m = v - c;
        return Math.round((r + m) * 255) << 16 | Math.round((g + m) * 255) << 8 | Math.round((b + m) * 255);
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
