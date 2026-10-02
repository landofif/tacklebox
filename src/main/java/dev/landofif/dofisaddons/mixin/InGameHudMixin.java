package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.hud.LevelInfo;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.gui.hud.bar.Bar;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Watches screen titles (FishOnMC shows the caught fish's icon as the title) and hides the
 * vanilla level number when the level HUD replaces it. Also draws the hotbar as flat system24 boxes.
 */
@Mixin(InGameHud.class)
public class InGameHudMixin {
    private static final Identifier HOTBAR = Identifier.ofVanilla("hud/hotbar");
    private static final Identifier HOTBAR_SELECTION = Identifier.ofVanilla("hud/hotbar_selection");
    private static final Identifier OFFHAND_LEFT = Identifier.ofVanilla("hud/hotbar_offhand_left");
    private static final Identifier OFFHAND_RIGHT = Identifier.ofVanilla("hud/hotbar_offhand_right");

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

    /** Hotbar sprites as flat boxes: one panel split into nine cells, an accent border on the selected one. */
    @WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void dofisaddons$flatHotbar(DrawContext ctx, RenderPipeline pipeline, Identifier texture, int x, int y, int w, int h,
                                        Operation<Void> original) {
        if (!S24.hotbar()) {
            original.call(ctx, pipeline, texture, x, y, w, h);
        } else if (HOTBAR.equals(texture)) {
            ctx.fill(x, y, x + w, y + h, S24.alpha(0xC8, S24.BG_4));
            S24.outline(ctx, x, y, w, h, S24.BORDER);
            for (int i = 1; i < 9; i++) {
                ctx.fill(x + 1 + i * 20, y + 1, x + 2 + i * 20, y + h - 1, S24.BORDER);
            }
        } else if (HOTBAR_SELECTION.equals(texture)) {
            ctx.fill(x + 2, y + 2, x + 22, y + 22, S24.alpha(0x30, S24.ACCENT));
            S24.outline(ctx, x + 2, y + 2, 20, 20, S24.ACCENT);
        } else if (OFFHAND_LEFT.equals(texture) || OFFHAND_RIGHT.equals(texture)) {
            int bx = OFFHAND_LEFT.equals(texture) ? x : x + 7;
            ctx.fill(bx, y + 1, bx + 22, y + 23, S24.alpha(0xC8, S24.BG_4));
            S24.outline(ctx, bx, y + 1, 22, 22, S24.BORDER);
        } else {
            original.call(ctx, pipeline, texture, x, y, w, h);
        }
    }
}
