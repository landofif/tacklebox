package dev.landofif.tacklebox.screen;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** When the question shows, and what yes and no do. */
class DisconnectConfirmTest {
    @Test
    void asksOnlyOnAServerWithTheSettingOn() {
        assertTrue(DisconnectConfirm.shouldAsk(true, false, false));
        assertFalse(DisconnectConfirm.shouldAsk(false, false, false));
        assertFalse(DisconnectConfirm.shouldAsk(true, true, false)); // singleplayer: save and quit
        assertFalse(DisconnectConfirm.shouldAsk(true, false, true)); // the press after yes
    }

    @Test
    void yesGoesBackThenPressesTheRealButton() {
        List<String> done = new ArrayList<>();
        DisconnectConfirm.answer(() -> done.add("back"), () -> done.add("press, leaving=" + DisconnectConfirm.leaving())).accept(true);
        assertEquals(List.of("back", "press, leaving=true"), done);
        assertFalse(DisconnectConfirm.leaving());
    }

    @Test
    void noOnlyGoesBack() {
        List<String> done = new ArrayList<>();
        DisconnectConfirm.answer(() -> done.add("back"), () -> done.add("press")).accept(false);
        assertEquals(List.of("back"), done);
    }

    @Test
    void aFailedPressDoesNotLeaveTheBypassOn() {
        try {
            DisconnectConfirm.answer(() -> {}, () -> {
                throw new IllegalStateException();
            }).accept(true);
        } catch (IllegalStateException expected) {
            // the leave itself failed
        }
        assertFalse(DisconnectConfirm.leaving());
    }
}
