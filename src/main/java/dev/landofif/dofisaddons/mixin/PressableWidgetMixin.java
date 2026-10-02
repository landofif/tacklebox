package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Buttons in the mod's screens: a flat system24 box instead of the stone texture. */
@Mixin(PressableWidget.class)
abstract class PressableWidgetMixin extends ClickableWidget {
    private PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatButton(DrawContext ctx, CallbackInfo ci) {
        if (S24.active()) {
            S24.box(ctx, getX(), getY(), getWidth(), getHeight(), isSelected(), this.active);
            ci.cancel();
        }
    }
}
