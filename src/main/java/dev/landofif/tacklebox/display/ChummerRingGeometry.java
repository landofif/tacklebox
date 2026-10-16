package dev.landofif.tacklebox.display;

/**
 * Where and how wide the chummer range ring is (docs/huds/chummer-range.md): a thin flat band of quads around the
 * chummer's hologram, 20 blocks out horizontally. No Minecraft classes, so the tests run it without the game;
 * {@link ChummerRing} feeds the vertices to the world renderer.
 */
public final class ChummerRingGeometry {
    /** The chummer's range, horizontal, measured from the middle of the hologram (luna measured 20.05 to the server's own circle). */
    public static final double RADIUS = 20.0;
    /** The band is this wide, half of it inside {@link #RADIUS} and half outside. */
    public static final double WIDTH = 0.2;
    public static final int SEGMENTS = 128;
    /** The text display floats this far above the ground (its head 0.5 lower); the ring sits on the ground plus {@link #LIFT}. */
    public static final double BELOW_TEXT = 2.0;
    public static final double LIFT = 0.02;
    /** Translucent: the colour setting is rgb only. */
    public static final int ALPHA = 0xA0;

    private static final float[] COS = new float[SEGMENTS + 1];
    private static final float[] SIN = new float[SEGMENTS + 1];

    static {
        for (int i = 0; i <= SEGMENTS; i++) {
            double angle = Math.PI * 2 * i / SEGMENTS;
            COS[i] = (float) Math.cos(angle);
            SIN[i] = (float) Math.sin(angle);
        }
    }

    private ChummerRingGeometry() {}

    /** Receives the corners of the quads, four per segment, going round the band. */
    @FunctionalInterface
    public interface Sink {
        void vertex(float x, float y, float z);
    }

    /** The height of the ring for a hologram at {@code textY}. */
    public static double ringY(double textY) {
        return textY - BELOW_TEXT + LIFT;
    }

    /** Whether a spot is inside the chummer's range, looking only at x and z. */
    public static boolean inRange(double chummerX, double chummerZ, double x, double z) {
        double dx = x - chummerX;
        double dz = z - chummerZ;
        return dx * dx + dz * dz <= RADIUS * RADIUS;
    }

    /** The ring colour: the rgb of the setting with the ring's alpha. */
    public static int argb(int rgb) {
        return ALPHA << 24 | (rgb & 0xFFFFFF);
    }

    /** The whole band around {@code (cx, y, cz)}, as {@link #SEGMENTS} quads. */
    public static void emit(float cx, float y, float cz, Sink sink) {
        float inner = (float) (RADIUS - WIDTH / 2);
        float outer = (float) (RADIUS + WIDTH / 2);
        for (int i = 0; i < SEGMENTS; i++) {
            sink.vertex(cx + COS[i] * inner, y, cz + SIN[i] * inner);
            sink.vertex(cx + COS[i] * outer, y, cz + SIN[i] * outer);
            sink.vertex(cx + COS[i + 1] * outer, y, cz + SIN[i + 1] * outer);
            sink.vertex(cx + COS[i + 1] * inner, y, cz + SIN[i + 1] * inner);
        }
    }
}
