package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.display.ChummerRingGeometry;
import dev.landofif.tacklebox.display.ChummerText;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Settings saved before the ring existed get it on in green, a saved choice survives a save and a load, and the
 * hologram text of luna's dump (2026-10-17) goes through the parser to a ring whose edge is the chummer's range.
 */
class ChummerRingIntegrationTest {
    /** The chummer's text display in luna's dump: where it floats and what it says. */
    private static final double X = -810.5, Y = 27.97998046875, Z = 1065.5;
    private static final String TEXT = "Chummer™\n\nActivated by meowfih\n\nBite Speed: +75\n14m 55s Remaining";

    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        TackleboxSettings.importLegacy(com.google.gson.JsonParser.parseString(json).getAsJsonObject()); // the options are no longer in the gson file
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsGetTheRingOnInGreen() {
        TackleboxSettings s = load("{\"swingSlowness\": 2.0, \"hideXpPopups\": true}");
        assertTrue(s.chummerRing);
        assertEquals(0x41F55B, s.chummerRingColor);
        assertTrue(load("{}").chummerRing);
    }

    @Test
    void savedChoiceRoundTrips() {
        TackleboxSettings s = load("{\"chummerRing\": false, \"chummerRingColor\": 16711680}");
        assertFalse(s.chummerRing);
        assertEquals(0xFF0000, s.chummerRingColor);

        TackleboxSettings fresh = load("{}");
        assertTrue(fresh.chummerRing);
        assertEquals(0x41F55B, fresh.chummerRingColor);
    }

    @Test
    void theColourKeepsOnlyRgb() {
        assertEquals(0xFFFFFF, load("{\"chummerRingColor\": -1}").chummerRingColor);
    }

    @Test
    void dumpTextBecomesAChummerWhoseRingMarksItsRange() {
        ChummerText.Chummer c = ChummerText.parse(TEXT).orElseThrow();
        assertEquals("meowfih", c.owner());
        assertEquals(75, c.bite());
        assertEquals("common", c.rarity());
        assertEquals(895, c.seconds());

        // luna stood 2.5 blocks away on the ground: inside. A spot 25 blocks out is not.
        assertTrue(ChummerRingGeometry.inRange(X, Z, X + 2.5, Z));
        assertTrue(ChummerRingGeometry.inRange(X, Z, X, Z - 19.9));
        assertFalse(ChummerRingGeometry.inRange(X, Z, X + 25, Z));

        // the ring is drawn camera relative around the hologram: a camera at luna's spot, on the ground
        double camX = X + 2.5, camY = 26.0 + 1.62, camZ = Z;
        List<float[]> corners = new ArrayList<>();
        ChummerRingGeometry.emit((float) (X - camX), (float) (26.0 + ChummerRingGeometry.LIFT - camY), (float) (Z - camZ),
                (x, y, z) -> corners.add(new float[]{x, y, z}));
        assertEquals(ChummerRingGeometry.SEGMENTS * 4, corners.size());
        for (float[] v : corners) {
            // between the ground (1.62 below the eyes) and one block above it
            assertTrue(v[1] >= 26.0 - camY - 1e-3 && v[1] <= 27.0 + ChummerRingGeometry.LIFT - camY + 1e-3);
            double wx = v[0] + camX, wz = v[2] + camZ; // back to the world
            double d = Math.hypot(wx - X, wz - Z);
            // the wall marks exactly the edge of the range, all the way round
            assertEquals(ChummerRingGeometry.RADIUS, d, 1e-3);
            assertTrue(ChummerRingGeometry.inRange(X, Z, wx, wz) || d - ChummerRingGeometry.RADIUS < 1e-3);
        }
    }
}
