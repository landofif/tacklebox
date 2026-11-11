package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.input.FishKey;
import net.minecraft.client.input.Input;
import net.minecraft.client.input.KeyboardInput;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The fish key held in the minigame counts as sneak (input/FishKey), whatever the sneak key is bound or toggled to. */
@Mixin(KeyboardInput.class)
abstract class KeyboardInputMixin extends Input {
    @Inject(method = "tick", at = @At("TAIL"))
    private void tacklebox$fishKey(CallbackInfo ci) {
        if (FishKey.sneaking()) {
            this.playerInput = FishKey.withSneak(this.playerInput);
        }
    }
}
