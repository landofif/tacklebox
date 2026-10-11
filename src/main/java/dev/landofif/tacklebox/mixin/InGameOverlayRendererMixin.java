package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.tacklebox.display.TotemPop;
import net.minecraft.client.gui.hud.InGameOverlayRenderer;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Scales the floating item (totem pop) by the trigger's size setting. */
@Mixin(InGameOverlayRenderer.class)
abstract class InGameOverlayRendererMixin {
    @WrapOperation(method = "renderFloatingItem", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/math/MatrixStack;scale(FFF)V"))
    private void tacklebox$scale(MatrixStack stack, float x, float y, float z, Operation<Void> original) {
        float s = TotemPop.scale;
        original.call(stack, x * s, y * s, z * s);
    }
}
