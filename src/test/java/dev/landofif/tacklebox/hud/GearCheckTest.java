package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GearCheckTest {
    private static final GearCheck.Place DANUBE = GearCheck.place("danube");

    @BeforeEach
    void setUp() {
        GearCheck.reset();
    }

    @Test
    void everyLocationHasItsOwnPetAndAKnownClimate() {
        assertEquals(17, GearCheck.PLACES.size());
        assertEquals(17, GearCheck.PLACES.stream().map(GearCheck.Place::petId).distinct().count());
        for (GearCheck.Place p : GearCheck.PLACES) {
            assertTrue(ServerInfo.LOCATIONS.containsKey(p.id()), p.id());
            assertTrue(GearCheck.CLIMATES.contains(p.climate()), p.climate());
            assertEquals(p.petId(), GearCheck.petId(p.pet()));
        }
        assertEquals("wolf", DANUBE.pet());
        assertEquals("marsh rabbit", GearCheck.place("everglades").pet());
        assertEquals("hippo", GearCheck.place("nile").pet());
        assertNull(GearCheck.place("crewisland"));
        assertNull(GearCheck.place(""));
    }

    @Test
    void chatLinesTellTheEquippedPet() {
        assertEquals("eagle", GearCheck.equipLine("PETS > Equipped your Eagle Pet"));
        assertEquals("marshrabbit", GearCheck.equipLine("PETS > Your Marsh Rabbit Pet is now level 12 → 13 (+1)"));
        assertEquals("", GearCheck.equipLine("PETS > Pet unequipped!"));
        assertNull(GearCheck.equipLine("PETS > You cannot move your active pet in your inventory!"));
        assertNull(GearCheck.equipLine("PETS > You must wait before doing that again!"));
        assertNull(GearCheck.equipLine("[12] someone > PETS > Equipped your Eagle Pet"));
    }

    @Test
    void theRightPetAndArmorGiveNoRows() {
        assertTrue(GearCheck.rows(DANUBE, "wolf", true, List.of(new GearCheck.Armor("chestplate", "continental"))).isEmpty());
        assertNull(GearCheck.chatLine(DANUBE, "wolf", true, List.of()));
    }

    @Test
    void aWrongPetNamesTheRightOneAndWhetherYouHaveIt() {
        assertEquals(List.of(new TextHud.Row("wrong pet", "use wolf · in inventory", GearCheck.RED, 0)),
                GearCheck.rows(DANUBE, "eagle", true, List.of()));
        assertEquals(List.of(new TextHud.Row("wrong pet", "use wolf · not in inventory", GearCheck.RED, 0)),
                GearCheck.rows(DANUBE, "eagle", false, List.of()));
        assertEquals("wrong pet at danube river: use a wolf pet (not in inventory)", GearCheck.chatLine(DANUBE, "eagle", false, List.of()));
    }

    @Test
    void noPetOrAnUnknownOneIsNoPetWarning() {
        assertTrue(GearCheck.rows(DANUBE, "", false, List.of()).isEmpty());
        assertTrue(GearCheck.rows(null, "eagle", false, List.of(new GearCheck.Armor("boots", "arid"))).isEmpty());
        assertNull(GearCheck.chatLine(null, "eagle", false, List.of()));
    }

    @Test
    void armorOfAnotherClimateIsNamedPieceByPiece() {
        List<GearCheck.Armor> worn = List.of(new GearCheck.Armor("chestplate", "Savanna"), new GearCheck.Armor("leggings", "continental"),
                new GearCheck.Armor("boots", "semi-arid"), new GearCheck.Armor("boots", "not a climate"));
        assertEquals(List.of(new TextHud.Row("wrong armor", "savanna chestplate, semi-arid boots · need continental", GearCheck.RED, 0)),
                GearCheck.rows(DANUBE, "wolf", true, worn));
        assertEquals("wrong pet at danube river: use a wolf pet (in inventory) · wrong armor: savanna chestplate, semi-arid boots, danube river is continental",
                GearCheck.chatLine(DANUBE, "duck", true, worn));
    }

    @Test
    void theSpawnHubIsOnlyAtCypressLake() {
        assertTrue(GearCheck.spawnHub("spawn", 120, 0));
        assertFalse(GearCheck.spawnHub("spawn", 200, 0));
        assertFalse(GearCheck.spawnHub("spawn", 120, 39));
        assertFalse(GearCheck.spawnHub("danube", 120, 0));
    }

    @Test
    void anArrivalComesOnceThreeSecondsAfterThePlaceChanged() {
        assertFalse(GearCheck.arrived("danube", 1000));
        assertFalse(GearCheck.arrived("danube", 3999));
        assertTrue(GearCheck.arrived("danube", 4000));
        assertFalse(GearCheck.arrived("danube", 4500));
        assertFalse(GearCheck.arrived("", 5000)); // the spawn hub or no place
        assertFalse(GearCheck.arrived("", 9000));
        assertFalse(GearCheck.arrived("nile", 10_000));
        assertFalse(GearCheck.arrived("kenai", 12_000)); // moved on before the check
        assertFalse(GearCheck.arrived("kenai", 14_000));
        assertTrue(GearCheck.arrived("kenai", 15_000));
    }
}
