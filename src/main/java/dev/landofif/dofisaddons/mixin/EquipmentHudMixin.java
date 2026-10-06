package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.compat.FoeRarity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Foecollabs equipment hud (the armour and rod parts row above the hotbar) like the hotbar: it moves with it,
 * has its own offset (dragged in the hud mover), and its slot squares follow the hotbar background setting.
 * By name, {@code require = 0}, see {@link BoxRendererMixin}.
 */
@Mixin(targets = "io.github.foecollab.screens.hud.EquipmentHud")
abstract class EquipmentHudMixin {
    @WrapMethod(method = "render(Lnet/minecraft/class_332;Lnet/minecraft/class_310;)V", remap = false, require = 0)
    private void dofisaddons$move(DrawContext ctx, MinecraftClient client, Operation<Void> original) {
        DofiSettings s = DofiSettings.get();
        ctx.getMatrices().pushMatrix();
        float k = s.equipScale / 100f;
        int cx = ctx.getScaledWindowWidth() / 2;
        int base = ctx.getScaledWindowHeight() - 28; // the bottom middle of foecollabs default row, where it grows from
        int[] off = s.equipOffset();
        ctx.getMatrices().translate(cx + s.hotbarX + s.equipX + off[0], base + s.hotbarY + s.equipY + off[1]);
        ctx.getMatrices().scale(k, k);
        ctx.getMatrices().translate(-cx, -base);
        original.call(ctx, client);
        ctx.getMatrices().popMatrix();
    }

    /** The rarity bar under a slot: left alone, recoloured, or a circle behind the item instead. */
    @WrapOperation(method = "renderBox", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_73198(IIIII)V", remap = false))
    private void dofisaddons$bar(DrawContext ctx, int x, int y, int w, int h, int color, Operation<Void> original) {
        DofiSettings s = DofiSettings.get();
        int shown = s.equipRecolor ? (color & 0xFF000000) | s.equipColor : color;
        if (!s.equipCircle) {
            original.call(ctx, x, y, w, h, shown);
        } else if ((color & 0xFFFFFF) != 0xFFFFFF || s.equipRecolor) { // plain items get no circle
            FoeRarity.disc(ctx, x + 2, y - 18, 0x66000000 | (shown & 0xFFFFFF)); // the bar sits just under the 20x20 slot
        }
    }

    @WrapOperation(method = "renderBox", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_25294(IIIII)V", remap = false))
    private void dofisaddons$square(DrawContext ctx, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        if (DofiSettings.get().hotbarBackground) {
            original.call(ctx, x1, y1, x2, y2, color);
        }
    }
}
