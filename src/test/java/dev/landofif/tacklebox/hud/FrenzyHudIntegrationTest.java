package dev.landofif.tacklebox.hud;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The chat lines of luna's logs (2026-10-10) go through the boosts hud into the server hud's frenzy line text. */
class FrenzyHudIntegrationTest {
    private static long at(int h, int m) {
        return LocalDateTime.of(2026, 10, 18, h, m).atZone(FrenzySchedule.ZONE).toInstant().toEpochMilli();
    }

    private static String line(long now, long left, String place) {
        return FrenzySchedule.describe(now, left, place, ms -> ms / 60_000 + "m");
    }

    @Test
    void scheduleThenAlertThenEnd() {
        BoostsHud.onChat(Text.literal("FRENZY » The Fish Frenzy has ended at Kenai River"));
        assertEquals("may spawn, 90m left", line(at(13, 30), BoostsHud.frenzyLeft(at(13, 30)), BoostsHud.frenzyPlace()));
        assertEquals("next window in 45m", line(at(15, 15), 0, ""));

        BoostsHud.onChat(Text.literal("FISH FRENZY » A Fish Frenzy is active at Kenai River (for 16m 56s)"));
        long now = System.currentTimeMillis();
        String active = line(now, BoostsHud.frenzyLeft(now), BoostsHud.frenzyPlace());
        assertEquals("Kenai River 16m", active);

        BoostsHud.onChat(Text.literal("FRENZY » The Fish Frenzy has ended at Kenai River"));
        assertEquals(0, BoostsHud.frenzyLeft(System.currentTimeMillis()));
    }
}
