package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TravelMapTest {
    private static final int X = 10, Y = 20, W = 530, H = 200;

    /** Whether a longitude/latitude is inside one of the outlines, by the same row crossings the screen fills with. */
    private static boolean inside(List<double[]> outlines, double lon, double lat) {
        TravelMap.View view = new TravelMap.View();
        int w = 3600, h = (int) (w / TravelMap.ASPECT);
        double px = view.sx(TravelMap.u(lon), 0, w), py = view.sy(TravelMap.v(lat), 0, h);
        for (double[] o : outlines) {
            double[][] p = TravelMap.project(o, view, 0, 0, w, h);
            double[] xs = TravelMap.crossings(p[0], p[1], (int) py);
            for (int i = 0; i + 1 < xs.length; i += 2) {
                if (px >= xs[i] && px <= xs[i + 1]) {
                    return true;
                }
            }
        }
        return false;
    }

    @Test
    void everyLocationHasAPinOnTheMap() {
        assertEquals(Locations.ALL.size(), TravelMap.PINS.size());
        for (Locations.Location l : Locations.ALL) {
            TravelMap.Pin p = TravelMap.pin(l.id());
            assertNotNull(p, l.id());
            assertTrue(p.u() > 0 && p.u() < 1 && p.v() > 0 && p.v() < 1, l.id());
        }
        assertNull(TravelMap.pin("atlantis"));
    }

    @Test
    void theOutlinesPutLandAndSeaWhereTheyAre() {
        assertTrue(inside(TravelMap.LAND, -100, 40));  // kansas
        assertTrue(inside(TravelMap.LAND, -60, -10));  // brazil
        assertTrue(inside(TravelMap.LAND, 20, 5));     // central africa
        assertTrue(inside(TravelMap.LAND, 10, 50));    // germany
        assertTrue(inside(TravelMap.LAND, 100, 60));   // siberia
        assertTrue(inside(TravelMap.LAND, 135, -25));  // australia
        assertTrue(inside(TravelMap.LAND, 138, 36));   // japan
        assertFalse(inside(TravelMap.LAND, -40, 30));  // the atlantic
        assertFalse(inside(TravelMap.LAND, -140, 10)); // the pacific
        assertFalse(inside(TravelMap.LAND, 80, -20));  // the indian ocean
        assertFalse(inside(TravelMap.LAND, 18, 35.5)); // the mediterranean sea
        assertFalse(inside(TravelMap.LAND, -90.5, 27)); // the gulf of mexico
        assertTrue(inside(TravelMap.WATER, 34, 43.5)); // the black sea
    }

    @Test
    void landPinsAreOnLandAndSeaPinsAtSea() {
        for (String id : List.of("cypress", "toledobend", "greatlakes", "danube", "amazon", "nile", "murray", "kenai", "biwa")) {
            TravelMap.Pin p = TravelMap.pin(id);
            assertTrue(inside(TravelMap.LAND, p.lon(), p.lat()), id);
        }
        for (String id : List.of("oilrig", "mediterranean")) {
            TravelMap.Pin p = TravelMap.pin(id);
            assertFalse(inside(TravelMap.LAND, p.lon(), p.lat()), id);
        }
    }

    @Test
    void crossingsOfASquareAndAnLShape() {
        double[] xs = {0, 10, 10, 0}, ys = {0, 0, 10, 10};
        assertArrayEquals(new double[]{0, 10}, TravelMap.crossings(xs, ys, 5));
        assertEquals(0, TravelMap.crossings(xs, ys, 10).length);
        assertEquals(0, TravelMap.crossings(xs, ys, -1).length);
        // a U: two prongs at the top
        double[] ux = {0, 2, 2, 8, 8, 10, 10, 0}, uy = {0, 0, 6, 6, 0, 0, 10, 10};
        assertArrayEquals(new double[]{0, 2, 8, 10}, TravelMap.crossings(ux, uy, 2));
        assertArrayEquals(new double[]{0, 10}, TravelMap.crossings(ux, uy, 8));
    }

    @Test
    void theWheelZoomsAroundTheMouseAndStopsAtTheEnds() {
        TravelMap.View v = new TravelMap.View();
        double mx = X + W * 0.25, my = Y + H * 0.75;
        // the map point under the mouse before
        double u = v.cu + (mx - X - W / 2.0) / (W * v.zoom), w = v.cv + (my - Y - H / 2.0) / (H * v.zoom);
        v.zoomAt(mx, my, X, Y, W, H, 3);
        assertEquals(Math.pow(1.25, 3), v.zoom, 1e-9);
        assertEquals(mx, v.sx(u, X, W), 1e-6);
        assertEquals(my, v.sy(w, Y, H), 1e-6);
        v.zoomAt(mx, my, X, Y, W, H, 50);
        assertEquals(TravelMap.MAX_ZOOM, v.zoom);
        v.zoomAt(mx, my, X, Y, W, H, -50);
        assertEquals(1, v.zoom);
        assertEquals(0.5, v.cu, 1e-9); // all of it shows again
        assertEquals(0.5, v.cv, 1e-9);
    }

    @Test
    void aDragMovesTheMapButNeverPastItsEdge() {
        TravelMap.View v = new TravelMap.View();
        v.drag(100, 50, W, H);
        assertEquals(0.5, v.cu, 1e-9); // nothing to move at zoom 1
        v.zoomAt(X + W / 2.0, Y + H / 2.0, X, Y, W, H, 4);
        double before = v.sx(0.5, X, W);
        v.drag(40, -10, W, H);
        assertEquals(before + 40, v.sx(0.5, X, W), 1e-6);
        v.drag(100_000, 100_000, W, H);
        assertEquals(X, v.sx(0, X, W), 1e-6); // the left and top edges stop at the frame
        assertEquals(Y, v.sy(0, Y, H), 1e-6);
    }

    @Test
    void crowdedPinsArePushedApartAndLonelyOnesStay() {
        double[][] pts = {{100, 100}, {100, 100}, {103, 101}, {300, 50}};
        TravelMap.spread(pts, 19);
        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 3; j++) {
                assertTrue(Math.hypot(pts[i][0] - pts[j][0], pts[i][1] - pts[j][1]) >= 18.9, i + " " + j);
            }
            assertTrue(Math.hypot(pts[i][0] - 100, pts[i][1] - 100) < 30, "stays near its place");
        }
        assertArrayEquals(new double[]{300, 50}, pts[3]);
    }

    @Test
    void allPinsCanBeClickedWithTheWholeWorldInView() {
        List<String> ids = TravelMap.PINS.stream().map(TravelMap.Pin::id).toList();
        double[][] at = TravelMap.place(ids, new TravelMap.View(), X, Y, W, H, 19);
        for (int i = 0; i < at.length; i++) {
            assertEquals(i, TravelMap.hit(at, at[i][0], at[i][1], 9), ids.get(i));
            for (int j = i + 1; j < at.length; j++) {
                assertTrue(Math.hypot(at[i][0] - at[j][0], at[i][1] - at[j][1]) >= 18.9, ids.get(i) + " " + ids.get(j));
            }
        }
        assertEquals(-1, TravelMap.hit(at, X + W * 0.5, Y + H * 0.95, 9)); // open sea
        // an unknown place has no spot on the map
        assertNull(TravelMap.place(List.of("", "kenai"), new TravelMap.View(), X, Y, W, H, 19)[0]);
    }
}
