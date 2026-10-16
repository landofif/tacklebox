package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChummerRingGeometryTest {
    private static List<float[]> ring(float cx, float y, float cz) {
        List<float[]> vertices = new ArrayList<>();
        ChummerRingGeometry.emit(cx, y, cz, (x, vy, z) -> vertices.add(new float[]{x, vy, z}));
        return vertices;
    }

    private static double distance(float[] v, float cx, float cz) {
        return Math.hypot(v[0] - cx, v[2] - cz);
    }

    @Test
    void oneQuadPerSegment() {
        assertEquals(ChummerRingGeometry.SEGMENTS * 4, ring(0, 0, 0).size());
    }

    @Test
    void theRingIsFlat() {
        for (float[] v : ring(3.5f, -1.25f, -7f)) {
            assertEquals(-1.25f, v[1]);
        }
    }

    @Test
    void everyCornerIsOnTheInnerOrOuterEdgeOfTheBandAroundTheCentre() {
        float cx = -10.5f; // any centre must work
        float cz = 17.25f;
        double inner = ChummerRingGeometry.RADIUS - ChummerRingGeometry.WIDTH / 2;
        double outer = ChummerRingGeometry.RADIUS + ChummerRingGeometry.WIDTH / 2;
        List<float[]> vertices = ring(cx, 0, cz);
        for (int i = 0; i < vertices.size(); i += 4) {
            assertEquals(inner, distance(vertices.get(i), cx, cz), 1e-3);
            assertEquals(outer, distance(vertices.get(i + 1), cx, cz), 1e-3);
            assertEquals(outer, distance(vertices.get(i + 2), cx, cz), 1e-3);
            assertEquals(inner, distance(vertices.get(i + 3), cx, cz), 1e-3);
        }
    }

    @Test
    void theBandIsClosedAndHasNoGaps() {
        List<float[]> v = ring(0, 0, 0);
        int quads = v.size() / 4;
        for (int i = 0; i < quads; i++) {
            float[] outerEnd = v.get(i * 4 + 2);
            float[] innerEnd = v.get(i * 4 + 3);
            float[] nextOuterStart = v.get(((i + 1) % quads) * 4 + 1);
            float[] nextInnerStart = v.get(((i + 1) % quads) * 4);
            assertEquals(outerEnd[0], nextOuterStart[0], 1e-4);
            assertEquals(outerEnd[2], nextOuterStart[2], 1e-4);
            assertEquals(innerEnd[0], nextInnerStart[0], 1e-4);
            assertEquals(innerEnd[2], nextInnerStart[2], 1e-4);
        }
    }

    @Test
    void theAreaIsThatOfAThinRing() {
        List<float[]> v = ring(0, 0, 0);
        double area = 0;
        for (int i = 0; i < v.size(); i += 4) { // each quad is a trapezoid, two triangles
            area += triangle(v.get(i), v.get(i + 1), v.get(i + 2)) + triangle(v.get(i), v.get(i + 2), v.get(i + 3));
        }
        double inner = ChummerRingGeometry.RADIUS - ChummerRingGeometry.WIDTH / 2;
        double outer = ChummerRingGeometry.RADIUS + ChummerRingGeometry.WIDTH / 2;
        assertEquals(Math.PI * (outer * outer - inner * inner), area, area * 0.002);
    }

    private static double triangle(float[] a, float[] b, float[] c) {
        return Math.abs((b[0] - a[0]) * (c[2] - a[2]) - (c[0] - a[0]) * (b[2] - a[2])) / 2;
    }

    @Test
    void theRingSitsAboveTheGroundTheHologramFloatsOver() {
        // luna's dump: the text display at y 27.98, her feet (the ground) at 26.0
        assertEquals(26.0, ChummerRingGeometry.ringY(27.98), 1e-9);
        assertEquals(ChummerRingGeometry.LIFT, ChummerRingGeometry.ringY(27.98) - (27.98 - ChummerRingGeometry.BELOW_TEXT), 1e-9);
    }

    @Test
    void rangeIsTwentyBlocksFlat() {
        assertEquals(20.0, ChummerRingGeometry.RADIUS);
        assertTrue(ChummerRingGeometry.inRange(-810.5, 1065.5, -810.5, 1065.5));
        assertTrue(ChummerRingGeometry.inRange(-810.5, 1065.5, -790.5, 1065.5)); // exactly 20 away
        assertTrue(ChummerRingGeometry.inRange(-810.5, 1065.5, -810.5 + 14, 1065.5 - 14)); // 19.8 on the diagonal
        assertFalse(ChummerRingGeometry.inRange(-810.5, 1065.5, -810.5, 1065.5 + 20.1));
        assertFalse(ChummerRingGeometry.inRange(-810.5, 1065.5, -810.5 + 15, 1065.5 + 15)); // 21.2 on the diagonal
    }

    @Test
    void colourIsTranslucentAndKeepsOnlyTheRgbOfTheSetting() {
        int argb = ChummerRingGeometry.argb(0x41F55B);
        assertEquals(ChummerRingGeometry.ALPHA, argb >>> 24);
        assertEquals(0x41F55B, argb & 0xFFFFFF);
        assertEquals(0x41F55B, ChummerRingGeometry.argb(0xFF41F55B) & 0xFFFFFF);
        assertTrue(ChummerRingGeometry.ALPHA > 0 && ChummerRingGeometry.ALPHA < 0xFF);
    }
}
