package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.display.Holograms;
import dev.landofif.dofisaddons.stats.StatsTracker;
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

    @Inject(method = "setOverlayMessage", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$overlay(Text message, boolean tinted, CallbackInfo ci) {
        if (message == null) {
            return;
        }
        String raw = message.getString();
        if (raw.isEmpty()) {
            return;
        }
        dev.landofif.dofisaddons.hud.XpEstimate.onPopup(raw);
        if (StatsTracker.settings().hideXpPopups && Holograms.isXpPopup(raw)) {
            this.overlayMessage = Text.empty();
            this.overlayRemaining = 0;
            ci.cancel();
        }
    }
}
