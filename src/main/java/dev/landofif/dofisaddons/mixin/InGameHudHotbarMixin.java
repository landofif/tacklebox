package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.compat.FoeRarity;
import dev.landofif.dofisaddons.display.SlotBox;
import dev.landofif.dofisaddons.display.HotbarSelection;
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
        int mid = ctx.getScaledWindowWidth() / 2;
        int pitch = s.hotbarPitch();
        if (path.equals("hud/hotbar") && s.hotbarLook != DofiSettings.BoxLook.DEFAULT) {
            if (s.hotbarLook != DofiSettings.BoxLook.OFF) { // our own box, as wide as the slots are spaced
                SlotBox.draw(ctx, mid - 4 * pitch - 11, y, mid + 4 * pitch + 11, y + h, s.hotbarLook);
            }
            return;
        }
        if (path.startsWith("hud/hotbar_offhand") && !s.hotbarOffhand) {
            return;
        }
        if (path.startsWith("hud/hotbar_selection") && s.hotbarSelection == DofiSettings.SelectionStyle.HYPR) {
            HotbarSelection.draw(ctx, (int) Math.round((x + w / 2.0 - (mid - 80)) / 20.0), mid, pitch, y + 1);
            return;
        }
        if (path.startsWith("hud/hotbar_selection")) { // the marker follows its slot when the slots are closer
            int slot = (int) Math.round((x + w / 2.0 - (mid - 80)) / 20.0);
            x += (slot - 4) * (pitch - 20);
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

    /** Slots closer together (or further apart) than vanilla's 20: each item moves toward the middle slot. */
    @WrapMethod(method = "renderHotbarItem")
    private void dofisaddons$pitch(DrawContext ctx, int x, int y, RenderTickCounter tick, PlayerEntity player, ItemStack stack, int seed, Operation<Void> original) {
        int pitch = DofiSettings.get().hotbarPitch();
        if (pitch == 20) {
            original.call(ctx, x, y, tick, player, stack, seed);
            return;
        }
        int slot = (int) Math.round((x - 3 - (ctx.getScaledWindowWidth() / 2 - 90)) / 20.0);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate((slot - 4) * (pitch - 20), 0);
        original.call(ctx, x, y, tick, player, stack, seed);
        ctx.getMatrices().popMatrix();
    }

    @Inject(method = "renderHotbarItem", at = @At("HEAD"))
    private void dofisaddons$circle(DrawContext ctx, int x, int y, RenderTickCounter tick, PlayerEntity player, ItemStack stack, int seed, CallbackInfo ci) {
        // the first and last slot are always full, nothing special
        if (DofiSettings.get().rarityCircles && stack != player.getInventory().getStack(0) && stack != player.getInventory().getStack(8)) {
            FoeRarity.circle(ctx, x, y, stack);
        }
    }
}
