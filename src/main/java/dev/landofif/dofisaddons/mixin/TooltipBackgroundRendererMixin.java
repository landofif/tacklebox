package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipBackgroundRenderer;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Tooltips: a flat bg-4 box with a 1px border. Only the default style: items with their own tooltip
 * style (the server can set one per item, e.g. for rarity) keep it.
 */
@Mixin(TooltipBackgroundRenderer.class)
abstract class TooltipBackgroundRendererMixin {
    @Inject(method = "render", at = @At("HEAD"), cancellable = true)
    private static void dofisaddons$flatTooltip(DrawContext ctx, int x, int y, int width, int height, Identifier texture, CallbackInfo ci) {
        if (texture == null && S24.everywhere()) {
            int bx = x - 4;
            int by = y - 4;
            int bw = width + 8;
            int bh = height + 8;
            ctx.fill(bx, by, bx + bw, by + bh, S24.alpha(0xF0, S24.BG_4));
            S24.outline(ctx, bx, by, bw, bh, S24.BORDER);
            ci.cancel();
        }
    }
}
