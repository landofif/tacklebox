package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.compat.FoeRarity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
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
    /** Set while the slot being drawn never gets a circle: the first and last hotbar slot and the helmet slot. */
    private static boolean tacklebox$skip;

    @Inject(method = "renderItemMarkerBackground(Lnet/minecraft/class_332;Lnet/minecraft/class_1735;)V", at = @At("HEAD"), remap = false, require = 0)
    private void tacklebox$plainSlot(DrawContext ctx, Slot slot, CallbackInfo ci) {
        int i = slot.getIndex();
        tacklebox$skip = slot.inventory instanceof PlayerInventory && (i == 0 || i == 8 || i == 39); // always full, nothing special
    }

    @Inject(method = "renderItemMarkerBackground(Lnet/minecraft/class_332;Lnet/minecraft/class_1799;II)V", at = @At("HEAD"), remap = false, require = 0)
    private void tacklebox$circle(DrawContext ctx, ItemStack stack, int x, int y, CallbackInfo ci) {
        if (!TackleboxSettings.get().rarityCircles || tacklebox$skip) {
            return;
        }
        FoeRarity.circle(ctx, x, y, stack);
    }
}
