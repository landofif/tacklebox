package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.util.Identifier;

import java.util.EnumMap;
import java.util.Map;

/** Client-side bobber skins and your own fishing line colour. Nobody else sees these. */
public final class BobberLooks {
    private static final Identifier VANILLA = Identifier.ofVanilla("textures/entity/fishing_hook.png");
    private static final Map<TackleboxSettings.BobberSkin, RenderLayer> LAYERS = new EnumMap<>(TackleboxSettings.BobberSkin.class);

    /** Set while a bobber is being drawn: is it yours? */
    public static boolean drawingMine;
    private static RenderLayer lastBobberLayer;

    private BobberLooks() {}

    /** The layer (texture) for the bobber being drawn right now. */
    public static RenderLayer bobberLayer() {
        TackleboxSettings s = TackleboxSettings.get();
        TackleboxSettings.BobberSkin skin = drawingMine || s.skinEveryone ? s.bobberSkin : TackleboxSettings.BobberSkin.DEFAULT;
        RenderLayer layer = LAYERS.computeIfAbsent(skin, k -> RenderLayers.entityCutout(k.texture == null
                ? VANILLA
                : Identifier.of("tacklebox", "textures/entity/bobber/" + k.texture + ".png")));
        lastBobberLayer = layer;
        return layer;
    }

    /** Wraps the fishing line drawing so your line uses your colour. The bobber itself is left alone. */
    public static OrderedRenderCommandQueue.Custom wrap(RenderLayer layer, OrderedRenderCommandQueue.Custom custom) {
        TackleboxSettings s = TackleboxSettings.get();
        if (!drawingMine || !s.customLineColor || layer == lastBobberLayer) {
            return custom;
        }
        int argb = 0xFF000000 | s.lineColor;
        return (entry, consumer) -> custom.render(entry, new Recolor(consumer, argb));
    }

    /** Passes everything through but replaces every colour with one fixed colour (keeping alpha). */
    private record Recolor(VertexConsumer inner, int argb) implements VertexConsumer {
        @Override
        public VertexConsumer vertex(float x, float y, float z) {
            inner.vertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer color(int red, int green, int blue, int alpha) {
            inner.color((argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF, alpha);
            return this;
        }

        @Override
        public VertexConsumer color(int color) {
            return color((color >> 16) & 0xFF, (color >> 8) & 0xFF, color & 0xFF, (color >>> 24) & 0xFF);
        }

        @Override
        public VertexConsumer color(float red, float green, float blue, float alpha) {
            return color(0, 0, 0, (int) (alpha * 255));
        }

        @Override
        public VertexConsumer texture(float u, float v) {
            inner.texture(u, v);
            return this;
        }

        @Override
        public VertexConsumer overlay(int u, int v) {
            inner.overlay(u, v);
            return this;
        }

        @Override
        public VertexConsumer light(int u, int v) {
            inner.light(u, v);
            return this;
        }

        @Override
        public VertexConsumer normal(float x, float y, float z) {
            inner.normal(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer lineWidth(float width) {
            inner.lineWidth(width);
            return this;
        }
    }
}
