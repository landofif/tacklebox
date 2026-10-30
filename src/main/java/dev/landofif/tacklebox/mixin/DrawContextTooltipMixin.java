package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.tacklebox.display.TooltipScroll;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.tooltip.TooltipPositioner;
import org.joml.Vector2i;
import org.joml.Vector2ic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Tall tooltips start at the top and scroll with the wheel (display/TooltipScroll). */
@Mixin(DrawContext.class)
abstract class DrawContextTooltipMixin {
    @WrapOperation(method = "drawTooltipImmediately", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/tooltip/TooltipPositioner;getPosition(IIIIII)Lorg/joml/Vector2ic;"))
    private Vector2ic tacklebox$scroll(TooltipPositioner positioner, int screenWidth, int screenHeight, int x, int y, int width, int height,
                                      Operation<Vector2ic> original) {
        Vector2ic pos = original.call(positioner, screenWidth, screenHeight, x, y, width, height);
        int top = TooltipScroll.top(pos.y(), width, height, screenHeight);
        return top == pos.y() ? pos : new Vector2i(pos.x(), top);
    }
}
