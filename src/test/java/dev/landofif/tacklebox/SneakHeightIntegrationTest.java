package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.display.SneakHeight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Settings saved before the sneak height existed stay vanilla; saved values load, clamp and survive a save. */
class SneakHeightIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepVanillaSneak() {
        TackleboxSettings s = load("{\"swingSlowness\": 2.0}");
        assertEquals(1.5, s.sneakHeight);
        assertEquals(1.27f, SneakHeight.eyeHeight(s.sneakHeight), 1e-4f);
    }

    @Test
    void savedValueRoundTripsAndIsClamped() {
        TackleboxSettings s = load("{\"sneakHeight\": 1.7}");
        assertEquals(1.7, load(new Gson().toJson(s)).sneakHeight);
        assertEquals(1.8, load("{\"sneakHeight\": 5}").sneakHeight);
        assertEquals(1.5, load("{\"sneakHeight\": 0.2}").sneakHeight);
    }
}
