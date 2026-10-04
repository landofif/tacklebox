package dev.landofif.dofisaddons.ui;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import org.joml.Matrix3x2f;
import org.jetbrains.annotations.Nullable;

/**
 * A rounded box ({@code t <= 0}) or a ring {@code t} wide, for the caelestia style. Drawn as geometry with a one
 * screen pixel alpha edge, so the corners are smooth at any gui scale instead of stepping by gui pixels.
 */
record Round(Matrix3x2f pose, float x, float y, float w, float h, float r, float t, float px, int color,
             @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds) implements SimpleGuiElementRenderState {
    private static final int SEGMENTS = 8;

    static void draw(DrawContext ctx, int x, int y, int w, int h, int r, float t, int color) {
        Matrix3x2f pose = new Matrix3x2f(ctx.getMatrices());
        float scale = MinecraftClient.getInstance().getWindow().getScaleFactor() * (float) Math.hypot(pose.m00(), pose.m01());
        ScreenRect scissor = ctx.scissorStack.peekLast();
        ScreenRect bounds = new ScreenRect(x, y, w, h).transformEachVertex(pose);
        ctx.state.addSimpleElement(new Round(pose, x, y, w, h, r, t, 1f / scale, color, scissor,
                scissor != null ? scissor.intersection(bounds) : bounds));
    }

    @Override
    public void setupVertices(VertexConsumer v) {
        int clear = color & 0xFFFFFF;
        float half = px / 2;
        band(v, -half, half, clear, color);
        if (t <= 0) {
            band(v, half, Math.min(w, h) / 2, color, color);
        } else {
            band(v, half, t - half, color, color);
            band(v, t - half, t + half, color, clear);
        }
    }

    /** The strip between the outline pulled in by {@code a} and by {@code b}, shaded {@code ca} to {@code cb}. */
    private void band(VertexConsumer v, float a, float b, int ca, int cb) {
        float[] p = contour(a), q = contour(b);
        for (int i = 0; i < p.length; i += 2) {
            int j = (i + 2) % p.length;
            v.vertex(pose, p[i], p[i + 1]).color(ca);
            v.vertex(pose, q[i], q[i + 1]).color(cb);
            v.vertex(pose, q[j], q[j + 1]).color(cb);
            v.vertex(pose, p[j], p[j + 1]).color(ca);
        }
    }

    /** Points clockwise round the box pulled in by {@code d}, each corner keeping its centre so insets line up. */
    private float[] contour(float d) {
        float cx = x + w / 2, cy = y + h / 2;
        float x0 = Math.min(x + d, cx), x1 = Math.max(x + w - d, cx);
        float y0 = Math.min(y + d, cy), y1 = Math.max(y + h - d, cy);
        float rr = Math.max(0, Math.min(r - d, Math.min(x1 - x0, y1 - y0) / 2));
        float[][] centres = {{x0 + rr, y0 + rr}, {x1 - rr, y0 + rr}, {x1 - rr, y1 - rr}, {x0 + rr, y1 - rr}};
        float[] out = new float[4 * (SEGMENTS + 1) * 2];
        int k = 0;
        for (int c = 0; c < 4; c++) {
            for (int s = 0; s <= SEGMENTS; s++) {
                double angle = Math.PI * (1 + (c + (double) s / SEGMENTS) / 2);
                out[k++] = centres[c][0] + rr * (float) Math.cos(angle);
                out[k++] = centres[c][1] + rr * (float) Math.sin(angle);
            }
        }
        return out;
    }

    @Override
    public RenderPipeline pipeline() {
        return RenderPipelines.GUI;
    }

    @Override
    public TextureSetup textureSetup() {
        return TextureSetup.empty();
    }
}
