package dev.landofif.tacklebox;

import dev.landofif.tacklebox.hud.CryptidHud;
import dev.landofif.tacklebox.hud.Cryptids;
import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.hud.ServerInfo;
import dev.landofif.tacklebox.trigger.Matcher;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real chat lines from luna's logs through the server hud and the cryptids hud, to the rows, the popups and the next moon. */
class CryptidsIntegrationTest {
    private final long savedRise = TackleboxSettings.get().moonRiseAt;

    @AfterEach
    void tearDown() {
        ServerHud.reset();
        Cryptids.reset();
        ServerInfo.time = "";
        ServerInfo.ampm = "";
        TackleboxSettings.get().moonRiseAt = savedRise;
        TackleboxSettings.get().cryptidOff.clear();
        TackleboxSettings.get().cryptidDry.clear();
    }

    private static void chat(String line, long now) {
        ServerHud.onChat(Text.literal(line));
        CryptidHud.onChat(Matcher.normalize(line), now);
    }

    private static Cryptids.State state(long now) {
        return CryptidHud.state(now);
    }

    @Test
    void moonRiseShowsTheSpotsPopsOnceAndSetsTheCycle() {
        long rise = 2_000_000_000L;
        chat("BLOOD MOON » A Blood Moon has risen! (+120 Bite Speed)", rise);
        assertEquals(rise, TackleboxSettings.get().moonRiseAt);

        List<Cryptids.Popup> pops = Cryptids.started(state(rise + 500));
        assertEquals(1, pops.size());
        assertEquals("blood moon", pops.get(0).title());
        assertTrue(Cryptids.started(state(rise + 1000)).isEmpty());

        List<Cryptids.Row> rows = Cryptids.rows(state(rise + 100_000), ms -> ms / 1000 + "s");
        assertEquals(new Cryptids.Row("blood moon", "500s", Cryptids.BLOOD, true), rows.get(0));
        assertEquals("anywhere", rows.get(3).label());

        chat("BLOOD MOON » The Blood Moon has set.", rise + Cryptids.MOON_LENGTH);
        rows = Cryptids.rows(state(rise + Cryptids.MOON_LENGTH), ms -> ms / 60_000 + "m");
        assertEquals(new Cryptids.Row("moon · toledo, great lakes", "150m", Cryptids.MOON, false), rows.get(1));
    }

    @Test
    void dryStreakCountsCatchesAndEndsOnTheDrop() {
        long rise = 3_000_000_000L;
        chat("FULL MOON » A Full Moon has risen! (+120 Bite Speed)", rise);
        chat("STORM » A Supercell has formed! (+150 Bite Speed)", rise);
        for (int i = 0; i < 5; i++) {
            CryptidHud.onCatch("toledobend", "normal", rise + i);
        }
        CryptidHud.onCatch("danube", "normal", rise + 10);
        var dry = TackleboxSettings.get().cryptidDry;
        assertEquals(5, dry.get("rougarou"));
        assertEquals(6, dry.get("lightning"));
        assertNull(dry.get("bessie"), "never fished at the great lakes");
        assertEquals("rougarou 1/300 · blood/claw (5)", Cryptids.rows(state(rise + 20), ms -> "").get(1).value());

        assertTrue(CryptidHud.onChat(Matcher.normalize("RARE CATCH! You pulled 1x Rougarou Claw"), rise + 30));
        assertEquals(0, dry.get("rougarou"));
        assertEquals(6, dry.get("lightning"));

        chat("STORM » The Supercell has dissipated!", rise + 40);
        CryptidHud.onCatch("toledobend", "normal", rise + 50);
        assertEquals(6, dry.get("lightning"), "no storm, no count");
        assertEquals(1, dry.get("rougarou"));
    }

    @Test
    void stormOngoingAtJoinStillCounts() {
        chat("STORM » A Thunderstorm is ongoing (+100 Bite Speed)", 1);
        assertEquals("thunderstorm", Cryptids.rows(state(2), ms -> "").get(0).label());
    }

    @Test
    void fabledPlaceAndTopBarClock() {
        long now = 5_000;
        chat("FABLED FISH EVENT", now);
        chat("Visit the Cypress Lake to", now + 10);
        ServerInfo.time = "2:53";
        ServerInfo.ampm = "AM";
        TackleboxSettings.get().cryptidOff.add("titanoboa");
        List<String> labels = Cryptids.rows(state(now), ms -> "").stream().map(Cryptids.Row::label).toList();
        assertEquals("fabled", labels.get(0));
        assertEquals("cypress", labels.get(1));
        assertTrue(labels.contains("bigfoot"));
        assertFalse(labels.contains("titanoboa"), "switched off in the settings");
    }
}
