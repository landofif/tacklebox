package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.landofif.tacklebox.display.TotemPop;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Scales the floating item (totem pop) by the trigger's size setting, and gives the mod's own pops a shorter spin as they leave. */
@Mixin(InGameOverlayRenderer.class)
abstract class InGameOverlayRendererMixin {
    @Shadow
    private ItemStack floatingItem;
    @Shadow
    private int floatingItemTimer;

    @WrapOperation(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void tacklebox$scale(MatrixStack stack, float x, float y, float z, Operation<Void> original) {
        float s = TotemPop.scale;
        original.call(stack, x * s, y * s, z * s);
    }

    /** The first rotationDegrees is the Y spin (900 * |sin|); the X and Z wobble after it stay vanilla. */
    @WrapOperation(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/util/math/RotationAxis;rotationDegrees(F)Lorg/joml/Quaternionf;", ordinal = 0))
    private Quaternionf tacklebox$spin(RotationAxis axis, float degrees, Operation<Quaternionf> original, @Local(argsOnly = true) float tickProgress) {
        return original.call(axis, TotemPop.spin(floatingItem, floatingItemTimer, tickProgress, degrees));
    }
}
