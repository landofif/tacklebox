package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.compat.FoeLureSort;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Foecollabs bait sorting helper also marks lures that match exactly. By name, {@code require = 0}, see {@link BoxRendererMixin}. */
@Mixin(targets = "io.github.foecollab.handler.BaitSortingHelperHandler")
abstract class BaitSortingHelperMixin {
    @Inject(method = "renderItemMarker", at = @At("HEAD"), remap = false, require = 0)
    private void dofisaddons$lures(DrawContext ctx, ItemStack stack, int x, int y, CallbackInfo ci) {
        FoeLureSort.render(ctx, stack, x, y);
    }
}
