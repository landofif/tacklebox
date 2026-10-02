package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.duck.BobberCatchAccess;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Still bobber": after each client tick, a bobber resting in water is put back on the surface, which
 * removes the random idle bobbing (it also overrides bobbing streamed from the server). A bite is left
 * alone: {@code caughtFish} is synced from the server, so the dip still shows. Ported from fishutils.
 */
@Mixin(FishingBobberEntity.class)
abstract class FishingBobberEntityMixin implements BobberCatchAccess {
    @Shadow
    private boolean caughtFish;

    @Shadow
    private @Nullable Entity hookedEntity;

    @Override
    public boolean dofisaddons$caughtFish() {
        return caughtFish;
    }

    @Inject(method = "tick", at = @At("TAIL"))
    private void dofisaddons$holdStill(CallbackInfo ci) {
        if (caughtFish || hookedEntity != null || !StatsTracker.settings().stillBobber) {
            return;
        }
        FishingBobberEntity self = (FishingBobberEntity) (Object) this;
        World world = self.getEntityWorld();
        if (!world.isClient()) {
            return;
        }
        BlockPos pos = self.getBlockPos();
        FluidState fluid = world.getFluidState(pos);
        if (!fluid.isIn(FluidTags.WATER)) {
            return;
        }
        double surface = pos.getY() + fluid.getHeight(world, pos);
        // only the idle bob near the surface, never a big jump
        if (Math.abs(self.getY() - surface) <= 0.6) {
            self.setPosition(self.getX(), surface, self.getZ());
        }
    }
}
