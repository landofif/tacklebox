package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.display.RodIdle;
import dev.landofif.tacklebox.display.TitleOverlay;
import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.trigger.TriggerPresets;
import dev.landofif.tacklebox.trigger.TriggerStore;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Saved seconds, a cast and a catch, then the clock's line through the trigger engine with the "rod not cast" preset. */
class RodIdleIntegrationTest {
    @AfterEach
    void tearDown() {
        RodIdle.reset();
        TriggerStore.all().clear();
        TitleOverlay.clear();
    }

    @Test
    void idleRodShowsTheTitle() {
        TackleboxSettings s = new Gson().fromJson("{\"rodIdleSeconds\": 3}", TackleboxSettings.class);
        s.sanitize();
        int seconds = s.rodIdleSeconds;
        assertEquals(RodIdle.MIN_SECONDS, seconds);
        assertEquals(60, new Gson().fromJson("{}", TackleboxSettings.class).rodIdleSeconds);

        Trigger t = TriggerPresets.rodIdle();
        t.fishOnMcOnly = false; // no server in tests
        TriggerStore.all().add(t);

        RodIdle.step(true, true, 0, seconds);
        String line = null;
        for (long now = 0; now <= seconds * 1000L && line == null; now += 50) {
            line = RodIdle.step(true, false, now, seconds);
        }
        assertEquals("TACKLEBOX » Rod not cast for 10s", line);
        assertTrue(TriggerEngine.onGameMessage(Text.literal(line), false));
        assertEquals(1, TitleOverlay.shown().size());
        assertEquals("cast your rod", TitleOverlay.shown().get(0).title().getString());
        assertEquals("not cast for 10s", TitleOverlay.shown().get(0).subtitle().getString());
    }
}
