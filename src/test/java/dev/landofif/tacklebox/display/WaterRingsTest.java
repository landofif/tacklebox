package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaterRingsTest {
    private static final long T = 1_000_000;

    @BeforeEach
    void on() {
        WaterRings.reset();
        TackleboxSettings s = TackleboxSettings.get();
        s.waterFx = true;
        s.waterBiteRings = s.waterWake = s.waterCatchRing = s.waterSparkles = true;
        s.waterOpacity = 60;
        s.waterColor = 0xD8F0FF;
    }

    @AfterEach
    void off() {
        TackleboxSettings s = TackleboxSettings.get();
        s.waterFx = false;
        s.waterBiteRings = s.waterWake = s.waterCatchRing = s.waterSparkles = true;
        s.waterOpacity = 60;
        s.waterColor = 0xD8F0FF;
        WaterRings.reset();
    }

    private static List<WaterRings.Ring> rings() {
        return WaterRings.rings();
    }

    @Test
    void aRingGrowsQuicklyThenSlowsAndFadesWithoutPopping() {
        WaterRings.Ring r = new WaterRings.Ring(0, 0, 0, T, 600, 0.2f, 1.0f, 0xFFFFFF, 0.6f);
        assertEquals(0.2f, r.radius(T), 1e-6);
        assertEquals(1.0f, r.radius(T + 600), 1e-6);
        assertEquals(1.0f, r.radius(T + 5000), 1e-6, "stays at its end size");
        assertTrue(r.radius(T + 300) - r.radius(T) > r.radius(T + 600) - r.radius(T + 300), "eases out");
        assertEquals(0, r.alpha(T), 1e-6, "starts clear: no pop");
        assertEquals(0, r.alpha(T + 600), 1e-6, "gone at the end");
        float peak = 0;
        for (long t = T; t <= T + 600; t += 10) {
            peak = Math.max(peak, r.alpha(t));
            assertTrue(r.alpha(t) <= 0.6f + 1e-6, "never above its strength");
        }
        assertTrue(peak > 0.5f, "close to its strength early on: " + peak);
        assertTrue(r.alpha(T + 100) > r.alpha(T + 300) && r.alpha(T + 300) > r.alpha(T + 500), "fades while it grows");
        assertFalse(r.over(T + 599));
        assertTrue(r.over(T + 600));
    }

    @Test
    void theRingIsAClosedFlatBand() {
        List<float[]> v = new ArrayList<>();
        WaterRings.emit(2f, 63.5f, -4f, 1.0f, (x, y, z) -> v.add(new float[]{x, y, z}));
        assertEquals(WaterRings.SEGMENTS * 4, v.size());
        float in = 1.0f - WaterRings.WIDTH / 2, out = 1.0f + WaterRings.WIDTH / 2;
        for (int i = 0; i < v.size(); i += 4) {
            assertEquals(in, Math.hypot(v.get(i)[0] - 2, v.get(i)[2] + 4), 1e-5);
            assertEquals(out, Math.hypot(v.get(i + 1)[0] - 2, v.get(i + 1)[2] + 4), 1e-5);
            assertEquals(out, Math.hypot(v.get(i + 2)[0] - 2, v.get(i + 2)[2] + 4), 1e-5);
            assertEquals(in, Math.hypot(v.get(i + 3)[0] - 2, v.get(i + 3)[2] + 4), 1e-5);
            for (int k = 0; k < 4; k++) {
                assertEquals(63.5f, v.get(i + k)[1], "flat on the water");
            }
            float[] next = v.get((i + 4) % v.size());
            assertEquals(v.get(i + 3)[0], next[0], 1e-5, "each quad starts where the last one ended");
            assertEquals(v.get(i + 3)[2], next[2], 1e-5);
        }
        List<float[]> tiny = new ArrayList<>();
        WaterRings.emit(0, 0, 0, 0.01f, (x, y, z) -> tiny.add(new float[]{x, y, z}));
        assertEquals(0, Math.hypot(tiny.get(0)[0], tiny.get(0)[2]), 1e-6, "the inner edge never crosses the middle");
    }

    @Test
    void biteRingsComeQuickerAsTheFishNears() {
        assertEquals(1.0, WaterRings.interval(10), 1e-9);
        assertEquals(1.0, WaterRings.interval(3), 1e-9);
        assertEquals(0.7, WaterRings.interval(2), 1e-9);
        assertEquals(0.35, WaterRings.interval(1), 1e-9);
        assertEquals(0.25, WaterRings.interval(0.5), 1e-9);
        assertEquals(0.25, WaterRings.interval(0), 1e-9);

        WaterRings.onBobber(5.5, 62.89, -3.5, T);
        WaterRings.tick(T, true, 2.0);
        assertEquals(1, rings().size(), "the first one at once");
        WaterRings.Ring first = rings().get(0);
        assertEquals(5.5, first.x());
        assertEquals(62.89, first.y());
        assertEquals(0.2f, first.from());
        assertEquals(1.0f, first.to());
        assertEquals(0xD8F0FF, first.rgb());
        assertEquals(0.6f, first.strength(), 1e-6);
        WaterRings.tick(T + 500, true, 1.5);
        assertEquals(List.of(T), born(), "not yet: 0.7 s apart at 2 s left");
        WaterRings.tick(T + 700, true, 1.3);
        assertEquals(List.of(T + 700), born(), "the next one, and the first one is over");
        WaterRings.tick(T + 1100, true, 0.9);
        assertEquals(List.of(T + 700), born(), "0.455 s apart at 1.3 s left");
        WaterRings.tick(T + 1200, true, 0.8);
        assertEquals(List.of(T + 700, T + 1200), born());
        WaterRings.tick(T + 1250, false, 0);
        assertEquals(List.of(T + 700, T + 1200), born(), "none while no fish swims in");
    }

    private static List<Long> born() {
        return rings().stream().map(WaterRings.Ring::born).toList();
    }

    @Test
    void theBiteGetsOneBiggerRing() {
        WaterRings.onBobber(0, 63, 0, T);
        WaterRings.onBite(T + 10);
        WaterRings.onBite(T + 900); // the server sends BITE! again while the fish is hooked
        WaterRings.onBite(T + 2000);
        assertEquals(1, rings().size());
        assertEquals(1.4f, rings().get(0).to());
        WaterRings.onBite(T + 5000);
        assertEquals(1, rings().size(), "the bobber was not seen for 2 s: no spot");
        WaterRings.onBobber(0, 63, 0, T + 7000);
        WaterRings.onBite(T + 7000);
        assertEquals(2, rings().size(), "a new bite after a pause");
    }

    @Test
    void theWakeKeepsItsSpacing() {
        WaterRings.onWake(4, 63, 0, T);
        WaterRings.onWake(3.9, 63, 0, T + 200); // 0.1 block: too close
        WaterRings.onWake(3.6, 63, 0, T + 250); // far enough
        WaterRings.onWake(3.2, 63, 0, T + 300); // far enough but only 50 ms later
        WaterRings.onWake(3.2, 63, 0, T + 400);
        assertEquals(List.of(4.0, 3.6, 3.2), rings().stream().map(WaterRings.Ring::x).toList());
        WaterRings.Ring wake = rings().get(0);
        assertEquals(0.15f, wake.from());
        assertEquals(0.6f, wake.to());
        assertEquals(700, wake.life());
        assertEquals(0.6f * 0.55f, wake.strength(), 1e-6, "fainter than the bite rings");
    }

    @Test
    void atMost48Rings() {
        for (int i = 0; i < 60; i++) {
            WaterRings.onWake(i, 63, 0, T + i * 200L);
        }
        assertEquals(WaterRings.MAX_RINGS, rings().size());
        assertEquals(12, rings().get(0).x(), "the oldest went first");
        assertEquals(59, rings().get(47).x());
    }

    @Test
    void theCatchRingTakesTheRarityColour() {
        assertEquals(0xFFFFFF, WaterRings.catchColor("common"));
        assertEquals(0.5f, WaterRings.catchStrength("common"));
        assertEquals(0x2B85C4, WaterRings.catchColor("rare"));
        assertEquals(0x1CD832, WaterRings.catchColor("epic"));
        assertEquals(0xD98103, WaterRings.catchColor("legendary"));
        assertEquals(0xC93832, WaterRings.catchColor("mythical"));
        assertEquals(0xDD7ACF, WaterRings.catchColor("special"));
        assertEquals(1f, WaterRings.catchStrength("rare"));
        assertEquals(0xFFFFFF, WaterRings.catchColor(""), "unknown: faint white like a common");
        assertEquals(0.5f, WaterRings.catchStrength(""));

        WaterRings.onBobber(1, 63, 2, T);
        assertNotNull(WaterRings.onCatch("common", T + 300));
        WaterRings.Ring common = rings().get(0);
        assertEquals(0.3f, common.from());
        assertEquals(1.5f, common.to());
        assertEquals(500, common.life());
        assertEquals(0.3f, common.strength(), 1e-6, "half of the 60% opacity");
        WaterRings.onCatch("mythical", T + 400);
        assertEquals(0xC93832, rings().get(1).rgb());
        assertEquals(0.6f, rings().get(1).strength(), 1e-6);
    }

    @Test
    void sparklesForLegendaryAndUp() {
        assertEquals(0, WaterRings.sparkles("common"));
        assertEquals(0, WaterRings.sparkles("rare"));
        assertEquals(0, WaterRings.sparkles("epic"));
        assertEquals(6, WaterRings.sparkles("legendary"));
        assertEquals(10, WaterRings.sparkles("mythical"));
        assertEquals(10, WaterRings.sparkles("special"));
        TackleboxSettings.get().waterSparkles = false;
        assertEquals(0, WaterRings.sparkles("mythical"));
    }

    @Test
    void theBobberSpotIsKeptTwoSeconds() {
        assertNull(WaterRings.bobber(T));
        WaterRings.onBobber(7, 62.9, 8, T);
        assertEquals(new WaterRings.Spot(7, 62.9, 8, T), WaterRings.bobber(T + 2000));
        assertNull(WaterRings.bobber(T + 2001));
        assertNull(WaterRings.onCatch("rare", T + 2500), "caught long after the bobber was seen: no ring");
        assertTrue(rings().isEmpty());
    }

    @Test
    void eachSwitchKeepsItsRingsAway() {
        TackleboxSettings s = TackleboxSettings.get();
        WaterRings.onBobber(0, 63, 0, T);
        s.waterBiteRings = false;
        WaterRings.tick(T, true, 1);
        WaterRings.onBite(T);
        s.waterWake = false;
        WaterRings.onWake(2, 63, 0, T);
        s.waterCatchRing = false;
        assertNotNull(WaterRings.onCatch("legendary", T), "the spot still goes to the sparkles");
        assertTrue(rings().isEmpty());

        s.waterBiteRings = s.waterWake = s.waterCatchRing = true;
        s.waterFx = false;
        WaterRings.tick(T + 3000, true, 1);
        WaterRings.onWake(5, 63, 0, T + 3000);
        assertNull(WaterRings.onCatch("legendary", T + 100));
        assertEquals(0, WaterRings.sparkles("legendary"));
        assertTrue(rings().isEmpty());
        assertFalse(WaterRings.wantsApproach(s));
        s.waterFx = true;
        s.waterBiteRings = false;
        assertTrue(WaterRings.wantsApproach(s), "the wake alone still wants the fish particles");
    }

    @Test
    void ringsThatAreOverGoAndSwitchingOffClearsThem() {
        WaterRings.onWake(0, 63, 0, T);
        WaterRings.tick(T + 699, false, 0);
        assertEquals(1, rings().size());
        WaterRings.tick(T + 700, false, 0);
        assertTrue(rings().isEmpty());
        WaterRings.onWake(1, 63, 0, T + 800);
        TackleboxSettings.get().waterFx = false;
        WaterRings.tick(T + 810, false, 0);
        assertTrue(rings().isEmpty());
    }
}
