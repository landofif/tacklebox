package dev.landofif.tacklebox.ui;

import net.minecraft.client.gui.DrawContext;

/**
 * The look of the settings screen, borrowed from a hyprland desktop: rounded tiles with gaps, a 2 colour gradient on
 * the focused one's border (hyprland's default cyan to green), a catppuccin style dark palette, and its animation
 * curves. The old settings screen was its main user; the rest of the mod keeps {@link Foe}.
 */
public final class Hypr {
    public static final int BASE = 0xB41E1E2E, BAR = 0xBE181825, SURFACE = 0xFF313244, OVERLAY = 0xFF45475A;
    public static final int TEXT = 0xFFCDD6F4, SUB = 0xFFA6ADC8, MUTED = 0xFF6C7086, CRUST = 0xFF11111B, RED = 0xFFF38BA8;
    /** hyprland's default inactive border, rgba(595959aa). */
    public static final int INACTIVE = 0xAA595959;

    private Hypr() {}

    /** The accent gradient's two colours, from the settings. */
    public static int acc1() {
        return 0xFF000000 | dev.landofif.tacklebox.TackleboxSettings.get().accent1;
    }

    public static int acc2() {
        return 0xFF000000 | dev.landofif.tacklebox.TackleboxSettings.get().accent2;
    }

    /** A colour per pixel, so borders and fills can be gradients. */
    public interface Paint {
        int at(int x, int y);
    }

    /** A cubic bezier like hyprland's {@code bezier = name, x1, y1, x2, y2}: time in, progress out (may overshoot). */
    public static final class Bezier {
        public static final Bezier OUT_QUINT = new Bezier(0.23, 1, 0.32, 1);
        /** The overshooting curve from hyprland's example config (it is what makes windows pop). */
        public static final Bezier OVERSHOOT = new Bezier(0.05, 0.9, 0.1, 1.05);
        private static final int N = 128;
        private final float[] table = new float[N + 1];

        private Bezier(double x1, double y1, double x2, double y2) {
            for (int i = 0; i <= N; i++) { // solved once, so a frame only looks values up
                double t = i / (double) N, lo = 0, hi = 1, s = t;
                for (int k = 0; k < 20; k++) {
                    if (curve(x1, x2, s) < t) {
                        lo = s;
                    } else {
                        hi = s;
                    }
                    s = (lo + hi) / 2;
                }
                table[i] = (float) curve(y1, y2, s);
            }
        }

        public float at(float t) {
            if (t <= 0) {
                return 0;
            }
            if (t >= 1) {
                return 1;
            }
            float f = t * N;
            int i = (int) f;
            return table[i] + (table[i + 1] - table[i]) * (f - i);
        }

        private static double curve(double a, double b, double s) {
            double u = 1 - s;
            return 3 * u * u * s * a + 3 * u * s * s * b + s * s * s;
        }
    }

    /** Moves {@code cur} toward {@code target}, frame rate independent ({@code rate} is per second). */
    public static float approach(float cur, float target, float dt, float rate) {
        return cur + (target - cur) * (1 - (float) Math.exp(-rate * dt));
    }

    public static float clamp01(float v) {
        return Math.max(0, Math.min(1, v));
    }

    /** The colour with its alpha scaled, for fading things in and out. */
    public static int alpha(int argb, float a) {
        return ((int) ((argb >>> 24) * clamp01(a)) << 24) | (argb & 0xFFFFFF);
    }

    public static int mix(int a, int b, float t) {
        t = clamp01(t);
        int out = 0;
        for (int shift = 0; shift <= 24; shift += 8) {
            int ca = (a >>> shift) & 0xFF, cb = (b >>> shift) & 0xFF;
            out |= (int) (ca + (cb - ca) * t) << shift;
        }
        return out;
    }

    /** Left to right gradient over {@code w} pixels from {@code x}. */
    public static Paint horizontal(int c1, int c2, int x, int w, float a) {
        return (px, py) -> alpha(mix(c1, c2, (px - x) / (float) Math.max(1, w)), a);
    }

