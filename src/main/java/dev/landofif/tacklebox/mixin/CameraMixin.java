package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.SneakHeight;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.Camera;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityPose;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** How far your own first person camera dips while sneaking. Only the camera changes, not the hitbox or the pose the server sees. */
@Mixin(Camera.class)
abstract class CameraMixin {
    @WrapOperation(method = "updateEyeHeight", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/entity/Entity;getStandingEyeHeight()F"))
    private float tacklebox$sneakHeight(Entity entity, Operation<Float> original) {
        float vanilla = original.call(entity);
        if (entity != MinecraftClient.getInstance().player) return vanilla;
        // Not isInSneakingPose(): ClientPlayerEntity overrides it with an input flag one tick behind the pose and the eye
        // height, which let vanilla's 1.27 through for one tick on every press.
        return SneakHeight.target(TackleboxSettings.get().sneakHeight, entity.isInPose(EntityPose.CROUCHING), vanilla);
    }
}
