package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The rod not cast clock: starts after the first cast, fires once per wait, rod away resets it. */
class RodIdleTest {
    @AfterEach
    void tearDown() {
        RodIdle.reset();
    }

    @Test
    void nothingBeforeTheFirstCast() {
        assertNull(RodIdle.step(true, false, 0, 60));
        assertNull(RodIdle.step(true, false, 600_000, 60));
    }

    @Test
    void firesOnceAfterTheTimeWithNoBobber() {
        assertNull(RodIdle.step(true, true, 0, 60)); // cast
        assertNull(RodIdle.step(true, false, 1_000, 60)); // caught: the wait starts
        assertNull(RodIdle.step(true, false, 60_999, 60));
        assertEquals("TACKLEBOX » Rod not cast for 60s", RodIdle.step(true, false, 61_000, 60));
        assertNull(RodIdle.step(true, false, 200_000, 60));
    }

    @Test
    void castingAgainStartsANewWait() {
        RodIdle.step(true, true, 0, 30);
        RodIdle.step(true, false, 0, 30);
        assertEquals(RodIdle.line(30), RodIdle.step(true, false, 30_000, 30));
        assertNull(RodIdle.step(true, true, 40_000, 30));
        assertNull(RodIdle.step(true, false, 41_000, 30));
        assertEquals(RodIdle.line(30), RodIdle.step(true, false, 71_000, 30));
    }

    @Test
    void puttingTheRodAwayResetsTheClock() {
        RodIdle.step(true, true, 0, 60);
        RodIdle.step(true, false, 0, 60);
        assertNull(RodIdle.step(false, false, 50_000, 60)); // other item in hand
        assertNull(RodIdle.step(true, false, 55_000, 60)); // back: counts from here
        assertNull(RodIdle.step(true, false, 114_999, 60));
        assertEquals(RodIdle.line(60), RodIdle.step(true, false, 115_000, 60));
    }

    @Test
    void leavingTheServerForgetsTheCast() {
        RodIdle.step(true, true, 0, 60);
        RodIdle.reset();
        assertNull(RodIdle.step(true, false, 0, 60));
        assertNull(RodIdle.step(true, false, 120_000, 60));
    }

    @Test
    void secondsAreClamped() {
        assertEquals(RodIdle.MIN_SECONDS, RodIdle.clamp(0));
        assertEquals(RodIdle.MAX_SECONDS, RodIdle.clamp(5000));
        assertEquals(90, RodIdle.clamp(90));
    }
}
