package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.display.SmoothCast;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Optional;

/** Smooth cast: drops the server's position updates for your own bobber while it is still flying. */
@Mixin(Entity.class)
abstract class EntityInterpolationMixin {
    @Inject(method = "updateTrackedPositionAndAngles(Ljava/util/Optional;Ljava/util/Optional;Ljava/util/Optional;)V",
            at = @At("HEAD"), cancellable = true)
    private void dofisaddons$smoothCast(Optional<?> pos, Optional<?> yaw, Optional<?> pitch, CallbackInfo ci) {
        if (SmoothCast.suppress((Entity) (Object) this)) {
            ci.cancel();
        }
    }
}
