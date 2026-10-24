package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.trigger.Matcher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Menu items as {@code Glyphs.plain} gives them for luna's /event dump of Halloween 2026, and the chat lines of her latest.log
 * of 2026-10-24 (real newlines where the log shows {@code \n}).
 */
class EventHudTest {
    private static EventHud.Item threshold(int n, String range, String... rewards) {
        List<String> lines = new java.util.ArrayList<>(List.of("threshold " + n, "", range, ""));
        lines.addAll(List.of(rewards));
        return new EventHud.Item("threshold " + n, lines);
    }

    private static List<EventHud.Item> menu(String have, String points) {
        return List.of(
                threshold(1, "[" + have + " pts|220 pts]", "$25k", "20x mozzarella cube", "graphic: halloween 2026", "", "1x standard bait package", "┗ freshwater"),
                threshold(2, "[" + have + " pts|840 pts]", "$50k", "50x chicken nugget", "", "1x standard bait package", "┗ freshwater"),
                threshold(3, "[" + have + " pts|1.5k pts]", "$80k", "50x cut carp", "tag:"),
                new EventHud.Item("event missions → " + points + " pts", List.of("event missions → " + points + " pts", "")),
                new EventHud.Item("welcome to halloween 2026!", List.of("welcome to halloween 2026!", "check out the event missions!")),
                new EventHud.Item("event leaderboards", List.of("event leaderboards", "your placement: unranked", "event points: " + points + " pts", "", "refreshes every 5 minutes.")));
    }

    private static boolean chat(String raw, long now) {
        return EventHud.onChat(Matcher.normalize(raw), now);
    }

    private static String mission(int gained, String name, String done, String progress) {
        return "\nHALLOWEEN 2026 EVENT MISSION [+" + gained + " pts]\n  🎣 " + name + " " + done + "\n    ┗ " + progress + "\n";
    }

    private static String value(List<String[]> rows, String label) {
        return rows.stream().filter(r -> r[0].equals(label)).map(r -> r[1]).findFirst().orElse(null);
    }

    @BeforeEach
    void clear() {
        EventHud.clear();
    }

    @Test
    void nothingReadYetShowsDashesOnlyInPreview() {
        assertTrue(EventHud.rows(false, true, false).isEmpty());
        assertEquals("-", EventHud.rows(true, true, false).get(0)[1]);
    }

    @Test
    void zeroPointsPointAtTheFirstThresholdAndItsRewards() {
        assertTrue(EventHud.read(menu("<none>", "<none>")));
        List<String[]> rows = EventHud.rows(false, true, false);
        assertArrayEquals(new String[]{"points", "0"}, rows.get(0));
        assertArrayEquals(new String[]{"threshold 1", "220 to go"}, rows.get(1));
        assertEquals("  $25k", rows.get(2)[0]);
        assertEquals("  1x standard bait package", rows.get(5)[0]);
        assertEquals("0/3", value(rows, "thresholds"));
        assertEquals("unranked", value(rows, "placement"));
        assertFalse(EventHud.read(menu("<none>", "<none>")), "reading the same menu again is no change");
    }

    @Test
    void pointsMoveToTheNextThreshold() {
        EventHud.read(menu("900", "900"));
        List<String[]> rows = EventHud.rows(false, true, false);
        assertEquals("900", value(rows, "points"));
        assertEquals("600 to go", value(rows, "threshold 3"));
        assertEquals("2/3", value(rows, "thresholds"));
        EventHud.read(menu("2k", "2k"));
        assertNotNull(value(EventHud.rows(false, true, false), "all thresholds done"));
    }

    @Test
    void aMenuWithoutThresholdsChangesNothing() {
        EventHud.read(menu("500", "500"));
        assertFalse(EventHud.read(List.of(new EventHud.Item("close", List.of()))));
        assertEquals("500", EventHud.rows(false, true, false).get(0)[1]);
    }

