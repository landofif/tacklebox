package dev.landofif.dofisaddons.display;

/**
 * Your own camera while sneaking. Vanilla sneaks from 1.8 to 1.5 blocks tall and the eyes drop from 1.62 to 1.27, so the
 * dip is 0.35 for 0.3 of height. The setting is the height you sneak down to: 1.5 is vanilla, 1.8 is no dip.
 */
public final class SneakHeight {
    public static final double STANDING = 1.8, VANILLA = 1.5;
    private static final float STANDING_EYES = 1.62f, DIP_PER_HEIGHT = 0.35f / 0.3f;

    private SneakHeight() {}

    public static double clamp(double height) {
        return Math.max(VANILLA, Math.min(STANDING, height));
    }

    /** The eye height for the camera to ease towards while sneaking. */
    public static float eyeHeight(double height) {
        return STANDING_EYES - (float) (STANDING - clamp(height)) * DIP_PER_HEIGHT;
    }
}
