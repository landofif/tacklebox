package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.chat.ChatBoxes;
import dev.landofif.dofisaddons.chat.ChatCopy;
import dev.landofif.dofisaddons.chat.ChatFilter;
import dev.landofif.dofisaddons.screen.ChatFilterScreen;
import dev.landofif.dofisaddons.trigger.Matcher;
import dev.landofif.dofisaddons.trigger.Trigger;
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
    @Unique
    private long dofisaddons$flashUntil;
    @Unique
    private int dofisaddons$flashX;
    @Unique
    private int dofisaddons$flashY;

    private ChatScreenMixin(Text title) {
        super(title);
    }

    @Shadow
    protected abstract boolean handleClickEvent(Style style, boolean insert);

    /** Chat boxes: links and click to reply work in them like in the chat box. */
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$boxClick(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 0 || ChatBoxes.at(click.x(), click.y()) == null) {
            return;
        }
        Style style = ChatBoxes.styleAt(click.x(), click.y());
        if (style != null && handleClickEvent(style, client.isShiftPressed())) {
            cir.setReturnValue(true);
        }
    }

    /** ctrl + f searches the whole chat history. */
    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$search(net.minecraft.client.input.KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        if (input.key() == org.lwjgl.glfw.GLFW.GLFW_KEY_F && input.hasCtrl()) {
            client.setScreen(new dev.landofif.dofisaddons.screen.ChatSearchScreen(this));
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "mouseScrolled", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$boxScroll(double mouseX, double mouseY, double horizontal, double vertical, CallbackInfoReturnable<Boolean> cir) {
        if (ChatBoxes.scroll(mouseX, mouseY, vertical)) {
            cir.setReturnValue(true);
        }
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dofisaddons$boxHover(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (ChatBoxes.at(mouseX, mouseY) != null) {
            ctx.drawHoverEvent(textRenderer, ChatBoxes.styleAt(mouseX, mouseY), mouseX, mouseY);
        }
    }

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$copyLine(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 1 || !DofiSettings.get().chatCopy) {
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
            dofisaddons$flashUntil = System.currentTimeMillis() + 1200;
            dofisaddons$flashX = (int) click.x();
            dofisaddons$flashY = (int) click.y();
        }
        cir.setReturnValue(true);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void dofisaddons$copiedTag(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (System.currentTimeMillis() > dofisaddons$flashUntil) {
            return;
        }
        Text label = Foe.text("copied");
        int w = textRenderer.getWidth(label) + 12;
        int x = dofisaddons$flashX + 8;
        int y = dofisaddons$flashY - 20;
        Foe.box(ctx, x, y, w, 18, 100);
        ctx.drawTextWithShadow(textRenderer, label, x + 6, y + 5, Foe.TEXT);
    }
}
