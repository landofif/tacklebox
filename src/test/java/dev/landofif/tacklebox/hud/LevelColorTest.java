package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LevelColorTest {
    @Test
    void bandsChangeAtTheirFirstLevel() {
        assertEquals(0xFF8A897E, LevelColor.of(0));
        assertEquals(0xFF8A897E, LevelColor.of(4));
        assertEquals(0xFF868A6E, LevelColor.of(5));
        assertEquals(0xFF868A6E, LevelColor.of(19));
        assertEquals(0xFFBBCF53, LevelColor.of(20));
        assertEquals(0xFFE8D41E, LevelColor.of(35));
    }

    @Test
    void theLastTwoHundredLevelsStepEveryTenAndThenItStaysWhite() {
        assertEquals(0xFFE81ED4, LevelColor.of(199));
        assertEquals(0xFFE81E61, LevelColor.of(200));
        assertEquals(0xFFE1C9DF, LevelColor.of(299));
        assertEquals(0xFFFFFEFA, LevelColor.of(300));
        assertEquals(0xFFFFFEFA, LevelColor.of(100000));
    }

    @Test
    void aNegativeLevelUsesTheFirstBand() {
        assertEquals(0xFF8A897E, LevelColor.of(-1));
    }
}
