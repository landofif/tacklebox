package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The square rarity mark covers the whole slot. */
class FoeRarityTest {
    @Test
    void squareIsTheWholeSlotAroundTheItem() {
        assertArrayEquals(new int[]{9, 19, 27, 37}, Rarity.slotSquare(10, 20));
    }

    @Test
    void neighbouringSquaresTouch() {
        // chest slots are 18 pixels apart, both ways
        assertEquals(Rarity.slotSquare(8, 18)[2], Rarity.slotSquare(26, 18)[0]);
        assertEquals(Rarity.slotSquare(8, 18)[3], Rarity.slotSquare(8, 36)[1]);
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
