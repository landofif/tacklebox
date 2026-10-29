package dev.landofif.tacklebox;

import dev.landofif.tacklebox.util.ConfigBackups;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** A config folder like luna's, started on ten days in a row: seven zips stay, the newest holds today's files and nothing else. */
class ConfigBackupsIntegrationTest {
    @TempDir
    Path dir;

    private static Map<String, String> unzip(Path zip) throws IOException {
        Map<String, String> out = new TreeMap<>();
        try (ZipInputStream z = new ZipInputStream(Files.newInputStream(zip))) {
            for (ZipEntry e; (e = z.getNextEntry()) != null; ) {
                out.put(e.getName(), new String(z.readAllBytes()));
            }
        }
        return out;
    }

    @Test
    void tenDaysOfGameStarts() throws IOException {
        Files.writeString(dir.resolve("triggers.json"), "[]");
        Files.writeString(dir.resolve("huds.json"), "{}");
        Files.writeString(dir.resolve("vaults.nbt"), "nbt");
        Files.writeString(dir.resolve("menu.png"), "picture");
        Files.writeString(dir.resolve("settings.json.broken"), "old");
        Files.createDirectories(dir.resolve("stats"));
        Files.writeString(dir.resolve("stats/4bb09b56.json"), "{\"fish\": 1}");
        Files.createDirectories(dir.resolve("guidump"));
        Files.writeString(dir.resolve("guidump/20261006-145646.txt"), "dump");

        LocalDate first = LocalDate.of(2026, 10, 20);
        for (int d = 0; d < 10; d++) {
            Files.writeString(dir.resolve("settings.json"), "{\"day\": " + d + "}");
            ConfigBackups.run(dir, first.plusDays(d), ConfigBackups.KEEP);
            ConfigBackups.run(dir, first.plusDays(d), ConfigBackups.KEEP); // a second start that day
        }

        List<String> zips = new ArrayList<>();
        try (Stream<Path> list = Files.list(dir.resolve("backups"))) {
            list.map(p -> p.getFileName().toString()).sorted().forEach(zips::add);
        }
        assertEquals(List.of("2026-10-23.zip", "2026-10-24.zip", "2026-10-25.zip", "2026-10-26.zip", "2026-10-27.zip",
                "2026-10-28.zip", "2026-10-29.zip"), zips);
        assertEquals(Map.of("huds.json", "{}", "settings.json", "{\"day\": 9}", "stats/4bb09b56.json", "{\"fish\": 1}",
                "triggers.json", "[]", "vaults.nbt", "nbt"), unzip(dir.resolve("backups/2026-10-29.zip")));
        assertEquals("{\"day\": 3}", unzip(dir.resolve("backups/2026-10-23.zip")).get("settings.json"));
    }
}
