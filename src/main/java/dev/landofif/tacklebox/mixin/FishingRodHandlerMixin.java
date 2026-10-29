package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.display.OtherBobbers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Foecollab spawns a client side item display with the bait under every bobber, other players too.
 * With "hide other bobbers" on we skip its tick for their bobbers, so no display is made at all.
 * By name, {@code require = 0}, see {@link BoxRendererMixin}.
 */
@Mixin(targets = "io.github.foecollab.handler.FishingRodHandler")
abstract class FishingRodHandlerMixin {
    @Inject(method = "tickEntities", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private void tacklebox$skipOthers(Entity entity, MinecraftClient client, CallbackInfo ci) {
        if (OtherBobbers.hidden(entity)) {
            ci.cancel();
        }
    }
}
