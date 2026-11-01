package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The collections overview as {@code Power.overview} reads it (lore lines already through {@code Glyphs.plain}). */
class PowerTest {
    @Test
    void aSpeciesCollectionGivesItsNextLevelFromTheTable() {
        Map<String, String> found = new HashMap<>();
        Set<String> finished = new HashSet<>();
        Power.overview("basses collection", List.of("basses collection", "click to open", "current level - [6]", "(43,178/50k)",
                "progress to level 7"), found, finished);
        assertEquals(Map.of("basses", "7|43178|50000|10000"), found);
        assertTrue(finished.isEmpty());
    }

    @Test
    void aCollectionIsFinishedWhenItsCountSaysMax() {
        // luna's catfishes at its top, screenshot 2026-11-01
        Map<String, String> found = new HashMap<>();
        Set<String> finished = new HashSet<>();
        Power.overview("catfishes collection", List.of("catfishes collection", "click to open", "current level - [7]", "(30,039/max)",
                "catch at:", "- cypress lake"), found, finished);
        assertEquals(Set.of("catfishes"), finished);
        assertTrue(found.isEmpty());
    }

    @Test
    void giganticAtEightIsNeitherFinishedNorASpecies() {
        // 10 levels with their own power: the overview must not drop what its level pages gave
        Map<String, String> found = new HashMap<>();
        Set<String> finished = new HashSet<>();
        Power.overview("gigantic collection", List.of("gigantic collection", "click to open", "current level - [8]", "(1,886/5k)",
                "progress to level 9"), found, finished);
        Power.overview("gigantic collection", List.of("gigantic collection", "current level - [3]", "(20/40)", "progress to level 4"), found, finished);
        assertTrue(finished.isEmpty());
        assertTrue(found.isEmpty());
    }
}
