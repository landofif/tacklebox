package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Every hud heading is off unless it is switched on (luna, 2026-11-17: "remove those titles by default"). */
class HudHeadingsTest {
    @Test
    void newSettingsHaveEveryHeadingOff() {
        TackleboxSettings s = new TackleboxSettings();
        assertFalse(s.levelBarsHeading, "level progress");
        assertFalse(s.nowPlayingHeading, "now playing");
        assertFalse(s.questsHeading, "quests left");
        assertFalse(s.boostsHeading, "active");
        assertFalse(s.eventHeading, "event");
        assertFalse(s.cryptidHeading, "cryptids");
        for (HudSpot spot : List.of(s.timersHud, s.slotsLeftHud, s.gearHud, s.miniEventHud)) {
            assertFalse(spot.heading, "timers, inventory, warning and mini event");
        }
    }

    @Test
    void aSpotStartsWithoutItsHeading() {
        assertFalse(new HudSpot().heading);
        assertFalse(new HudSpot(4, 4).heading);
    }
}
