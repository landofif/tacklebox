package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

class LocationsTest {
    @Test
    void theMediterraneanSeaIsShownAsMed() {
        assertEquals("med", Locations.shown("mediterranean sea"));
        assertEquals("med rays", Locations.shown("mediterranean sea rays"));
        assertEquals("Med", Locations.shown("Mediterranean Sea"));
        assertEquals("1x Mediterranean Shard", Locations.shown("1x Mediterranean Shard")); // not the place
        assertEquals("kenai river", Locations.shown("kenai river"));
    }

    @Test
    void thePlaceItselfIsNamedMedAndStillFoundByTheServersName() {
        Locations.Location med = Locations.byName("Mediterranean Sea");
        assertEquals("med", med.name());
        assertSame(med, Locations.byName("mediterranean sea")); // the boss bar's name, lowercased
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
