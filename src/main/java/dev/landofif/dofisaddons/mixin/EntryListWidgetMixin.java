package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.EntryListWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Lists (worlds, servers, options, controls): no textured strip, 1px separators, accent selection. */
@Mixin(EntryListWidget.class)
abstract class EntryListWidgetMixin {
    @Inject(method = "drawMenuListBackground", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatList(DrawContext ctx, CallbackInfo ci) {
        if (S24.everywhere()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawHeaderAndFooterSeparators", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatSeparators(DrawContext ctx, CallbackInfo ci) {
        if (S24.everywhere()) {
            EntryListWidget<?> self = (EntryListWidget<?>) (Object) this;
            ctx.fill(self.getX(), self.getY() - 1, self.getRight(), self.getY(), S24.BORDER);
            ctx.fill(self.getX(), self.getBottom(), self.getRight(), self.getBottom() + 1, S24.BORDER);
            ci.cancel();
        }
    }

    /** Selection: the outer fill becomes a bg-2 box with a 1px border (accent when focused)... */
    @WrapOperation(method = "drawSelectionHighlight", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 0))
    private void dofisaddons$flatSelection(DrawContext ctx, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        if (S24.everywhere()) {
            ctx.fill(x1, y1, x2, y2, S24.BG_2);
            S24.outline(ctx, x1, y1, x2 - x1, y2 - y1, color == -1 ? S24.ACCENT : S24.TEXT_5);
        } else {
            original.call(ctx, x1, y1, x2, y2, color);
        }
    }

    /** ...and the black inside is dropped. */
    @WrapOperation(method = "drawSelectionHighlight", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;fill(IIIII)V", ordinal = 1))
    private void dofisaddons$noSelectionInside(DrawContext ctx, int x1, int y1, int x2, int y2, int color, Operation<Void> original) {
        if (!S24.everywhere()) {
            original.call(ctx, x1, y1, x2, y2, color);
        }
    }
}
