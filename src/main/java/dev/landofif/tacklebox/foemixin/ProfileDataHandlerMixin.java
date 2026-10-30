package dev.landofif.tacklebox.foemixin;

import dev.landofif.tacklebox.compat.FoeCatch;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Every catch of yours as foecollab sees it, see {@link FoeCatch}. By name, {@code require = 0}, see {@link BoxRendererMixin}. */
@Mixin(targets = "io.github.foecollab.handler.ProfileDataHandler")
abstract class ProfileDataHandlerMixin {
    @Inject(method = "updateStatsOnCatch(Lio/github/foecollab/FOMC/Types/Fish;)V", at = @At("HEAD"), remap = false, require = 0)
    private void tacklebox$catch(@Coerce Object fish, CallbackInfo ci) {
        FoeCatch.onCatch(fish);
    }
}
