package dev.landofif.tacklebox.display;

/**
 * Predicts how long until the bite from the fish's approach. Once a fish swims toward your bobber the server
 * sends a fishing particle each tick at a distance from the bobber that shrinks to 0 at the bite. The shrink
 * speed varies (rain faster, no open sky slower), so it is measured from the recent particles instead of
 * assumed. Idea and numbers from fishutils. Display only; it never touches the particles.
 */
public final class BiteCountdown {
    /** Coherent inward particles needed before a number shows, so a stray particle cannot flash one. */
    static final int CONFIRM_COUNT = 4;
    static final long STREAK_GAP_MS = 350;
    static final double R_JUMP_MARGIN = 1.0;
    /** The countdown hides when no approach particle arrived for this long. */
    static final long TIMEOUT_MS = 600;
    /** After a bite the burst of particles can look like an approach. */
    static final long BITE_COOLDOWN_MS = 1200;
    static final long SLOPE_WINDOW_MS = 1200;
    static final double MIN_SPEED = 0.5, MAX_SPEED = 4.0; // blocks per second
    /** Largest |cos| of the angle to the perpendicular that still counts as the bobber's own particle. */
    static final double PERP_TOLERANCE = 0.30;
    static final double CAPTURE_RANGE = 10.0;
    static final double DEFAULT_SPEED = 2.0;
    private static final int SAMPLES = 128;
    /** An action bar (the reeling minigame) keeps the countdown hidden this long after its last message. */
    static final long ACTION_BAR_MS = 3000;

    private static long biteAt, lastApproach, cooldownUntil, actionBarUntil;
    private static int streak, head, size;
    private static double lastR = Double.MAX_VALUE;
    private static final long[] sampleT = new long[SAMPLES];
    private static final double[] sampleR = new double[SAMPLES];

    private BiteCountdown() {}

    /** Forget everything (no bobber out, or the bite just happened). */
    public static void reset() {
        biteAt = 0;
        streak = 0;
        lastR = Double.MAX_VALUE;
        size = 0;
    }

    /** The bite happened: stop and ignore the particle burst for a moment. */
    public static void onBite(long now) {
        reset();
        cooldownUntil = now + BITE_COOLDOWN_MS;
    }

    public static void noteActionBar(boolean present, long now) {
        actionBarUntil = present ? now + ACTION_BAR_MS : 0;
    }

    /** One confirmed own approach particle, {@code r} blocks from the bobber. */
    public static void onApproach(double r, long now) {
        if (now < cooldownUntil) {
            return;
        }
        boolean fresh = now - lastApproach > STREAK_GAP_MS;
        if (fresh || r > lastR + R_JUMP_MARGIN) {
            streak = 0;
            size = 0;
        }
        if (fresh) {
            biteAt = 0;
        }
        streak++;
        lastR = r;
        lastApproach = now;
        sampleT[head] = now;
        sampleR[head] = r;
        head = (head + 1) % SAMPLES;
        if (size < SAMPLES) {
            size++;
        }
        if (streak >= CONFIRM_COUNT) {
            long predicted = now + timeToBite(now, r);
            // only ever sooner, so the number ticks down even when packets arrive unevenly
            biteAt = biteAt > 0 ? Math.min(biteAt, predicted) : predicted;
        }
    }

    private static long timeToBite(long now, double r) {
        double speed = inwardSpeed(now); // blocks per ms
        if (speed <= 0) {
            return Math.round(r / DEFAULT_SPEED * 1000);
        }
        return Math.round(r / Math.max(MIN_SPEED / 1000, Math.min(MAX_SPEED / 1000, speed)));
    }

    /** Blocks per ms over the recent samples, 0 until there is a usable span. */
    private static double inwardSpeed(long now) {
        if (size < 2) {
            return 0;
        }
        int newest = (head - 1 + SAMPLES) % SAMPLES;
        long newT = sampleT[newest], oldT = newT;
        double newR = sampleR[newest], oldR = newR;
        for (int k = 1; k < size; k++) {
            int i = (newest - k + SAMPLES) % SAMPLES;
            if (sampleT[i] < now - SLOPE_WINDOW_MS) {
                break;
            }
            oldT = sampleT[i];
            oldR = sampleR[i];
        }
        long dt = newT - oldT;
        double dr = oldR - newR;
        return dt < 200 || dr <= 0 ? 0 : dr / dt;
    }

    /**
     * How far a particle at {@code (rx, rz)} from a bobber, moving {@code (vx, vz)}, is from the exact
     * perpendicular the bobber's own approach particles have: 0 for the owner, near 1 for anything else.
     * Returns 1 when the particle has no sideways speed to judge by.
     */
    public static double perpendicularError(double rx, double rz, double vx, double vz) {
        double vLen = Math.sqrt(vx * vx + vz * vz), rLen = Math.sqrt(rx * rx + rz * rz);
        if (vLen < 1.0e-6) {
            return 1;
        }
        return rLen < 1.0e-4 ? 0 : Math.abs((rx * vx + rz * vz) / (rLen * vLen));
    }

    public static boolean active(long now) {
        return biteAt > 0 && now - lastApproach <= TIMEOUT_MS && now >= actionBarUntil;
    }

    /** Seconds until the predicted bite; only meaningful while {@link #active}. */
    public static float seconds(long now) {
        return Math.max(0, (biteAt - now) / 1000f);
    }
}
