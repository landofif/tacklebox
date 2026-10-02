package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.chat.ChatCopy;
import dev.landofif.dofisaddons.chat.ChatFilter;
import dev.landofif.dofisaddons.screen.ChatFilterScreen;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.trigger.Matcher;
import dev.landofif.dofisaddons.trigger.Trigger;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * With chat open: right click a message to copy it, shift + right click to start a hide rule for it.
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

    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$copyLine(Click click, boolean doubled, CallbackInfoReturnable<Boolean> cir) {
        if (click.button() != 1 || !StatsTracker.settings().chatCopy) {
            return;
        }
        Text message = ChatCopy.messageAt(click.x(), click.y());
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
        Text label = Mono.text("copied");
        int w = textRenderer.getWidth(label) + 8;
        int x = dofisaddons$flashX + 8;
        int y = dofisaddons$flashY - 14;
        ctx.fill(x, y, x + w, y + 13, S24.BG_4);
        S24.outline(ctx, x, y, w, 13, S24.ACCENT);
        ctx.drawText(textRenderer, label, x + 4, y + 3, S24.TEXT_1, false);
    }
}
