package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.display.WordSwap;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Word swaps: the text every draw and width call gets goes through {@link WordSwap} first. */
@Mixin(TextRenderer.class)
abstract class TextRendererMixin {
    private static final String ORDERED = "Lnet/minecraft/text/OrderedText;";

    @ModifyVariable(method = "draw(" + ORDERED + "FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V", at = @At("HEAD"), argsOnly = true)
    private OrderedText tacklebox$draw(OrderedText text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "prepare(" + ORDERED + "FFIZZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;", at = @At("HEAD"), argsOnly = true)
    private OrderedText tacklebox$prepare(OrderedText text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "getWidth(" + ORDERED + ")I", at = @At("HEAD"), argsOnly = true)
    private OrderedText tacklebox$width(OrderedText text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V", at = @At("HEAD"), argsOnly = true)
    private String tacklebox$drawString(String text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;", at = @At("HEAD"), argsOnly = true)
    private String tacklebox$prepareString(String text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String tacklebox$widthString(String text) {
        return WordSwap.swap(text);
    }
}
