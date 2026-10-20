package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Bite focus is off for settings saved before it existed, and a saved on survives a save and load. */
class BiteFocusIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        TackleboxSettings.importLegacy(com.google.gson.JsonParser.parseString(json).getAsJsonObject()); // the options are no longer in the gson file
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsStayOff() {
        assertFalse(load("{\"biteOn\": true}").biteFocus);
    }

    @Test
    void savedOnRoundTrips() {
        TackleboxSettings s = load("{\"biteFocus\": true}");
        assertTrue(load(new Gson().toJson(s)).biteFocus);
    }
}
