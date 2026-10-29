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

    /** A bar holding both the time/location and the community goal must still give the location. */
    @Test
    void barWithTimeAndGoalStillGivesTheLocation() {
        ServerInfo.locationName = "";
        assertTrue(ServerInfo.readBar("\uF039 12:30 PM 21\u00b0 Cairns  Community Goal: 21.28K/100k Catches"));
        org.junit.jupiter.api.Assertions.assertEquals("cairns", ServerInfo.locationName);
        org.junit.jupiter.api.Assertions.assertEquals(21280, CommunityGoal.current());
    }

    @Test
    void goalOnlyBarIsNotTheTimeBar() {
        ServerInfo.locationName = "kenai river";
        org.junit.jupiter.api.Assertions.assertFalse(ServerInfo.readBar("Community Goal: 21.28K/100k Catches"));
        org.junit.jupiter.api.Assertions.assertEquals("kenai river", ServerInfo.locationName);
    }
}
