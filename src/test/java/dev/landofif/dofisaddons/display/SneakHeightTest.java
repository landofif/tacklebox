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

    @Test
    void standingHeightRemovesVanillasDipWhileSneaking() {
        assertEquals(1.62f, SneakHeight.target(1.8, true, 1.27f), 1e-4f); // vanilla would give 1.27
    }

    @Test
    void notSneakingKeepsVanillasEyes() {
        assertEquals(1.62f, SneakHeight.target(1.5, false, 1.62f), 1e-4f);
    }

    /**
     * Camera.updateEyeHeight runs once per tick, before the player ticks. ClientPlayerEntity.isInSneakingPose() is an input
     * flag set a tick after the real pose (and the eye height vanilla returns) changed, so gating on it let one tick of
     * vanilla's 1.27 through on every press (the camera dipped to 1.445 and climbed back).
     */
    private static float lowestCamera(double height, boolean gateOnRealPose) {
        float camera = 1.62f, lowest = camera;
        for (int tick = 0; tick < 30; tick++) {
            boolean pose = tick >= 3 && tick < 20;      // real pose, changes in the same tick as vanilla's eye height
            boolean flag = tick >= 4 && tick < 21;      // inSneakingPose, one tick behind
            float vanilla = pose ? 1.27f : 1.62f;
            camera += (SneakHeight.target(height, gateOnRealPose ? pose : flag, vanilla) - camera) * 0.5f;
            lowest = Math.min(lowest, camera);
        }
        return lowest;
    }

    @Test
    void laggingFlagGateDipsOnEveryPress() {
        assertEquals(1.445f, lowestCamera(1.8, false), 1e-3f);
    }

    @Test
    void realPoseGateNeverDipsAtStandingHeight() {
        assertEquals(1.62f, lowestCamera(1.8, true), 1e-4f);
    }

    @Test
    void realPoseGateEasesToTheMiddleValueWithoutOvershoot() {
        assertEquals(1.445f, lowestCamera(1.65, true), 5e-3f);
        assertEquals(true, lowestCamera(1.65, true) >= SneakHeight.eyeHeight(1.65) - 1e-4f);
    }
}
