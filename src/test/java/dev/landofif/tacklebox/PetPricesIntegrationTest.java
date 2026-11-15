package dev.landofif.tacklebox;

import dev.landofif.tacklebox.menu.PetPrices;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** The downloaded sheet end to end: checked, turned into the table, and the download time through a save deciding the next download. */
class PetPricesIntegrationTest {
    @Test
    void aDownloadIsCheckedShownAndNotRepeatedForADay() throws Exception {
        String body = new String(getClass().getResourceAsStream("/dumps/petprices-20261115.csv").readAllBytes(), StandardCharsets.UTF_8);
        long now = 1_800_000_000_000L;
        TackleboxSettings s = new Gson().fromJson("{}", TackleboxSettings.class);
        s.sanitize();
        assertTrue(PetPrices.due(s.petPricesAt, 0, now)); // a fresh install downloads when the screen opens

        assertTrue(PetPrices.looksRight(body));
        List<List<String>> table = PetPrices.table(PetPrices.parse(body));
        assertEquals("Mythic", table.get(0).get(5));
        assertEquals("10.4m", table.get(17).get(5)); // 93%, mythic
        s.petPricesAt = now;

        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        back.sanitize();
        assertFalse(PetPrices.due(back.petPricesAt, 0, now + 23 * 3_600_000L)); // after a restart the same day
        assertTrue(PetPrices.due(back.petPricesAt, 0, now + PetPrices.DAY));
        assertEquals("updated 23h ago", PetPrices.age(back.petPricesAt, now + 23 * 3_600_000L));
    }
}
