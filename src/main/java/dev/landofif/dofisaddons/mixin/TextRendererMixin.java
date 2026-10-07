package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.display.WordSwap;
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
    private OrderedText dofisaddons$draw(OrderedText text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "prepare(" + ORDERED + "FFIZZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;", at = @At("HEAD"), argsOnly = true)
    private OrderedText dofisaddons$prepare(OrderedText text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "getWidth(" + ORDERED + ")I", at = @At("HEAD"), argsOnly = true)
    private OrderedText dofisaddons$width(OrderedText text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "draw(Ljava/lang/String;FFIZLorg/joml/Matrix4f;Lnet/minecraft/client/render/VertexConsumerProvider;Lnet/minecraft/client/font/TextRenderer$TextLayerType;II)V", at = @At("HEAD"), argsOnly = true)
    private String dofisaddons$drawString(String text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "prepare(Ljava/lang/String;FFIZI)Lnet/minecraft/client/font/TextRenderer$GlyphDrawable;", at = @At("HEAD"), argsOnly = true)
    private String dofisaddons$prepareString(String text) {
        return WordSwap.swap(text);
    }

    @ModifyVariable(method = "getWidth(Ljava/lang/String;)I", at = @At("HEAD"), argsOnly = true)
    private String dofisaddons$widthString(String text) {
        return WordSwap.swap(text);
    }
}
