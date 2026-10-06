package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.hud.LevelInfo;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Watches screen titles (FishOnMC shows the caught fish's icon as the title) and hides the
 * vanilla level number when the level HUD replaces it.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {

    @Inject(method = "setTitle", at = @At("HEAD"))
    private void dofisaddons$onTitle(Text title, CallbackInfo ci) {
        StatsTracker.onTitle(title);
        dev.landofif.dofisaddons.display.BobberTimer.onTitle(title);
    }

    /** In 1.21.11 the level number is drawn by Bar.drawExperienceLevel inside renderMainHud. */
    // a wrap, not a redirect: foecollab redirects the same call, and two redirects on one call crash
    @WrapOperation(method = "renderMainHud", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"))
    private void dofisaddons$levelNumber(DrawContext context, TextRenderer textRenderer, int level, Operation<Void> original) {
        if (!LevelInfo.hideVanilla()) {
            original.call(context, textRenderer, level);
        }
    }
}
