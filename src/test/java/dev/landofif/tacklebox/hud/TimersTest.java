package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TimersTest {
    private static long at(String utc) {
        return Instant.parse(utc).toEpochMilli();
    }

    @AfterEach
    void tearDown() {
        Timers.reset();
    }

    @Test
    void tackleShopFollowsTheGridOfLunasLogs() {
        // restock lines in the logs (local time): 2026-10-16 05:00 CEST, 2026-10-25 04:00 CET (the night the EU clock changed), 2026-11-14 20:00 CET
        for (String restock : new String[]{"2026-10-16T03:00:00Z", "2026-10-25T03:00:00Z", "2026-11-14T19:00:00Z", "2026-11-02T23:00:00Z"}) {
            assertEquals(1000, Timers.tackleIn(at(restock) - 1000), restock);
            assertEquals(Timers.TACKLE_PERIOD, Timers.tackleIn(at(restock)), restock);
        }
        assertEquals(90 * 60_000, Timers.tackleIn(at("2026-11-15T13:30:00Z")));
    }

    @Test
    void cosmeticStoreResetsAtMidnightUtc() {
        assertEquals(60_000, Timers.cosmeticIn(at("2026-11-15T23:59:00Z")));
        assertEquals(Timers.DAY, Timers.cosmeticIn(at("2026-11-16T00:00:00Z")));
    }

    @Test
    void cosmeticTitleOncePerResetAndNotOnJoin() {
        assertNull(Timers.cosmeticReset(at("2026-11-15T23:59:59Z")), "the first look only remembers");
        assertNull(Timers.cosmeticReset(at("2026-11-15T23:59:59.500Z")));
        assertEquals(new Timers.Popup("cosmetic store", "reset"), Timers.cosmeticReset(at("2026-11-16T00:00:00.200Z")));
        assertNull(Timers.cosmeticReset(at("2026-11-16T00:00:00.700Z")));
        Timers.reset();
        assertNull(Timers.cosmeticReset(at("2026-11-17T00:00:05Z")), "joined after a reset: no title");
    }

    @Test
    void chatLines() {
        assertTrue(Timers.restockLine("TACKLE SHOP > > Tackle items restocked! Check the Fish Pro Shop to see your selection."));
        assertFalse(Timers.restockLine("TACKLE SHOP > You have purchased Crankbait for $9K!"));
        assertFalse(Timers.restockLine("[142] someone > TACKLE SHOP > > Tackle items restocked!"));
        assertTrue(Timers.voteLine("VOTE > You received a Standard Bait Package"));
        assertFalse(Timers.voteLine("[93] someone > VOTE > You received a thing"));
    }

    @Test
    void votesResetAtMidnightUtc() {
        List<Long> votes = new ArrayList<>();
        Timers.addVote(votes, at("2026-11-17T15:42:55Z"));
        Timers.addVote(votes, at("2026-11-17T15:45:25Z"));
        assertEquals(2, Timers.votes(votes, at("2026-11-17T23:59:59Z")));
        assertEquals(0, Timers.votes(votes, at("2026-11-18T00:00:00Z")), "a new day starts at 0, not 24 hours after each vote");
        Timers.addVote(votes, at("2026-11-18T00:01:00Z"));
        assertEquals(1, votes.size(), "yesterday's are dropped when one is added");
        for (int i = 0; i < 10; i++) {
            Timers.addVote(votes, at("2026-11-18T08:00:00Z") + i);
        }
        assertEquals(Timers.VOTE_SITES, Timers.votes(votes, at("2026-11-18T09:00:00Z")));
    }

    @Test
    void rows() {
        long now = at("2026-11-15T22:00:00Z");
        List<TextHud.Row> rows = Timers.rows(now, List.of(now - 1000), true, ms -> ms / 60_000 + "m");
        assertEquals(List.of(new TextHud.Row("tackle shop", "60m"), new TextHud.Row("cosmetic store", "120m"), new TextHud.Row("votes", "1/7")), rows);
        assertEquals(2, Timers.rows(now, List.of(), false, ms -> "").size());
    }
}
