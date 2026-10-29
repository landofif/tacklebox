package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NearbyPlayersTest {
    @Test
    void hidesPlayersNearYouOrYourBobber() {
        assertTrue(NearbyPlayers.hides(true, false, 9, 400, 4));
        assertTrue(NearbyPlayers.hides(true, false, 400, 16, 4)); // exactly at the range
        assertFalse(NearbyPlayers.hides(true, false, 17, 17, 4));
    }

    @Test
    void keepsNpcsCrewAndFriends() {
        assertFalse(NearbyPlayers.hides(false, false, 1, 1, 4)); // not in the tab list: an npc
        assertFalse(NearbyPlayers.hides(true, true, 1, 1, 4));
    }

    @Test
    void tagWindowOverAPlayer() {
        assertTrue(NearbyPlayers.over(0.2, 0.2, 2.2));
        assertFalse(NearbyPlayers.over(0, 0, 1.0)); // at chest height
        assertFalse(NearbyPlayers.over(0.8, 0, 2.2)); // beside them
        assertFalse(NearbyPlayers.over(0, 0, 4.5));
    }

    @Test
    void rangeIsClamped() {
        assertEquals(2, NearbyPlayers.clampRange(0));
        assertEquals(12, NearbyPlayers.clampRange(40));
        assertEquals(4, NearbyPlayers.clampRange(4));
    }
}
