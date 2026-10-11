package dev.landofif.dofisaddons.compat;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Lore copied from l's F8 dump of 2026-10-11 (frame glyphs included). */
class FoeDailyMissionsTest {
    private static final String F = " 퀃 ";
    private static final String ROD = "🎣";

    private static List<String> dump() {
        return List.of(
                " 퀁    Daily Missions",
                F + "                                                  ",
                F + "   Base Missions",
                F + "   - " + ROD + " Anywhere  ",
                F + "      ┗ (169/250)",
                F + "   - " + ROD + " Quests Completed  ✔",
                F + "      ┗ (3/3)",
                F + "   - " + ROD + "   ✔",
                F + "      ┗ (1/1)",
                F + " ",
                F + "   Lofoten Islands Missions",
                F + "   - " + ROD + " Lumpfish  ",
                F + "      ┗ (4/14)",
                F + "   - " + ROD + " Opah  ✔",
                F + "      ┗ (4/4)",
                F + "   - " + ROD + " Atlantic Bluefin Tuna  ",
                F + "      ┗ (0/3)",
                F + " ",
                F + "   Rerolls in 22h 47m 47s",
                " 퀄  ");
    }

    @Test
    void keepsOnlyUnfinishedMissionsInOrder() {
        List<FoeDailyMissions.Mission> m = FoeDailyMissions.parse(dump());
        assertEquals(List.of(
                new FoeDailyMissions.Mission("Base Missions", "Anywhere", 169, 250),
                new FoeDailyMissions.Mission("Lofoten Islands Missions", "Lumpfish", 4, 14),
                new FoeDailyMissions.Mission("Lofoten Islands Missions", "Atlantic Bluefin Tuna", 0, 3)), m);
    }

    @Test
    void glyphNamedMissionKeepsItsGlyph() {
        List<String> lore = List.of(F + "   - " + ROD + "   ", F + "      ┗ (0/1)");
        assertEquals("", FoeDailyMissions.parse(lore).get(0).goal());
    }

    @Test
    void oldOneLineFormatAndHeadersGiveNothing() {
        assertTrue(FoeDailyMissions.parse(List.of(F + "   | Total Caught (250/250)", F + "   Base Missions")).isEmpty());
    }

    @Test
    void thousandsSeparatorsAreRead() {
        List<String> lore = List.of(F + "   - " + ROD + " Anywhere  ", F + "      ┗ (1,341/2,000)");
        assertEquals(new FoeDailyMissions.Mission("", "Anywhere", 1341, 2000), FoeDailyMissions.parse(lore).get(0));
    }

    @Test
    void oldGoalNamesMapToTheNewMissions() {
        assertEquals("Anywhere", FoeDailyMissions.renameGoal("Total Caught"));
        assertEquals("\uF037", FoeDailyMissions.renameGoal("Mythical Caught"));
        assertEquals("Shards Caught", FoeDailyMissions.renameGoal("Shards Caught"));
        assertEquals("Contest Participation", FoeDailyMissions.renameGoal("Contest Participation"));
    }
}
