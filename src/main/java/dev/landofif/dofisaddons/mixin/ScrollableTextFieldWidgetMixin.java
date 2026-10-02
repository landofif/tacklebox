package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ScrollableTextFieldWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Multi-line text boxes (the HUD lines editor) in the mod's screens: a flat box like the fields. */
@Mixin(ScrollableTextFieldWidget.class)
abstract class ScrollableTextFieldWidgetMixin {
    @Inject(method = "drawBox", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatBox(DrawContext ctx, CallbackInfo ci) {
        if (S24.active()) {
            ScrollableTextFieldWidget self = (ScrollableTextFieldWidget) (Object) this;
            int x = self.getX();
            int y = self.getY();
            int w = self.getWidth();
            int h = self.getHeight();
            ctx.fill(x, y, x + w, y + h, S24.BG_4);
            S24.outline(ctx, x, y, w, h, self.isFocused() ? S24.ACCENT : self.isHovered() ? S24.TEXT_5 : S24.BORDER);
            ci.cancel();
        }
    }
}
