package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** Moon prediction from luna's logged moonrises (Middle European time), the clock windows, the rows, the switches and the popups. */
class CryptidsTest {
    private static final ZoneId ZONE = ZoneId.of("Europe/Berlin");

    private static long at(int month, int day, int h, int m, int s) {
        return LocalDateTime.of(2026, month, day, h, m, s).atZone(ZONE).toInstant().toEpochMilli();
    }

    private static Cryptids.State state(long now, String moon, long rise, String storm, boolean rainbow, String fabled, int clock, Set<String> off) {
        return new Cryptids.State(now, moon, rise, storm, rainbow, fabled, clock, off, java.util.Map.of());
    }

    private static Cryptids.State quiet(long now, long rise, int clock) {
        return state(now, "", rise, "", false, "", clock, Set.of());
    }

    @AfterEach
    void tearDown() {
        Cryptids.reset();
    }

    @Test
    void nextMoonIs160MinutesAfterTheLastRise() {
        long rise = at(10, 31, 13, 22, 57); // logged, then 16:02:57 and 18:43:11 the same day
        assertEquals(at(10, 31, 16, 2, 57), Cryptids.nextMoon(rise, rise + 60_000));
        assertEquals(at(10, 31, 18, 42, 57), Cryptids.nextMoon(rise, at(10, 31, 16, 3, 0)));
        assertEquals(at(10, 31, 16, 2, 57), Cryptids.nextMoon(rise, rise), "the rise itself counts as past");
        assertEquals(-1, Cryptids.nextMoon(0, rise));
    }

    @Test
    void theLoggedAnchorStillPredictsTheMoonAfterIt() {
        assertEquals(at(11, 1, 21, 22, 55), Cryptids.nextMoon(Cryptids.SEEN_RISE, at(11, 1, 19, 0, 0)));
    }

    @Test
    void aShiftedCycleFollowsTheNewRise() {
        long old = at(10, 23, 22, 42, 51); // before the shift of 2026-10-25
        long shifted = at(10, 25, 16, 2, 55);
        assertNotEquals(at(10, 25, 18, 42, 54), Cryptids.nextMoon(old, shifted + 60_000));
        assertEquals(at(10, 25, 18, 42, 55), Cryptids.nextMoon(shifted, shifted + 60_000));
    }

    @Test
    void clockReadsTwelveAsMidnightAndNoon() {
        assertEquals(2 * 60 + 53, Cryptids.clockMinutes("2:53", "AM"));
        assertEquals(53, Cryptids.clockMinutes("12:53", "AM"));
        assertEquals(12 * 60 + 5, Cryptids.clockMinutes("12:05", "PM"));
        assertEquals(23 * 60, Cryptids.clockMinutes("11:00", "PM"));
        assertEquals(-1, Cryptids.clockMinutes("", ""));
        assertEquals(-1, Cryptids.clockMinutes("x:y", "AM"));
    }

    @Test
    void windowsOpenFromTheirFirstToTheirLastHour() {
        Cryptids.Window bigfoot = Cryptids.WINDOWS.get(0);
        assertFalse(Cryptids.open(bigfoot, 59));
        assertTrue(Cryptids.open(bigfoot, 60));
        assertTrue(Cryptids.open(bigfoot, 179));
        assertFalse(Cryptids.open(bigfoot, 180));
    }

    @Test
    void timeToAWindowGoesPastMidnight() {
        // 11 PM to 1 AM is two game hours, 100 real seconds at 20 real minutes a game day
        assertEquals(100_000, Cryptids.gameMs(23 * 60, 60));
        assertEquals(0, Cryptids.gameMs(60, 60));
    }

    @Test
    void openWindowShowsWhereAndTimeLeft() {
        List<Cryptids.Row> rows = Cryptids.rows(quiet(0, 0, Cryptids.clockMinutes("2:53", "AM")), ms -> ms / 1000 + "s");
        assertEquals(new Cryptids.Row("bigfoot", "5s", Cryptids.CLOCK, true), rows.get(0));
        assertEquals(new Cryptids.Row("cypress", "bigfoot 1/300 · tooth/fur (0)", 0, false), rows.get(1));
        assertEquals(new Cryptids.Row("titanoboa", "55s", Cryptids.CLOCK, true), rows.get(2));
        assertEquals(new Cryptids.Row("amazon", "titanoboa 1/1500 · titan's fang (0)", 0, false), rows.get(3));
        assertEquals(new Cryptids.Row("next", "", 0, true), rows.get(4));
        assertEquals(new Cryptids.Row("bessie · great lakes", "55s", Cryptids.CLOCK, false), rows.get(5));
        assertEquals(6, rows.size(), "no moon was seen, so no moon line");
    }

    @Test
    void bloodMoonAddsTheCapsuleAndCountsDown() {
        long rise = 1_000_000;
        List<Cryptids.Row> rows = Cryptids.rows(state(rise + 60_000, "Blood Moon", rise, "", false, "", -1, Set.of()), ms -> ms / 1000 + "s");
        assertEquals(new Cryptids.Row("blood moon", "540s", Cryptids.BLOOD, true), rows.get(0));
        assertEquals("toledo", rows.get(1).label());
        assertEquals("rougarou 1/300 · blood/claw (0)", rows.get(1).value());
        assertEquals("great lakes", rows.get(2).label());
        assertEquals("anywhere", rows.get(3).label());
        assertEquals("infusion capsule 1/300 (0)", rows.get(3).value());
        assertEquals(4, rows.size(), "no next moon while one is up, no clock");
    }

