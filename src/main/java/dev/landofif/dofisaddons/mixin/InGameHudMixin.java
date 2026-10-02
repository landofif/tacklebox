package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.hud.LevelInfo;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
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
    @Redirect(method = "renderMainHud", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/hud/bar/Bar;drawExperienceLevel(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/font/TextRenderer;I)V"))
    private void dofisaddons$levelNumber(DrawContext context, TextRenderer textRenderer, int level) {
        if (!LevelInfo.hideVanilla()) {
            Bar.drawExperienceLevel(context, textRenderer, level);
        }
    }
}
