package dev.landofif.tacklebox.compat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

/** The square rarity mark covers the whole slot. */
class FoeRarityTest {
    @Test
    void squareIsTheWholeSlotAroundTheItem() {
        assertArrayEquals(new int[]{9, 19, 27, 37}, FoeRarity.slotSquare(10, 20));
    }

    @Test
    void neighbouringSquaresTouch() {
        // chest slots are 18 pixels apart, both ways
        assertEquals(FoeRarity.slotSquare(8, 18)[2], FoeRarity.slotSquare(26, 18)[0]);
        assertEquals(FoeRarity.slotSquare(8, 18)[3], FoeRarity.slotSquare(8, 36)[1]);
    }
}
