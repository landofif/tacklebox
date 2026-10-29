package dev.landofif.tacklebox.compat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/** Your crew and friends as foecollab saved them (its profile data), read by reflection at most once a second. */
public final class FoeFriends {
    private static Set<UUID> kept = Set.of();
    private static long readAt;

    private FoeFriends() {}

    /** Crewmates and friends; empty without foecollab. */
    public static Set<UUID> kept() {
        long now = System.currentTimeMillis();
        if (now - readAt < 1000) {
            return kept;
        }
        readAt = now;
        Set<UUID> next = new HashSet<>();
        try {
            Object profiles = Class.forName("io.github.foecollab.handler.ProfileDataHandler").getMethod("instance").invoke(null);
            Object data = profiles.getClass().getField("profileData").get(profiles);
            for (String list : new String[] {"crewMembers", "friends"}) {
                if (data.getClass().getField(list).get(data) instanceof Collection<?> ids) {
                    for (Object id : ids) {
                        if (id instanceof UUID uuid) {
                            next.add(uuid);
                        }
                    }
                }
            }
        } catch (ReflectiveOperationException | LinkageError | RuntimeException e) {
            // no foecollab, or it changed: nobody is kept
        }
        kept = next;
        return kept;
    }
}
