package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Settings saved before the heading switches, through a load and a save: the new headings come up off, saved ones are kept. */
class HudHeadingsIntegrationTest {
    @Test
    void oldSettingsGetTheNewHeadingsOffAndKeepTheirOwn() {
        // the shape of luna's settings.json of 2026-11-17 (no heading in the timers spot, level progress heading saved off),
        // with a now playing heading saved on, as someone who never switched it off has it
        String old = "{\"levelBarsHeading\":false,\"nowPlayingHeading\":true,\"boostsHud\":true,"
                + "\"timersHud\":{\"on\":true,\"pos\":{\"x\":0,\"y\":168,\"right\":true,\"bottom\":false,\"pinned\":true},\"scale\":105,\"align\":\"RIGHT\"}}";
        TackleboxSettings s = new Gson().fromJson(old, TackleboxSettings.class);
        s.sanitize();
        assertFalse(s.timersHud.heading, "the timers heading is new, so off");
        assertFalse(s.slotsLeftHud.heading, "a spot the file did not have");
        assertFalse(s.boostsHeading);
        assertFalse(s.levelBarsHeading);
        assertTrue(s.nowPlayingHeading, "a saved heading keeps its value; the new default only reaches new settings");
        assertEquals(105, s.timersHud.scale, "the rest of the spot is kept");

        s.timersHud.heading = true;
        s.boostsHeading = true;
        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        back.sanitize();
        assertTrue(back.timersHud.heading, "switched on survives a save");
        assertTrue(back.boostsHeading);
        assertFalse(back.gearHud.heading);
    }
}
