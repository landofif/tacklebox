package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Locale;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LocationsTest {
    /** The names luna chose on 2026-11-15, in the travel menu's order. */
    private static final List<String> NAMES = List.of("cypress", "kenai", "biwa", "murray", "everglades", "key", "toledo", "great lakes",
            "danube", "rig", "amazon", "med", "cape", "hawaii", "lofoten", "cairns", "nile");

    @Test
    void everyPlaceHasItsShortName() {
        assertEquals(NAMES, Locations.ALL.stream().map(Locations.Location::name).toList());
    }

    @Test
    void theServersNamesAreShownShort() {
        // the boss bar's names (ServerInfo.LOCATIONS), as sent and lowercase
        List<String> sent = List.of("Cypress Lake", "Kenai River", "Lake Biwa", "Murray River", "Everglades", "Key West", "Toledo Bend Reservoir",
                "Great Lakes", "Danube River", "Oil Rig", "Amazon River", "Mediterranean Sea", "Cape Cod", "Hawaii", "Lofoten Islands", "Cairns",
                "Nile River");
        List<String> shown = List.of("Cypress", "Kenai", "Biwa", "Murray", "Everglades", "Key", "Toledo", "Great Lakes", "Danube", "Rig", "Amazon",
                "Med", "Cape", "Hawaii", "Lofoten", "Cairns", "Nile");
        for (int i = 0; i < sent.size(); i++) {
            assertEquals(shown.get(i), Locations.shown(sent.get(i)));
            assertEquals(NAMES.get(i), Locations.shown(sent.get(i).toLowerCase(Locale.ROOT)));
            assertEquals(NAMES.get(i), Locations.byName(sent.get(i)).name()); // the place is still found by the full name
        }
        assertEquals("toledo", Locations.shown("toledo bend"));
        assertEquals("med rays", Locations.shown("mediterranean sea rays"));
        assertEquals("Catch 70x Rare fish at Murray", Locations.shown("Catch 70x Rare fish at Murray River"));
        assertEquals("1x Mediterranean Shard", Locations.shown("1x Mediterranean Shard")); // not the place
        assertEquals("nile perch", Locations.shown("nile perch"));
    }

    @Test
    void everyPlaceStillHasItsBaitData() {
        for (Locations.Location l : Locations.ALL) {
            BaitPicker.Loc wiki = BaitPicker.loc(l);
            assertNotNull(wiki, l.id());
            assertSame(l, Locations.byName(wiki.name()), wiki.name());
        }
        assertEquals("Mediterranean Sea", BaitPicker.loc(Locations.byName("mediterranean")).name());
    }
}
