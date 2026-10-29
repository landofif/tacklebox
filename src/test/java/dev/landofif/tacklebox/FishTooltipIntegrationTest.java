package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Settings saved before the short fish tooltips existed keep them off; a saved switch loads and survives a save. */
class FishTooltipIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepFishTooltipsOff() {
        assertFalse(load("{\"swingSlowness\": 2.0}").shortFishTooltips);
    }

    @Test
    void savedSwitchRoundTrips() {
        TackleboxSettings s = load("{\"shortFishTooltips\": true}");
        assertTrue(s.shortFishTooltips);
        assertTrue(load(new Gson().toJson(s)).shortFishTooltips);
    }
}
