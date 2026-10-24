package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Items as {@code Glyphs.plain} gives them for luna's /event dump of Halloween 2026 (the art glyphs are already gone). */
class EventHudTest {
    private static EventHud.Item threshold(int n, String range, String... rewards) {
        List<String> lines = new java.util.ArrayList<>(List.of("threshold " + n, "", range, ""));
        lines.addAll(List.of(rewards));
        return new EventHud.Item("threshold " + n, lines);
    }

    private static List<EventHud.Item> menu(String have, String book, String points) {
        return List.of(
                threshold(1, "[" + have + " pts|220 pts]", "$25k", "20x mozzarella cube", "graphic: halloween 2026", "", "1x standard bait package", "┗ freshwater"),
                threshold(2, "[" + have + " pts|840 pts]", "$50k", "50x chicken nugget", "", "1x standard bait package", "┗ freshwater"),
                threshold(3, "[" + have + " pts|1.5k pts]", "$80k", "50x cut carp", "tag:"),
                new EventHud.Item("event missions → " + book + " pts", List.of("event missions → " + book + " pts", "")),
                new EventHud.Item("welcome to halloween 2026!", List.of("welcome to halloween 2026!", "check out the event missions!")),
                new EventHud.Item("event leaderboards", List.of("event leaderboards", "your placement: unranked", "event points: " + points + " pts", "", "refreshes every 5 minutes.")));
    }

    @BeforeEach
    void clear() {
        EventHud.clear();
    }

    @Test
    void nothingReadYetShowsDashesOnlyInPreview() {
        assertTrue(EventHud.rows(false).isEmpty());
        assertEquals("-", EventHud.rows(true).get(0)[1]);
    }

    @Test
    void zeroPointsPointAtTheFirstThresholdAndItsRewards() {
        assertTrue(EventHud.read(menu("<none>", "<none>", "<none>")));
        List<String[]> rows = EventHud.rows(false);
        assertArrayEquals(new String[]{"points", "0"}, rows.get(0));
        assertArrayEquals(new String[]{"threshold 1", "220 to go"}, rows.get(1));
        assertEquals("  $25k", rows.get(2)[0]);
        assertEquals("  1x standard bait package", rows.get(5)[0]);
        assertArrayEquals(new String[]{"thresholds", "0/3"}, rows.get(6));
        assertArrayEquals(new String[]{"placement", "unranked"}, rows.get(7));
    }

    @Test
    void pointsMoveToTheNextThreshold() {
        EventHud.read(menu("900", "900", "900"));
        List<String[]> rows = EventHud.rows(false);
        assertArrayEquals(new String[]{"points", "900"}, rows.get(0));
        assertArrayEquals(new String[]{"threshold 3", "600 to go"}, rows.get(1));
        assertArrayEquals(new String[]{"thresholds", "2/3"}, rows.get(rows.size() - 2));
    }

    @Test
    void everyThresholdDone() {
        EventHud.read(menu("2k", "2k", "2k"));
        assertEquals("all thresholds done", EventHud.rows(false).get(1)[0]);
    }

    @Test
    void aMenuWithoutThresholdsChangesNothing() {
        EventHud.read(menu("500", "500", "500"));
        assertFalse(EventHud.read(List.of(new EventHud.Item("close", List.of()))));
        assertEquals("500", EventHud.rows(false).get(0)[1]);
    }

    @Test
    void shortForms() {
        assertEquals("1.5K", EventHud.fmt(1500));
        assertEquals("2.25M", EventHud.fmt(2_250_000));
        assertEquals("840", EventHud.fmt(840));
        assertEquals("1K", EventHud.fmt(1000));
    }
}
