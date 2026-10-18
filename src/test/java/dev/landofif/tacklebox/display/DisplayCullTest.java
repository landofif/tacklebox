package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DisplayCullTest {
    @Test
    void hidesOnlyBeyondRangeWhenOn() {
        assertFalse(DisplayCull.hidden(true, 24 * 24, 24));
        assertTrue(DisplayCull.hidden(true, 24 * 24 + 1, 24));
        assertFalse(DisplayCull.hidden(false, 1e6, 24));
    }

    @Test
    void rangeIsClamped() {
        assertEquals(8, DisplayCull.clampRange(0));
        assertEquals(64, DisplayCull.clampRange(500));
        assertTrue(DisplayCull.hidden(true, 9 * 9, -5)); // range 8
    }
}
