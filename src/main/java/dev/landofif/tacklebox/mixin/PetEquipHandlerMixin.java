package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.compat.FoePet;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives foecollab its equipped pet back when its login scan missed it, see {@link FoePet}. By name, {@code require = 0}, see {@link BoxRendererMixin}. */
@Mixin(targets = "io.github.foecollab.handler.PetEquipHandler")
abstract class PetEquipHandlerMixin {
    @Inject(method = "tick", at = @At("TAIL"), remap = false, require = 0)
    private void tacklebox$remember(MinecraftClient client, CallbackInfo ci) {
        FoePet.restore(this, client);
    }
}
