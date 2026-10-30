package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.menu.ChestScale;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Rarity shape, page wheel, tall tooltip scroll and chest menu scale: old settings keep them off, saved ones load and survive a save. */
class MenuItemsIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepEverythingAsBefore() {
        TackleboxSettings s = load("{\"rarityCircles\": true}");
        assertTrue(s.rarityCircles);
        assertEquals(TackleboxSettings.RarityShape.CIRCLE, s.rarityShape);
        assertFalse(s.pageWheel);
        assertFalse(s.tooltipScroll);
        assertEquals(0, s.chestScale);
        assertEquals(3, ChestScale.guiScale(true, s.chestScale, 3)); // a chest keeps the games scale
    }

    @Test
    void savedValuesRoundTrip() {
        TackleboxSettings s = load("{\"rarityShape\": \"SQUARE\", \"pageWheel\": true, \"tooltipScroll\": true, \"chestScale\": 2}");
        TackleboxSettings back = load(new Gson().toJson(s));
        assertEquals(TackleboxSettings.RarityShape.SQUARE, back.rarityShape);
        assertTrue(back.pageWheel);
        assertTrue(back.tooltipScroll);
        assertEquals(2, back.chestScale);
        assertEquals(2, ChestScale.guiScale(true, back.chestScale, 3));
    }

    @Test
    void badValuesAreMended() {
        assertEquals(TackleboxSettings.RarityShape.CIRCLE, load("{\"rarityShape\": \"HEXAGON\"}").rarityShape);
        assertEquals(6, load("{\"chestScale\": 40}").chestScale);
        assertEquals(0, load("{\"chestScale\": -3}").chestScale);
    }
}
