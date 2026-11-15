package dev.landofif.tacklebox.menu;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * The travel map's geometry (luna, 2026-11-15): the continents as rough outlines drawn in code (no picture file), where each
 * location sits, and the view (zoom and pan). Pure, so the tests run it: {@code screen/TravelMapScreen} draws it over the
 * server's travel menu. The idea of the travel menu as a world map comes from FOE+ (itshaxr/FOE-Plus, TravelMapHandler), which
 * builds on FOE-R (DannyPX) and FOECollab (Kimberlyow); the outlines and positions here are tacklebox's own. No code was copied.
 * <p>
 * The map is a plain longitude/latitude grid from 180 west to 180 east and {@link #TOP} north to {@link #BOTTOM} south; a
 * point on it is (u, v), both 0 to 1.
 */
public final class TravelMap {
    public static final double TOP = 78, BOTTOM = -58;
    /** Width over height of the whole map. */
    public static final double ASPECT = 360 / (TOP - BOTTOM);
    public static final double MAX_ZOOM = 8;

    /** Where a location is in the real world; {@code id} is the one in {@link Locations#ALL}. */
    public record Pin(String id, double lon, double lat) {
        public double u() {
            return TravelMap.u(lon);
        }

        public double v() {
            return TravelMap.v(lat);
        }
    }

    public static final List<Pin> PINS = List.of(
            new Pin("cypress", -94.1, 32.7),        // "based off of Caddo Lake" (the server's own tooltip)
            new Pin("kenai", -150.8, 60.5),
            new Pin("biwa", 136.1, 35.3),
            new Pin("murray", 141.5, -35.2),
            new Pin("everglades", -80.9, 25.6),
            new Pin("keywest", -81.8, 24.5),
            new Pin("toledobend", -93.6, 31.2),
            new Pin("greatlakes", -84.5, 45.0),
            new Pin("danube", 24.0, 44.5),
            new Pin("oilrig", -90.5, 28.0),         // the Gulf of Mexico
            new Pin("amazon", -60.0, -3.2),
            new Pin("mediterranean", 18.0, 35.5),
            new Pin("capecod", -70.2, 41.9),
            new Pin("hawaii", -156.6, 20.8),
            new Pin("lofoten", 14.3, 68.2),
            new Pin("cairns", 145.8, -16.9),
            new Pin("nile", 32.5, 26.0));

    private TravelMap() {}

    public static double u(double lon) {
        return (lon + 180) / 360;
    }

    public static double v(double lat) {
        return (TOP - lat) / (TOP - BOTTOM);
    }

    /** The pin of a location id, or null for a place the map does not know. */
    public static Pin pin(String id) {
        for (Pin p : PINS) {
            if (p.id().equals(id)) {
                return p;
            }
        }
        return null;
    }

    /** What part of the map shows: the zoom (1 = all of it) and the point in the middle. */
    public static final class View {
        public double zoom = 1, cu = 0.5, cv = 0.5;

        /** Screen x of map point u inside a map area starting at {@code x}, {@code w} wide. */
        public double sx(double u, int x, int w) {
            return x + w / 2.0 + (u - cu) * w * zoom;
        }

        public double sy(double v, int y, int h) {
            return y + h / 2.0 + (v - cv) * h * zoom;
        }

        /** The mouse wheel: {@code steps} notches in or out, keeping the map point under the mouse where it is. */
        public void zoomAt(double mx, double my, int x, int y, int w, int h, double steps) {
            double u = cu + (mx - x - w / 2.0) / (w * zoom), v = cv + (my - y - h / 2.0) / (h * zoom);
            zoom = Math.max(1, Math.min(MAX_ZOOM, zoom * Math.pow(1.25, steps)));
            cu = u - (mx - x - w / 2.0) / (w * zoom);
            cv = v - (my - y - h / 2.0) / (h * zoom);
            clamp();
        }

        /** A drag by that many screen pixels. */
        public void drag(double dx, double dy, int w, int h) {
            cu -= dx / (w * zoom);
            cv -= dy / (h * zoom);
            clamp();
        }

        /** The map never leaves a gap at an edge. */
        private void clamp() {
            double half = 0.5 / zoom;
            cu = Math.max(half, Math.min(1 - half, cu));
            cv = Math.max(half, Math.min(1 - half, cv));
        }
    }

    /**
     * Where an outline crosses the middle of pixel row {@code y}, sorted: the land is between the 1st and 2nd, the 3rd and
     * 4th crossing and so on. {@code xs}, {@code ys} are the outline's corners on screen.
     */
    public static double[] crossings(double[] xs, double[] ys, int y) {
        double row = y + 0.5;
        double[] out = new double[xs.length];
        int n = 0;
        for (int i = 0, j = xs.length - 1; i < xs.length; j = i++) {
            if ((ys[i] <= row) != (ys[j] <= row)) {
                out[n++] = xs[i] + (row - ys[i]) / (ys[j] - ys[i]) * (xs[j] - xs[i]);
            }
        }
        out = Arrays.copyOf(out, n);
        Arrays.sort(out);
        return out;
    }

    /**
     * Pins that would sit on top of each other are pushed apart until each pair is at least {@code min} pixels apart (as far
     * as 40 rounds get it), so the Gulf coast's five places can all be clicked with the whole world in view. Pins with room
     * stay where they are.
     *
     * @param pts {x, y} per pin, changed in place
     */
    public static void spread(double[][] pts, double min) {
        for (int round = 0; round < 40; round++) {
            boolean moved = false;
            for (int i = 0; i < pts.length; i++) {
                for (int j = i + 1; j < pts.length; j++) {
                    double dx = pts[j][0] - pts[i][0], dy = pts[j][1] - pts[i][1];
                    double d = Math.hypot(dx, dy);
                    if (d >= min) {
                        continue;
                    }
                    if (d < 0.01) { // the same spot: any direction, the same one every time
                        dx = Math.cos(i + j);
                        dy = Math.sin(i + j);
                        d = 1;
                    }
                    double push = (min - d) / 2 + 0.01;
                    pts[i][0] -= dx / d * push;
                    pts[i][1] -= dy / d * push;
                    pts[j][0] += dx / d * push;
                    pts[j][1] += dy / d * push;
                    moved = true;
                }
            }
            if (!moved) {
                return;
            }
        }
    }

    /** The index of the pin under the mouse (within {@code reach} pixels on both axes, the nearest one), or -1. */
    public static int hit(double[][] pts, double mx, double my, double reach) {
        int best = -1;
        double bestD = Double.MAX_VALUE;
        for (int i = 0; i < pts.length; i++) {
            double dx = Math.abs(pts[i][0] - mx), dy = Math.abs(pts[i][1] - my);
            if (dx <= reach && dy <= reach && dx + dy < bestD) {
                best = i;
                bestD = dx + dy;
            }
        }
        return best;
    }

    /** The land: one outline per array, as longitude, latitude pairs. Rough on purpose. */
    public static final List<double[]> LAND = List.of(
            // north and central america
            new double[]{-168, 65, -166, 68, -157, 71, -141, 70, -125, 70, -110, 68, -95, 68, -86, 69, -81, 64, -94, 60, -93, 57, -82, 55,
                    -79, 52, -78, 57, -77, 62, -72, 62, -65, 60, -62, 57, -56, 52, -60, 47, -66, 44, -70, 43.5, -70, 41.5, -74, 40, -76, 35,
                    -81, 31, -80.2, 25.2, -81.5, 25, -83, 29, -85, 30, -90, 29.5, -94, 29.5, -97, 27, -97.5, 22, -95, 18.5, -91, 18.5, -90, 21,
                    -87, 21, -87.5, 17, -84, 15.5, -83.5, 11, -82, 9, -79.5, 9.5, -77.5, 8, -80, 7.5, -83, 8.5, -85.5, 10, -87.5, 13, -91, 14,
                    -94, 16, -97, 15.8, -102, 18, -105.5, 20, -109.5, 23, -112, 26, -114, 29, -117, 32.5, -120.5, 34.5, -124, 40, -124.5, 48,
                    -128, 51, -131, 54.5, -135, 58, -140, 60, -146, 61, -152, 59, -158, 57, -163, 55, -158, 58.5, -162, 60, -165, 62, -161, 64.5},
            // south america
            new double[]{-77.5, 8, -72, 12, -67, 10.5, -61, 10.5, -57, 6, -52, 4.5, -50, 0, -44, -2.5, -38.5, -3.5, -35, -6, -35, -9, -39, -14,
                    -39, -18, -41, -22, -48, -25.5, -48.5, -28, -53, -34, -58, -34.5, -57, -38, -62, -39, -65, -42, -66, -47, -69, -51, -68, -54.5,
                    -72, -54, -75, -50, -74, -44, -73, -38, -71.5, -30, -70, -18.5, -76, -13.5, -81, -6, -80, -2, -80, 1, -77, 4},
            // greenland, baffin island, the far north of canada, iceland
            new double[]{-73, 78, -60, 76, -55, 70, -51, 64, -45, 60, -42, 62, -38, 66, -27, 68.5, -22, 72, -20, 77, -30, 78},
            new double[]{-80, 73, -72, 71.5, -62, 67, -65, 62.5, -70, 62.5, -75, 65, -73, 68, -85, 70, -88, 73},
            new double[]{-120, 72, -105, 73, -95, 75, -80, 76.5, -75, 78, -95, 78, -120, 76},
            new double[]{-24, 65.5, -22, 64, -18, 63.5, -14, 65, -16, 66.5, -22, 66.3},
            // europe and asia
            new double[]{-9, 43, -1.5, 43.5, -1, 46, -4.5, 48.5, 2, 51, 4, 52.5, 8.5, 54, 8, 57, 12, 56, 13, 54.5, 19, 54.5, 21, 56.5, 24, 59,
                    29, 60, 23, 60, 21.5, 63, 25, 65.5, 22, 66, 17, 62, 18.5, 59.5, 16.5, 56.5, 13, 55.5, 11, 59, 5, 59, 5, 62, 12, 66, 15, 69,
                    22, 70.5, 28, 71, 33, 69, 41, 66.5, 44, 68, 54, 68.5, 60, 69, 68, 69, 73, 72, 80, 73, 90, 75.5, 104, 77.5, 113, 74,
                    130, 71.5, 140, 72.5, 160, 70, 170, 70, 180, 69, 180, 65, 177, 62.5, 170, 60, 163, 58, 162, 55, 157, 51, 156, 57,
                    155, 59.5, 143, 59, 135, 55, 141, 53, 140, 48, 135, 43.5, 130, 42.5, 127.5, 39.5, 129.5, 37, 129, 35, 126.5, 34.5,
                    126, 37.5, 125, 39.5, 121, 40.5, 118, 39, 122, 37, 119.5, 35, 121.5, 31, 122, 29, 120, 26, 117, 23.5, 113.5, 22.5,
                    110, 21, 108, 21.5, 106, 19, 109, 14, 109, 11.5, 105, 9, 104.5, 10.5, 101, 13, 100, 13.5, 99.5, 9.5, 101, 6.5, 103.5, 1.5,
                    101.5, 2.5, 98.5, 8, 98, 13, 97, 17, 94.5, 16, 92, 21, 89, 22, 87, 21, 82, 16.5, 80, 13.5, 80, 10, 77.5, 8, 76, 11.5,
                    73, 18, 72.5, 21, 69, 22.5, 67, 25, 62, 25, 57, 25.5, 56.5, 27, 51, 27.5, 50, 30, 48, 30, 50, 26.5, 51.5, 24, 56, 24.5,
                    56.5, 26.2, 59.5, 22.5, 55, 17, 52, 16, 45, 12.7, 43, 12.7, 42.5, 16.5, 39, 21, 35, 28, 34.5, 29.5, 34.5, 31.3, 36, 36,
                    32, 36.5, 27.5, 37, 26.5, 39.5, 26, 41, 24, 40.5, 23, 38, 22.5, 36.5, 21, 38, 19.5, 40.5, 19.5, 42, 16, 43.5, 13.5, 45.5,
                    12.5, 44, 16, 41.5, 18.5, 40, 16.5, 39, 16, 38, 15.6, 40, 13, 41, 10.5, 43, 8.5, 44.3, 6, 43, 3, 43, 3, 41.5, 0, 39.5,
                    -0.5, 38, -2, 36.8, -5.5, 36, -9, 37, -9.5, 39},
            // britain, ireland
            new double[]{-5.5, 50, 1.5, 51.2, 0.5, 53, -1.5, 55, -2, 57.5, -4, 58.5, -6, 57, -5, 54.5, -3, 53.5, -5, 52},
            new double[]{-10, 52, -6, 52.2, -6, 54.8, -8.5, 55, -10, 54},
            // africa, madagascar
            new double[]{32.5, 30, 34, 27, 35.5, 24, 37, 21, 38.5, 18, 43, 12, 43.5, 11.5, 51, 12, 49, 6, 44, 1.5, 40.5, -3, 39, -7, 40.5, -11,
                    40.5, -15, 35, -20, 35.5, -24, 33, -26, 30.5, -31, 26, -34, 20, -34.8, 18, -33, 15, -27, 12, -18, 13.5, -12, 12, -5,
                    9, -1, 9.5, 4, 5, 6, -4, 5, -8, 4.5, -13, 8.5, -17, 14.5, -16.5, 21, -13, 27.5, -9.5, 30.5, -6, 35.8, -2, 35.2, 3, 37,
                    10, 37.3, 11, 34, 15, 32.3, 20, 31, 25, 32, 31, 31.3},
            new double[]{49.3, -12, 50.3, -16, 47.5, -25, 44, -24, 43.5, -21, 44.5, -16.5, 48, -13.5},
            // japan
            new double[]{130, 31.5, 131.5, 33.5, 133, 34.5, 135, 34, 137, 34.7, 140, 35.2, 141, 38, 141.5, 41, 140, 41, 139.5, 38.5, 136.5, 37,
                    135.8, 35.6, 132.5, 35.5, 130.5, 34, 129.7, 33},
            new double[]{140, 42, 141.5, 45.3, 145, 44, 144, 43, 141, 42},
            // australia, tasmania, new zealand, new guinea
            new double[]{114, -22, 114, -26, 115.5, -33.5, 118, -35, 123.5, -34, 129, -31.7, 133, -32, 136, -35, 140, -38, 144, -38.5,
                    147, -38, 150, -37, 153, -30, 153, -25, 149.5, -22, 146, -19, 145.5, -15, 143.5, -14, 142.5, -10.8, 141.5, -13, 141, -17,
                    139.5, -17.5, 136, -15, 136.5, -12, 131, -12, 129.5, -15, 126, -14, 122, -18, 118, -20.5},
            new double[]{145, -41, 148, -41, 148, -43, 146, -43.5},
            new double[]{173, -35, 175.5, -37, 178, -38, 175, -41.5, 174, -39.5},
            new double[]{172.5, -41, 174, -42, 170, -46, 167, -46, 168.5, -44},
            new double[]{131, -1, 138, -2, 145, -5, 150.5, -10, 146, -9, 141, -9, 138, -8, 134, -4},
            // borneo, sumatra, java, the philippines, sri lanka
            new double[]{109, 1.5, 113, 3.5, 117, 7, 119, 5, 117.5, 1, 116, -4, 110, -3},
            new double[]{95.5, 5.5, 98, 4.5, 103.5, -1, 106, -5.5, 104, -5.5, 100.5, -1.5},
            new double[]{105.5, -6.5, 108.5, -6.8, 114, -7.5, 114.5, -8.5, 106, -7.5},
            new double[]{120.5, 18.5, 122.3, 17, 121.5, 14, 124, 13, 126, 7, 122, 7, 120.5, 14},
            new double[]{80, 9.5, 81.8, 7.5, 80.5, 6, 79.8, 7.5},
            // cuba, hispaniola, hawaii
            new double[]{-85, 22, -80, 23, -75, 20.5, -77.5, 19.8, -82, 22},
            new double[]{-74.3, 19.8, -70, 19.8, -68.5, 18.5, -71.5, 17.8, -74.3, 18.5},
            new double[]{-156.2, 20.3, -154.8, 19.6, -155.6, 18.9, -156.1, 19.4},
            new double[]{-158.3, 21.7, -157.6, 21.6, -156, 20.6, -156.7, 20.5});

    /** Inland seas, drawn over the land in the sea's colour: the black sea and the caspian sea. */
    public static final List<double[]> WATER = List.of(
            new double[]{28, 41.5, 28.5, 44.5, 31, 46.5, 35, 45.5, 37.5, 47, 39.5, 45, 41.5, 42, 38, 41, 33, 42, 29.5, 41},
            new double[]{47, 45.5, 51, 46.5, 53, 45, 52.5, 41, 54, 39, 53.5, 37, 50, 37.3, 49, 40.5, 47.5, 43});

    /** The corners of an outline on screen, {xs, ys}, for a map area at {@code x, y} of {@code w} by {@code h}. */
    public static double[][] project(double[] outline, View view, int x, int y, int w, int h) {
        int n = outline.length / 2;
        double[] xs = new double[n], ys = new double[n];
        for (int i = 0; i < n; i++) {
            xs[i] = view.sx(u(outline[2 * i]), x, w);
            ys[i] = view.sy(v(outline[2 * i + 1]), y, h);
        }
        return new double[][]{xs, ys};
    }

    /** The pins on screen, in the order of {@code ids}, pushed apart; an id the map does not know gives null there. */
    public static double[][] place(List<String> ids, View view, int x, int y, int w, int h, double min) {
        List<double[]> known = new ArrayList<>();
        double[][] out = new double[ids.size()][];
        for (int i = 0; i < ids.size(); i++) {
            Pin p = pin(ids.get(i));
            if (p != null) {
                out[i] = new double[]{view.sx(p.u(), x, w), view.sy(p.v(), y, h)};
                known.add(out[i]);
            }
        }
        spread(known.toArray(new double[0][]), min);
        return out;
    }
}
