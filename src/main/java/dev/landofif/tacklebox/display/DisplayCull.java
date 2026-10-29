package dev.landofif.tacklebox.display;

/** Distance rule for "hide far displays": the servers display entities are cheap to hide and costly to draw at spawn. */
public final class DisplayCull {
    public static final int MIN_RANGE = 8;
    public static final int MAX_RANGE = 64;

    private DisplayCull() {}

    public static int clampRange(int range) {
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, range));
    }

    /** True when the setting is on and the display is farther than the range from the camera. */
    public static boolean hidden(boolean on, double distanceSq, int range) {
        double r = clampRange(range);
        return on && distanceSq > r * r;
    }
}
