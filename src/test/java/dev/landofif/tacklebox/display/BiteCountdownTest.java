package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BiteCountdownTest {
    @BeforeEach
    void clean() {
        BiteCountdown.onBite(0);
        BiteCountdown.noteActionBar(false, 0);
    }

    /** A fish at 2 blocks/s: one particle per tick (50 ms), 0.1 block closer each. */
    private static void approach(long start, double r0, int ticks) {
        for (int i = 0; i < ticks; i++) {
            BiteCountdown.onApproach(r0 - 0.1 * i, start + 50L * i);
        }
    }

    @Test
    void hiddenUntilFourParticles() {
        long t0 = 10_000;
        approach(t0, 6, 3);
        assertFalse(BiteCountdown.active(t0 + 100));
        approach(t0, 6, 4);
        assertTrue(BiteCountdown.active(t0 + 150));
    }

    @Test
    void predictsTheBiteFromTheMeasuredSpeed() {
        long t0 = 10_000;
        approach(t0, 6, 10); // r = 5.1 at the last particle, 2 blocks/s: 2.55 s left
        assertEquals(2.55f, BiteCountdown.seconds(t0 + 450), 0.15f);
    }

    @Test
    void tracksARainSpeedFish() {
        long t0 = 10_000;
        for (int i = 0; i < 10; i++) {
            BiteCountdown.onApproach(6 - 0.2 * i, t0 + 50L * i); // 4 blocks/s
        }
        assertEquals(1.05f, BiteCountdown.seconds(t0 + 450), 0.15f);
    }

    @Test
    void neverCountsBackUp() {
        long t0 = 10_000;
        approach(t0, 6, 10);
        float before = BiteCountdown.seconds(t0 + 450);
        BiteCountdown.onApproach(5.0, t0 + 700); // late packet: would predict later
        assertTrue(BiteCountdown.seconds(t0 + 450) <= before + 0.001f);
    }

    @Test
    void hidesWhenParticlesStop() {
        long t0 = 10_000;
        approach(t0, 6, 10);
        assertFalse(BiteCountdown.active(t0 + 450 + BiteCountdown.TIMEOUT_MS + 1));
    }

    @Test
    void biteStopsItAndIgnoresTheBurst() {
        long t0 = 10_000;
        approach(t0, 6, 10);
        BiteCountdown.onBite(t0 + 500);
        assertFalse(BiteCountdown.active(t0 + 510));
        approach(t0 + 600, 1, 10);
        assertFalse(BiteCountdown.active(t0 + 1100));
    }

    @Test
    void actionBarHidesIt() {
        long t0 = 10_000;
        approach(t0, 6, 10);
        BiteCountdown.noteActionBar(true, t0 + 460);
        assertFalse(BiteCountdown.active(t0 + 470));
        BiteCountdown.noteActionBar(false, t0 + 480);
        assertTrue(BiteCountdown.active(t0 + 490));
    }

    @Test
    void ownerIsThePerpendicularOne() {
        // particle 3 blocks east of the bobber, moving north: perpendicular to the line to the bobber
        assertEquals(0, BiteCountdown.perpendicularError(3, 0, 0, -0.5), 1e-9);
        // moving straight at the bobber: not ours
        assertEquals(1, BiteCountdown.perpendicularError(3, 0, -0.5, 0), 1e-9);
        assertEquals(1, BiteCountdown.perpendicularError(3, 0, 0, 0), 1e-9);
    }
}
