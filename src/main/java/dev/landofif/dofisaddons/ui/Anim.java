package dev.landofif.dofisaddons.ui;

/** Small timing helpers for the UI animations: progress over a duration and easing curves. */
public final class Anim {
    private Anim() {}

    public static long now() {
        return System.nanoTime() / 1_000_000L;
    }

    /** 0 → 1 over {@code durationMs} from {@code startMs}, clamped. */
    public static float progress(long startMs, long durationMs) {
        if (startMs <= 0) {
            return 1f;
        }
        return Math.max(0f, Math.min(1f, (now() - startMs) / (float) durationMs));
    }

    /** Fast start, soft landing. */
    public static float easeOut(float t) {
        float u = 1f - t;
        return 1f - u * u * u;
    }

    /** Moves {@code value} toward {@code target}, covering the whole 0..1 range in {@code fullMs}. */
    public static float approach(float value, float target, long elapsedMs, long fullMs) {
        float step = elapsedMs / (float) fullMs;
        return value < target ? Math.min(target, value + step) : Math.max(target, value - step);
    }

    /** Blends two ARGB colours. */
    public static int mix(int from, int to, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int a = (int) ((from >>> 24) + ((to >>> 24) - (from >>> 24)) * t);
        int r = (int) ((from >> 16 & 0xFF) + ((to >> 16 & 0xFF) - (from >> 16 & 0xFF)) * t);
        int g = (int) ((from >> 8 & 0xFF) + ((to >> 8 & 0xFF) - (from >> 8 & 0xFF)) * t);
        int b = (int) ((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
        return a << 24 | r << 16 | g << 8 | b;
    }
}
