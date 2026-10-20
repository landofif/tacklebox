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

    @Test
    void subtractLeavesTheUncoveredParts() {
        var pieces = HudCover.subtract(new java.util.ArrayList<>(java.util.List.of(new int[]{0, 100, 200, 100})), new int[]{50, 120, 100, 40});
        long area = 0;
        for (int[] p : pieces) {
            assertFalse(HudCover.overlaps(p[0], p[1], p[2], p[3], new int[]{50, 120, 100, 40}));
            area += (long) p[2] * p[3];
        }
        org.junit.jupiter.api.Assertions.assertEquals(200 * 100 - 100 * 40, area);
    }

    @Test
    void subtractWholeCoverGivesNothing() {
        assertTrue(HudCover.subtract(new java.util.ArrayList<>(java.util.List.of(new int[]{60, 130, 20, 10})), new int[]{50, 120, 100, 40}).isEmpty());
    }
}
