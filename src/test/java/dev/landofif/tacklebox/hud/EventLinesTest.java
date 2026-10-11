package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lines copied from l's logs of 2026-10-10. */
class EventLinesTest {
    @Test
    void frenzyAlertBodyGivesHoursAndPlace() {
        EventLines.Frenzy f = EventLines.frenzy("Fish at Kenai River for 1 hour of buffed rates");
        assertEquals("Kenai River", f.place());
        assertEquals(3_600_000L, f.remainingMs());
    }

    @Test
    void frenzyReminderGivesTimeLeft() {
        assertEquals((16 * 60 + 56) * 1000L, EventLines.frenzy("FISH FRENZY » A Fish Frenzy is active at Kenai River (for 16m 56s)").remainingMs());
        assertEquals(92_000L, EventLines.frenzy("FISH FRENZY » A Fish Frenzy is active at Kenai River (for 1m 32s)").remainingMs());
        assertEquals(3_600_000L + 5_000L, EventLines.frenzy("FISH FRENZY » A Fish Frenzy is active at Kenai River (for 1h 5s)").remainingMs());
    }

    @Test
    void frenzyEndAndUnrelatedLines() {
        assertTrue(EventLines.frenzyEnded("FRENZY » The Fish Frenzy has ended at Kenai River"));
        assertFalse(EventLines.frenzyEnded("FISH FRENZY ALERT"));
        assertNull(EventLines.frenzy("! [133] Demonstrations » 5mins left on frenzy :("));
        assertNull(EventLines.frenzy("FISH FRENZY ALERT"));
    }

    @Test
    void fabledStartAndEnd() {
        assertTrue(EventLines.fabledHeader("FABLED FISH EVENT"));
        assertFalse(EventLines.fabledHeader("FABLED » something"));
        assertEquals("Cypress Lake", EventLines.fabledStart("Visit the Cypress Lake to"));
        assertNull(EventLines.fabledStart("try and catch it!"));
        assertTrue(EventLines.fabledEnded("Caught by Freddy_2005"));
        assertFalse(EventLines.fabledEnded("Visit the Cypress Lake to"));
    }
}
