package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Settings saved before the short pet tooltips existed keep them off; the pet and fish switches are separate and survive a save. */
class PetTooltipIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        TackleboxSettings.importLegacy(com.google.gson.JsonParser.parseString(json).getAsJsonObject()); // the options are no longer in the gson file
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepPetTooltipsOff() {
        assertFalse(load("{\"shortFishTooltips\": true}").shortPetTooltips);
    }

    @Test
    void petSwitchRoundTripsWithoutTouchingTheFishSwitch() {
        TackleboxSettings s = load("{\"shortPetTooltips\": true}");
        assertTrue(s.shortPetTooltips);
        assertFalse(s.shortFishTooltips);
        TackleboxSettings fresh = load("{}");
        assertFalse(fresh.shortPetTooltips);
        assertFalse(fresh.shortFishTooltips);
    }
}
