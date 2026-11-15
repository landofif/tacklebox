package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class InventoryHudTest {
    @Test
    void threeRowsOfNine() {
        assertArrayEquals(new int[]{0, 0}, InventoryHud.cell(0));
        assertArrayEquals(new int[]{8 * 18, 0}, InventoryHud.cell(8));
        assertArrayEquals(new int[]{0, 18}, InventoryHud.cell(9));
        assertArrayEquals(new int[]{8 * 18, 2 * 18}, InventoryHud.cell(26));
        assertArrayEquals(new int[]{162, 54}, InventoryHud.size());
    }

    @Test
    void startsAboveTheHotbar() {
        assertEquals(9, InventoryHud.FIRST); // 0 to 8 are the hotbar
        assertEquals(27, InventoryHud.COLS * InventoryHud.ROWS);
    }
}
