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

    @Test
    void hudShrinksBackToTheGamesScale() {
        assertEquals(2f, ChestScale.hudShrink(2, 3) * 3, 1e-6); // a hud pixel stays 2 screen pixels with the chest at 3
        assertEquals(4f, ChestScale.hudShrink(4, 2) * 2, 1e-6);
        assertEquals(1f, ChestScale.hudShrink(3, 3));
    }
}
