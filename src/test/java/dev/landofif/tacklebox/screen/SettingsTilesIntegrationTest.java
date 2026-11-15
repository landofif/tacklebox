package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.screen.SettingsPages.Page;
import dev.landofif.tacklebox.screen.SettingsPages.Row;
import dev.landofif.tacklebox.screen.SettingsPages.Tile;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/** The real pages as the three columns show them: the agreed pages and groups, an icon on each, every setting once. */
class SettingsTilesIntegrationTest {
    private static Tile tile(List<Page> pages, String page, String title) {
        return pages.stream().filter(p -> p.name().equals(page)).findFirst().orElseThrow()
                .tiles().stream().filter(t -> t.title().equals(title)).findFirst().orElseThrow();
    }

    private static List<String> groups(List<Page> pages, String page) {
        return pages.stream().filter(p -> p.name().equals(page)).findFirst().orElseThrow().tiles().stream().map(Tile::title).toList();
    }

    @Test
    void theEightPagesInOrder() {
        assertEquals(List.of("look", "menus", "huds", "inventory", "fishing", "chat", "world", "tools"),
                SettingsPages.build().stream().map(Page::name).toList());
    }

    @Test
    void groupsSitOnTheAgreedPages() {
        List<Page> pages = SettingsPages.build();
        assertEquals(List.of("accent", "pause menu", "hotbar"), groups(pages, "look"));
        assertEquals(List.of("menus", "presets", "chest menus", "vaults", "tooltips"), groups(pages, "menus"));
        assertEquals(List.of("quests left", "level progress", "active boosts", "event", "mini event", "cryptids", "timers", "wrong gear",
                "server hud", "now playing"), groups(pages, "huds"));
        assertEquals(List.of("equipment", "inventory hud", "slots left"), groups(pages, "inventory"));
        assertEquals(List.of("bobber", "skin", "line", "catch title", "rare drops", "chummer"), groups(pages, "fishing"));
        assertEquals(List.of("chat", "dry streaks"), groups(pages, "chat"));
        assertEquals(List.of("nametags", "clutter", "crowds", "lag", "screenshots"), groups(pages, "world"));
    }

    @Test
    void everyPageAndGroupHasAnIconAndRows() {
        for (Page p : SettingsPages.build()) {
            assertNotNull(p.icon(), p.name());
            assertFalse(p.tiles().isEmpty(), p.name());
            for (Tile t : p.tiles()) {
                assertNotNull(t.icon(), t.title());
                assertTrue(t.icon().matches("[a-z_]+"), t.icon());
                assertFalse(t.rows().isEmpty(), t.title());
            }
        }
    }

    @Test
    void everySettingIsThereOnce() {
        Set<String> names = new HashSet<>();
        int n = 0;
        for (Page p : SettingsPages.build()) {
            for (Tile t : p.tiles()) {
                for (Row r : t.rows()) {
                    assertTrue(names.add(r.name), "twice: " + r.name);
                    n++;
                }
            }
        }
        // settings that used to sit on other pages, by the names they always had
        for (String name : List.of("accent colour", "ask before disconnect", "hotbar look", "box outline colour", "box fill opacity",
                "fishonmc menu", "travel map", "equipment hud", "inventory hud", "slots left hud", "catch title", "rare drops",
                "chummer range ring", "hide login messages", "dry streak texts", "wrong gear warning", "mini event hud",
                "screenshot preview", "pet prices", "move huds")) {
            assertTrue(names.contains(name), name);
        }
        assertTrue(n >= 150, "only " + n + " settings");
    }

    @Test
    void onlyToolsIsAPageOfLinks() {
        for (Page p : SettingsPages.build()) {
            assertEquals(p.name().equals("tools"), p.links(), p.name());
        }
    }

    @Test
    void switchesAreRowsOfTheirGroup() {
        for (Page p : SettingsPages.build()) {
            for (Tile t : p.tiles()) {
                if (t.main() != null) {
                    assertTrue(t.rows().contains(t.main()), t.title());
                }
            }
        }
    }

    @Test
    void theSwitchInTheGroupListFlipsTheHud() {
        List<Page> pages = SettingsPages.build();
        TackleboxSettings s = TackleboxSettings.get();
        boolean was = s.cryptidHud;
        Tile cryptids = tile(pages, "huds", "cryptids");
        assertEquals("cryptids hud", cryptids.main().name);
        cryptids.main().click(0, 0);
        assertEquals(!was, s.cryptidHud);
        cryptids.main().click(0, 0);
        assertEquals(was, s.cryptidHud);
    }

    @Test
    void hudGroupsHaveSwitches() {
        List<Page> pages = SettingsPages.build();
        for (String t : groups(pages, "huds")) {
            assertNotNull(tile(pages, "huds", t).main(), t);
        }
        assertEquals("equipment hud", tile(pages, "inventory", "equipment").main().name);
        assertEquals("inventory hud", tile(pages, "inventory", "inventory hud").main().name);
        assertEquals("slots left hud", tile(pages, "inventory", "slots left").main().name);
        assertEquals("catch title", tile(pages, "fishing", "catch title").main().name);
        assertNull(tile(pages, "look", "hotbar").main(), "the hotbar has no on/off");
    }
}
