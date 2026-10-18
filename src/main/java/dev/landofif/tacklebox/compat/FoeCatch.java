package dev.landofif.tacklebox.compat;

import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.menu.Compendium;

import java.lang.reflect.Field;

/**
 * Foecollab already sees every catch of yours (title, subtitle, then the new fish stack) and hands the fish to
 * {@code ProfileDataHandler.updateStatsOnCatch(Fish)}; this reads species, variant, size and location off that fish by reflection
 * and passes them to {@link Compendium}, so the mod needs no catch detection of its own.
 */
public final class FoeCatch {
    private static Field fishId, variant, size, location, constantId;
    private static boolean failed;

    private FoeCatch() {}

    public static void onCatch(Object fish) {
        if (failed) {
            return;
        }
        try {
            if (fishId == null) {
                Class<?> c = fish.getClass();
                variant = c.getField("variant");
                size = c.getField("size");
                location = c.getField("location");
                constantId = Class.forName("io.github.foecollab.FOMC.Constant").getField("ID");
                fishId = c.getField("fishId");
            }
            dev.landofif.tacklebox.hud.CommunityGoal.onCatch(System.currentTimeMillis());
            Compendium.onCatch((String) fishId.get(fish), (String) constantId.get(variant.get(fish)),
                    (String) constantId.get(size.get(fish)), (String) constantId.get(location.get(fish)));
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true; // a foecollab update changed the fish: stop quietly, the compendium pages still work
            Tacklebox.LOGGER.warn("compendium: cannot read foecollab's fish: {}", e.toString());
        }
    }
}
