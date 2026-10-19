package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HudCoverTest {
    private static final int[] CHAT = {10, 100, 100, 50};

    @Test
    void overlapIsDetected() {
        assertTrue(HudCover.overlaps(50, 120, 20, 20, CHAT));
        assertTrue(HudCover.overlaps(100, 140, 40, 40, CHAT));
    }

    @Test
    void touchingEdgesAndFarAwayDoNotCount() {
        assertFalse(HudCover.overlaps(110, 100, 20, 20, CHAT));
        assertFalse(HudCover.overlaps(10, 150, 20, 20, CHAT));
        assertFalse(HudCover.overlaps(300, 300, 20, 20, CHAT));
    }
}
