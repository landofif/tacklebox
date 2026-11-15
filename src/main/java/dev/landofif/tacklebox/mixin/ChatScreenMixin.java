package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.chat.ChatBoxes;
import dev.landofif.tacklebox.chat.ChatCopy;
import dev.landofif.tacklebox.chat.ChatFilter;
import dev.landofif.tacklebox.chat.ChatSearch;
import dev.landofif.tacklebox.chat.ChatTimes;
import dev.landofif.tacklebox.screen.ChatFilterScreen;
import dev.landofif.tacklebox.trigger.Matcher;
import dev.landofif.tacklebox.trigger.Trigger;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With chat open: right click a message to copy it, shift + right click to start a hide rule for it.
 * The separate chat boxes scroll, show hover text and take clicks here too.
 * Only the clipboard and the mods own screens are touched; nothing is sent.
 */
@Mixin(ChatScreen.class)
abstract class ChatScreenMixin extends Screen {
    private ChatScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    protected abstract boolean handleClickEvent(Style style, boolean insert);

    /** Chat boxes: links and click to reply work in them like in the chat box. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tacklebox$boxClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0 || ChatSearch.active() || ChatBoxes.at(click.x(), click.y()) == null) {
            return;
        }
        Style style = ChatBoxes.styleAt(click.x(), click.y());
        if (style != null && handleClickEvent(style, client.isShiftPressed())) {
            cir.setReturnValue(true);
        }
    }

    /** The scroll bars (vanilla chat and the boxes) can be dragged. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tacklebox$bars(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() == 0 && (ChatBoxes.pressBar(click.x(), click.y()) || dev.landofif.tacklebox.chat.ChatBar.press(click.x(), click.y()))) {
            cir.setReturnValue(true);
        }
    }

    /** ctrl + f search: left click on a message leaves the search and scrolls to it (after the scroll bars, before links). */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tacklebox$searchJump(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0 || !ChatSearch.active()) {
            return;
        }
        boolean jumped = ChatBoxes.at(click.x(), click.y()) != null
                ? ChatBoxes.jumpTo(click.x(), click.y())
                : ChatCopy.jumpTo(click.x(), click.y());
        if (jumped) {
            chatField.setText("");
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("HEAD"))
    private void tacklebox$dragBars(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        boolean held = org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getHandle(), org.lwjgl.glfw.GLFW.GLFW_MOUSE_BUTTON_LEFT) == org.lwjgl.glfw.GLFW.GLFW_PRESS;
        dev.landofif.tacklebox.chat.ChatBar.tick(mouseX, mouseY, held);
        ChatBoxes.dragBar(mouseY, held);
    }

    @Shadow
    protected net.minecraft.client.gui.widget.TextFieldWidget chatField;

    /** Escape skips the command suggestions (which would only hide themselves), so one press closes chat. */
    @WrapOperation(method = "keyPressed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ChatInputSuggestor;keyPressed(Lnet/minecraft/client/input/KeyInput;)Z"))
    private boolean tacklebox$escapeCloses(net.minecraft.client.gui.screen.ChatInputSuggestor suggestor, net.minecraft.client.input.KeyInput input, Operation<Boolean> original) {
        return dev.landofif.tacklebox.chat.ChatKeys.toSuggestions(input) && original.call(suggestor, input);
    }

    /** Tab right after "here" puts in your location and instance, see chat/ChatKeys. */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void tacklebox$here(net.minecraft.client.input.KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (!input.isTab() || input.hasShift() || ChatSearch.active()) {
            return;
        }
        dev.landofif.tacklebox.chat.ChatKeys.Edit edit = dev.landofif.tacklebox.chat.ChatKeys.here(chatField.getText(), chatField.getCursor());
        if (edit != null) {
            chatField.setText(edit.text());
            chatField.setCursor(edit.cursor(), false);
            cir.setReturnValue(true);
        }
    }

    /** ctrl + f turns the chat box into a search: only messages containing what you type stay visible. */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void tacklebox$search(net.minecraft.client.input.KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_F && input.hasCtrl()) {
            if (!ChatSearch.toggle(chatField.getText())) {
                chatField.setText("");
            }
            cir.setReturnValue(true);
        } else if (ChatSearch.active() && (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_ENTER || input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_KP_ENTER)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "onChatFieldUpdate", at = @At("HEAD"))
    private void tacklebox$searchTyped(String text, CallbackInfo ci) {
        ChatSearch.typed(text);
    }

    @Inject(method = "removed", at = @At("HEAD"))
    private void tacklebox$searchLeave(CallbackInfo ci) {
        ChatSearch.leave();
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void tacklebox$searchHint(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ChatSearch.active()) {
            ctx.drawText(textRenderer, Foe.text("search · ctrl f to leave").withColor(Foe.DIM & 0xFFFFFF), 4, height - 26, 0xFFFFFFFF, false);
        }
    }

    /** Vanilla chat resets its own scroll when the screen closes; the boxes (and a send that keeps the screen open) follow here. */
    @Inject(method = {"removed", "sendMessage"}, at = @At("HEAD"))
    private void tacklebox$toBottom(CallbackInfo ci) {
        ChatBoxes.scrollToBottom();
        client.inGameHud.getChatHud().resetScroll();
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void tacklebox$boxScroll(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (ChatBoxes.scroll(mouseX, mouseY, vertical)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void tacklebox$boxHover(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ChatBoxes.at(mouseX, mouseY) != null) {
            Style style = ChatBoxes.styleAt(mouseX, mouseY);
            String time = ChatBoxes.timeAt(mouseX, mouseY);
            if (style != null && style.getHoverEvent() != null) {
                ctx.drawHoverEvent(textRenderer, style, mouseX, mouseY);
            } else if (time != null) {
                ctx.drawTooltip(textRenderer, Foe.text(time), mouseX, mouseY);
            }
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void tacklebox$copyLine(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 1 || !TackleboxSettings.get().chatCopy) {
            return;
        }
        Text message = ChatBoxes.at(click.x(), click.y()) != null
                ? ChatBoxes.messageAt(click.x(), click.y())
                : ChatCopy.messageAt(click.x(), click.y());
        if (message == null) {
            return;
        }
        String plain = ChatCopy.plain(message);
        if (client.isShiftPressed()) {
            ChatFilter rule = new ChatFilter(Matcher.normalize(message.getString()), Trigger.MatchMode.EXACT);
            client.setScreen(new ChatFilterScreen(null, rule));
        } else {
            client.keyboard.setClipboard(plain);
            client.player.sendMessage(Text.literal("tacklebox » ").withColor(Foe.TITLE & 0xFFFFFF).append(Text.literal("message copied").withColor(Foe.DIM & 0xFFFFFF)), false);
        }
        cir.setReturnValue(true);
    }

    /** The bar you type in has no background. */
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V"))
    private void tacklebox$clearInput(DrawContext ctx, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
    }

    /** A light background behind the whole message under the mouse, so it is clear which lines belong together. */
    @Inject(method = "render", at = @At("TAIL"))
    private void tacklebox$hoverMessage(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ChatBoxes.at(mouseX, mouseY) != null) {
            return;
        }
        float[] b = ChatCopy.boundsAt(mouseX, mouseY);
        if (b != null) {
            ctx.fill(Math.round(b[0]), Math.round(b[1]), Math.round(b[2]), Math.round(b[3]), 0x30FFFFFF);
        }
        Text message = ChatCopy.messageAt(mouseX, mouseY);
        long at = message == null || ChatTimes.hasHover(message) ? -1 : ChatTimes.at(message);
        if (at >= 0) {
            ctx.drawTooltip(textRenderer, Foe.text(ChatTimes.format(at)), mouseX, mouseY);
        }
    }
}
