package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.CheckboxWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

/** Checkboxes: a flat box with an accent square when ticked. */
@Mixin(CheckboxWidget.class)
abstract class CheckboxWidgetMixin {
    @Shadow
    private boolean checked;

    @WrapOperation(method = "drawIcon", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIIII)V"))
    private void dofisaddons$flatCheckbox(DrawContext ctx, RenderPipeline pipeline, Identifier texture, int x, int y, int w, int h, int color,
                                          Operation<Void> original) {
        if (!S24.everywhere()) {
            original.call(ctx, pipeline, texture, x, y, w, h, color);
            return;
        }
        CheckboxWidget self = (CheckboxWidget) (Object) this;
        boolean hot = self.isFocused() || self.isHovered();
        ctx.fill(x, y, x + w, y + h, hot ? S24.BG_1 : S24.BG_2);
        S24.outline(ctx, x, y, w, h, hot ? S24.ACCENT : S24.BORDER);
        if (checked) {
            int inset = Math.max(3, w / 4);
            ctx.fill(x + inset, y + inset, x + w - inset, y + h - inset, S24.ACCENT);
        }
    }
}
