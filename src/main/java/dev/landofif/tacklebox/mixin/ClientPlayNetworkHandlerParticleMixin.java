package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.display.BobberTimer;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.network.packet.s2c.play.ParticleS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Reads the fish's approach particles for the bite countdown. Never cancels a particle. */
@Mixin(ClientPlayNetworkHandler.class)
abstract class ClientPlayNetworkHandlerParticleMixin {
    @Inject(method = "onParticle", at = @At("HEAD"))
    private void tacklebox$particle(ParticleS2CPacket packet, CallbackInfo ci) {
        BobberTimer.onParticle(packet);
    }
}
