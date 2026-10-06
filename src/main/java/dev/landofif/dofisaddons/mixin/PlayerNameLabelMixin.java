package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.DofiSettings;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** "Hide all nametags" also hides the vanilla name label over players (the server tags are entities). */
@Mixin(PlayerEntityRenderer.class)
abstract class PlayerNameLabelMixin {
    @Inject(method = "renderLabelIfPresent", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$hideLabel(PlayerEntityRenderState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                       CameraRenderState camera, CallbackInfo ci) {
        if (DofiSettings.get().hideAllNametags) {
            ci.cancel();
        }
    }
}
