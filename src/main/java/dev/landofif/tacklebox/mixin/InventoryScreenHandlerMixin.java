package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.compat.FoeButtons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Foecollab builds each custom inventory button from its saved name, description and icon. Around that, {instance}
 * in those texts is swapped for the instance you are on, then put back so the saved button keeps the placeholder.
 * By name, {@code require = 0}, see {@link BoxRendererMixin}.
 */
@Mixin(targets = "io.github.foecollab.handler.InventoryScreenHandler")
abstract class InventoryScreenHandlerMixin {
    @Inject(method = "assembleModernButton", at = @At("HEAD"), remap = false, require = 0)
    private void tacklebox$fill(@Coerce Object button, int x, int y, int size, MinecraftClient client, CallbackInfoReturnable<ClickableWidget> ci) {
        FoeButtons.fill(button);
    }

    @Inject(method = "assembleModernButton", at = @At("RETURN"), remap = false, require = 0)
    private void tacklebox$restore(@Coerce Object button, int x, int y, int size, MinecraftClient client, CallbackInfoReturnable<ClickableWidget> ci) {
        FoeButtons.restore(button);
    }
}
