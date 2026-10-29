package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

/** The cycle table of luna's sheet (Middle European time, 2026-10-18 is still summer time). */
class FrenzyScheduleTest {
    private static Instant at(int day, int h, int m) {
        return LocalDateTime.of(2026, 10, day, h, m).atZone(FrenzySchedule.ZONE).toInstant();
    }

    private static long min(long ms) {
        return Duration.ofMillis(ms).toMinutes();
    }

    @Test
    void windowOpensAtTheCycleStart() {
        FrenzySchedule.State s = FrenzySchedule.at(at(18, 13, 0));
        assertTrue(s.open());
        assertEquals(120, min(s.ms()));
    }

    @Test
    void insideTheWindowCountsToItsEnd() {
        FrenzySchedule.State s = FrenzySchedule.at(at(18, 14, 30));
        assertTrue(s.open());
        assertEquals(30, min(s.ms()));
    }

    @Test
    void windowClosesAtTwoHoursAndTheNextOpensAtThree() {
        FrenzySchedule.State closed = FrenzySchedule.at(at(18, 15, 0));
        assertFalse(closed.open());
        assertEquals(60, min(closed.ms()));
        FrenzySchedule.State next = FrenzySchedule.at(at(18, 16, 0));
        assertTrue(next.open());
        assertEquals(120, min(next.ms()));
    }

    @Test
    void lastCycleOfTheDayWrapsPastMidnight() {
        assertTrue(FrenzySchedule.at(at(18, 22, 0)).open());
        FrenzySchedule.State after = FrenzySchedule.at(at(19, 0, 30));
        assertFalse(after.open());
        assertEquals(30, min(after.ms())); // next window 01:00
        FrenzySchedule.State s = FrenzySchedule.at(at(19, 1, 0));
        assertTrue(s.open());
    }

    @Test
    void everyStartOfTheSheetOpensAWindow() {
        for (int h : new int[]{13, 16, 19, 22, 1, 4, 7, 10}) {
            assertTrue(FrenzySchedule.at(at(18, h, 0)).open(), "cycle at " + h);
            assertEquals(120, min(FrenzySchedule.at(at(18, h, 0)).ms()), "cycle at " + h);
        }
    }

    @Test
    void winterTimeFollowsTheLocalClock() {
        Instant winter = LocalDateTime.of(2026, 11, 2, 14, 0).atZone(FrenzySchedule.ZONE).toInstant();
        FrenzySchedule.State s = FrenzySchedule.at(winter);
        assertTrue(s.open());
        assertEquals(60, min(s.ms()));
    }
}
