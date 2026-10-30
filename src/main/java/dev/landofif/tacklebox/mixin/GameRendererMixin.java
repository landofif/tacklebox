package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import dev.landofif.tacklebox.menu.ChestScale;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.GameRenderer;
import net.minecraft.client.render.RenderTickCounter;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The hud keeps the games gui scale while a server chest is open at its own (menu/ChestScale). Fabric draws hud
 * elements added last (tacklebox's huds, trigger titles) with the subtitles, which the game draws after the screen.
 */
@Mixin(GameRenderer.class)
abstract class GameRendererMixin {
    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;render(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/client/render/RenderTickCounter;)V"))
    private void tacklebox$hudScale(InGameHud hud, DrawContext context, RenderTickCounter counter, Operation<Void> original) {
        ChestScale.drawHud(MinecraftClient.getInstance(), context, () -> original.call(hud, context, counter));
    }

    @WrapOperation(method = "render", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/hud/InGameHud;renderDeferredSubtitles()V"))
    private void tacklebox$lastHudScale(InGameHud hud, Operation<Void> original, @Local DrawContext context) {
        ChestScale.drawHud(MinecraftClient.getInstance(), context, () -> original.call(hud));
    }
}
