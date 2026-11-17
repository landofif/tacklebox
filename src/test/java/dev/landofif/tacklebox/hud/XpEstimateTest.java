package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.menu.FishMenu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Location levels: 20 is the top, xp past it never makes a level 21. */
class XpEstimateTest {
    @Test
    void lunasSavedBiwaIsLevelTwentyNotTwentyOne() {
        // luna's settings.json of 2026-11-17: "biwa": [20.0, 2315250.0], the capped compendium count times the cost of a level 21
        assertEquals(20, (int) XpEstimate.toNext(new double[]{20, 2_315_250})[0]);
    }

    @Test
    void xpPastTheTopNeverAddsALevel() {
        double[] at = XpEstimate.toNext(new double[]{20, 14_420_000});
        assertEquals(20, (int) at[0]);
        assertEquals(14_420_000, XpEstimate.need(21) - at[1], 0.5, "what is left to a level 21 gives back the xp past the max");
    }

    @Test
    void compendiumCountPastTheMaxIsKept() {
        FishMenu.Info biwa = FishMenu.info("Lake Biwa\nLevel: [20] (14.42M/2.32M)\nCatches: 7,955");
        assertEquals(20, biwa.level());
        assertEquals(1f, biwa.progress(), "the progress stops at 1");
        assertEquals(14_420_000, biwa.xp(), 0.5);
        FishMenu.Info key = FishMenu.info("Key West\nLevel: [11] (929.25/432K)");
        assertEquals(11, key.level());
        assertEquals(929.25, key.xp(), 0.001);
    }

    @Test
    void readKeepsTheXpPastTheMaxAtTheTopOnly() {
        XpEstimate.read("test_top", FishMenu.info("Lake Biwa\nLevel: [20] (14.42M/2.32M)"));
        assertEquals(14_420_000, XpEstimate.need(21) - XpEstimate.toNext("test_top")[1], 0.5);
        XpEstimate.read("test_mid", FishMenu.info("Key West\nLevel: [11] (216K/432K)"));
        assertEquals(216_000, XpEstimate.need(12) - XpEstimate.toNext("test_mid")[1], 0.5);
    }

    @Test
    void levelRowShowsOnlyTheXpPastTheTop() {
        double[] top = {20, XpEstimate.need(21) - 14_420_000};
        assertEquals("14.4M", LevelBars.locationXp(top, false));
        assertEquals("14.42M", LevelBars.locationXp(top, true));
        assertEquals("929/432Klxp", LevelBars.locationXp(new double[]{11, XpEstimate.need(12) - 929.25}, false));
    }

    @Test
    void levelsBelowTheTopStillGoUp() {
        assertEquals(12, (int) XpEstimate.toNext(new double[]{11, XpEstimate.need(12) + 100})[0]);
        double[] top = XpEstimate.toNext(new double[]{19, XpEstimate.need(20) + 5000});
        assertEquals(20, (int) top[0], "19 to 20 by chat xp");
        assertEquals(5000, XpEstimate.need(21) - top[1], 0.5, "the rest is past the max");
    }
}
