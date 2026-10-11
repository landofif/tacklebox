package dev.landofif.tacklebox.mixin;

import net.minecraft.client.toast.Toast;
import net.minecraft.client.toast.ToastManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** No toasts at all (recipe unlocks, advancements, tutorial hints, system messages). */
@Mixin(ToastManager.class)
abstract class ToastManagerMixin {
    @Inject(method = "add", at = @At("HEAD"), cancellable = true)
    private void tacklebox$noToasts(Toast toast, CallbackInfo ci) {
        ci.cancel();
    }
}
