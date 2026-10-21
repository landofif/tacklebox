package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.display.BiteCountdown;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The timer style defaults to counting up, survives a save and load, and a full approach ends in a countdown. */
class BiteCountdownIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsCountUp() {
        assertEquals(TackleboxSettings.TimerStyle.UP, load("{\"timerMode\": \"HUD\"}").timerStyle);
        assertEquals(TackleboxSettings.TimerStyle.UP, load("{\"timerStyle\": null}").timerStyle);
    }

    @Test
    void savedCountdownRoundTrips() {
        TackleboxSettings s = load("{\"timerStyle\": \"COUNTDOWN\"}");
        assertEquals(TackleboxSettings.TimerStyle.COUNTDOWN, load(new Gson().toJson(s)).timerStyle);
    }

    @Test
    void approachThenBite() {
        BiteCountdown.onBite(0);
        long t0 = 50_000;
        for (int i = 0; i < 20; i++) {
            BiteCountdown.onApproach(6 - 0.1 * i, t0 + 50L * i);
        }
        assertTrue(BiteCountdown.active(t0 + 1000));
        assertTrue(BiteCountdown.seconds(t0 + 1000) < 2.5f);
        BiteCountdown.onBite(t0 + 1100);
        assertFalse(BiteCountdown.active(t0 + 1110));
    }
}
