package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PetPricesTest {
    @Test
    void parsesQuotesCommasAndBothLineEndings() {
        assertEquals(List.of(List.of("a", "b", ""), List.of("1", "x, y", "say \"hi\""), List.of("two\nlines", "z")),
                PetPrices.parse("a,b,\r\n1,\"x, y\",\"say \"\"hi\"\"\"\n\"two\nlines\",z"));
        assertTrue(PetPrices.parse("").isEmpty());
        assertEquals(List.of(List.of("a", "b")), PetPrices.parse("a,b\n"));
    }

    @Test
    void theTableKeepsCellsAsWrittenAndDropsOnlyEmptyColumnsAndOuterEmptyRows() {
        List<List<String>> t = PetPrices.table(PetPrices.parse(",,\nRating,Common,,Note\n80%,125,, \n,,,\n,,,\n20%,100k,,\"Varies, a lot\"\n,,,\n"));
        assertEquals(List.of(List.of("Rating", "Common", "Note"), List.of("80%", "125", ""), List.of("", "", ""),
                List.of("20%", "100k", "Varies, a lot")), t);
    }

    @Test
    void anErrorPageIsNotTheSheet() {
        assertTrue(PetPrices.looksRight("Rating (%),Common\n80%,125\n"));
        assertFalse(PetPrices.looksRight("<!DOCTYPE html><html><body>Sign in</body></html>"));
        assertFalse(PetPrices.looksRight(""));
        assertFalse(PetPrices.looksRight(null));
        assertFalse(PetPrices.looksRight("just one line of text"));
    }

    @Test
    void oneDownloadADayAndARetryTenMinutesAfterAFailure() {
        long now = 1_800_000_000_000L;
        assertTrue(PetPrices.due(0, 0, now));                           // never downloaded
        assertFalse(PetPrices.due(now - PetPrices.DAY + 1, 0, now));    // a copy of today
        assertTrue(PetPrices.due(now - PetPrices.DAY, 0, now));
        assertFalse(PetPrices.due(0, now - PetPrices.RETRY + 1, now));  // just failed
        assertTrue(PetPrices.due(0, now - PetPrices.RETRY, now));
        assertFalse(PetPrices.due(now - 1000, now - PetPrices.DAY, now));
    }

    @Test
    void theAgeReadsShort() {
        long now = 1_800_000_000_000L;
        assertEquals("updated just now", PetPrices.age(now - 20_000, now));
        assertEquals("updated 59m ago", PetPrices.age(now - 59 * 60_000L, now));
        assertEquals("updated 3h ago", PetPrices.age(now - 3 * 3_600_000L - 5, now));
        assertEquals("updated 2d ago", PetPrices.age(now - 2 * PetPrices.DAY, now));
    }
}
