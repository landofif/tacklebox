package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.compat.FoeDailyMissions;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

/** The daily mission hud's lines in the colours of the Daily Missions tooltip, with a header per section, see {@link FoeDailyMissions#styleLines}. By name, {@code require = 0}, see {@link BoxRendererMixin}. */
@Mixin(targets = "io.github.foecollab.handler.screens.hud.DailyQuestTrackerHudHandler")
abstract class DailyQuestTrackerHudHandlerMixin {
    @Inject(method = "buildQuestText", at = @At("RETURN"), remap = false, require = 0)
    private void dofisaddons$style(CallbackInfoReturnable<List<Object>> cir) {
        FoeDailyMissions.styleLines(cir.getReturnValue());
    }
}
