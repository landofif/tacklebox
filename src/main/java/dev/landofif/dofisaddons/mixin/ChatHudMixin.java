package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.DofiSettings;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.landofif.dofisaddons.chat.ChatBar;
import dev.landofif.dofisaddons.chat.ChatBoxes;
import dev.landofif.dofisaddons.chat.ChatTimes;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.MessageSignatureData;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
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
    private static boolean stacking;
    private static String lastPlain = "", lastShown = "";
    private static int lastCount;

    /** Vanilla keeps 100 lines, here 2000 (ctrl + f searches them). */
    @org.spongepowered.asm.mixin.injection.ModifyConstant(method = {"addVisibleMessage", "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"}, constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 100))
    private int dofisaddons$history(int max) {
        return 2000;
    }

    /** Lines a chat box takes (dms, crew chat) skip the main chat, see chat/ChatBoxes. */
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), cancellable = true)
    private void dofisaddons$chatBoxes(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo ci) {
        ChatTimes.record(message);
        if (stacking) {
            return;
        }
        if (ChatBoxes.route(message, indicator)) {
            ci.cancel();
        }
        if (ci.isCancelled()) {
            return;
        }
        DofiSettings st = DofiSettings.get();
        if (st != null && st.chatStack) {
            ChatHudAccessor acc = (ChatHudAccessor) this;
            String plain = message.getString();
            var lines = acc.dofisaddons$messages();
            if (!plain.isBlank() && plain.equals(lastPlain) && !lines.isEmpty()
                    && lines.getFirst().content().getString().equals(lastShown)) {
                lines.removeFirst();
                acc.dofisaddons$refresh();
                lastCount++;
                lastShown = plain + " (x" + lastCount + ")";
                stacking = true;
                try {
                    ((ChatHud) (Object) this).addMessage(message.copy().append(Text.literal(" (x" + lastCount + ")").formatted(net.minecraft.util.Formatting.GRAY)), signature, indicator);
                } finally {
                    stacking = false;
                }
                ci.cancel();
                return;
            }
            lastPlain = plain;
            lastShown = plain;
            lastCount = 1;
        }
    }

    private static final String RENDER = "render(Lnet/minecraft/client/gui/hud/ChatHud$Backend;IIZ)V";
    private static final String FILL = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;fill(IIIII)V";

    /** Smooth scrolling and the thick scroll bar, see chat/ChatBar. */
    @Inject(method = RENDER, at = @At("HEAD"))
    private void dofisaddons$scrollBegin(ChatHud.Backend backend, int h, int tick, boolean expanded, CallbackInfo ci) {
        ChatBar.begin((ChatHud) (Object) this);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void dofisaddons$scrollEnd(ChatHud.Backend backend, int h, int tick, boolean expanded, CallbackInfo ci) {
        ChatBar.end();
    }

    /** One more line while the lines are between two positions, so the top does not show a gap. */
    @ModifyReturnValue(method = "getVisibleLineCount", at = @At("RETURN"))
    private int dofisaddons$extraLine(int count) {
        return ChatBar.extra ? count + 1 : count;
    }

    /** vanilla's scroll bar is the second and third fill of the render: ours replaces the first, the second is dropped */
    @WrapOperation(method = RENDER, at = @At(value = "INVOKE", target = FILL, ordinal = 1))
    private void dofisaddons$bar(ChatHud.Backend backend, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        ChatBar.draw(backend, x1, y1);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE", target = FILL, ordinal = 2))
    private void dofisaddons$noThumb(ChatHud.Backend backend, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    }

    @org.spongepowered.asm.mixin.Unique
    private int dofisaddons$before;

    /** A line added while you are scrolled up moves the target with it (vanilla), the shown position has to follow. */
    @Inject(method = "addVisibleMessage", at = @At("HEAD"))
    private void dofisaddons$beforeAdd(net.minecraft.client.gui.hud.ChatHudLine line, CallbackInfo ci) {
        dofisaddons$before = ((ChatHudAccessor) this).dofisaddons$scrolled();
    }

    @Inject(method = "addVisibleMessage", at = @At("RETURN"))
    private void dofisaddons$afterAdd(net.minecraft.client.gui.hud.ChatHudLine line, CallbackInfo ci) {
        ChatBar.added(dofisaddons$before, ((ChatHudAccessor) this).dofisaddons$scrolled());
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;updatePose(Ljava/util/function/Consumer;)V"))
    private void dofisaddons$moveChat(ChatHud.Backend backend, Consumer<Matrix3x2f> transformer, Operation<Void> original) {
        DofiSettings s = DofiSettings.get();
        int dx = s == null ? 0 : s.chatX;
        int dy = s == null ? 0 : s.chatY;
        original.call(backend, (Consumer<Matrix3x2f>) pose -> {
            pose.translate(dx, dy);
            transformer.accept(pose);
            ChatBar.pose.set(pose);
            pose.translate(0, ChatBar.shift); // the easing: lines slide down to where they are
        });
    }
}