    @Test
    void nextListIsSoonestFirst() {
        long rise = 1_000_000, now = rise + Cryptids.CYCLE - 30_000; // moon in 30 s
        List<Cryptids.Row> rows = Cryptids.rows(quiet(now, rise, 0), ms -> ms / 1000 + "s"); // midnight: bigfoot in 50 s
        assertEquals("next", rows.get(0).label());
        assertEquals(new Cryptids.Row("moon · toledo, great lakes", "30s", Cryptids.MOON, false), rows.get(1));
        assertEquals("bigfoot · cypress", rows.get(2).label());
        assertEquals("titanoboa · amazon", rows.get(3).label());
        assertEquals("bessie · great lakes", rows.get(4).label());
    }

    @Test
    void stormsRainbowAndFabled() {
        List<Cryptids.Row> rows = Cryptids.rows(state(0, "", 0, "Supercell", true, "Cypress Lake", -1, Set.of()), ms -> "");
        assertEquals(List.of("rainbow", "danube", "supercell", "anywhere", "fabled", "cypress"), rows.stream().map(Cryptids.Row::label).toList());
        assertEquals("lightning bottle 1/250 (0)", rows.get(3).value());
        assertEquals("fabled fish 1/3000 (0)", rows.get(5).value());
        assertEquals("lightning bottle 1/500 (0)", Cryptids.rows(state(0, "", 0, "Thunderstorm", false, "", -1, Set.of()), ms -> "").get(1).value());
    }

    @Test
    void switchedOffKindsAreHiddenEverywhere() {
        long rise = 1_000_000;
        Set<String> off = Set.of("moons", "bigfoot", "titanoboa", "bessie");
        assertTrue(Cryptids.rows(state(rise + 60_000, "Full Moon", rise, "", false, "", 90, off), ms -> "").isEmpty());
        assertTrue(Cryptids.started(state(rise + 60_000, "Full Moon", rise, "", false, "", 90, off)).isEmpty());
        List<String> labels = Cryptids.rows(state(0, "", 0, "Thunderstorm", true, "", -1, Set.of("rainbow")), ms -> "").stream().map(Cryptids.Row::label).toList();
        assertEquals(List.of("thunderstorm", "anywhere"), labels);
    }

    @Test
    void dryStreakShowsInGreyBrackets() {
        Cryptids.State s = new Cryptids.State(0, "", 0, "", true, "", -1, Set.of(), java.util.Map.of("garaboncias", 42));
        assertEquals("garaboncias 1/250 · concoction/essence (42)", Cryptids.rows(s, ms -> "").get(1).value());
    }

    @Test
    void catchesCountOnlyAtTheEventsPlace() {
        long rise = 1_000_000;
        Cryptids.State moon = state(rise + 1000, "Blood Moon", rise, "Thunderstorm", false, "", 90, Set.of());
        assertEquals(Set.of("rougarou", "capsule", "lightning"), Cryptids.counting(moon, "toledobend"));
        assertEquals(Set.of("capsule", "lightning", "bigfoot"), Cryptids.counting(moon, "spawnhub"), "1:30 am: bigfoot at cypress lake");
        assertEquals(Set.of("capsule", "lightning"), Cryptids.counting(moon, "nile"));
        assertEquals(Set.of("fabled"), Cryptids.counting(state(0, "", 0, "", false, "Key West", -1, Set.of()), "keywest"));
        assertTrue(Cryptids.counting(state(0, "", 0, "", false, "Key West", -1, Set.of("fabled")), "keywest").isEmpty(), "switched off: not counted");
    }

    @Test
    void ownRareCatchesEndTheirStreak() {
        assertEquals("rougarou", Cryptids.dropKey("rare catch! you pulled a 1x rougarou claw"));
        assertEquals("rougarou", Cryptids.dropKey("RARE CATCH! You pulled 1x Rougarou Blood"));
        assertEquals("lightning", Cryptids.dropKey("RARE CATCH! You pulled 1x Lightning in a Bottle"));
        assertEquals("capsule", Cryptids.dropKey("RARE CATCH! You pulled a 1x Infusion Capsule"));
        assertEquals("bessie", Cryptids.dropKey("RARE CATCH! You pulled 1x Silver Scale"));
        assertEquals("garaboncias", Cryptids.dropKey("RARE CATCH! You pulled 1x Wizard's Vile Concoction"));
        assertEquals("titanoboa", Cryptids.dropKey("RARE CATCH! You pulled 1x Titan's Fang"));
        assertEquals("bigfoot", Cryptids.dropKey("RARE CATCH! You pulled 1x Bigfoot Tooth"));
        assertNull(Cryptids.dropKey("[366] name > RARE CATCH! You pulled 1x Rougarou Claw"), "someone else's drop");
        assertNull(Cryptids.dropKey("RARE CATCH! You pulled a 1x Subtropical Shard"));
    }

    @Test
    void popupOncePerEvent() {
        Cryptids.State on = state(0, "", 0, "", true, "", -1, Set.of());
        List<Cryptids.Popup> first = Cryptids.started(on);
        assertEquals(List.of(new Cryptids.Popup("rainbow", "danube garaboncias 1/250", Cryptids.RAINBOW)), first);
        assertTrue(Cryptids.started(on).isEmpty(), "still on: no second popup");
        Cryptids.started(state(0, "", 0, "", false, "", -1, Set.of()));
        assertEquals(1, Cryptids.started(on).size(), "a new rainbow pops again");
    }

    @Test
    void moonPopupNamesEverySpot() {
        Cryptids.Popup p = Cryptids.started(state(0, "Blood Moon", 0, "", false, "", -1, Set.of())).get(0);
        assertEquals("blood moon", p.title());
        assertEquals("toledo rougarou 1/300 · great lakes bessie 1/500 · anywhere infusion capsule 1/300", p.subtitle());
    }
}
