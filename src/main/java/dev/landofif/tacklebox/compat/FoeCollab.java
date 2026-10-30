package dev.landofif.tacklebox.compat;

import net.fabricmc.loader.api.FabricLoader;

/** Whether foecollab is installed. Tacklebox runs without it; everything that reaches into foecollab is an extra behind this. */
public final class FoeCollab {
    private static Boolean loaded;

    private FoeCollab() {}

    public static boolean loaded() {
        if (loaded == null) {
            try {
                loaded = FabricLoader.getInstance().isModLoaded("foecollab");
            } catch (RuntimeException | LinkageError e) {
                loaded = false; // unit tests have no loader
            }
        }
        return loaded;
    }
}
