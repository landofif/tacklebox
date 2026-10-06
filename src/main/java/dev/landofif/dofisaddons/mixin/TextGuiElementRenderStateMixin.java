package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import org.joml.Matrix3x2fc;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Text that other mods draw scaled up (foecollabs title) would use the glyph variant for scale 1 and look
 * pixelated. While a gui text is prepared, {@link Mono#drawScale} holds its matrix scale so
 * {@link Mono#everywhere} picks a sharper variant.
 */
@Mixin(TextGuiElementRenderState.class)
abstract class TextGuiElementRenderStateMixin {
    @Shadow
    @Final
    public Matrix3x2fc matrix;

    @Inject(method = "prepare", at = @At("HEAD"))
    private void dofisaddons$scaleIn(CallbackInfoReturnable<?> cir) {
        Mono.drawScale = (float) Math.sqrt(matrix.m00() * matrix.m00() + matrix.m01() * matrix.m01());
    }

    @Inject(method = "prepare", at = @At("RETURN"))
    private void dofisaddons$scaleOut(CallbackInfoReturnable<?> cir) {
        Mono.drawScale = 1f;
    }
}
