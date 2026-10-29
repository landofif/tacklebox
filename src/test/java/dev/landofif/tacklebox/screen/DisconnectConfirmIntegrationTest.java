package dev.landofif.tacklebox.screen;

import com.google.gson.Gson;
import dev.landofif.tacklebox.TackleboxSettings;
import it.unimi.dsi.fastutil.booleans.BooleanConsumer;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pause menu's disconnect button with saved settings, played the way the mixin does it: the press opens the question,
 * no goes back and stays, yes goes back and the real press leaves. Old settings files without the key ask too.
 */
class DisconnectConfirmIntegrationTest {
    /** A pause menu on a server: the button press either opens the question or leaves. */
    private static final class Menu {
        final boolean setting;
        String screen = "pause menu";
        boolean left;
        BooleanConsumer question;

        Menu(TackleboxSettings s) {
            setting = s.confirmDisconnect;
        }

        void pressDisconnect() {
            if (DisconnectConfirm.shouldAsk(setting, false, DisconnectConfirm.leaving())) {
                screen = "question";
                question = DisconnectConfirm.answer(() -> screen = "pause menu", this::pressDisconnect);
                return;
            }
            left = true;
            screen = "title screen";
        }
    }

    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void noStaysYesLeaves() {
        Menu m = new Menu(load("{\"chatCopy\": true}"));
        m.pressDisconnect();
        assertEquals("question", m.screen);
        m.question.accept(false);
        assertEquals("pause menu", m.screen);
        assertFalse(m.left);

        m.pressDisconnect();
        m.question.accept(true);
        assertTrue(m.left);
        assertEquals("title screen", m.screen);
        assertFalse(DisconnectConfirm.leaving());
    }

    @Test
    void settingOffLeavesAtOnce() {
        Menu m = new Menu(load("{\"confirmDisconnect\": false}"));
        m.pressDisconnect();
        assertTrue(m.left);
    }
}
