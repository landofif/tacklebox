package dev.landofif.tacklebox.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** The mod used to be called dofisaddons. Moves {@code config/dofisaddons} to the new config folder the first time it starts. */
public final class ConfigMigration {
    private static final String OLD_NAME = "dofisaddons";

    private ConfigMigration() {}

    /** True if the old folder was moved. Does nothing when there is no old folder or the new one already exists. */
    public static boolean run(Path configDir, String newName) throws IOException {
        Path old = configDir.resolve(OLD_NAME);
        Path target = configDir.resolve(newName);
        if (!Files.isDirectory(old) || Files.exists(target)) {
            return false;
        }
        Files.move(old, target);
        return true;
    }
}
