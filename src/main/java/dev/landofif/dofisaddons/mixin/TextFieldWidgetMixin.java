package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Single-line text fields in the mod's screens: a flat box, accent border while focused. */
@Mixin(TextFieldWidget.class)
abstract class TextFieldWidgetMixin {
    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void dofisaddons$flatField(DrawContext ctx, RenderPipeline pipeline, Identifier texture, int x, int y, int w, int h,
                                       Operation<Void> original) {
        if (S24.active()) {
            TextFieldWidget self = (TextFieldWidget) (Object) this;
            ctx.fill(x, y, x + w, y + h, S24.BG_4);
            S24.outline(ctx, x, y, w, h, self.isFocused() ? S24.ACCENT : self.isHovered() ? S24.TEXT_5 : S24.BORDER);
        } else {
            original.call(ctx, pipeline, texture, x, y, w, h);
        }
    }
}
