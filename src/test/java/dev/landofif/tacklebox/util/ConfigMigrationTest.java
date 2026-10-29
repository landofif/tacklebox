package dev.landofif.tacklebox.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigrationTest {
    @TempDir
    Path config;

    @Test
    void movesTheOldFolderWithEverythingInIt() throws IOException {
        Path old = Files.createDirectories(config.resolve("dofisaddons"));
        Files.writeString(old.resolve("settings.json"), "{\"a\":1}");
        Files.writeString(Files.createDirectories(old.resolve("stats")).resolve("day.json"), "{}");

        assertTrue(ConfigMigration.run(config, "tacklebox"));

        assertFalse(Files.exists(config.resolve("dofisaddons")));
        assertEquals("{\"a\":1}", Files.readString(config.resolve("tacklebox").resolve("settings.json")));
        assertTrue(Files.isRegularFile(config.resolve("tacklebox").resolve("stats").resolve("day.json")));
    }

    @Test
    void leavesBothAloneWhenTheNewFolderIsThere() throws IOException {
        Files.createDirectories(config.resolve("dofisaddons"));
        Files.writeString(config.resolve("dofisaddons").resolve("settings.json"), "old");
        Files.createDirectories(config.resolve("tacklebox"));
        Files.writeString(config.resolve("tacklebox").resolve("settings.json"), "new");

        assertFalse(ConfigMigration.run(config, "tacklebox"));

        assertEquals("old", Files.readString(config.resolve("dofisaddons").resolve("settings.json")));
        assertEquals("new", Files.readString(config.resolve("tacklebox").resolve("settings.json")));
    }

    @Test
    void doesNothingWithoutAnOldFolder() throws IOException {
        assertFalse(ConfigMigration.run(config, "tacklebox"));
        assertFalse(Files.exists(config.resolve("tacklebox")));
    }

    @Test
    void ignoresAFileCalledLikeTheOldFolder() throws IOException {
        Files.writeString(config.resolve("dofisaddons"), "not a folder");
        assertFalse(ConfigMigration.run(config, "tacklebox"));
        assertTrue(Files.isRegularFile(config.resolve("dofisaddons")));
    }
}
