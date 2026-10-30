package dev.landofif.tacklebox.ui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class FoeBoxTest {
    @Test
    void plainBoxKeepsTheButtonDarkAndScalesItsAlpha() {
        assertEquals(0xAA181825, Foe.plainBox(100));
        assertEquals(0x55181825, Foe.plainBox(50));
        assertEquals(0x00181825, Foe.plainBox(0));
        assertEquals(0xAA181825, Foe.plainBox(150));
    }
}
