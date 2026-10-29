package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.screen.DisconnectConfirm;
import net.minecraft.client.gui.screen.GameMenuScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Ask before disconnect: the disconnect button's press (lambda {@code method_19836} in initWidgets) waits for a yes. */
@Mixin(GameMenuScreen.class)
public abstract class GameMenuScreenMixin extends Screen {
    protected GameMenuScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "method_19836", at = @At("HEAD"), cancellable = true)
    private void tacklebox$askFirst(ButtonWidget button, CallbackInfo ci) {
        if (DisconnectConfirm.ask(this.client, this, button)) {
            ci.cancel();
        }
    }
}
