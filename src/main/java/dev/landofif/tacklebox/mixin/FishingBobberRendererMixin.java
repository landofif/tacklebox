package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.duck.BobberStateAccess;
import dev.landofif.tacklebox.display.BobberLooks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.command.RenderCommandQueue;
import net.minecraft.client.render.entity.FishingBobberEntityRenderer;
import net.minecraft.client.render.entity.state.FishingBobberEntityState;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Bobber skins and line colour, purely visual. Every hook is optional (require = 0): if a future
 * Minecraft changes this renderer, the looks just stop applying instead of crashing the game.
 */
@Mixin(FishingBobberEntityRenderer.class)
public class FishingBobberRendererMixin {
    private static final String RENDER = "render(Lnet/minecraft/client/render/entity/state/FishingBobberEntityState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;Lnet/minecraft/client/render/state/CameraRenderState;)V";

    @Inject(method = "updateRenderState(Lnet/minecraft/entity/projectile/FishingBobberEntity;Lnet/minecraft/client/render/entity/state/FishingBobberEntityState;F)V",
            at = @At("TAIL"), require = 0)
    private void tacklebox$markOwner(FishingBobberEntity bobber, FishingBobberEntityState state, float tickProgress, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ((BobberStateAccess) state).tacklebox$setMine(client.player != null && bobber.getPlayerOwner() == client.player);
    }

    @Inject(method = RENDER, at = @At("HEAD"), require = 0)
    private void tacklebox$begin(FishingBobberEntityState state, MatrixStack matrices, OrderedRenderCommandQueue queue,
                                   CameraRenderState camera, CallbackInfo ci) {
        BobberLooks.drawingMine = ((BobberStateAccess) state).tacklebox$isMine();
    }

    @Redirect(method = RENDER, require = 0, at = @At(value = "FIELD", opcode = Opcodes.GETSTATIC,
            target = "Lnet/minecraft/client/render/entity/FishingBobberEntityRenderer;LAYER:Lnet/minecraft/client/render/RenderLayer;"))
    private RenderLayer tacklebox$skin() {
        return BobberLooks.bobberLayer();
    }

    @Redirect(method = RENDER, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/OrderedRenderCommandQueue;submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V"))
    private void tacklebox$line(OrderedRenderCommandQueue queue, MatrixStack matrices, RenderLayer layer, OrderedRenderCommandQueue.Custom custom) {
        queue.submitCustom(matrices, layer, BobberLooks.wrap(layer, custom));
    }

    @Redirect(method = RENDER, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/render/command/RenderCommandQueue;submitCustom(Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/RenderLayer;Lnet/minecraft/client/render/command/OrderedRenderCommandQueue$Custom;)V"))
    private void tacklebox$lineBatched(RenderCommandQueue queue, MatrixStack matrices, RenderLayer layer, OrderedRenderCommandQueue.Custom custom) {
        queue.submitCustom(matrices, layer, BobberLooks.wrap(layer, custom));
    }
}
