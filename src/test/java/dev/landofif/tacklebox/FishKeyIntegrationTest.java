package dev.landofif.tacklebox;

import dev.landofif.tacklebox.input.FishKey;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Two rounds of fishing on the fish key alone, tick by tick, with the action bar and titles as the server sends them:
 * cast, reel in on the bite, steer the minigame by holding and letting go, cast again right after the catch. The mod
 * clicks exactly once per press and never on its own.
 */
class FishKeyIntegrationTest {
    private long now = 100_000;
    private int clicks;
    private int sneakTicks;

    @BeforeEach
    @AfterEach
    void clean() {
        FishKey.reset();
    }

    private static Text frame(int progress) {
        return Text.literal("猪" + (char) (0xEC00 + progress)).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", "fishing_1"))));
    }

    /** One client tick of 50 ms: the key's press events and state, then what the game would read. */
    private void tick(int presses, boolean down) {
        if (FishKey.step(presses, down, true, FishKey.minigame(now), false)) {
            clicks++;
        }
        if (FishKey.sneaking()) {
            sneakTicks++;
        }
        now += 50;
    }

    private void idle(int ticks) {
        for (int i = 0; i < ticks; i++) {
            tick(0, false);
        }
    }

    private void tap() {
        tick(1, true);
        tick(0, false);
    }

    private void round(boolean caught) {
        int before = clicks;
        tap(); // cast
        assertEquals(before + 1, clicks);
        idle(60); // waiting for the bite: nothing happens without a press
        FishKey.onTitle("BITE!", now);
        idle(10); // the bite alone does not reel in
        assertEquals(before + 1, clicks);

        tick(1, true); // reel in, and keep the key down
        assertEquals(before + 2, clicks);
        int sneakBefore = sneakTicks;
        for (int i = 0; i < 20; i++) { // minigame: a frame per tick, key held
            FishKey.onActionBar(frame(i), now);
            tick(i % 3, true); // key repeat while held
        }
        assertEquals(20, sneakTicks - sneakBefore);
        for (int i = 20; i < 40; i++) { // let go: the bar falls
            FishKey.onActionBar(frame(i), now);
            tick(0, false);
        }
        assertEquals(20, sneakTicks - sneakBefore);
        for (int i = 40; i < 60; i++) { // tapping it
            FishKey.onActionBar(frame(i), now);
            tick(i % 2 == 0 ? 1 : 0, i % 2 == 0);
        }
        assertEquals(30, sneakTicks - sneakBefore);
        assertEquals(before + 2, clicks); // no right click during the minigame

        FishKey.onActionBar(frame(60), now);
        tick(1, true); // still holding when it ends
        if (caught) {
            FishKey.onTitle("", now);
            FishKey.onActionBar(Text.literal("+66xp"), now);
        } else {
            FishKey.onActionBar(Text.empty(), now);
        }
        assertFalse(FishKey.minigame(now));
        tick(1, true); // the key is still down: no cast, no sneak
        assertFalse(FishKey.sneaking());
        assertEquals(before + 2, clicks);
        tick(0, false);
    }

    @Test
    void twoRoundsOnOneKey() {
        round(true);
        tick(1, true); // the cast for the next round works on the very next press
        assertEquals(3, clicks);
        tick(0, false);
        tick(1, true); // reel it straight back in
        tick(0, false);
        assertEquals(4, clicks);

        round(false);
        assertEquals(6, clicks);
        idle(200);
        assertEquals(6, clicks);
        assertFalse(FishKey.sneaking());
    }

    @Test
    void aMinigameThatJustStopsGivesTheKeyBackAfterTheGap() {
        tap();
        tick(1, true);
        tick(0, false);
        for (int i = 0; i < 20; i++) {
            FishKey.onActionBar(frame(i), now);
            tick(0, false);
        }
        assertTrue(FishKey.minigame(now));
        idle(3); // 150 ms without a frame at a frame per tick
        tick(0, false);
        assertFalse(FishKey.minigame(now));
        tap();
        assertEquals(3, clicks);
    }

    /** The fish is caught while frames still come: the bobber is gone, so the very next press casts instead of sneaking. */
    @Test
    void theBobberGoneGivesTheKeyBackWhileFramesStillCome() {
        tap(); // cast
        tap(); // reel in
        for (int i = 0; i < 10; i++) {
            FishKey.onActionBar(frame(i), now);
            tick(0, false);
        }
        FishKey.onActionBar(frame(10), now); // a late frame in the same tick the bobber goes
        assertTrue(FishKey.step(1, true, true, FishKey.minigame(now, false), false));
        assertFalse(FishKey.sneaking());
    }

    /**
     * The fish key on right mouse: the game makes the right clicks itself (one per press here, as its use key does), the
     * mod never adds one, and in the minigame the game's click is dropped while the button is sneak.
     */
    @Test
    void sharedWithTheUseKey() {
        int gameClicks = 0;
        boolean[] downs = new boolean[60];
        downs[0] = true; // cast
        downs[10] = true; // reel in
        for (int i = 20; i < 40; i++) {
            downs[i] = true; // held in the minigame, and two ticks past its end
        }
        downs[45] = true; // cast again
        boolean was = false;
        for (int i = 0; i < downs.length; i++) {
            if (i >= 15 && i < 38) {
                FishKey.onActionBar(frame(i), now);
            } else if (i == 38) {
                FishKey.onTitle("\uE0A7", now);
            }
            boolean press = downs[i] && !was;
            was = downs[i];
            assertFalse(FishKey.step(press ? 1 : 0, downs[i], true, FishKey.minigame(now), true)); // never the mods click
            if ((press || downs[i]) && !FishKey.blockUse()) { // the games use key: a press, and its repeat while held
                gameClicks++;
            }
            if (FishKey.sneaking()) {
                sneakTicks++;
            }
            now += 50;
        }
        assertEquals(3, gameClicks); // cast, reel in, cast again: nothing in the minigame or from holding through its end
        assertEquals(18, sneakTicks);
        assertFalse(FishKey.blockUse());
    }
}
