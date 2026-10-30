package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.menu.MenuRows;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The /event panel rows, with thresholds and missions as luna's Halloween 2026 dumps have them. */
class EventPageTest {
    @Test
    void thresholdShowsTwoRewardsAndItsBar() {
        MenuRows.Row row = EventPage.threshold(new EventHud.Threshold(2, 420, 840, List.of("$50k", "50x chicken nugget", "1x standard bait package")));
        assertEquals("Threshold 2", row.name());
        assertEquals("$50k · 50x chicken nugget", row.sub());
        assertEquals(0.5f, row.progress(), 1e-4);
        assertEquals("420/840", row.amount());
    }

    @Test
    void reachedThresholdIsFullAndDoesNotCountPastItsNeed() {
        MenuRows.Row row = EventPage.threshold(new EventHud.Threshold(1, 2610, 220, List.of("$25k")));
        assertTrue(row.done());
        assertEquals("220/220", row.amount());
        assertEquals("", EventPage.threshold(new EventHud.Threshold(7, 0, 0, List.of())).sub());
    }

    @Test
    void missionKinds() {
        assertEquals(new MenuRows.Row("wels catfish", "50 pts each", -1, 0.3f, "3/10"),
                EventPage.mission(new EventHud.Mission("wels catfish", "partial", 3, 10, 150, 50, false, -1)));
        assertEquals(new MenuRows.Row("freshwater", "2 pts every 100, 10 pts so far", -1, 0.06f, "6/100"),
                EventPage.mission(new EventHud.Mission("freshwater", "repeat", 6, 100, 10, 2, false, -1)));
        assertEquals(new MenuRows.Row("heaviest arapaima", "best weight", -1, -1, "50.86 pts"),
                EventPage.mission(new EventHud.Mission("heaviest arapaima", "weight", 0, 0, 50.86, 50.86, false, -1)));
        assertEquals(1f, EventPage.mission(new EventHud.Mission("murray cod", "once", 1, 1, 500, 500, true, -1)).progress());
    }
}
