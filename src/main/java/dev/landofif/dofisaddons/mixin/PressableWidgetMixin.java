package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.ui.Anim;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Buttons: a flat system24 box instead of the stone texture (the mods screens, or everywhere if set).
 * The hover colour fades in and out, a press sends an accent ripple out from where you clicked, and a
 * selected tab's underline grows out from the middle.
 */
@Mixin(PressableWidget.class)
abstract class PressableWidgetMixin extends ClickableWidget {
    @Unique
    private static final long HOVER_MS = 110;
    @Unique
    private static final long PRESS_MS = 320;
    @Unique
    private static final long UNDERLINE_MS = 200;

    @Unique
    private float dofisaddons$hover;
    @Unique
    private long dofisaddons$lastFrame;
    @Unique
    private long dofisaddons$pressedAt;
    @Unique
    private float dofisaddons$pressX;
    @Unique
    private long dofisaddons$underlineAt;

    private PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "onClick", at = @At("HEAD"))
    private void dofisaddons$pressedByMouse(Click click, boolean doubled, CallbackInfo ci) {
        dofisaddons$pressedAt = Anim.now();
        dofisaddons$pressX = (float) (click.x() - getX());
    }

    @Inject(method = "keyPressed", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/widget/PressableWidget;onPress(Lnet/minecraft/client/input/AbstractInput;)V"))
    private void dofisaddons$pressedByKey(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
        dofisaddons$pressedAt = Anim.now();
        dofisaddons$pressX = getWidth() / 2f;
    }

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatButton(DrawContext ctx, CallbackInfo ci) {
        if (!S24.everywhere()) {
            return;
        }
        ci.cancel();
        int x = getX();
        int y = getY();
        int w = getWidth();
        int h = getHeight();
        long now = Anim.now();
        long elapsed = dofisaddons$lastFrame == 0 ? 0 : Math.min(100, now - dofisaddons$lastFrame);
        dofisaddons$lastFrame = now;
        dofisaddons$hover = Anim.approach(dofisaddons$hover, isSelected() && this.active ? 1f : 0f, elapsed, HOVER_MS);

        if (!this.active && !S24.active()) {
            S24.button(ctx, x, y, w, h, false, false); // a disabled vanilla button: dimmed, no motion
            return;
        }
        float hv = dofisaddons$hover;
        S24.rect(ctx, x, y, w, h, Anim.mix(S24.BG_2, S24.BG_1, hv), Anim.mix(S24.BORDER, S24.ACCENT, hv));

        // press: an accent band spreading out from the click, fading as it goes
        float p = Anim.progress(dofisaddons$pressedAt, PRESS_MS);
        if (p < 1f) {
            float e = Anim.easeOut(p);
            float reach = e * w;
            int left = (int) Math.max(x + 1, x + dofisaddons$pressX - reach);
            int right = (int) Math.min(x + w - 1, x + dofisaddons$pressX + reach);
            int alpha = (int) ((1f - p) * (1f - p) * 0x70);
            S24.fill(ctx, left, y + 1, right - left, h - 2, S24.alpha(alpha, S24.ACCENT));
            S24.outline(ctx, x, y, w, h, Anim.mix(Anim.mix(S24.BORDER, S24.ACCENT, hv), S24.ACCENT, 1f - p));
        }

        // in the mods screens an inactive button is the selected tab: its underline grows from the middle
        if (!this.active) {
            if (dofisaddons$underlineAt == 0) {
                dofisaddons$underlineAt = now;
            }
            float u = Anim.easeOut(Anim.progress(dofisaddons$underlineAt, UNDERLINE_MS));
            if (S24.round()) { // caelestia: the pill fills with accent instead
                S24.fill(ctx, x, y, w, h, S24.alpha((int) (0x60 * u), S24.ACCENT));
                return;
            }
            int half = (int) ((w - 2) / 2f * u);
            int mid = x + w / 2;
            ctx.fill(mid - half, y + h - 2, mid + half, y + h - 1, S24.ACCENT);
        } else {
            dofisaddons$underlineAt = 0;
        }
    }
}
