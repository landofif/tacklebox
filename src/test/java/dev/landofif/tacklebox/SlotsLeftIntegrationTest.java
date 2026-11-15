package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The slots left hud's settings through a save, settings from before it existed, and a number out of range. */
class SlotsLeftIntegrationTest {
    @Test
    void oldSettingsGetItOffAtFive() {
        TackleboxSettings old = new Gson().fromJson("{\"slotsLeftHud\":null}", TackleboxSettings.class);
        old.sanitize();
        assertFalse(old.slotsLeftHud.on);
        assertEquals(5, old.slotsLeftAt);
        assertEquals(260, old.slotsLeftHud.pos.y);
    }

    @Test
    void theNumberSurvivesASaveAndStaysInRange() {
        TackleboxSettings s = new TackleboxSettings();
        s.sanitize();
        s.slotsLeftHud.on = true;
        s.slotsLeftAt = 12;
        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        back.sanitize();
        assertTrue(back.slotsLeftHud.on);
        assertEquals(12, back.slotsLeftAt);

        back.slotsLeftAt = 99;
        back.sanitize();
        assertEquals(36, back.slotsLeftAt);
        back.slotsLeftAt = -3;
        back.sanitize();
        assertEquals(0, back.slotsLeftAt);
    }
}
