package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ScrollableWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Scrollbars: a slim track with an accent thumb instead of the textured one. */
@Mixin(ScrollableWidget.class)
abstract class ScrollableWidgetMixin {
    @Shadow
    protected abstract boolean overflows();

    @Shadow
    protected abstract int getScrollbarX();

    @Shadow
    protected abstract int getScrollbarThumbHeight();

    @Shadow
    protected abstract int getScrollbarThumbY();

    @Shadow
    protected abstract boolean isInScrollbar(double mouseX, double mouseY);

    @Inject(method = "drawScrollbar", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatScrollbar(DrawContext ctx, int mouseX, int mouseY, CallbackInfo ci) {
        if (!S24.everywhere()) {
            return;
        }
        ci.cancel();
        if (!overflows()) {
            return;
        }
        ScrollableWidget self = (ScrollableWidget) (Object) this;
        int x = getScrollbarX() + 2;
        int top = getScrollbarThumbY();
        ctx.fill(x, self.getY(), x + 2, self.getBottom(), S24.BORDER);
        ctx.fill(x, top, x + 2, top + getScrollbarThumbHeight(), isInScrollbar(mouseX, mouseY) ? S24.ACCENT : S24.ACCENT_DIM);
    }
}
