package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.PressableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Flat buttons: every button is a flat see-through fill instead of the bevelled texture. */
@Mixin(PressableWidget.class)
abstract class PressableWidgetMixin extends ClickableWidget {
    private PressableWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @Inject(method = "drawButton", at = @At("HEAD"), cancellable = true)
    private void tacklebox$flatButton(DrawContext ctx, CallbackInfo ci) {
        if (!TackleboxSettings.get().flatButtons) {
            return;
        }
        ci.cancel();
        if (active) {
            Foe.button(ctx, getX(), getY(), getWidth(), getHeight(), isSelected());
        } else {
            Foe.buttonOff(ctx, getX(), getY(), getWidth(), getHeight()); // disabled: darker, grey text
        }
    }
}
