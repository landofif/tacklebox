package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The sheet as downloaded on 2026-11-15 (src/test/resources/dumps/petprices-20261115.csv) through the parser and the copy on disk. */
class PetPricesDiskTest {
    private static String sheet() throws Exception {
        return new String(PetPricesDiskTest.class.getResourceAsStream("/dumps/petprices-20261115.csv").readAllBytes(), StandardCharsets.UTF_8);
    }

    @Test
    void theRealSheetBecomesTheTableCellForCell() throws Exception {
        assertTrue(PetPrices.looksRight(sheet()));
        List<List<String>> t = PetPrices.table(PetPrices.parse(sheet()));
        assertEquals(27, t.size());
        assertEquals(List.of("Rating (%)", "Common", "Rare", "Epic", "Legendary", "Mythic", "Max Scale/Luck", ""), t.get(0));
        assertEquals(List.of("<70%", "2k", "4k", "8k", "16k", "32k+", "Adds up to 100k", ""), t.get(1));
        assertEquals("125", t.get(4).get(1)); // the common column has no k from 80% up: shown as written
        assertEquals(List.of("100%", "4-6m", "8-12m", "16-24m", "32-48m", "Priceless", "N/A", "Varies greatly, depending on individual buyer/pet."), t.get(24));
        assertTrue(t.get(25).stream().allMatch(String::isEmpty)); // the sheet's gap before its last row
        assertEquals("20%", t.get(26).get(0));
        assertTrue(t.stream().allMatch(r -> r.size() == 8));
    }

    @Test
    void theCopyOnDiskGivesTheSameTableAfterARestart(@TempDir Path dir) throws Exception {
        Path file = dir.resolve("tacklebox").resolve("petprices.csv");
        assertTrue(PetPrices.load(file).isEmpty()); // nothing yet
        PetPrices.store(file, sheet());
        assertEquals(PetPrices.table(PetPrices.parse(sheet())), PetPrices.load(file));
        assertFalse(Files.exists(file.resolveSibling("petprices.csv.tmp")));
        PetPrices.store(file, "Rating,Common\n90%,500\n"); // the next day's download replaces it
        assertEquals(List.of(List.of("Rating", "Common"), List.of("90%", "500")), PetPrices.load(file));
    }
}
