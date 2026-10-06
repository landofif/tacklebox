package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.compat.FoeRarity;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The hotbar: moved by an offset (set in foecollabs hud mover), optionally without its slot background, with rarity circles. */
@Mixin(InGameHud.class)
abstract class InGameHudHotbarMixin {
    @WrapMethod(method = "renderHotbar")
    private void dofisaddons$move(DrawContext ctx, RenderTickCounter tick, Operation<Void> original) {
        DofiSettings s = DofiSettings.get();
        ctx.getMatrices().pushMatrix();
        float k = s.hotbarScale / 100f;
        int cx = ctx.getScaledWindowWidth() / 2;
        int bottom = ctx.getScaledWindowHeight();
        ctx.getMatrices().translate(cx + s.hotbarX, bottom + s.hotbarY); // grows from the bottom middle
        ctx.getMatrices().scale(k, k);
        ctx.getMatrices().translate(-cx, -bottom);
        original.call(ctx, tick);
        ctx.getMatrices().popMatrix();
    }

    @WrapOperation(method = "renderHotbar", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void dofisaddons$background(DrawContext ctx, RenderPipeline pipeline, Identifier id, int x, int y, int w, int h, Operation<Void> original) {
        DofiSettings s = DofiSettings.get();
        String path = id.getPath();
        if ((path.equals("hud/hotbar") && !s.hotbarBackground) || (path.startsWith("hud/hotbar_offhand") && !s.hotbarOffhand)) {
            return;
        }
        if (path.startsWith("hud/hotbar_selection") && s.hotbarSelection != DofiSettings.SelectionStyle.FRAME) {
            int color = 0xFF000000 | s.hotbarSelectColor;
            double cx = x + w / 2.0, cy = y + h / 2.0;
            if (s.hotbarSelection == DofiSettings.SelectionStyle.ROUND) {
                double outer = 9.5, inner = 8;
                for (int row = (int) -outer; row <= outer; row++) {
                    double dy = row + 0.5;
                    double ho = Math.sqrt(Math.max(0, outer * outer - dy * dy));
                    double hi = Math.abs(dy) >= inner ? 0 : Math.sqrt(inner * inner - dy * dy);
                    int py = (int) Math.round(cy + dy - 0.5);
                    if (hi == 0) {
                        ctx.fill((int) Math.round(cx - ho), py, (int) Math.round(cx + ho), py + 1, color);
                    } else {
                        ctx.fill((int) Math.round(cx - ho), py, (int) Math.round(cx - hi), py + 1, color);
                        ctx.fill((int) Math.round(cx + hi), py, (int) Math.round(cx + ho), py + 1, color);
                    }
                }
            } else if (s.hotbarSelection == DofiSettings.SelectionStyle.SIMPLE) {
                ctx.fill((int) cx - 8, y + h - 3, (int) cx + 8, y + h - 1, color);
            }
            return;
        }
        original.call(ctx, pipeline, id, x, y, w, h);
    }

    @Inject(method = "renderHotbarItem", at = @At("HEAD"))
    private void dofisaddons$circle(DrawContext ctx, int x, int y, RenderTickCounter tick, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        // the first and last slot are always full, nothing special
        if (DofiSettings.get().rarityCircles && stack != player.getInventory().getStack(0) && stack != player.getInventory().getStack(8)) {
            FoeRarity.circle(ctx, x, y, stack);
        }
    }
}