    /** hyprland's border gradient: {@code c1} to {@code c2} along {@code angle} (radians), over the box. */
    public static Paint angled(int c1, int c2, int x, int y, int w, int h, double angle, float a) {
        double cos = Math.cos(angle), sin = Math.sin(angle);
        double p1 = w * cos, p2 = h * sin, p3 = p1 + p2;
        double min = Math.min(0, Math.min(p1, Math.min(p2, p3))), max = Math.max(0, Math.max(p1, Math.max(p2, p3)));
        return (px, py) -> alpha(mix(c1, c2, (float) (((px - x) * cos + (py - y) * sin - min) / Math.max(1e-6, max - min))), a);
    }

    /** How many pixels the corner of a circle of radius {@code r} cuts off on row {@code i} (from the edge). */
    private static int cut(int r, int i) {
        if (r <= 0) {
            return 0;
        }
        double dy = r - i - 0.5;
        return (int) Math.round(r - Math.sqrt(Math.max(0, r * r - dy * dy)));
    }

    /** A filled run, in pieces of {@code chunk} pixels along its long side so a gradient can change colour. */
    private static void seg(DrawContext c, int x1, int y1, int x2, int y2, Paint p, int chunk) {
        if (x2 <= x1 || y2 <= y1) {
            return;
        }
        boolean horizontal = x2 - x1 >= y2 - y1;
        int len = horizontal ? x2 - x1 : y2 - y1;
        for (int o = 0; o < len; o += chunk) {
            int e = (int) Math.min(len, (long) o + chunk);
            int ax = horizontal ? x1 + o : x1, bx = horizontal ? x1 + e : x2;
            int ay = horizontal ? y1 : y1 + o, by = horizontal ? y2 : y1 + e;
            int col = p.at((ax + bx) / 2, (ay + by) / 2);
            if ((col >>> 24) != 0) {
                c.fill(ax, ay, bx, by, col);
            }
        }
    }

    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, int color) {
        rrect(c, x, y, w, h, r, (px, py) -> color, Integer.MAX_VALUE);
    }

    /** A rounded rectangle, one fill per row of the corners and one for the middle. */
    public static void rrect(DrawContext c, int x, int y, int w, int h, int r, Paint p, int chunk) {
        r = Math.min(r, Math.min(w, h) / 2);
        for (int i = 0, k; i < r; i = k) { // corner rows with the same inset are one fill
            int o = cut(r, i);
            for (k = i + 1; k < r && cut(r, k) == o; k++) {
            }
            seg(c, x + o, y + i, x + w - o, y + k, p, chunk);
            seg(c, x + o, y + h - k, x + w - o, y + h - i, p, chunk);
        }
        seg(c, x, y + r, x + w, y + h - r, p, chunk);
    }

    public static void ring(DrawContext c, int x, int y, int w, int h, int r, int t, int color) {
        ring(c, x, y, w, h, r, t, (px, py) -> color, Integer.MAX_VALUE);
    }

    /** The outline of a rounded rectangle, {@code t} pixels thick (at most {@code r}). */
    public static void ring(DrawContext c, int x, int y, int w, int h, int r, int t, Paint p, int chunk) {
        r = Math.min(r, Math.min(w, h) / 2);
        t = Math.min(t, r);
        int ri = r - t;
        for (int i = 0; i < r; i++) {
            int o = cut(r, i);
            for (int side = 0; side < 2; side++) {
                int yy = side == 0 ? y + i : y + h - 1 - i;
                if (i < t) {
                    seg(c, x + o, yy, x + w - o, yy + 1, p, chunk);
                } else {
                    int in = t + cut(ri, i - t);
                    seg(c, x + o, yy, x + in, yy + 1, p, chunk);
                    seg(c, x + w - in, yy, x + w - o, yy + 1, p, chunk);
                }
            }
        }
        seg(c, x, y + r, x + t, y + h - r, p, chunk);
        seg(c, x + w - t, y + r, x + w, y + h - r, p, chunk);
    }
}
