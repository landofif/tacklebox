package dev.landofif.tacklebox;

import dev.landofif.tacklebox.hud.HudSpot;
import dev.landofif.tacklebox.hud.Timers;
import dev.landofif.tacklebox.hud.TimersHud;
import dev.landofif.tacklebox.trigger.Matcher;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Real chat lines from luna's logs through the normalizer and the timers hud, and the hud's settings through a save. */
class TimersIntegrationTest {
    @AfterEach
    void tearDown() {
        TackleboxSettings.get().votes.clear();
    }

    @Test
    void voteLinesFromTheLogCountAndAreSaved() {
        long now = 1_800_000_000_000L;
        // the server puts a pack glyph between "a" and the package's name
        assertTrue(TimersHud.onChat(Matcher.normalize("VOTE » You received a  Standard Bait Package"), now));
        assertTrue(TimersHud.onChat(Matcher.normalize("VOTE » You received a  Standard Bait Package"), now + 5000));
        assertFalse(TimersHud.onChat(Matcher.normalize("[93]  Wungle44 » VOTE » You received a thing"), now + 6000));
        assertEquals(2, Timers.votes(TackleboxSettings.get().votes, now + 7000));

        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(TackleboxSettings.get()), TackleboxSettings.class);
        assertEquals(2, back.votes.size());
    }

    @Test
    void restockLineOfTheLogIsARestock() {
        assertTrue(Timers.restockLine(Matcher.normalize("TACKLE SHOP » » Tackle items restocked! Check the Fish Pro Shop to see your selection.")));
    }

    @Test
    void settingsSavedBeforeTheHudExistedGetItOffAtItsHome() {
        TackleboxSettings old = new Gson().fromJson("{\"timersHud\":null,\"votes\":null}", TackleboxSettings.class);
        old.sanitize();
        assertFalse(old.timersHud.on);
        assertEquals(360, old.timersHud.pos.x);
        assertEquals(100, old.timersHud.scale);
        assertNotNull(old.votes);

        HudSpot moved = old.timersHud;
        moved.on = true;
        moved.scale = 150;
        moved.pos.moveTo(960, 540, 700, 400, 100, 50);
        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(old), TackleboxSettings.class);
        back.sanitize();
        assertTrue(back.timersHud.on);
        assertEquals(150, back.timersHud.scale);
        assertTrue(back.timersHud.pos.right);
        back.timersHud.reset();
        assertEquals(360, back.timersHud.pos.x);
        assertEquals(100, back.timersHud.scale);
    }
}
