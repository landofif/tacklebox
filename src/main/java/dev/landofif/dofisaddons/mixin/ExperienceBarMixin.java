package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.hud.LevelInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.bar.ExperienceBar;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Hides the vanilla XP bar on FishOnMC (it shows your fishing level), when the setting is on. */
@Mixin(ExperienceBar.class)
public class ExperienceBarMixin {
    @Inject(method = "renderBar", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$hideBar(DrawContext context, RenderTickCounter tickCounter, CallbackInfo ci) {
        if (LevelInfo.hideVanilla()) {
            ci.cancel();
        }
    }
}
