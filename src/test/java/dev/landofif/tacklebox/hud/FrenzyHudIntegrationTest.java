package dev.landofif.tacklebox.hud;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The chat lines of luna's logs (2026-10-10) go through the boosts hud into the server hud's frenzy line text. */
class FrenzyHudIntegrationTest {
    private static long at(int h, int m) {
        return LocalDateTime.of(2026, 10, 18, h, m).atZone(FrenzySchedule.ZONE).toInstant().toEpochMilli();
    }

    private static String line(long now, long left, String place) {
        return FrenzySchedule.describe(now, left, place, BoostsHud.frenzySeen(), ms -> ms / 60_000 + "m");
    }

    @Test
    void windowThatHadItsFrenzyCountsToTheNextOne() {
        assertEquals("in 90m", FrenzySchedule.describe(at(14, 30), 0, "", at(13, 20), ms -> ms / 60_000 + "m"));
        assertEquals("spawn? 90m", FrenzySchedule.describe(at(13, 30), 0, "", at(12, 20), ms -> ms / 60_000 + "m"));
    }

    @Test
    void scheduleThenAlertThenEnd() {
        BoostsHud.onChat(Text.literal("FRENZY » The Fish Frenzy has ended at Kenai River"));
        assertEquals("spawn? 90m", line(at(13, 30), BoostsHud.frenzyLeft(at(13, 30)), BoostsHud.frenzyPlace()));
        assertEquals("in 45m", line(at(15, 15), 0, ""));

        BoostsHud.onChat(BoostsHudColorsTest.line("FISH FRENZY", 0xFFAA00, " » A ", 0xFFFFFF, "Fish Frenzy", 0xFFAA00, " is active at ", 0xFFFFFF,
                "Kenai River", 0xFFFF55, " (for 16m 56s)", 0x555555));
        long now = System.currentTimeMillis();
        String active = line(now, BoostsHud.frenzyLeft(now), BoostsHud.frenzyPlace());
        assertEquals("Kenai 16m", active); // the place by its short name
        // the active hud row in the colours of the line (luna 2026-10-28)
        BoostsHud.Row row = BoostsHud.rows().stream().filter(r -> r.label().get(0).text().equals("fish frenzy")).findFirst().orElseThrow();
        assertEquals(List.of(new BoostsHud.Part("fish frenzy", 0xFFAA00), new BoostsHud.Part("Kenai", 0xFFFF55)), row.label());

        BoostsHud.onChat(Text.literal("FRENZY » The Fish Frenzy has ended at Kenai River"));
        assertEquals(0, BoostsHud.frenzyLeft(System.currentTimeMillis()));

        // the short name keeps the colour the line gave the full name
        BoostsHud.onChat(BoostsHudColorsTest.line("FISH FRENZY", 0xFFAA00, " » A ", 0xFFFFFF, "Fish Frenzy", 0xFFAA00, " is active at ", 0xFFFFFF,
                "Mediterranean Sea", 0xFFFF55, " (for 16m 56s)", 0x555555));
        assertEquals("Med", BoostsHud.frenzyPlace());
        row = BoostsHud.rows().stream().filter(r -> r.label().get(0).text().equals("fish frenzy")).findFirst().orElseThrow();
        assertEquals(List.of(new BoostsHud.Part("fish frenzy", 0xFFAA00), new BoostsHud.Part("Med", 0xFFFF55)), row.label());
        BoostsHud.onChat(Text.literal("FRENZY » The Fish Frenzy has ended at Mediterranean Sea"));
        assertEquals(0, BoostsHud.frenzyLeft(System.currentTimeMillis()));
    }
}
