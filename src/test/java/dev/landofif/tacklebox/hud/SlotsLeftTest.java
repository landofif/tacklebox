package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SlotsLeftTest {
    @Test
    void showsAtOrUnderTheNumber() {
        assertTrue(SlotsLeft.rows(6, 5, false).isEmpty());
        assertEquals(List.of(new TextHud.Row("slots left", "5")), SlotsLeft.rows(5, 5, false));
        assertEquals(List.of(new TextHud.Row("slots left", "2")), SlotsLeft.rows(2, 5, false));
    }

    @Test
    void thirtySixShowsItAlwaysAndZeroOnlyWhenFull() {
        assertFalse(SlotsLeft.rows(36, 36, false).isEmpty());
        assertTrue(SlotsLeft.rows(1, 0, false).isEmpty());
        assertFalse(SlotsLeft.rows(0, 0, false).isEmpty());
    }

    @Test
    void redWhenFull() {
        assertEquals(0xFFFF5555, SlotsLeft.rows(0, 5, false).get(0).valueColor());
        assertEquals(0, SlotsLeft.rows(1, 5, false).get(0).valueColor());
    }

    @Test
    void theMoverAlwaysShowsIt() {
        assertFalse(SlotsLeft.rows(30, 5, true).isEmpty());
    }
}
