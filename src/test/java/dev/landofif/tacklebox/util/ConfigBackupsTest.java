package dev.landofif.tacklebox.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Which files go in, one zip per day, and only the newest few stay. */
class ConfigBackupsTest {
    @TempDir
    Path dir;

    @Test
    void leavesOutBackupsDumpsPicturesAndLeftovers() {
        assertTrue(ConfigBackups.kept(Path.of("settings.json")));
        assertTrue(ConfigBackups.kept(Path.of("stats", "abc.json")));
        assertTrue(ConfigBackups.kept(Path.of("vaults.nbt")));
        for (Path p : List.of(Path.of("backups", "2026-10-28.zip"), Path.of("guidump", "x.txt"), Path.of("menu.png"),
                Path.of("hud.PNG"), Path.of("settings.json.broken"), Path.of("settings.json.tmp"), Path.of("bg.jpg"))) {
            assertFalse(ConfigBackups.kept(p), p.toString());
        }
    }

    @Test
    void oneZipPerDay() throws IOException {
        Files.writeString(dir.resolve("settings.json"), "{}");
        LocalDate day = LocalDate.of(2026, 10, 29);
        assertEquals(dir.resolve("backups/2026-10-29.zip"), ConfigBackups.run(dir, day, 7));
        assertNull(ConfigBackups.run(dir, day, 7));
        assertFalse(Files.exists(dir.resolve("backups/2026-10-29.zip.tmp")));
    }

    @Test
    void nothingToBackUpMakesNoZip() throws IOException {
        assertNull(ConfigBackups.run(dir.resolve("missing"), LocalDate.of(2026, 10, 29), 7));
        assertNull(ConfigBackups.run(dir, LocalDate.of(2026, 10, 29), 7));
        assertFalse(Files.exists(dir.resolve("backups")));
    }

    @Test
    void pruneKeepsTheNewestZipsOnly() throws IOException {
        Path b = Files.createDirectories(dir.resolve("backups"));
        for (int d = 1; d <= 9; d++) {
            Files.writeString(b.resolve(LocalDate.of(2026, 10, d) + ".zip"), "");
        }
        Files.writeString(b.resolve("mine.zip"), "");
        ConfigBackups.prune(b, 7);
        assertFalse(Files.exists(b.resolve("2026-10-01.zip")));
        assertFalse(Files.exists(b.resolve("2026-10-02.zip")));
        assertTrue(Files.exists(b.resolve("2026-10-03.zip")));
        assertTrue(Files.exists(b.resolve("2026-10-09.zip")));
        assertTrue(Files.exists(b.resolve("mine.zip")));
    }
}
