package dev.landofif.dofisaddons;

import com.google.gson.Gson;
import dev.landofif.dofisaddons.display.SneakHeight;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Settings saved before the sneak height existed stay vanilla; saved values load, clamp and survive a save. */
class SneakHeightIntegrationTest {
    private static DofiSettings load(String json) {
        DofiSettings s = new Gson().fromJson(json, DofiSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepVanillaSneak() {
        DofiSettings s = load("{\"swingSlowness\": 2.0}");
        assertEquals(1.5, s.sneakHeight);
        assertEquals(1.27f, SneakHeight.eyeHeight(s.sneakHeight), 1e-4f);
    }

    @Test
    void savedValueRoundTripsAndIsClamped() {
        DofiSettings s = load("{\"sneakHeight\": 1.7}");
        assertEquals(1.7, load(new Gson().toJson(s)).sneakHeight);
        assertEquals(1.8, load("{\"sneakHeight\": 5}").sneakHeight);
        assertEquals(1.5, load("{\"sneakHeight\": 0.2}").sneakHeight);
    }
}
