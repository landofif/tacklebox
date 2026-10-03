package dev.landofif.dofisaddons.mixin;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.compat.FoeLook;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Only acts while foecollab's huds or screens draw (see compat/FoeLook): panels, buttons, title gaps, no theme frames. */
@Mixin(DrawContext.class)
abstract class DrawContextMixin {
    @Inject(method = "fill(IIIII)V", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$foePanel(int x1, int y1, int x2, int y2, int color, CallbackInfo ci) {
        if (FoeLook.active() && FoeLook.fill((DrawContext) (Object) this, x1, y1, x2, y2, color)) {
            ci.cancel();
        } else if (FoeLook.inScreen() && FoeLook.screenFill((DrawContext) (Object) this, x1, y1, x2, y2, color)) {
            ci.cancel();
        }
    }

    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V", at = @At("HEAD"))
    private void dofisaddons$foeTitle(TextRenderer tr, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        if (FoeLook.active()) {
            FoeLook.text((DrawContext) (Object) this, tr, text, x, y);
        }
    }

    @Inject(method = "drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V",
            at = @At("HEAD"), cancellable = true)
    private void dofisaddons$foeFrame(RenderPipeline pipeline, Identifier sprite, int x, int y, int w, int h, int color, CallbackInfo ci) {
        if ((FoeLook.active() || FoeLook.inScreen()) && FoeLook.skipTexture(sprite)) {
            ci.cancel();
        }
    }
}
