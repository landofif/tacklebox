package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.compat.FoePet;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Gives foecollab its equipped pet back when its login scan missed it, see {@link FoePet}. By name, {@code require = 0}, see {@link BoxRendererMixin}. */
@Mixin(targets = "io.github.foecollab.handler.PetEquipHandler")
abstract class PetEquipHandlerMixin {
    @Inject(method = "tick(Lnet/minecraft/class_310;)V", at = @At("TAIL"), remap = false, require = 0)
    private void dofisaddons$remember(MinecraftClient client, CallbackInfo ci) {
        FoePet.restore(this, client);
    }
}
