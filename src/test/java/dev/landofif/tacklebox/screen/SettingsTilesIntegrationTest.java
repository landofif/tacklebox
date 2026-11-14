package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.screen.SettingsPages.Page;
import dev.landofif.tacklebox.screen.SettingsPages.Tile;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The real pages as the grid shows them: every tile opens and goes back, and a switch on the grid changes its setting. */
class SettingsTilesIntegrationTest {
    private static Tile tile(List<Page> pages, String page, String title) {
        return pages.stream().filter(p -> p.name().equals(page)).findFirst().orElseThrow()
                .tiles().stream().filter(t -> t.title().equals(title)).findFirst().orElseThrow();
    }

    @Test
    void everyTileOpensAndGoesBack() {
        List<Page> pages = SettingsPages.build();
        SettingsNav nav = new SettingsNav();
        for (int p = 0; p < pages.size(); p++) {
            nav.page(p);
            List<Tile> tiles = pages.get(p).tiles();
            assertFalse(tiles.isEmpty(), pages.get(p).name());
            for (int i = 0; i < tiles.size(); i++) {
                assertFalse(tiles.get(i).rows().isEmpty(), tiles.get(i).title());
                nav.open(i);
                assertTrue(nav.opened());
                assertTrue(nav.back());
                assertEquals(p, nav.page);
            }
        }
    }

    @Test
    void switchesAreRowsOfTheirTile() {
        for (Page p : SettingsPages.build()) {
            for (Tile t : p.tiles()) {
                if (t.main() != null) {
                    assertTrue(t.rows().contains(t.main()), t.title());
                }
            }
        }
    }

    @Test
    void gridSwitchFlipsTheHud() {
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
    void hudTilesHaveSwitches() {
        List<Page> pages = SettingsPages.build();
        for (String t : List.of("equipment", "quests left", "catch title", "active boosts", "event", "cryptids", "level progress", "now playing", "server hud")) {
            assertNotNull(tile(pages, "huds", t).main(), t);
        }
        assertNull(tile(pages, "huds", "hotbar").main(), "the hotbar has no on/off");
    }
}
