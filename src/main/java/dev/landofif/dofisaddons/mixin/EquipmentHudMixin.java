package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.DofiSettings;
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
        ctx.getMatrices().translate(s.hotbarX + s.equipX, s.hotbarY + s.equipY);
        original.call(ctx, client);
        ctx.getMatrices().popMatrix();
    }

    @WrapOperation(method = "renderBox", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_25294(IIIII)V", remap = false))
    private void dofisaddons$square(DrawContext ctx, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        if (DofiSettings.get().hotbarBackground) {
            original.call(ctx, x1, y1, x2, y2, color);
        }
    }
}
