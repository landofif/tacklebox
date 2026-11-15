package dev.landofif.tacklebox;

import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Settings saved before the option existed keep the block outline; the saved choice survives a save. */
class BlockOutlineIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepTheOutline() {
        assertFalse(load("{\"swingSlowness\": 2.0}").hideBlockOutline);
    }

    @Test
    void savedChoiceRoundTrips() {
        TackleboxSettings s = load("{\"hideBlockOutline\": true}");
        assertTrue(s.hideBlockOutline);
        assertTrue(load(new Gson().toJson(s)).hideBlockOutline);
    }
}
