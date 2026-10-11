package dev.landofif.dofisaddons.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SneakHeightTest {
    @Test
    void vanillaHeightIsVanillasSneakEyes() {
        assertEquals(1.27f, SneakHeight.eyeHeight(1.5), 1e-4f);
    }

    @Test
    void standingHeightDoesNotDip() {
        assertEquals(1.62f, SneakHeight.eyeHeight(1.8), 1e-4f);
    }

    @Test
    void inBetweenDipsLess() {
        assertEquals(1.445f, SneakHeight.eyeHeight(1.65), 1e-4f);
        assertEquals(true, SneakHeight.eyeHeight(1.7) > SneakHeight.eyeHeight(1.6));
    }

    @Test
    void outOfRangeIsClamped() {
        assertEquals(1.27f, SneakHeight.eyeHeight(1.0), 1e-4f);
        assertEquals(1.62f, SneakHeight.eyeHeight(2.5), 1e-4f);
    }
}
