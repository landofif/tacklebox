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
        assertTrue(EventHud.rows(false, true).isEmpty());
        assertEquals("-", EventHud.rows(true, true).get(0)[1]);
    }

    @Test
    void zeroPointsPointAtTheFirstThresholdAndItsRewards() {
        assertTrue(EventHud.read(menu("<none>", "<none>")));
        List<String[]> rows = EventHud.rows(false, true);
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
        List<String[]> rows = EventHud.rows(false, true);
        assertEquals("900", value(rows, "points"));
        assertEquals("600 to go", value(rows, "threshold 3"));
        assertEquals("2/3", value(rows, "thresholds"));
        EventHud.read(menu("2k", "2k"));
        assertNotNull(value(EventHud.rows(false, true), "all thresholds done"));
    }

    @Test
    void aMenuWithoutThresholdsChangesNothing() {
        EventHud.read(menu("500", "500"));
        assertFalse(EventHud.read(List.of(new EventHud.Item("close", List.of()))));
        assertEquals("500", EventHud.rows(false, true).get(0)[1]);
    }

    private static final List<String> BOOK = List.of("event missions → 2.61k pts", "",
            "- 🎣 river monsters", "┗ (1/10) [24 pts|2 pts] ∞",
            "- 🎣 freshwater", "┗ (6/100) [10 pts|2 pts] ∞",
            "- 🎣 amazon", "┗ (40/1500) [0 pts|400 pts]",
            "- 🎣 heaviest arapaima", "┗ (pts = weight) 50.86 pts",
            "- 🎣 tiger muskellunge ≥ 18lb ✔", "┗ (1/1) [500 pts|500 pts]",
            "- 🎣 murray cod ✔", "┗ (10/10) [500 pts|50 pts] partial",
            "- 🎣 wels catfish", "┗ (3/10) [150 pts|50 pts] partial",
            "- 🎣 river monsters albino", "┗ (0/1) [0 pts|10 pts] ∞");

    private static List<EventHud.Item> bookMenu() {
        List<Integer> colors = new java.util.ArrayList<>(java.util.Collections.nCopies(BOOK.size(), -1));
        colors.set(2, 0x55FFAA);
        colors.set(4, 0x55AAFF);
        colors.set(6, 0xFFAA00);
        return List.of(new EventHud.Item("event missions → 2.61k pts", BOOK, colors));
    }

    @Test
    void theBookListsEveryMissionAndTheHudKeepsTheOpenAndTheRepeatableOnes() {
        assertTrue(EventHud.read(bookMenu()));
        List<String[]> rows = EventHud.rows(false, true);
        assertEquals("1/10 24pts/2pts", value(rows, "river monsters"));
        assertEquals("40/1500 400pts", value(rows, "amazon"));
        assertNull(value(rows, "heaviest arapaima"), "left out");
        assertEquals("3/10 150pts/50pts", value(rows, "wels catfish"));
        assertNull(value(rows, "river monsters albino"), "left out");
        assertNull(value(rows, "tiger muskellunge ≥ 18lb"), "done ones are hidden");
        assertNull(value(rows, "murray cod"));
        assertNull(value(EventHud.rows(false, false), "amazon"), "missions switched off");
        assertFalse(EventHud.read(bookMenu()), "reading the same book again is no change");
    }

    @Test
    void aCatchAtALocationCountsItsMission() {
        EventHud.read(bookMenu());
        assertTrue(EventHud.onCatch("amazon"));
        assertEquals("41/1500 400pts", value(EventHud.rows(false, true), "amazon"));
        assertTrue(EventHud.onCatch("kenai"), "fresh water: the Freshwater mission counts it");
        assertFalse(EventHud.onCatch("keywest"), "salt water, and no mission for the location");
        assertEquals("8/100 10pts/2pts", value(EventHud.rows(false, true), "freshwater"), "amazon and kenai are fresh water");
    }

    @Test
    void missionsTakeTheMenusColours() {
        EventHud.read(bookMenu());
        List<String[]> rows = EventHud.rows(false, true);
        String[] river = rows.stream().filter(r -> r[0].equals("river monsters")).findFirst().orElseThrow();
        assertEquals(Integer.toString(0xFF55FFAA), river[2]);
        assertEquals(Integer.toString(0xFF55FF55), river[3]);
        assertEquals(Integer.toString(0xFF55FF55), EventHud.rows(false, true).stream().filter(r -> r[0].equals("amazon")).findFirst().orElseThrow()[2], "gold becomes bright green");
        String[] unknown = rows.stream().filter(r -> r[0].equals("wels catfish")).findFirst().orElseThrow();
        assertEquals(Integer.toString(0xFFFFFFFF), unknown[2]);
    }

    @Test
    void chatKeepsTheBookedMissionsCurrentAndKeepsTheirColour() {
        EventHud.read(menu("0", "0"));
        EventHud.read(bookMenu());
        chat(mission(50, "Wels Catfish", "", "(4/10) [200 pts|50 pts] Partial"), 10_000);
        assertEquals("4/10 200pts/50pts", value(EventHud.rows(false, true), "wels catfish"));
        chat(mission(2, "River Monsters", "", "(1/10) [26 pts|2 pts] ∞"), 20_000);
        assertEquals("1/10 26pts/2pts", value(EventHud.rows(false, true), "river monsters"), "the chat line says 0, the book count stays");
        assertEquals(Integer.toString(0xFF55FFAA), EventHud.rows(false, true).stream().filter(r -> r[0].equals("river monsters")).findFirst().orElseThrow()[2]);
        chat(mission(50, "Wels Catfish", "✔", "(10/10) [500 pts|50 pts] Partial"), 30_000);
        assertNull(value(EventHud.rows(false, true), "wels catfish"), "done: gone");
    }

    @Test
    void rewardsOfTheNextThresholdKeepTheirColours() {
        EventHud.Item head = new EventHud.Item("threshold 1", List.of("threshold 1", "", "[<none> pts|220 pts]", "", "$25k", "20x mozzarella cube"), List.of(0xFF5555, -1, 0xFFFF55, -1, 0x55FF55, -1));
        EventHud.read(List.of(head, new EventHud.Item("event leaderboards", List.of("your placement: unranked", "event points: <none> pts"))));
        List<String[]> rows = EventHud.rows(false, true);
        String[] money = rows.stream().filter(r -> r[0].equals("  $25k")).findFirst().orElseThrow();
        assertEquals(Integer.toString(0xFF55FF55), money[2]);
    }

    @Test
    void oneTimeAndWeightMissionsFromChat() {
        EventHud.read(menu("0", "0"));
        chat(mission(500, "Tiger Muskellunge ≥ 18lb", "✔", "(1/1) [500 pts|500 pts] "), 10_000);
        chat("\nHALLOWEEN 2026 EVENT MISSION [+50.86 pts]\n  🎣 Heaviest Arapaima \n    ┗ (pts = Weight) 50.86 pts \n", 20_000);
        List<String[]> rows = EventHud.rows(false, true);
        assertNull(value(rows, "tiger muskellunge ≥ 18lb"));
        assertEquals("550.86", value(rows, "points"));
    }

    @Test
    void tierUpSetsThePointsAndTheMissionOfTheSameCatchIsNotCountedTwice() {
        EventHud.read(menu("200", "200"));
        assertTrue(chat("HALLOWEEN 2026 EVENT THRESHOLD tier up! \n[250 pts|220 pts]", 100_000));
        chat(mission(50, "Murray Cod", "", "(5/10) [250 pts|50 pts] Partial"), 100_000);
        assertEquals("250", value(EventHud.rows(false, true), "points"));
        assertEquals("5/10 250pts/50pts", value(EventHud.rows(false, true), "murray cod"));
        chat(mission(50, "Murray Cod", "", "(6/10) [300 pts|50 pts] Partial"), 160_000);
        assertEquals("300", value(EventHud.rows(false, true), "points"));
    }

    @Test
    void aPlayerQuotingTheLinesInChatIsIgnored() {
        EventHud.read(menu("100", "100"));
        assertFalse(chat("[373] Realstranden » HALLOWEEN 2026 EVENT THRESHOLD tier up! [500 pts|220 pts]", 1));
        assertFalse(chat("[353] flown06 » Heaviest Arapaima (pts = Weight) 143.04 pts", 2));
        assertEquals("100", value(EventHud.rows(false, true), "points"));
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
