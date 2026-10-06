package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.DofiSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Swing speed for your own arm animation. Only how it looks changes, nothing is sent to the server. */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {
    @Inject(method = "getHandSwingDuration", at = @At("RETURN"), cancellable = true, require = 0)
    private void dofisaddons$swingDuration(CallbackInfoReturnable<Integer> cir) {
        double slow = DofiSettings.get().swingSlowness;
        if (slow != 1.0 && (Object) this == MinecraftClient.getInstance().player) {
            cir.setReturnValue(Math.max(1, (int) Math.round(cir.getReturnValue() * slow)));
        }
    }
}
