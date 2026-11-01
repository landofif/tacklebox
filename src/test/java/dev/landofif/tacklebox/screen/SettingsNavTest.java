package dev.landofif.tacklebox.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsNavTest {
    @Test
    void columnsFollowWidth() {
        assertEquals(1, SettingsNav.cols(300));
        assertEquals(2, SettingsNav.cols(400));
        assertEquals(3, SettingsNav.cols(600));
        assertEquals(4, SettingsNav.cols(900));
    }

    @Test
    void gridFillsRowsLeftToRight() {
        int[] tx = new int[9], ty = new int[9], tw = new int[9];
        int h = SettingsNav.grid(9, 900, 6, 40, tx, ty, tw);
        assertEquals((900 - 3 * 6) / 4, tw[0]);
        assertEquals(0, tx[0]);
        assertEquals(tw[0] + 6, tx[1]);
        assertEquals(0, ty[3]);
        assertEquals(46, ty[4]);
        assertEquals(0, tx[4]);
        assertEquals(92, ty[8]);
        assertEquals(3 * 40 + 2 * 6, h);
        for (int i = 0; i < 9; i++) {
            assertTrue(tx[i] + tw[i] <= 900, "tile " + i + " runs past the edge");
        }
    }

    @Test
    void narrowGridIsOneColumn() {
        int[] tx = new int[3], ty = new int[3], tw = new int[3];
        assertEquals(3 * 40 + 2 * 6, SettingsNav.grid(3, 300, 6, 40, tx, ty, tw));
        assertEquals(300, tw[0]);
        assertEquals(0, tx[2]);
        assertEquals(92, ty[2]);
    }

    @Test
    void emptyGridTakesNoRoom() {
        assertEquals(0, SettingsNav.grid(0, 900, 6, 40, new int[0], new int[0], new int[0]));
    }

    @Test
    void escClosesTheTileBeforeTheScreen() {
        SettingsNav nav = new SettingsNav();
        assertFalse(nav.back(), "on the grid esc closes the screen");
        nav.open(2);
        assertTrue(nav.opened());
        assertTrue(nav.back());
        assertFalse(nav.opened());
        assertFalse(nav.back());
    }

    @Test
    void pageChangeLandsOnTheGrid() {
        SettingsNav nav = new SettingsNav();
        nav.open(1);
        nav.page(3);
        assertEquals(3, nav.page);
        assertFalse(nav.opened());
    }
}
