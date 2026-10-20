package dev.landofif.tacklebox;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigButton;
import com.teamresourceful.resourcefulconfig.api.annotations.ConfigEntry;
import com.teamresourceful.resourcefulconfig.api.loader.ConfigParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The options moved to Resourceful Config (2026-10-20): its parser takes the annotated class, every option and enum value has a
 * label in the lang file, and settings.json of before the move is read once into the options.
 */
class ResourcefulConfigIntegrationTest {
    private static JsonObject lang() throws IOException {
        Path file = Path.of("src/main/resources/assets/tacklebox/lang/en_us.json");
        return JsonParser.parseString(Files.readString(file, StandardCharsets.UTF_8)).getAsJsonObject();
    }

    @Test
    void theParserAcceptsTheClass() {
        var config = ConfigParser.tryParse(TackleboxSettings.class); // Configurator needs a running Fabric loader, its parser does not
        assertNotNull(config);
        assertTrue(config.elements().size() > 80, "found " + config.elements().size());
    }

    @Test
    void everyOptionAndButtonIsStaticAndHasItsTexts() throws IOException {
        JsonObject lang = lang();
        int entries = 0;
        for (Field f : TackleboxSettings.class.getDeclaredFields()) {
            ConfigEntry e = f.getAnnotation(ConfigEntry.class);
            if (e != null) {
                entries++;
                assertTrue(Modifier.isStatic(f.getModifiers()), f.getName() + " must be static");
                assertEquals(f.getName(), e.id());
                assertTrue(lang.has(e.translation()), "no label for " + f.getName());
                assertTrue(lang.has(e.translation() + ".desc"), "no description for " + f.getName());
                if (f.getType().isEnum()) {
                    for (Object c : f.getType().getEnumConstants()) {
                        String key = ((com.teamresourceful.resourcefulconfig.api.types.info.Translatable) c).getTranslationKey();
                        assertTrue(lang.has(key), "no text for " + key);
                    }
                }
            }
            if (f.isAnnotationPresent(ConfigButton.class)) {
                assertTrue(Modifier.isStatic(f.getModifiers()) && Modifier.isFinal(f.getModifiers()) && f.getType() == Runnable.class, f.getName());
            }
        }
        assertTrue(entries > 70, "found " + entries);
    }

    @Test
    void anOldSettingsFileFillsTheOptionsOnceAndTheRestStaysDefault() {
        TackleboxSettings.importLegacy(JsonParser.parseString(
                "{\"flatMenus\": false, \"hotbarLook\": \"FILL\", \"questsAlign\": \"CENTRE\", \"accent1\": 255, \"hotbarSpacing\": 18,"
                        + " \"equipPlace\": \"NOT_A_PLACE\", \"levelBars\": null}").getAsJsonObject());
        assertFalse(TackleboxSettings.flatMenus);
        assertEquals(TackleboxSettings.BoxLook.FILL, TackleboxSettings.hotbarLook);
        assertEquals(TackleboxSettings.HudAlign.CENTRE, TackleboxSettings.questsAlign);
        assertEquals(255, TackleboxSettings.accent1);
        assertEquals(18, TackleboxSettings.hotbarSpacing);
        assertEquals(TackleboxSettings.EquipPlace.ABOVE, TackleboxSettings.equipPlace); // an unknown value falls back
        assertTrue(TackleboxSettings.levelBars); // null keeps the default
        assertEquals(0x00FF99, TackleboxSettings.accent2);

        TackleboxSettings.importLegacy(new JsonObject()); // a second import starts from the defaults again
        assertTrue(TackleboxSettings.flatMenus);
        assertEquals(TackleboxSettings.BoxLook.HYPR, TackleboxSettings.hotbarLook);
    }
}
