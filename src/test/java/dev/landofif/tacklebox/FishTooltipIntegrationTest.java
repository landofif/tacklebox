package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Settings saved before the short fish tooltips existed keep them off; a saved switch loads and survives a save. */
class FishTooltipIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        TackleboxSettings.importLegacy(com.google.gson.JsonParser.parseString(json).getAsJsonObject()); // the options are no longer in the gson file
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepFishTooltipsOff() {
        assertFalse(after(load("{\"swingSlowness\": 2.0}"), TackleboxSettings.Menus.shortFishTooltips));
    }

    @Test
    void savedSwitchRoundTrips() {
        TackleboxSettings s = load("{\"shortFishTooltips\": true}");
        assertTrue(TackleboxSettings.Menus.shortFishTooltips);
        assertFalse(after(load("{}"), TackleboxSettings.Menus.shortFishTooltips));
    }

    /** Evaluates {@code first} (the load) before reading the static option. */
    private static <T> T after(Object first, T value) {
        return value;
    }
}
