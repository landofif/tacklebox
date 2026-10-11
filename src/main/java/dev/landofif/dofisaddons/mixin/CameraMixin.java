package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.display.SneakHeight;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** How far your own first person camera dips while sneaking. Only the camera changes, not the hitbox or the pose the server sees. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @WrapOperation(method = "updateEyeHeight", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getStandingEyeHeight()F"))
    private float dofisaddons$sneakHeight(Entity entity, Operation<Float> original) {
        double height = DofiSettings.get().sneakHeight;
        if (height < SneakHeight.STANDING && entity == MinecraftClient.getInstance().player && entity.isInSneakingPose()) {
            return SneakHeight.eyeHeight(height);
        }
        return original.call(entity);
    }
}
