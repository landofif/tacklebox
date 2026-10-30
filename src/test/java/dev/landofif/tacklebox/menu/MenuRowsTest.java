package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lore lines from luna's gui dumps (main menu 2026-10-11, compendium 2026-10-06, crew 2026-10-10, quests 2026-10-29). */
class MenuRowsTest {
    private static final String P = " 퀃    ";
    private static final String HEAD = " 퀁    ";
    private static final String CLICK = P + " ᴄʟɪᴄᴋ ᴛᴏ ᴠɪᴇᴡ";

    @Test
    void cleanDropsGlyphsAndEmojiButKeepsCase() {
        assertEquals("Catch 70x Rare fish", MenuRows.clean(P + "Catch 70x Rare fish"));
        assertEquals("- Anywhere", MenuRows.clean(P + "- 🎣 Anywhere  "));
        assertEquals("", MenuRows.clean(P));
    }

    @Test
    void buttonSubIsTheFirstDescriptionNotTheNameOrTheHint() {
        MenuRows.Row row = MenuRows.of("Fish Compendium", List.of(HEAD + "Fish Compendium", CLICK, P + "      ",
                P + "Your personal fishing codex.", P + "View all sorts of fish data here,"));
        assertEquals("Fish Compendium", row.name());
        assertEquals("Your personal fishing codex.", row.sub());
        assertEquals(-1, row.progress());
        assertEquals(-1, row.level());
    }

    @Test
    void profileHeadGivesLevelAndProgress() {
        MenuRows.Row row = MenuRows.of("Fishing Profile", List.of(HEAD + "Profile of meowfih",
                P + "ꜰɪsʜɪɴɢ sᴛᴀᴛs", P + "Fishing Level [276]",
                P + "                        (1.18M/3.8M)", P + "+ Prospect 561 (1/34.51)"));
        assertEquals("Profile of meowfih", row.sub());
        assertEquals(276, row.level());
        assertEquals(1.18 / 3.8, row.progress(), 1e-4);
        assertEquals("1.18M/3.8M", row.amount());
    }

    @Test
    void compendiumProgressPastTheMaxIsFull() {
        MenuRows.Row row = MenuRows.of("Lake Biwa", List.of(HEAD + "Lake Biwa", P + "Level: [17] (11.85M/2.32M)", P + "Runes Discovered: (1/15)"));
        assertEquals(17, row.level());
        assertEquals(1f, row.progress());
        assertEquals("Level: [17]", row.sub());
        assertTrue(row.done());
    }

    @Test
    void crewCollectionWithCommaNumbers() {
        MenuRows.Row row = MenuRows.of("Basses Collection", List.of(HEAD + "Basses Collection",
                P + "ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ", P + "Current Level - [6]",
                P + "                        (43,178/50K)", P + "Progress to Level 7"));
        assertEquals(6, row.level());
        assertEquals(43178 / 50000f, row.progress(), 1e-4);
        assertEquals("43,178/50K", row.amount());
        assertEquals("Current Level - [6]", row.sub());
        assertFalse(row.done());
    }

    @Test
    void questJoinsGoalAndPlaceAndReadsTheSmallCapsProgress() {
        MenuRows.Row row = MenuRows.of("Fishing Quest #1", List.of(HEAD + "Fishing Quest #1",
                P + "ʜᴀʀᴅ ꞯᴜᴇsᴛ", P + "Catch 70x Rare fish", P + "at Murray River",
                P + "ɪɴ ᴘʀᴏɢʀᴇss (25/70)", P + "21.58K-30.82Kxp"));
        assertEquals("Catch 70x Rare fish at Murray River", row.sub());
        assertEquals(25 / 70f, row.progress(), 1e-4);
        assertEquals("25/70", row.amount());
    }

    @Test
    void travelItemShowsClimateWaterAndTemperature() {
        MenuRows.Row row = MenuRows.of("Cypress Lake", List.of(HEAD + "Cypress Lake", P + "A bald cypress filled swampland",
                P + " Teleport | View Species ", P + "Climate: Subtropical", P + "It is currently -5°C.",
                P + "Low of 8°C, high of 16°C.", P + "Water: Freshwater"));
        assertEquals("Subtropical · Freshwater · -5°C", row.sub());
    }

    @Test
    void missionsAndRerolls() {
        List<String> lore = List.of(HEAD + "Daily Missions", P + "Base Missions",
                P + "- 🎣 Anywhere  ", P + "   ┗ (204/250)",
                P + "- 🎣 Quests Completed  ✔", P + "   ┗ (3/3)",
                P + "- 🎣   ✔", P + "   ┗ (1/1)", P + " ",
                P + "Lofoten Islands Missions", P + "- 🎣 Lumpfish  ", P + "   ┗ (5/14)", P + " ",
                P + "Rerolls in 20h 13m 53s");
        List<MenuRows.Mission> m = MenuRows.missions(lore);
        assertEquals(List.of(new MenuRows.Mission("Base", "Anywhere", 204, 250),
                new MenuRows.Mission("Base", "Quests Completed", 3, 3),
                new MenuRows.Mission("Base", "mission", 1, 1),
                new MenuRows.Mission("Lofoten Islands", "Lumpfish", 5, 14)), m);
        assertTrue(m.get(1).done());
        assertFalse(m.get(0).done());
        assertEquals("Rerolls in 20h 13m 53s", MenuRows.rerolls(lore));
    }

    @Test
    void numbers() {
        assertEquals(1_180_000, MenuRows.number("1.18M"), 1e-6);
        assertEquals(43178, MenuRows.number("43,178"), 1e-6);
        assertEquals(0, MenuRows.number("."));
    }
}
