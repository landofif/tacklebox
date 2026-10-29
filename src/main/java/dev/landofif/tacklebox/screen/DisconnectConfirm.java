package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;

/** Ask before disconnect (setting {@code confirmDisconnect}): the pause menu's disconnect button opens a yes/no screen first (mixin/GameMenuScreenMixin). */
public final class DisconnectConfirm {
    /** Set while yes presses the real button again, so that press goes through. */
    private static boolean leaving;

    private DisconnectConfirm() {}

    static boolean shouldAsk(boolean setting, boolean singleplayer, boolean leaving) {
        return setting && !singleplayer && !leaving;
    }

    /** Called when the disconnect button is pressed; true when it opened the question and the press must stop. */
    public static boolean ask(MinecraftClient client, Screen menu, ButtonWidget button) {
        if (!shouldAsk(TackleboxSettings.get().confirmDisconnect, client.isInSingleplayer(), leaving)) {
            return false;
        }
        client.setScreen(new ConfirmScreen(answer(() -> client.setScreen(menu), () -> button.onPress(null)),
                Foe.bold("disconnect?"), Foe.sentence("leave the server?"), Foe.text("disconnect"), Foe.text("stay")));
        return true;
    }

    /** Yes goes back to the pause menu and presses the real button (vanilla's leave, report draft check included); no only goes back. */
    static BooleanConsumer answer(Runnable back, Runnable press) {
        return yes -> {
            back.run();
            if (yes) {
                leaving = true;
                try {
                    press.run();
                } finally {
                    leaving = false;
                }
            }
        };
    }

    static boolean leaving() {
        return leaving;
    }
}