    @Test
    void limitedMissionCountsUpAndFinishedOnesHideUnlessAsked() {
        EventHud.read(menu("0", "0"));
        assertTrue(chat(mission(50, "Murray Cod", "", "(1/10) [50 pts|50 pts] Partial"), 10_000));
        assertTrue(chat(mission(50, "Murray Cod", "", "(2/10) [100 pts|50 pts] Partial"), 20_000));
        List<String[]> rows = EventHud.rows(false, true, false);
        assertEquals("2/10", value(rows, "murray cod"));
        assertEquals("100", value(rows, "points"));
        assertNull(value(EventHud.rows(false, false, false), "murray cod"), "missions switched off");

        chat(mission(50, "Murray Cod", "✔", "(10/10) [500 pts|50 pts] Partial"), 30_000);
        assertNull(value(EventHud.rows(false, true, false), "murray cod"));
        assertEquals("done", value(EventHud.rows(false, true, true), "murray cod"));
    }

    @Test
    void repeatableMissionShowsWhatItGaveSoFar() {
        EventHud.read(menu("0", "0"));
        chat(mission(2, "River Monsters", "", "(0/10) [2 pts|2 pts] ∞"), 10_000);
        chat(mission(2, "River Monsters", "", "(0/10) [4 pts|2 pts] ∞"), 20_000);
        assertEquals("∞ 4 pts", value(EventHud.rows(false, true, false), "river monsters"));
        assertEquals("4", value(EventHud.rows(false, true, false), "points"));
    }

    @Test
    void oneTimeAndWeightMissions() {
        EventHud.read(menu("0", "0"));
        chat(mission(500, "Tiger Muskellunge ≥ 18lb", "✔", "(1/1) [500 pts|500 pts] "), 10_000);
        chat("\nHALLOWEEN 2026 EVENT MISSION [+50.86 pts]\n  🎣 Heaviest Arapaima \n    ┗ (pts = Weight) 50.86 pts \n", 20_000);
        List<String[]> rows = EventHud.rows(false, true, true);
        assertEquals("done", value(rows, "tiger muskellunge ≥ 18lb"));
        assertEquals("50.86 pts", value(rows, "heaviest arapaima"));
        assertEquals("550.86", value(rows, "points"));
    }

    @Test
    void tierUpSetsThePointsAndTheMissionOfTheSameCatchIsNotCountedTwice() {
        EventHud.read(menu("200", "200"));
        assertTrue(chat("HALLOWEEN 2026 EVENT THRESHOLD tier up! \n[250 pts|220 pts]", 100_000));
        chat(mission(50, "Murray Cod", "", "(5/10) [250 pts|50 pts] Partial"), 100_000);
        assertEquals("250", value(EventHud.rows(false, true, false), "points"));
        assertEquals("5/10", value(EventHud.rows(false, true, false), "murray cod"));
        chat(mission(50, "Murray Cod", "", "(6/10) [300 pts|50 pts] Partial"), 160_000);
        assertEquals("300", value(EventHud.rows(false, true, false), "points"));
    }

    @Test
    void aPlayerQuotingTheLinesInChatIsIgnored() {
        EventHud.read(menu("100", "100"));
        assertFalse(chat("[373] Realstranden » HALLOWEEN 2026 EVENT THRESHOLD tier up! [500 pts|220 pts]", 1));
        assertFalse(chat("[353] flown06 » Heaviest Arapaima (pts = Weight) 143.04 pts", 2));
        assertEquals("100", value(EventHud.rows(false, true, false), "points"));
    }

    @Test
    void stateSurvivesTheSettingsFile() {
        EventHud.read(menu("100", "100"));
        chat(mission(2, "River Monsters", "", "(0/10) [2 pts|2 pts] \u221E"), 10_000);
        var gson = new com.google.gson.Gson();
        EventHud.State saved = new EventHud.State();
        EventHud.bind(saved);
        EventHud.read(menu("100", "100"));
        chat(mission(2, "River Monsters", "", "(0/10) [2 pts|2 pts] \u221E"), 10_000);
        EventHud.State back = gson.fromJson(gson.toJson(saved), EventHud.State.class);
        assertEquals(saved.thresholds, back.thresholds);
        assertEquals(saved.missions, back.missions);
        assertEquals(saved.points, back.points);
    }

    @Test
    void shortForms() {
        assertEquals("1.5K", EventHud.fmt(1500));
        assertEquals("2.25M", EventHud.fmt(2_250_000));
        assertEquals("840", EventHud.fmt(840));
        assertEquals("1K", EventHud.fmt(1000));
    }
}
