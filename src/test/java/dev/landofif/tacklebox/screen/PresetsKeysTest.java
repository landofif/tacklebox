package dev.landofif.tacklebox.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Number key n equips the nth card of the page that is showing, and nothing past the last card. */
class PresetsKeysTest {
    @Test
    void keyPicksTheCardOnThisPage() {
        assertEquals(0, PresetsScreen.keyCard(0, 5)); // key 1
        assertEquals(4, PresetsScreen.keyCard(4, 5)); // key 5
    }

    @Test
    void keyPastTheLastCardDoesNothing() {
        assertEquals(-1, PresetsScreen.keyCard(6, 5)); // key 7 on a page of five
        assertEquals(-1, PresetsScreen.keyCard(2, 2));
        assertEquals(-1, PresetsScreen.keyCard(0, 0)); // the page has not loaded yet
    }
}
