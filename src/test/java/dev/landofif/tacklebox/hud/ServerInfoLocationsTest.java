package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.menu.Locations;
import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ServerInfoLocationsTest {
    /** The boss bar name must lead to a known location, or the level bars, quests left and server hud lose it. */
    @Test
    void everyLocationTheMenusKnowCanBeReadFromTheBossBar() {
        for (Locations.Location loc : Locations.ALL) {
            boolean readable = ServerInfo.LOCATIONS.values().stream()
                    .anyMatch(name -> Locations.byName(name.toLowerCase(Locale.ROOT)) == loc);
            assertTrue(readable, loc.id() + " is missing from ServerInfo.LOCATIONS");
        }
    }
}
