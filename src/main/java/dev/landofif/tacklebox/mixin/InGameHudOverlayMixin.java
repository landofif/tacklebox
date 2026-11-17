package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.Holograms;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Every action bar message passes through setOverlayMessage, whichever packet brought it. Hides the
 * "+66xp" reward line and clears the minigame frame it would replace.
 */
@Mixin(InGameHud.class)
abstract class InGameHudOverlayMixin {
    @Shadow
    private @Nullable Text overlayMessage;
    @Shadow
    private int overlayRemaining;
    @Shadow
    private @Nullable Text title;
    @Shadow
    private @Nullable Text subtitle;
    @Shadow
    private int titleRemainTicks;

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void tacklebox$overlay(Text message, boolean tinted, CallbackInfo ci) {
        String raw = message == null ? "" : message.getString();
        dev.landofif.tacklebox.display.BiteCountdown.noteActionBar(!raw.isEmpty(), System.currentTimeMillis());
        dev.landofif.tacklebox.input.FishKey.onActionBar(message, System.currentTimeMillis());
        if (raw.isEmpty()) {
            return;
        }
        dev.landofif.tacklebox.hud.XpEstimate.onPopup(raw);
        if (TackleboxSettings.get().hideXpPopups && Holograms.isXpPopup(raw)) {
            this.overlayMessage = Text.empty();
            this.overlayRemaining = 0;
            ci.cancel();
        }
    }

    @Inject(method = "setTitle", at = @At("HEAD"), cancellable = true)
    private void tacklebox$title(Text title, CallbackInfo ci) {
        dev.landofif.tacklebox.display.BiteFocus.onTitle(title);
        dev.landofif.tacklebox.display.CatchWatch.onTitle(title);
        dev.landofif.tacklebox.input.FishKey.onTitle(title == null ? "" : title.getString(), System.currentTimeMillis());
        if (dev.landofif.tacklebox.display.BobberTimer.onTitle(title)) {
            ci.cancel();
        }
    }

    /**
     * The catch title (display/CatchTitle) hides the server's own after it was set, not by cancelling: foecollab reads its
     * catches from setTitle and setSubtitle too, and a cancel at the head could run before its hooks.
     */
    @Inject(method = "setTitle", at = @At("TAIL"))
    private void tacklebox$catchTitle(Text shown, CallbackInfo ci) {
        if (dev.landofif.tacklebox.display.CatchTitle.onTitle(shown, System.currentTimeMillis())) {
            this.title = null;
            this.subtitle = null;
            this.titleRemainTicks = 0;
        }
    }

    @Inject(method = "setSubtitle", at = @At("TAIL"))
    private void tacklebox$catchSubtitle(Text shown, CallbackInfo ci) {
        dev.landofif.tacklebox.display.CatchWatch.onSubtitle(shown == null ? null : shown.getString(), System.currentTimeMillis());
        if (dev.landofif.tacklebox.display.CatchTitle.onSubtitle(shown, System.currentTimeMillis())) {
            this.subtitle = null;
        }
    }
}
