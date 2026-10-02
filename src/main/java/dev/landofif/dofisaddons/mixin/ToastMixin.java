package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.toast.AdvancementToast;
import net.minecraft.client.toast.RecipeToast;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.client.toast.TutorialToast;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Toasts (advancements, recipes, system messages, tutorial hints): a flat panel with an accent edge
 * instead of the stone frame. Recipe and tutorial toasts write in black and dark purple for the light
 * frame, so those colours are lifted to read on the dark panel.
 */
@Mixin({AdvancementToast.class, RecipeToast.class, SystemToast.class, TutorialToast.class})
abstract class ToastMixin {
    @WrapOperation(method = "draw", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void dofisaddons$flatToast(DrawContext ctx, RenderPipeline pipeline, Identifier texture, int x, int y, int w, int h,
                                       Operation<Void> original) {
        if (S24.everywhere() && texture.getPath().startsWith("toast/")) {
            ctx.fill(x, y, x + w, y + h, S24.alpha(0xF0, S24.BG_4));
            S24.outline(ctx, x, y, w, h, S24.BORDER);
            ctx.fill(x, y, x + 2, y + h, S24.ACCENT);
        } else {
            original.call(ctx, pipeline, texture, x, y, w, h);
        }
    }

    @WrapOperation(method = "draw", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/Text;IIIZ)V"))
    private void dofisaddons$readableText(DrawContext ctx, TextRenderer tr, Text text, int x, int y, int color, boolean shadow,
                                          Operation<Void> original) {
        original.call(ctx, tr, text, x, y, S24.everywhere() ? dofisaddons$lift(color) : color, shadow);
    }

    @WrapOperation(method = "draw", require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V"))
    private void dofisaddons$readableLines(DrawContext ctx, TextRenderer tr, net.minecraft.text.OrderedText text, int x, int y, int color,
                                           boolean shadow, Operation<Void> original) {
        original.call(ctx, tr, text, x, y, S24.everywhere() ? dofisaddons$lift(color) : color, shadow);
    }

    /** Dark text colours (made for the light frame) become accent or text-2; the alpha is kept. */
    private static int dofisaddons$lift(int color) {
        int rgb = color & 0xFFFFFF;
        int luma = ((rgb >> 16 & 0xFF) * 3 + (rgb >> 8 & 0xFF) * 6 + (rgb & 0xFF)) / 10;
        if (luma >= 0x60) {
            return color;
        }
        int lifted = rgb == 0 ? S24.TEXT_2 : S24.ACCENT;
        return (color & 0xFF000000) | (lifted & 0xFFFFFF);
    }
}
