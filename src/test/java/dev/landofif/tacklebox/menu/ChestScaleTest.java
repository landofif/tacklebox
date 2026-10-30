package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

/** Which gui scale the window gets. */
class ChestScaleTest {
    @Test
    void chestUsesItsOwnScaleWhenSet() {
        assertEquals(2, ChestScale.guiScale(true, 2, 4));
        assertEquals(5, ChestScale.guiScale(true, 5, 0));
    }

    @Test
    void zeroOrNoChestKeepsTheGamesScale() {
        assertEquals(4, ChestScale.guiScale(true, 0, 4));
        assertEquals(4, ChestScale.guiScale(false, 2, 4));
        assertEquals(0, ChestScale.guiScale(false, 2, 0)); // the games auto scale stays auto
    }

    @Test
    void noScreenIsNoChest() {
        assertFalse(ChestScale.isChest(null));
    }
}
