package dev.landofif.tacklebox.display;

import java.util.List;
import java.util.function.ToDoubleFunction;

/**
 * Whose bobber a fish's approach particle swims to, for the bite countdown and "only my fish". The owner is the
 * bobber within {@link BiteCountdown#CAPTURE_RANGE} whose line to the particle is most perpendicular to the
 * particle's sideways speed (see {@link BiteCountdown#perpendicularError}).
 */
public final class OwnFish {
    private OwnFish() {}

    /** The bobber the particle at (px, pz) moving (vx, vz) heads for, or null when none fits. */
    public static <T> T owner(double px, double pz, double vx, double vz, List<T> bobbers, ToDoubleFunction<T> x, ToDoubleFunction<T> z) {
        T best = null;
        double bestErr = BiteCountdown.PERP_TOLERANCE;
        for (T b : bobbers) {
            double rx = px - x.applyAsDouble(b), rz = pz - z.applyAsDouble(b);
            if (rx * rx + rz * rz > BiteCountdown.CAPTURE_RANGE * BiteCountdown.CAPTURE_RANGE) {
                continue;
            }
            double err = BiteCountdown.perpendicularError(rx, rz, vx, vz);
            if (err < bestErr) {
                bestErr = err;
                best = b;
            }
        }
        return best;
    }

    /** Drop the particle: the setting is on and it swims to a bobber that is not yours (also when you have none out). */
    public static boolean drops(boolean setting, Object owner, Object mine) {
        return setting && owner != null && owner != mine;
    }
}
