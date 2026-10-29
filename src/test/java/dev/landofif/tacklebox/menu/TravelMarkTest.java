package dev.landofif.tacklebox.menu;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.nbt.StringNbtReader;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The /travel item names and data of luna's dump during a frenzy at Toledo Bend (2026-10-29 20:15). */
class TravelMarkTest {
    @Test
    void frenzyInTheNameIsGreen() {
        assertEquals(Locations.FRENZY, Locations.mark("Toledo Bend Reservoir *FRENZY!*", ""));
        assertEquals(Locations.FRENZY, Locations.mark("Oil Rig *FRENZY!*", "")); // the 2026-10-24 dump
    }

    @Test
    void plainLocationsAreNotMarked() {
        assertEquals(0, Locations.mark("Oil Rig", ""));
        assertEquals(0, Locations.mark("Cypress Lake", ""));
        assertEquals(0, Locations.mark("Return", ""));
    }

    @Test
    void fabledPlaceIsRed() {
        assertEquals(Locations.FABLED, Locations.mark("Cypress Lake", "Cypress Lake"));
        assertEquals(0, Locations.mark("Kenai River", "Cypress Lake"));
        assertEquals(0, Locations.mark("Return", "Somewhere Else"));
    }

    @Test
    void frenzyGoesBeforeFabled() {
        assertEquals(Locations.FRENZY, Locations.mark("Cypress Lake *FRENZY!*", "Cypress Lake"));
    }

    @Test
    void onlyFastTravelItemsAreTravelItems() throws CommandSyntaxException {
        assertTrue(Locations.isTravelItem(StringNbtReader.readCompound("{fastTravel:1b}")));
        assertFalse(Locations.isTravelItem(StringNbtReader.readCompound("{filter:\"normal\"}"))); // the compendium filter hopper
    }
}
