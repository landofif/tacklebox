package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.menu.MenuRows;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The menu panel's model from the items of luna's dumps (main menu 2026-10-11, /travel 2026-10-29): every button
 * gets a row with a line about it, the profile gives the header level, a /travel place gets its climate and its frenzy mark,
 * the menus that share the crew title get their own names, and settings saved before the redo still load.
 */
class MenuPanelIntegrationTest {
    private static final String P = " 퀃    ";
    private static final String HEAD = " 퀁    ";
    private static final String CLICK = P + " ᴄʟɪᴄᴋ ᴛᴏ ᴠɪᴇᴡ";

    private static List<String> button(String name, String about) {
        return List.of(HEAD + name, CLICK, P + "                    ", P + about, " 퀄  ");
    }

    @Test
    void buttonRowsProfileAndDailyMissionsRow() {
        List<String[]> buttons = List.of(
                new String[]{"Daily Cosmetic Store", "View your daily cosmetic selection!"},
                new String[]{"Recipes", "View our custom item recipes here."},
                new String[]{"Fish Compendium", "Your personal fishing codex."},
                new String[]{"Collections", "View all information for fishing"},
                new String[]{"Quests", "View your quests! (/quests)"},
                new String[]{"Turbo Travel", "Travel to a location from anywhere."},
                new String[]{"Crafting", "Access your workbench. (/craft)"});
        for (String[] b : buttons) {
            MenuRows.Row row = MenuRows.of(b[0], button(b[0], b[1]));
            assertEquals(b[0], row.name());
            assertEquals(b[1], row.sub(), b[0]);
            assertEquals(-1, row.progress(), b[0]); // "(/quests)" is not a count
        }

        MenuRows.Row profile = MenuRows.of("Fishing Profile", List.of(HEAD + "Profile of meowfih",
                P + "Fishing Level [276]", P + "                        (1.18M/3.8M)", P + "🎣 Time Fishing 23h 18m 51s"));
        assertEquals(276, profile.level());
        assertEquals("1.18M/3.8M", profile.amount());

        List<String> daily = List.of(HEAD + "Daily Missions", P + "      ", P + "Base Missions",
                P + "- 🎣 Anywhere  ", P + "   ┗ (204/250)",
                P + "- 🎣 Shards Caught  ✔", P + "   ┗ (3/3)", P + " ",
                P + "Lofoten Islands Missions",
                P + "- 🎣 Greenland Shark  ✔", P + "   ┗ (2/2)",
                P + "- 🎣 Atlantic Bluefin Tuna  ", P + "   ┗ (0/3)", P + " ",
                P + "Rerolls in 20h 13m 53s");
        // the Daily Missions row itself shows the first count it finds, which is the first mission
        assertEquals("204/250", MenuRows.of("Daily Missions", daily).amount());
    }

    @Test
    void travelPlaceGetsClimateAndFrenzyMark() {
        String name = "Toledo Bend Reservoir *FRENZY!*";
        List<String> lore = List.of(HEAD + "Toledo Bend Reservoir", P + "A reservoir on the Texas Louisiana border.",
                P + " Teleport | View Species ", P + "Climate: Subtropical", P + "It is currently 21°C.",
                P + "Water: Freshwater", P + "✔ FAST TRAVEL AVAILABLE!");
        Locations.Location loc = Locations.byName(Glyphs.plain(Text.literal(name)));
        assertNotNull(loc);
        assertEquals("Subtropical · Freshwater · 21°C", MenuRows.of(name, lore).sub());
        assertEquals(Locations.FRENZY, Locations.mark(name, ServerHud.fabledPlace()));
    }

    @Test
    void menusUnderTheCrewTitleAreNamedByTheirItems() {
        // the title of all three in luna's dumps (2026-10-06 crew, 2026-10-08 location leveling, 2026-10-10 collections)
        Text title = Text.literal("\uEEE4\uD552");
        assertEquals("crew", Glyphs.menuName(title));
        List<String> overview = List.of("Basses Collection", "Billfishes Collection", "Bonytongues Collection", "Return", "Close");
        List<String> leveling = List.of("Location Leveling Data", "Level 1", "Level 2", "Return", "Leveling Bonuses");
        List<String> crew = List.of("Empty Crew Display", "Daily Missions (Resets in ---)", "Crew Info", "Crew Leveling",
                "\uEEE4\uEEE1 \uD001 \uEEE8\uEEE7\uEEE5\uEEE2   meowfih (Online)", "Return", "Close");
        assertEquals("collections", FishMenu.crewTitleMenu(plain(overview)));
        assertEquals("location leveling", FishMenu.crewTitleMenu(plain(leveling)));
        assertEquals("crew", FishMenu.crewTitleMenu(plain(crew)));
    }

    private static List<String> plain(List<String> names) {
        return names.stream().map(n -> Glyphs.plain(Text.literal(n)).trim()).toList();
    }

    @Test
    void settingsFromBeforeTheRedoStillLoad() {
        TackleboxSettings s = new Gson().fromJson("{\"fishMenu\": true, \"menuAccent\": true, \"menuAccentOpacity\": 60,"
                + " \"menuCells\": {\"main:quests\": [1, 0]}, \"menuSize\": 80}", TackleboxSettings.class);
        s.sanitize();
        assertTrue(s.fishMenu);
        assertEquals(80, s.menuSize);
        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        assertFalse(new Gson().toJson(back).contains("menuCells"));
    }
}
