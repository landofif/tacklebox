package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.compat.FoeRarity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * A see-through circle in the items rarity colour behind it, drawn where foecollab draws its slot
 * backgrounds (before the item). By name, {@code require = 0}, see {@link BoxRendererMixin}.
 */
@Mixin(targets = "io.github.foecollab.handler.ItemMarkerHandler")
abstract class ItemMarkerHandlerMixin {
    @Inject(method = "renderItemMarkerBackground(Lnet/minecraft/class_332;Lnet/minecraft/class_1799;II)V", at = @At("HEAD"), remap = false, require = 0)
    private void dofisaddons$circle(DrawContext ctx, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (!DofiSettings.get().rarityCircles) {
            return;
        }
        int rgb = FoeRarity.color(stack);
        if (rgb < 0) {
            return;
        }
        int argb = 0x66 << 24 | rgb;
        for (int row = 0; row < 16; row++) {
            double dy = row + 0.5 - 8;
            int half = (int) Math.round(Math.sqrt(64 - dy * dy));
            ctx.fill(x + 8 - half, y + row, x + 8 + half, y + row + 1, argb);
        }
    }
}
