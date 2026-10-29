package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.tacklebox.TackleboxSettings;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.landofif.tacklebox.chat.ChatBar;
import dev.landofif.tacklebox.chat.ChatBoxes;
import dev.landofif.tacklebox.chat.ChatPeek;
import dev.landofif.tacklebox.chat.ChatTimes;
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
    private static boolean stacking, rewriting;

    /** The line without the time stamp, so stacking compares only the words. */
    private static String bare(String s, TackleboxSettings st) {
        return st.chatTimeMode > 0 ? ChatTimes.bare(s) : s;
    }
    private static String lastPlain = "", lastShown = "";
    private static int lastCount;

    /** Vanilla keeps 100 lines, here 2000 (ctrl + f searches them). */
    @org.spongepowered.asm.mixin.injection.ModifyConstant(method = {"addVisibleMessage", "addMessage(Lnet/minecraft/client/gui/hud/ChatHudLine;)V"}, constant = @org.spongepowered.asm.mixin.injection.Constant(intValue = 100))
    private int tacklebox$history(int max) {
        return 2000;
    }

    /** The up arrow history keeps plain messages too, after a restart and a reconnect (chat/SentHistory). */
    @Inject(method = "<init>", at = @At("RETURN"))
    private void tacklebox$loadSent(CallbackInfo ci) {
        dev.landofif.tacklebox.chat.SentHistory.load(((ChatHud) (Object) this).getMessageHistory());
    }

    @Inject(method = "clear", at = @At("RETURN"))
    private void tacklebox$reloadSent(boolean clearHistory, CallbackInfo ci) {
        if (clearHistory) {
            dev.landofif.tacklebox.chat.SentHistory.load(((ChatHud) (Object) this).getMessageHistory());
        }
    }

    @Inject(method = "addToMessageHistory", at = @At("RETURN"))
    private void tacklebox$saveSent(String message, CallbackInfo ci) {
        dev.landofif.tacklebox.chat.SentHistory.save(((ChatHud) (Object) this).getMessageHistory());
    }

    /** Lines a chat box takes (dms, crew chat) skip the main chat, see chat/ChatBoxes. */
    @Inject(method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V",
            at = @At("HEAD"), cancellable = true)
    private void tacklebox$chatBoxes(Text message, @Nullable MessageSignatureData signature, @Nullable MessageIndicator indicator, CallbackInfo ci) {
        TackleboxSettings st = TackleboxSettings.get();
        if (!rewriting) {
            Text out = ChatTimes.rewrite(message, st == null ? 0 : st.chatTimeMode, System.currentTimeMillis());
            if (st != null) {
                out = dev.landofif.tacklebox.chat.ChatHighlight.apply(out, net.minecraft.client.MinecraftClient.getInstance().getSession().getUsername(),
                        st.chatHighlightName, st.chatHighlights);
            }
            if (out != message) {
                rewriting = true;
                try {
                    ((ChatHud) (Object) this).addMessage(out, signature, indicator);
                } finally {
                    rewriting = false;
                }
                ci.cancel();
                return;
            }
        }
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
        if (st != null && st.chatStack) {
            ChatHudAccessor acc = (ChatHudAccessor) this;
            String plain = bare(message.getString(), st);
            var lines = acc.tacklebox$messages();
            if (!plain.isBlank() && plain.equals(lastPlain) && !lines.isEmpty()
                    && bare(lines.getFirst().content().getString(), st).equals(lastShown)) {
                lines.removeFirst();
                acc.tacklebox$refresh();
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
    private void tacklebox$scrollBegin(ChatHud.Backend backend, int h, int tick, boolean expanded, CallbackInfo ci) {
        ChatBar.begin((ChatHud) (Object) this);
    }

    @Inject(method = RENDER, at = @At("RETURN"))
    private void tacklebox$scrollEnd(ChatHud.Backend backend, int h, int tick, boolean expanded, CallbackInfo ci) {
        ChatBar.end();
    }

    /**
     * The peek key (chat/ChatPeek) draws the hud chat like open chat: all lines, none faded, the
     * scroll bar. Only the flag passed on changes, so the hud backend (no hover, no clicks) stays.
     */
    @WrapOperation(method = "render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;IIIZZ)V",
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;" + RENDER))
    private void tacklebox$peekRender(ChatHud hud, ChatHud.Backend backend, int h, int tick, boolean focused, Operation<Void> original) {
        original.call(hud, backend, h, tick, ChatPeek.focused(focused));
    }

    /** While peeking: the open height, and a new line keeps the place you scrolled to. */
    @WrapOperation(method = {"getHeight()I", "addVisibleMessage"},
            at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/ChatHud;isChatFocused()Z"))
    private boolean tacklebox$peekFocused(ChatHud hud, Operation<Boolean> original) {
        return ChatPeek.focused(original.call(hud));
    }

    /** One more line while the lines are between two positions, so the top does not show a gap. */
    @ModifyReturnValue(method = "getVisibleLineCount", at = @At("RETURN"))
    private int tacklebox$extraLine(int count) {
        return ChatBar.extra ? count + 1 : count;
    }

    /** vanilla's scroll bar is the second and third fill of the render: ours replaces the first, the second is dropped */
    @WrapOperation(method = RENDER, at = @At(value = "INVOKE", target = FILL, ordinal = 1))
    private void tacklebox$bar(ChatHud.Backend backend, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        ChatBar.draw(backend, x1, y1);
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE", target = FILL, ordinal = 2))
    private void tacklebox$noThumb(ChatHud.Backend backend, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    }

    /** ctrl + f search: messages that do not match never become visible lines (refresh() rebuilds them when it ends). */
    @Inject(method = "addVisibleMessage", at = @At("HEAD"), cancellable = true)
    private void tacklebox$searchHide(net.minecraft.client.gui.hud.ChatHudLine line, CallbackInfo ci) {
        if (dev.landofif.tacklebox.chat.ChatSearch.hides(line.content())) {
            ci.cancel();
        }
    }

    @org.spongepowered.asm.mixin.Unique
    private int tacklebox$before;

    /** A line added while you are scrolled up moves the target with it (vanilla), the shown position has to follow. */
    @Inject(method = "addVisibleMessage", at = @At("HEAD"))
    private void tacklebox$beforeAdd(net.minecraft.client.gui.hud.ChatHudLine line, CallbackInfo ci) {
        tacklebox$before = ((ChatHudAccessor) this).tacklebox$scrolled();
    }

    @Inject(method = "addVisibleMessage", at = @At("RETURN"))
    private void tacklebox$afterAdd(net.minecraft.client.gui.hud.ChatHudLine line, CallbackInfo ci) {
        ChatBar.added(tacklebox$before, ((ChatHudAccessor) this).tacklebox$scrolled());
    }

    @WrapOperation(method = RENDER, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/ChatHud$Backend;updatePose(Ljava/util/function/Consumer;)V"))
    private void tacklebox$moveChat(ChatHud.Backend backend, Consumer<Matrix3x2f> transformer, Operation<Void> original) {
        TackleboxSettings s = TackleboxSettings.get();
        final int dx;
        final int dy;
        if (s == null) {
            dx = 0;
            dy = 0;
        } else {
            var window = net.minecraft.client.MinecraftClient.getInstance().getWindow();
            int sw = window.getScaledWidth();
            s.chatPos.measure(sw, window.getScaledHeight(), ChatBar.width(), 0);
            dx = s.chatPos.left(sw);
            dy = s.chatY;
        }
        original.call(backend, (Consumer<Matrix3x2f>) pose -> {
            pose.translate(dx, dy);
            transformer.accept(pose);
            ChatBar.pose.set(pose);
            pose.translate(0, ChatBar.shift); // the easing: lines slide down to where they are
        });
    }
}
