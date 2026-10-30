package dev.landofif.tacklebox;

import dev.landofif.tacklebox.menu.FishMenu;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * luna, 2026-10-30: after {@code /stats <player>} the level bars showed that player's level progress. The profile
 * item says whose it is ("Profile of meowfih", plain and lowercased like {@code Glyphs.plain}); a menu with
 * someone else's profile must not be read as yours (level, power, collections, event placement).
 */
class OthersProfileIntegrationTest {
    private static final List<String> MINE = List.of("profile of meowfih", "fishing stats and progression", "fishing level [276]", "(1.18m/3.8m)");
    private static final List<String> THEIRS = List.of("profile of freddy_2005", "fishing level [301]", "(3m/4.22m)");
    private static final List<String> POWER = List.of("total power: 425.18k");

    @Test
    void ownerIsReadFromTheProfileLine() {
        assertEquals("meowfih", FishMenu.profileOwner(MINE));
        assertEquals("freddy_2005", FishMenu.profileOwner(THEIRS));
        assertNull(FishMenu.profileOwner(POWER));
    }

    @Test
    void someoneElsesStatsPageIsNotYours() {
        assertTrue(FishMenu.othersProfile(List.of(THEIRS, POWER), "meowfih"));
        assertTrue(FishMenu.othersProfile(List.of(POWER, THEIRS), "Meowfih"));
    }

    @Test
    void yourOwnPagesAndMenusWithoutAProfileAreRead() {
        assertFalse(FishMenu.othersProfile(List.of(MINE, POWER), "meowfih"));
        assertFalse(FishMenu.othersProfile(List.of(MINE), "MeowFih"));
        assertFalse(FishMenu.othersProfile(List.of(POWER), "meowfih"));
        assertFalse(FishMenu.othersProfile(List.of(), "meowfih"));
    }
}
