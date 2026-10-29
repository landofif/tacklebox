package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Settings saved before the black sky existed keep the normal sky; the saved choice survives a save. */
class BlackSkyIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepNormalSky() {
        assertFalse(load("{\"swingSlowness\": 2.0}").blackSky);
    }

    @Test
    void savedChoiceRoundTrips() {
        TackleboxSettings s = load("{\"blackSky\": true}");
        assertTrue(s.blackSky);
        assertTrue(load(new Gson().toJson(s)).blackSky);
    }
}
