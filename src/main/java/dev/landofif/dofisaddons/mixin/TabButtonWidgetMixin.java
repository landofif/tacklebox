package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.TabButtonWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Tabs (create world, some option screens): flat boxes, the current tab underlined in accent. */
@Mixin(TabButtonWidget.class)
abstract class TabButtonWidgetMixin {
    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"))
    private void dofisaddons$flatTab(DrawContext ctx, RenderPipeline pipeline, Identifier texture, int x, int y, int w, int h,
                                     Operation<Void> original) {
        if (!S24.everywhere()) {
            original.call(ctx, pipeline, texture, x, y, w, h);
            return;
        }
        TabButtonWidget self = (TabButtonWidget) (Object) this;
        boolean hot = self.isSelected();
        ctx.fill(x, y, x + w, y + h, hot ? S24.BG_1 : S24.BG_2);
        S24.outline(ctx, x, y, w, h, hot ? S24.ACCENT : S24.BORDER);
    }

    /** The current tab's background texture patch and white underline are replaced by an accent line. */
    @Inject(method = "renderBackgroundTexture", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$noTabTexture(DrawContext ctx, int left, int top, int right, int bottom, CallbackInfo ci) {
        if (S24.everywhere()) {
            ctx.fill(left - 1, bottom - 2, right + 1, bottom - 1, S24.ACCENT);
            ci.cancel();
        }
    }

    @Inject(method = "drawCurrentTabLine", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$noTabLine(DrawContext ctx, TextRenderer tr, int color, CallbackInfo ci) {
        if (S24.everywhere()) {
            ci.cancel();
        }
    }
}
