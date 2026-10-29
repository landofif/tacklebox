package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnFishTest {
    private static double[] at(double x, double z) {
        return new double[] {x, z};
    }

    private static double[] owner(double px, double pz, double vx, double vz, List<double[]> bobbers) {
        return OwnFish.owner(px, pz, vx, vz, bobbers, b -> b[0], b -> b[1]);
    }

    @Test
    void picksTheBobberTheParticleCirclesInTowards() {
        double[] mine = at(0, 0), other = at(3, 0);
        // 2 blocks north of mine, moving sideways (east): perpendicular to mine, nearly along the line to the other
        assertSame(mine, owner(0, -2, 1, 0, List.of(mine, other)));
        // 2 blocks north of the other bobber, moving sideways: the other's fish
        assertSame(other, owner(3, -2, 1, 0, List.of(mine, other)));
    }

    @Test
    void ignoresFarBobbersAndUnclearParticles() {
        assertNull(owner(0, -2, 1, 0, List.of(at(0, -20))));
        assertNull(owner(0, -2, 0, 1, List.of(at(0, 0)))); // moving straight at it: not an approach particle
        assertNull(owner(0, -2, 0, 0, List.of(at(0, 0)))); // no sideways speed to judge by
    }

    @Test
    void dropsOnlyOthersFishWhenOn() {
        Object mine = new Object(), other = new Object();
        assertTrue(OwnFish.drops(true, other, mine));
        assertTrue(OwnFish.drops(true, other, null)); // no line out: every fish is someone else's
        assertFalse(OwnFish.drops(true, mine, mine));
        assertFalse(OwnFish.drops(true, null, mine));
        assertFalse(OwnFish.drops(false, other, mine));
    }
}
