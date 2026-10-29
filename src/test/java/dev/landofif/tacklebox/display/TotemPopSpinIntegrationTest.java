package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Every frame of a 40 tick pop through the check the mixin makes: the mod's own pop leaves with a short spin, a real totem stays vanilla. */
class TotemPopSpinIntegrationTest {
    private static final int FRAMES_PER_TICK = 3;

    @AfterEach
    void reset() {
        TotemPop.shown = null;
    }

    private static float vanilla(int timer, float tickProgress) {
        return 900f * (float) Math.abs(Math.sin(TotemPop.curve((40 - timer + tickProgress) / 40f) * Math.PI));
    }

    @Test
    void ownPopComesInLikeVanillaAndLeavesWith300Degrees() {
        Object ours = new Object();
        TotemPop.shown = ours;
        float most = 0f;
        float before = 0f;
        boolean leaving = false;
        for (int timer = 40; timer > 0; timer--) {
            for (int frame = 0; frame < FRAMES_PER_TICK; frame++) {
                float tickProgress = frame / (float) FRAMES_PER_TICK;
                float vanilla = vanilla(timer, tickProgress);
                float drawn = TotemPop.spin(ours, timer, tickProgress, vanilla);
                if (!leaving && drawn != vanilla) {
                    leaving = true;
                    assertTrue(timer <= 20, "left early at timer " + timer);
                }
                if (!leaving) {
                    assertEquals(vanilla, drawn, "timer " + timer);
                } else {
                    assertTrue(drawn >= before - 1e-3f, "turned back at timer " + timer);
                    most = Math.max(most, drawn);
                }
                before = drawn;
            }
        }
        assertTrue(leaving);
        assertTrue(most > 1100f && most <= 1200f, "last spin " + most);
    }

    @Test
    void realTotemAfterOwnPopStaysVanilla() {
        TotemPop.shown = new Object();
        Object totem = new Object();
        for (int timer = 40; timer > 0; timer--) {
            float vanilla = vanilla(timer, 0.5f);
            assertEquals(vanilla, TotemPop.spin(totem, timer, 0.5f, vanilla));
            assertEquals(vanilla, TotemPop.spin(null, timer, 0.5f, vanilla));
        }
    }
}
