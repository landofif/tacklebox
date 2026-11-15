package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MiniEventTest {
    /** A /minievent answer from luna's log (2026-11, Mediterranean Sea), one entry per chat message. */
    static final List<String> ANSWER = List.of(
            "MED SEA MINI EVENT » Ends in 2d 1h 13m 26s",
            "Daily missions reroll in 18h 13m 26s",
            " - 🎣 Mediterranean Sea  \n    ┗ (59/100) ∞ x25",
            " - 🎣 Lynx Pet  \n    ┗ (0/1) ∞ x10",
            " - 🎣 Mediterranean Sea ɢɪɢᴀɴᴛɪᴄ  ✔\n    ┗ (2/2) One Timer",
            " - 🎣 Mediterranean Sea   \n    ┗ (0/1) One Timer",
            " - 🎣 Mediterranean Sea   \n    ┗ (0/1) One Timer",
            " - 🎣 Mediterranean Sea   \n    ┗ (0/1) One Timer",
            " - 🎣 Mediterranean Sea Rays  \n    ┗ (16/18) Daily",
            " - 🎣 Mediterranean Sea Tunas  ✔\n    ┗ (23/23) Daily",
            " - 🎣 Seabreams  ✔\n    ┗ (35/35) Daily",
            " - 🎣 Mediterranean Sea  \n    ┗ (9/137) Daily",
            " - 🎣 Adriatic Sturgeon  \n    ┗ (0/1) Daily",
            " - 🎣 Mediterranean Sea Atlantic Bluefin Tuna  \n    ┗ (1/3) Daily");

    private static final long T = 1_800_000_000_000L;

    @BeforeEach
    void setUp() {
        MiniEvent.reset();
    }

    static void answer(long now) {
        for (String line : ANSWER) {
            MiniEvent.onChat(line, now);
        }
    }

    private static MiniEvent.Fish fish(String location, String variant, String size, String group, String name) {
        return new MiniEvent.Fish(location, variant, size, "common", group, name);
    }

    @Test
    void readsTheLoggedAnswer() {
        answer(T);
        assertTrue(MiniEvent.on(T));
        assertEquals("med sea mini event", MiniEvent.title());
        assertEquals(12, MiniEvent.missions().size());
        MiniEvent.Mission first = MiniEvent.missions().get(0);
        assertEquals("mediterranean sea", first.name);
        assertEquals(59, first.cur);
        assertEquals(100, first.max);
        assertEquals("∞ x25", first.mark);
        assertTrue(first.repeats());
        assertEquals("mediterranean sea gigantic", MiniEvent.missions().get(2).name);
        assertEquals("mediterranean sea albino", MiniEvent.missions().get(3).name);
        assertEquals("mediterranean sea melanistic", MiniEvent.missions().get(4).name);
        assertEquals("mediterranean sea trophy", MiniEvent.missions().get(5).name);
        assertEquals("daily", MiniEvent.missions().get(8).mark);
    }

    @Test
    void rowsShowTheClocksAndLeaveOutFinishedOneTimersAndDailies() {
        answer(T);
        List<TextHud.Row> rows = MiniEvent.rows(T + 26_000);
        assertEquals(new TextHud.Row("ends in", "2d 1h"), rows.get(0));
        assertEquals(new TextHud.Row("daily reroll", "18h 13m"), rows.get(1));
        assertEquals(new TextHud.Row("med", "59/100 · ∞ x25", 0, 0), rows.get(2)); // the place is shown short, the mission keeps its name
        assertEquals("mediterranean sea", MiniEvent.missions().get(0).name);
        assertTrue(rows.stream().anyMatch(r -> r.label().equals("med rays")));
        assertTrue(rows.stream().noneMatch(r -> r.label().contains("mediterranean")));
        assertEquals(11, rows.size()); // 2 clocks + 12 missions - 3 finished
        assertTrue(rows.stream().noneMatch(r -> r.label().equals("seabreams")));
        assertTrue(MiniEvent.rows(T + MiniEvent.duration("2d 1h 13m 26s")).isEmpty()); // the event is over
    }

    @Test
    void durationsAndClocks() {
        assertEquals(((2 * 24 + 4) * 3600 + 54) * 1000L, MiniEvent.duration("2d 4h 54s"));
        assertEquals("18h 03m", MiniEvent.clock(MiniEvent.duration("18h 3m 59s")));
        assertEquals("4:05", MiniEvent.clock(245_000));
    }

    @Test
    void aCatchCountsForTheMissionsItFits() {
        answer(T);
        MiniEvent.onCatch(fish("mediterranean", "normal", "adult", "rays", "Blonde Skate"), T + 1000);
        assertEquals(60, MiniEvent.missions().get(0).cur);   // any fish at the place, repeating
        assertEquals(10, MiniEvent.missions().get(11 - 2).cur); // any fish at the place, daily
        assertEquals(17, MiniEvent.missions().get(6).cur);   // rays
        assertEquals(0, MiniEvent.missions().get(1).cur);    // lynx pet: left to the server
        assertEquals(0, MiniEvent.missions().get(3).cur);    // albino
        assertEquals(1, MiniEvent.missions().get(11).cur);   // another species

        MiniEvent.onCatch(fish("mediterranean", "albino", "gigantic", "tunas", "atlantic bluefin tuna"), T + 2000);
        assertEquals(1, MiniEvent.missions().get(3).cur);
        assertEquals(0, MiniEvent.missions().get(4).cur);
        assertEquals(2, MiniEvent.missions().get(2).cur);    // gigantic was done: stays at its goal
        assertEquals(2, MiniEvent.missions().get(11).cur);   // the species by name
        assertEquals(23, MiniEvent.missions().get(7).cur);   // tunas was done

        MiniEvent.onCatch(fish("kenai", "normal", "adult", "seabreams", "Adriatic Sturgeon"), T + 3000);
        assertEquals(61, MiniEvent.missions().get(0).cur);   // not at the place
        assertEquals(1, MiniEvent.missions().get(10).cur);   // a species mission names no place
    }

    @Test
    void countsReadsPlaceSizeVariantRarityNameAndGroup() {
        MiniEvent.Fish f = new MiniEvent.Fish("spawnhub", "trophy", "large", "mythic", "bass", "Largemouth Bass");
        assertTrue(MiniEvent.counts("cypress lake", f));
        assertTrue(MiniEvent.counts("cypress lake trophy", f));
        assertTrue(MiniEvent.counts("cypress lake mythical large", f));
        assertTrue(MiniEvent.counts("largemouth bass", f));
        assertTrue(MiniEvent.counts("cypress lake basses", f) || MiniEvent.counts("cypress lake bass", f));
        assertFalse(MiniEvent.counts("cypress lake gigantic", f));
        assertFalse(MiniEvent.counts("cypress lake albino", f));
        assertFalse(MiniEvent.counts("kenai river", f));
        assertFalse(MiniEvent.counts("bullfrog pet", f));
        assertTrue(MiniEvent.counts("toledo bend reservoir", new MiniEvent.Fish("toledobend", "normal", "adult", "", "", "x")));
        assertTrue(MiniEvent.counts("toledo bend", new MiniEvent.Fish("toledobend", "normal", "adult", "", "", "x")));
    }

    @Test
    void aMissionCompleteBlockUpdatesThatMission() {
        answer(T);
        assertFalse(MiniEvent.onChat("\nMED SEA MINI EVENT » MISSION COMPLETE", T + 60_000));
        assertFalse(MiniEvent.onChat(" - 🎣 Mediterranean Sea  \n    ┗ (0/100) ∞ x26\n", T + 60_000));
        assertEquals(0, MiniEvent.missions().get(0).cur);
        assertEquals("∞ x26", MiniEvent.missions().get(0).mark);
        assertEquals(9, MiniEvent.missions().get(9).cur); // the daily of the same name is untouched
        assertEquals(12, MiniEvent.missions().size());
        // a mission line out of nowhere changes nothing
        MiniEvent.onChat(" - 🎣 Mediterranean Sea  \n    ┗ (5/100) ∞ x30", T + 120_000);
        assertEquals("∞ x26", MiniEvent.missions().get(0).mark);
    }

    @Test
    void theFirstSendIsFortySecondsInThenEveryTenMinutesAndNotWithAScreenOpen() {
        assertFalse(MiniEvent.due(T, false));
        assertFalse(MiniEvent.due(T + 39_000, false));
        assertFalse(MiniEvent.due(T + 40_000, true));
        assertTrue(MiniEvent.due(T + 41_000, false));
        answer(T + 41_500);
        assertFalse(MiniEvent.due(T + 42_000, false));
        assertFalse(MiniEvent.due(T + 41_000 + MiniEvent.REFRESH - 1, false));
        assertTrue(MiniEvent.due(T + 41_000 + MiniEvent.REFRESH, false));
    }

    @Test
    void withNoEventItAsksEveryHalfHourAndCatchesDoNotAsk() {
        assertFalse(MiniEvent.due(T, false));
        assertTrue(MiniEvent.due(T + MiniEvent.FIRST, false));
        MiniEvent.askAfterCatch(T + 50_000);
        assertFalse(MiniEvent.due(T + MiniEvent.FIRST + MiniEvent.REFRESH, false));
        assertTrue(MiniEvent.due(T + MiniEvent.FIRST + MiniEvent.IDLE_REFRESH, false));
    }

    @Test
    void aCatchAsksFiveSecondsLaterButNotWithinAMinuteOfTheLastSend() {
        MiniEvent.due(T, false);
        assertTrue(MiniEvent.due(T + MiniEvent.FIRST, false));
        answer(T + MiniEvent.FIRST + 500);
        long sent = T + MiniEvent.FIRST;
        MiniEvent.askAfterCatch(sent + 10_000);
        assertFalse(MiniEvent.due(sent + 15_000, false));
        assertFalse(MiniEvent.due(sent + MiniEvent.CATCH_GAP - 1, false));
        assertTrue(MiniEvent.due(sent + MiniEvent.CATCH_GAP, false));
        MiniEvent.askAfterCatch(sent + 200_000);
        assertFalse(MiniEvent.due(sent + 204_000, false));
        assertTrue(MiniEvent.due(sent + 205_000, false));
    }

    @Test
    void onlyTheAnswerToTheModsOwnSendIsHidden() {
        // typed by the player: read, shown
        for (String line : ANSWER) {
            assertFalse(MiniEvent.onChat(line, T), line);
        }
        MiniEvent.due(T + 10_000, false);
        assertTrue(MiniEvent.due(T + 10_000 + MiniEvent.FIRST, false));
        long sent = T + 10_000 + MiniEvent.FIRST;
        assertFalse(MiniEvent.onChat("[188] DoguukanCn » w", sent + 100));
        for (String line : ANSWER) {
            assertTrue(MiniEvent.onChat(line, sent + 300), line);
        }
        assertEquals(12, MiniEvent.missions().size());
        // typed again right after: the hiding ended with the answer
        for (String line : ANSWER) {
            assertFalse(MiniEvent.onChat(line, sent + 5000), line);
        }
        // the announcements are never hidden
        assertTrue(MiniEvent.due(sent + MiniEvent.REFRESH, false));
        assertFalse(MiniEvent.onChat("MED SEA MINI EVENT » You have completed every Mini Event Mission in the Mediterranean today!", sent + MiniEvent.REFRESH + 100));
        assertFalse(MiniEvent.onChat("KENAI MINI EVENT » Mini Event missions are now available. (/minievent)", sent + MiniEvent.REFRESH + 100));
        assertFalse(MiniEvent.onChat("[404] someone » MED SEA MINI EVENT MISSION COMPLETE - Lynx Pet", sent + MiniEvent.REFRESH + 100));
    }

    @Test
    void anotherAnswerToTheModsSendIsHiddenAndClearsTheEvent() {
        answer(T);
        MiniEvent.due(T, false);
        assertTrue(MiniEvent.due(T + MiniEvent.FIRST, false));
        assertTrue(MiniEvent.onChat("MINI EVENT » There is no event running right now.", T + MiniEvent.FIRST + 200));
        assertFalse(MiniEvent.on(T + MiniEvent.FIRST + 300));
        assertTrue(MiniEvent.rows(T + MiniEvent.FIRST + 300).isEmpty());
    }

    // ---------------------------------------------------------------- colours

    private static final int PINK = 0xFFFF55FF, GREEN = 0xFF55FF55, GOLD = 0xFFFFAA00, GRAY = 0xFFAAAAAA, PURPLE = 0xFFAA00AA;

    /** The colour of every character of {@code line}: {@code runs} are a text of the line and its colour in turn, the rest has none. */
    private static int[] colors(String line, Object... runs) {
        int[] out = new int[line.length()];
        int from = 0;
        for (int i = 0; i < runs.length; i += 2) {
            int at = line.indexOf((String) runs[i], from);
            assertTrue(at >= 0, (String) runs[i]);
            from = at + ((String) runs[i]).length();
            java.util.Arrays.fill(out, at, from, (Integer) runs[i + 1]);
        }
        return out;
    }

    private static void colored(String line, Object... runs) {
        MiniEvent.onChat(line, colors(line, runs), T);
    }

    private static TextHud.Row row(String label) {
        return MiniEvent.rows(T).stream().filter(r -> r.label().equals(label)).findFirst().orElseThrow();
    }

    @Test
    void aMissionKeepsTheColoursOfItsChatLine() {
        MiniEvent.onChat(ANSWER.get(0), T);
        colored(" - 🎣 Lynx Pet  \n    ┗ (0/1) ∞ x12", "Lynx Pet", PINK, "(0/1)", GRAY, "∞", PURPLE, "x12", GRAY);
        TextHud.Row row = row("lynx pet");
        assertEquals("0/1 · ∞ x12", row.value());
        assertEquals(List.of(new TextHud.Part("lynx pet", PINK)), row.labelParts());
        assertEquals(List.of(new TextHud.Part("0/1 · ", 0), new TextHud.Part("∞ ", PURPLE), new TextHud.Part("x12", GRAY)), row.valueParts());
    }

    @Test
    void aNameInTwoColoursAndABadgeInTheColourBeforeIt() {
        MiniEvent.onChat(ANSWER.get(0), T);
        colored(" - 🎣 Mediterranean Sea ɢɪɢᴀɴᴛɪᴄ  \n    ┗ (1/2) One Timer", "Mediterranean Sea", GREEN, "ɢɪɢᴀɴᴛɪᴄ", GOLD);
        assertEquals(List.of(new TextHud.Part("med", GREEN), new TextHud.Part(" gigantic", GOLD)), row("med gigantic").labelParts());
        assertEquals(List.of(), row("med gigantic").valueParts()); // the mark has no colour of its own
        colored(" - 🎣 Mediterranean Sea \uF041  \n    ┗ (0/1) One Timer", "Mediterranean Sea", GREEN, "\uF041", 0xFFFFFFFF);
        assertEquals(List.of(new TextHud.Part("med albino", GREEN)), row("med albino").labelParts());
    }

    @Test
    void noColoursAndAReachedGoalAreDrawnAsBefore() {
        answer(T);
        assertEquals(new TextHud.Row("lynx pet", "0/1 · ∞ x10"), row("lynx pet"));
        MiniEvent.reset();
        MiniEvent.onChat(ANSWER.get(0), T);
        colored(" - 🎣 Mediterranean Sea  \n    ┗ (100/100) ∞ x25", "Mediterranean Sea", GREEN, "∞", PURPLE);
        TextHud.Row done = row("med");
        assertEquals(MiniEvent.GREEN, done.valueColor());
        assertEquals(List.of(), done.valueParts()); // all green, as a reached goal always was
        assertEquals(List.of(new TextHud.Part("med", GREEN)), done.labelParts());
    }
}
