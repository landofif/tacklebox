package dev.landofif.tacklebox.foemixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.Constant;
import org.spongepowered.asm.mixin.injection.ModifyConstant;

/** The "s" after foecollabs bobber timer (1.3s) as a small capital, like the rest of the text. By name, {@code require = 0}. */
@Mixin(targets = "io.github.foecollab.screens.hud.BobberTimerHud")
abstract class BobberTimerHudMixin {
    @ModifyConstant(method = "timerText", constant = @Constant(stringValue = "%.1fs"), remap = false, require = 0)
    private static String tacklebox$smallS(String format) {
        return "%.1fꜱ";
    }
}
