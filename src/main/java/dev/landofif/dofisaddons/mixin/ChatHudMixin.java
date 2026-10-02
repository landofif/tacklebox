package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.gui.hud.ChatHud;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/**
 * Moves the chat box by the offset set in the move screen. Drawing, hover tooltips and clicks all go
 * through this one pose, so links and hover text keep working where the chat is.
 */
@Mixin(ChatHud.class)
abstract class ChatHudMixin {
    @WrapOperation(method = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;updatePose(Ljava/util/function/Consumer;)V"))
    private void dofisaddons$moveChat(ChatHud.Backend backend, Consumer<Matrix3x2f> transformer, Operation<Void> original) {
        DofiSettings s = StatsTracker.settings();
        if (s == null || (s.chatX == 0 && s.chatY == 0)) {
            original.call(backend, transformer);
            return;
        }
        int dx = s.chatX;
        int dy = s.chatY;
        original.call(backend, (Consumer<Matrix3x2f>) pose -> {
            pose.translate(dx, dy);
            transformer.accept(pose);
        });
    }
}
