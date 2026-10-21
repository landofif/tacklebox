package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BiteFocusTest {
    @Test
    void firstBiteFocuses() {
        assertTrue(BiteFocus.due(5000, 0));
    }

    @Test
    void biteInsideCooldownIsSkipped() {
        assertFalse(BiteFocus.due(10_000 + 1999, 10_000));
    }

    @Test
    void biteAfterCooldownFocuses() {
        assertTrue(BiteFocus.due(10_000 + 2000, 10_000));
    }

    @Test
    void focusesByTitleOnly() {
        assertEquals(List.of("hyprctl", "dispatch", "hl.dsp.focus({ window = 'initialtitle:^Minecraft.*' })"), BiteFocus.COMMAND);
    }
}
