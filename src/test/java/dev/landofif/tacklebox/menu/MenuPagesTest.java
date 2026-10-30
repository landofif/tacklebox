package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which items the wheel clicks, from the page items in luna's gui dumps. */
class MenuPagesTest {
    @Test
    void namedNextAndBack() {
        // compendium, vaults and presets: a gunpowder named Next in slot 52, Back in slot 46
        assertTrue(MenuPages.isPage("Next", "", true));
        assertTrue(MenuPages.isPage("Back", "", false));
        assertFalse(MenuPages.isPage("Next", "", false));
        assertFalse(MenuPages.isPage("Back", "", true));
    }

    @Test
    void auctionHouseNextIsInTheFirstLoreLine() {
        String lore = Glyphs.plain(Text.literal(" 퀁    Next"));
        assertTrue(MenuPages.isPage("Gunpowder", lore, true));
        assertFalse(MenuPages.isPage("Gunpowder", lore, false));
    }

    @Test
    void returnAndOtherItemsAreNeverClicked() {
        assertFalse(MenuPages.isPage("Return", "", false));
        assertFalse(MenuPages.isPage("Close", "", true));
        assertFalse(MenuPages.isPage("Leveling Bonuses", "Leveling Bonuses", true));
        assertFalse(MenuPages.isPage("Bread", "", true));
        assertFalse(MenuPages.isPage("Next Level Rewards", "", true)); // only the whole word counts
    }

    @Test
    void questMenusAreLeftOut() {
        assertTrue(MenuPages.excluded(Glyphs.menuName(Text.literal("픹"))));
        assertFalse(MenuPages.excluded(Glyphs.menuName(Text.literal("픂"))));
        assertFalse(MenuPages.excluded("personal vault #2"));
    }

    @Test
    void oneClickPerTurnWhileThePageLoads() {
        assertTrue(MenuPages.ready(1000, 0));
        assertFalse(MenuPages.ready(1000 + MenuPages.COOLDOWN_MS - 1, 1000));
        assertTrue(MenuPages.ready(1000 + MenuPages.COOLDOWN_MS, 1000));
    }
}
