package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The square rarity mark is 14x14 around the item, so neighbouring squares keep a gap. */
class FoeRarityTest {
    @Test
    void squareIsCentredOnTheItem() {
        assertArrayEquals(new int[]{11, 21, 25, 35}, Rarity.markSquare(10, 20));
    }

    @Test
    void neighbouringSquaresKeepAGap() {
        // chest slots are 18 pixels apart, both ways
        assertEquals(4, Rarity.markSquare(26, 18)[0] - Rarity.markSquare(8, 18)[2]);
        assertEquals(4, Rarity.markSquare(8, 36)[1] - Rarity.markSquare(8, 18)[3]);
    }

    @Test
    void rarityIdsGiveTheServersColours() {
        assertEquals(0xFFFFFF, Rarity.color("common"));
        assertEquals(0x2B85C4, Rarity.color("rare"));
        assertEquals(0xD98103, Rarity.color("legendary"));
        assertEquals(0xDD7ACF, Rarity.color("special"));
        assertEquals(-1, Rarity.color(""));
        assertEquals(-1, Rarity.color("default"));
    }
}
