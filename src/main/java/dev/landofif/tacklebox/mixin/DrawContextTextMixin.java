package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.hud.FoeCover;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reports every drawn text to {@link FoeCover}, which measures foecollab's huds with it. */
@Mixin(DrawContext.class)
abstract class DrawContextTextMixin {
    @Inject(method = "drawText(Lnet/minecraft/client/font/TextRenderer;Lnet/minecraft/text/OrderedText;IIIZ)V", at = @At("HEAD"))
    private void tacklebox$measure(TextRenderer tr, OrderedText text, int x, int y, int color, boolean shadow, CallbackInfo ci) {
        FoeCover.text((DrawContext) (Object) this, tr, text, x, y);
    }
}
