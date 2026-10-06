package dev.landofif.dofisaddons.ui;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.client.gui.render.state.SimpleGuiElementRenderState;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.texture.TextureSetup;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix3x2f;

/** A ring (or part of one) drawn as geometry, for progress rings: starts at the top and runs clockwise. */
public record Arc(Matrix3x2f pose, float cx, float cy, float r, float t, float from, float to, int color,
                  @Nullable ScreenRect scissorArea, @Nullable ScreenRect bounds) implements SimpleGuiElementRenderState {
    /** {@code from} and {@code to} are fractions of a full turn, 0 = top. */
    public static void draw(DrawContext ctx, float cx, float cy, float r, float t, float from, float to, int color) {
        if (to <= from) {
            return;
        }
        Matrix3x2f pose = new Matrix3x2f(ctx.getMatrices());
        ScreenRect scissor = ctx.scissorStack.peekLast();
        ScreenRect bounds = new ScreenRect((int) (cx - r), (int) (cy - r), (int) (r * 2) + 1, (int) (r * 2) + 1).transformEachVertex(pose);
        ctx.state.addSimpleElement(new Arc(pose, cx, cy, r, t, from, to, color, scissor,
                scissor != null ? scissor.intersection(bounds) : bounds));
    }

    @Override
    public void setupVertices(VertexConsumer v) {
        int steps = Math.max(2, (int) Math.ceil((to - from) * 64));
        for (int i = 0; i < steps; i++) {
            double a0 = (from + (to - from) * i / steps) * Math.PI * 2 - Math.PI / 2;
            double a1 = (from + (to - from) * (i + 1) / steps) * Math.PI * 2 - Math.PI / 2;
            v.vertex(pose, cx + r * (float) Math.cos(a0), cy + r * (float) Math.sin(a0)).color(color);
            v.vertex(pose, cx + (r - t) * (float) Math.cos(a0), cy + (r - t) * (float) Math.sin(a0)).color(color);
            v.vertex(pose, cx + (r - t) * (float) Math.cos(a1), cy + (r - t) * (float) Math.sin(a1)).color(color);
            v.vertex(pose, cx + r * (float) Math.cos(a1), cy + r * (float) Math.sin(a1)).color(color);
        }
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
