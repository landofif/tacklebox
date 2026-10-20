package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.chat.ChatBox;
import dev.landofif.tacklebox.util.ConfigMigration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * An install from before the rename (luna's settings.json of 2026-10-11 in {@code config/dofisaddons}) goes through the first
 * start of the renamed mod, which moves the folder, then through a second start that must not touch the moved settings.
 */
class ConfigMigrationIntegrationTest {
    private static final String OLD = """
            {"levelBars": true, "levelBarsX": 745, "levelBarsY": 1,
             "chatBoxes": [
               {"name": "dms", "enabled": true, "x": 61.458333333333336, "y": 74.07407407407408, "width": 180, "lines": 7}]}
            """;

    @TempDir
    Path config;

    private TackleboxSettings readSettings() throws IOException {
        String raw = Files.readString(config.resolve(Tacklebox.MOD_ID).resolve("settings.json"));
        TackleboxSettings s = new Gson().fromJson(raw, TackleboxSettings.class);
        TackleboxSettings.importLegacy(com.google.gson.JsonParser.parseString(raw).getAsJsonObject());
        s.sanitize();
        return s;
    }

    @Test
    void oldInstallKeepsItsSettingsAndPicturesAcrossTwoStarts() throws IOException {
        Path old = Files.createDirectories(config.resolve("dofisaddons"));
        Files.writeString(old.resolve("settings.json"), OLD);
        Files.write(old.resolve("menu.png"), new byte[] {1, 2, 3});

        assertTrue(ConfigMigration.run(config, Tacklebox.MOD_ID)); // first start after the rename
        TackleboxSettings s = readSettings();
        assertTrue(TackleboxSettings.Huds.levelBars);
        assertTrue(TackleboxSettings.Huds.levelBarsBar); // a setting added later is on in an old file that does not have it
        ChatBox dms = s.chatBoxes.get(0);
        assertEquals("dms", dms.name);
        assertEquals(180, dms.width);
        assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(config.resolve(Tacklebox.MOD_ID).resolve("menu.png")));

        Files.writeString(config.resolve(Tacklebox.MOD_ID).resolve("settings.json"), "{\"levelBars\": false}"); // changed in the new version
        assertFalse(ConfigMigration.run(config, Tacklebox.MOD_ID)); // second start
        readSettings();
        assertFalse(TackleboxSettings.Huds.levelBars);
    }
}
