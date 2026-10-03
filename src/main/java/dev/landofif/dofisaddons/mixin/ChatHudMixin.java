package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.chat.ChatBoxes;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.gui.hud.ChatHud;
import org.joml.Matrix3x2f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Consumer;

/**
 * Sends lines that a chat box takes to it instead (chat/ChatBoxes), and moves the chat box by the
 * offset set in the move screen. Drawing, hover tooltips and clicks all go
 * through this one pose, so links and hover text keep working where the chat is.
 */
@Mixin(ChatHud.class)
abstract class ChatHudMixin {
    /** Lines a chat box takes (dms, crew chat) skip the main chat, see chat/ChatBoxes. */
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), cancellable = true)
    private void dofisaddons$chatBoxes(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo ci) {
        if (ChatBoxes.route(message, indicator)) {
            ci.cancel();
        }
    }

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
