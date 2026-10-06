package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Foecollabs hud boxes in its hud mover (blue fill, white edge lines) drawn like ours: a thin outline, orange
 * under the mouse. By name, {@code require = 0}, see {@link BoxRendererMixin}.
 */
@Mixin(targets = "io.github.foecollab.screens.widget.movablebox.MovableBoxWidget")
abstract class MovableBoxWidgetMixin {
    @WrapOperation(method = "method_48579", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/class_332;method_25294(IIIII)V", remap = false))
    private void dofisaddons$outline(DrawContext ctx, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        if (x2 - x1 > 2 && y2 - y1 > 2) { // the fill
            ctx.drawStrokedRectangle(x1, y1, x2 - x1, y2 - y1, ((ClickableWidget) (Object) this).isHovered() ? Foe.ACCENT : Foe.FAINT);
        } else {
            original.call(ctx, x1, y1, x2, y2, color); // the alignment line stays as foecollab draws it
        }
    }
}
