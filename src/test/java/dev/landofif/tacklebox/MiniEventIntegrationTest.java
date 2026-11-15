package dev.landofif.tacklebox;

import dev.landofif.tacklebox.hud.MiniEvent;
import dev.landofif.tacklebox.hud.MiniEventHud;
import dev.landofif.tacklebox.hud.TextHud;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A /minievent answer from luna's log through the hud's chat hook with the setting on and off, catches in between, and the hud's settings through a save. */
class MiniEventIntegrationTest {
    private static final List<String> ANSWER = List.of(
            "MED SEA MINI EVENT » Ends in 2d 4h 54s",
            "Daily missions reroll in 21h 54s",
            " - 🎣 Mediterranean Sea  \n    ┗ (75/100) ∞ x20",
            " - 🎣 Lynx Pet  \n    ┗ (0/1) ∞ x10",
            " - 🎣 Mediterranean Sea ɢɪɢᴀɴᴛɪᴄ  ✔\n    ┗ (2/2) One Timer",
            " - 🎣 Mediterranean Sea Rays  \n    ┗ (1/18) Daily",
            " - 🎣 Seabreams  \n    ┗ (2/35) Daily");
    private static final long T = 1_800_000_000_000L;

    @BeforeEach
    @AfterEach
    void clean() {
        MiniEvent.reset();
        TackleboxSettings.get().miniEventHud.on = false;
    }

    @Test
    void offReadsAndHidesNothing() {
        for (String line : ANSWER) {
            assertFalse(MiniEventHud.onChat(line, T));
        }
        assertFalse(MiniEvent.on(T));
        MiniEventHud.onCatch(new MiniEvent.Fish("mediterranean", "normal", "adult", "common", "rays", "Blonde Skate"), T);
        assertTrue(MiniEvent.rows(T).isEmpty());
    }

    @Test
    void theQuietAnswerIsHiddenReadAndCountedOnUntilTheNextOne() {
        TackleboxSettings.get().miniEventHud.on = true;
        MiniEvent.due(T, false);
        assertTrue(MiniEvent.due(T + 40_000, false)); // the hud's tick sends /minievent here
        for (String line : ANSWER) {
            assertTrue(MiniEventHud.onChat(line, T + 40_200), line);
        }
        List<TextHud.Row> rows = MiniEvent.rows(T + 40_200);
        assertEquals("med sea mini event", MiniEvent.title());
        assertEquals(new TextHud.Row("ends in", "2d 4h"), rows.get(0));
        assertEquals(new TextHud.Row("daily reroll", "21h 00m"), rows.get(1));
        assertEquals(List.of("mediterranean sea", "lynx pet", "mediterranean sea rays", "seabreams"),
                rows.subList(2, rows.size()).stream().map(TextHud.Row::label).toList());

        // a ray at the mediterranean sea: three missions go up at once, and the server is asked again a minute after the last send
        MiniEventHud.onCatch(new MiniEvent.Fish("mediterranean", "normal", "adult", "common", "rays", "Blonde Skate"), T + 50_000);
        rows = MiniEvent.rows(T + 50_000);
        assertEquals("76/100 · ∞ x20", rows.get(2).value());
        assertEquals("0/1 · ∞ x10", rows.get(3).value());
        assertEquals("2/18 · daily", rows.get(4).value());
        assertEquals("2/35 · daily", rows.get(5).value());
        assertFalse(MiniEvent.due(T + 55_000, false));
        assertTrue(MiniEvent.due(T + 100_000, false));

        // the server's own numbers replace the local count
        for (String line : ANSWER) {
            assertTrue(MiniEventHud.onChat(line, T + 100_300));
        }
        assertEquals("75/100 · ∞ x20", MiniEvent.rows(T + 100_300).get(2).value());
    }

    @Test
    void theHudSurvivesASaveAndOldSettingsGetItOffAtItsHome() {
        TackleboxSettings old = new Gson().fromJson("{\"miniEventHud\":null}", TackleboxSettings.class);
        old.sanitize();
        assertFalse(old.miniEventHud.on);
        assertEquals(360, old.miniEventHud.pos.x);
        old.miniEventHud.on = true;
        old.miniEventHud.scale = 120;
        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(old), TackleboxSettings.class);
        back.sanitize();
        assertTrue(back.miniEventHud.on);
        assertEquals(120, back.miniEventHud.scale);
    }
}
