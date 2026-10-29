package dev.landofif.tacklebox.util;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Daily backups of config/tacklebox: on the first game start of a day everything in it goes into backups/yyyy-MM-dd.zip, and
 * only the newest {@link #KEEP} zips stay. Left out: the backups themselves, gui dumps, pictures, and .tmp/.broken files.
 * Restore by unzipping one into config/tacklebox while the game is closed.
 */
public final class ConfigBackups {
    public static final int KEEP = 7;
    public static final String FOLDER = "backups";

    private ConfigBackups() {}

    /** Makes today's zip if there is none yet and deletes the oldest past {@code keep}; the zip made, or null. */
    public static Path run(Path dir, LocalDate today, int keep) throws IOException {
        if (!Files.isDirectory(dir)) {
            return null;
        }
        Path backups = dir.resolve(FOLDER);
        Path zip = backups.resolve(today + ".zip");
        Path made = null;
        if (!Files.exists(zip)) {
            List<Path> files = files(dir);
            if (!files.isEmpty()) {
                Files.createDirectories(backups);
                Path tmp = backups.resolve(today + ".zip.tmp");
                try (OutputStream out = Files.newOutputStream(tmp); ZipOutputStream z = new ZipOutputStream(out)) {
                    for (Path f : files) {
                        z.putNextEntry(new ZipEntry(dir.relativize(f).toString().replace('\\', '/')));
                        Files.copy(f, z);
                        z.closeEntry();
                    }
                }
                Files.move(tmp, zip, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
                made = zip;
            }
        }
        prune(backups, keep);
        return made;
    }

    /** The files a backup holds, sorted. */
    static List<Path> files(Path dir) throws IOException {
        List<Path> out = new ArrayList<>();
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile).filter(f -> kept(dir.relativize(f))).sorted().forEach(out::add);
        }
        return out;
    }

    static boolean kept(Path rel) {
        String top = rel.getName(0).toString();
        if (top.equals(FOLDER) || top.equals("guidump")) {
            return false;
        }
        String name = rel.getFileName().toString().toLowerCase();
        return !(name.endsWith(".tmp") || name.endsWith(".broken") || name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg"));
    }

    /** Deletes daily zips past the newest {@code keep}; other files in the folder are left alone. */
    static void prune(Path backups, int keep) throws IOException {
        if (!Files.isDirectory(backups)) {
            return;
        }
        List<Path> zips;
        try (Stream<Path> list = Files.list(backups)) {
            zips = list.filter(p -> p.getFileName().toString().matches("\\d{4}-\\d{2}-\\d{2}\\.zip"))
                    .sorted(Comparator.comparing((Path p) -> p.getFileName().toString()).reversed()).toList();
        }
        for (int i = keep; i < zips.size(); i++) {
            Files.deleteIfExists(zips.get(i));
        }
    }
}
