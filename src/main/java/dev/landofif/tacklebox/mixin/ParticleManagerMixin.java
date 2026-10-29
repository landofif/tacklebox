package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.display.ParticleSwitches;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleManager;
import net.minecraft.particle.ParticleEffect;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Drops particles of the types switched off on the particles screen (display/ParticleSwitches). */
@Mixin(ParticleManager.class)
abstract class ParticleManagerMixin {
    @Inject(method = "addParticle(Lnet/minecraft/particle/ParticleEffect;DDDDDD)Lnet/minecraft/client/particle/Particle;",
            at = @At("HEAD"), cancellable = true)
    private void tacklebox$switches(ParticleEffect effect, double x, double y, double z, double vx, double vy, double vz,
                                    CallbackInfoReturnable<Particle> cir) {
        if (ParticleSwitches.blocked(effect.getType())) {
            cir.setReturnValue(null);
        }
    }
}
