package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Watches screen titles: FishOnMC shows the caught fish's icon as the title. Read-only. */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    @Inject(method = "setTitle", at = @At("HEAD"))
    private void dofisaddons$onTitle(Text title, CallbackInfo ci) {
        StatsTracker.onTitle(title);
        dev.landofif.dofisaddons.display.BobberTimer.onTitle(title);
    }
}
