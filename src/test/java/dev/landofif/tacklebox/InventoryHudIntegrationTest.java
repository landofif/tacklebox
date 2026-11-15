package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The inventory hud's settings through a save, and settings from before it existed. */
class InventoryHudIntegrationTest {
    @Test
    void oldSettingsGetItOffWithSlotsOn() {
        TackleboxSettings old = new Gson().fromJson("{\"inventoryHud\":null}", TackleboxSettings.class);
        old.sanitize();
        assertFalse(old.inventoryHud.on);
        assertTrue(old.inventoryHudCells);
        assertEquals(4, old.inventoryHud.pos.x);
        assertEquals(200, old.inventoryHud.pos.y);
    }

    @Test
    void aMovedAndResizedHudSurvivesASave() {
        TackleboxSettings s = new TackleboxSettings();
        s.sanitize();
        s.inventoryHud.on = true;
        s.inventoryHudCells = false;
        s.inventoryHud.resize(4);
        s.inventoryHud.pos.moveTo(960, 540, 790, 480, 162, 54);
        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        back.sanitize();
        assertTrue(back.inventoryHud.on);
        assertFalse(back.inventoryHudCells);
        assertEquals(120, back.inventoryHud.scale);
        assertTrue(back.inventoryHud.pos.right && back.inventoryHud.pos.bottom);
        back.inventoryHud.pos.measure(960, 540, 162, 54); // the size is not saved: the first draw measures it
        assertEquals(790, back.inventoryHud.pos.left(960));
    }
}
