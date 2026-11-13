package dev.landofif.tacklebox.input;

import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.PlayerInput;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class FishKeyTest {
    @BeforeEach
    void clean() {
        FishKey.reset();
    }

    private static Text inFont(String font, String s) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("minecraft", font))));
    }

    @Test
    void aPressIsOneRightClick() {
        assertTrue(FishKey.step(1, true, true, false, false));
        assertFalse(FishKey.sneaking());
    }

    @Test
    void holdingTheKeyNeverClicksAgain() {
        assertTrue(FishKey.step(1, true, true, false, false));
        for (int i = 0; i < 40; i++) { // a held keyboard key repeats its press event
            assertFalse(FishKey.step(2, true, true, false, false));
        }
        assertFalse(FishKey.step(0, false, true, false, false));
        assertTrue(FishKey.step(1, true, true, false, false)); // let go and pressed again
    }

    @Test
    void aTapInsideOneTickStillClicks() {
        assertTrue(FishKey.step(1, false, true, false, false));
        assertTrue(FishKey.step(1, false, true, false, false));
    }

    @Test
    void nothingWithoutAPress() {
        for (int i = 0; i < 100; i++) {
            assertFalse(FishKey.step(0, false, true, false, false));
            assertFalse(FishKey.sneaking());
        }
    }

    @Test
    void notReadyDoesNothing() { // no rod, a screen open, another server
        assertFalse(FishKey.step(1, true, false, false, false));
        assertFalse(FishKey.sneaking());
        assertFalse(FishKey.step(1, true, false, true, false));
        assertFalse(FishKey.sneaking());
    }

    @Test
    void inTheMinigameTheKeyIsSneakWhileHeld() {
        assertFalse(FishKey.step(1, true, true, true, false));
        assertTrue(FishKey.sneaking());
        assertFalse(FishKey.step(1, true, true, true, false));
        assertTrue(FishKey.sneaking());
        assertFalse(FishKey.step(0, false, true, true, false));
        assertFalse(FishKey.sneaking());
    }

    @Test
    void heldThroughTheEndOfTheMinigameDoesNotCast() {
        FishKey.step(1, true, true, true, false);
        assertFalse(FishKey.step(1, true, true, false, false)); // the minigame ended with the key down
        assertFalse(FishKey.sneaking());
        assertFalse(FishKey.step(0, false, true, false, false));
        assertTrue(FishKey.step(1, true, true, false, false)); // the next press casts
    }

    @Test
    void heldFromTheReelClickIntoTheMinigameSneaks() {
        assertTrue(FishKey.step(1, true, true, false, false));
        assertFalse(FishKey.step(0, true, true, true, false));
        assertTrue(FishKey.sneaking());
    }

    @Test
    void framesAreRecognisedByFontOrGlyph() {
        assertTrue(FishKey.isFrame(inFont("fishing_1", "abc")));
        assertTrue(FishKey.isFrame(Text.literal("").append(Text.literal(" ")).append(inFont("fishing_3", "x"))));
        assertTrue(FishKey.isFrame(Text.literal("猪")));
        assertTrue(FishKey.isFrame(Text.literal("")));
        assertTrue(FishKey.isFishingFont("hud/fishing_2"));
        assertFalse(FishKey.isFrame(Text.literal("+66xp")));
        assertFalse(FishKey.isFrame(inFont("default", "+66xp")));
        assertFalse(FishKey.isFrame(inFont("fishing_1", "")));
        assertFalse(FishKey.isFrame(Text.empty()));
        assertFalse(FishKey.isFrame(null));
    }

    @Test
    void anotherActionBarLineEndsTheMinigameAtOnce() {
        FishKey.onActionBar(true, 1000);
        assertTrue(FishKey.minigame(1000));
        FishKey.onActionBar(false, 1050);
        assertFalse(FishKey.minigame(1050));
    }

    @Test
    void theCatchTitleEndsItAndALateFrameDoesNotBringItBack() {
        FishKey.onActionBar(true, 1000);
        FishKey.onTitle("", 1050);
        assertFalse(FishKey.minigame(1050));
        FishKey.onActionBar(true, 1060);
        assertFalse(FishKey.minigame(1060));
        FishKey.onActionBar(true, 1050 + FishKey.QUIET); // the next minigame
        assertTrue(FishKey.minigame(1050 + FishKey.QUIET));
    }

    @Test
    void theBobberGoneEndsItAtOnce() {
        FishKey.onActionBar(true, 1000);
        assertTrue(FishKey.minigame(1010, true));
        assertFalse(FishKey.minigame(1020, false)); // caught: the bobber is gone, frames or not
        assertFalse(FishKey.minigame(1030, true));
    }

    @Test
    void aFrameWithoutTheBobberIsNotTheMinigame() {
        FishKey.onActionBar(true, 1000);
        assertFalse(FishKey.minigame(1000, false));
    }

    @Test
    void otherTitlesDoNotEndIt() {
        FishKey.onActionBar(true, 1000);
        FishKey.onTitle("BITE!", 1010);
        assertTrue(FishKey.minigame(1010));
    }

    @Test
    void noFramesEndItAfterTheFirstGap() {
        FishKey.onActionBar(true, 1000);
        assertTrue(FishKey.minigame(1000 + FishKey.FIRST_GAP));
        assertFalse(FishKey.minigame(1000 + FishKey.FIRST_GAP + 1));
    }

    @Test
    void theGapFollowsTheFrameRhythm() {
        for (long t = 1000; t <= 2000; t += 50) { // a frame per tick
            FishKey.onActionBar(true, t);
        }
        assertEquals(150, FishKey.gap());
        assertTrue(FishKey.minigame(2150));
        assertFalse(FishKey.minigame(2151));

        FishKey.reset();
        for (long t = 1000; t <= 3000; t += 250) { // a slow server
            FishKey.onActionBar(true, t);
        }
        assertEquals(550, FishKey.gap());
        assertTrue(FishKey.minigame(3300));
    }

    @Test
    void theGapStaysInItsBounds() {
        FishKey.onActionBar(true, 0);
        FishKey.onActionBar(true, 400);
        FishKey.onActionBar(true, 800);
        FishKey.onActionBar(true, 800 + 450);
        assertEquals(950, FishKey.gap());
        FishKey.onActionBar(true, 800 + 450 + 900);
        assertEquals(FishKey.MAX_GAP, FishKey.gap());
    }

    @Test
    void sneakIsAddedToTheInputAndNothingElseChanges() {
        PlayerInput in = new PlayerInput(true, false, true, false, true, false, true);
        PlayerInput out = FishKey.withSneak(in);
        assertEquals(new PlayerInput(true, false, true, false, true, true, true), out);
        assertSame(out, FishKey.withSneak(out));
    }

    @Test
    void onTheUseKeyTheModNeverClicksItself() {
        assertFalse(FishKey.step(1, true, true, false, true)); // the game's use key makes this click
        assertFalse(FishKey.blockUse());
    }

    @Test
    void onTheUseKeyTheGamesClickIsDroppedInTheMinigame() {
        FishKey.step(1, true, true, true, true);
        assertTrue(FishKey.blockUse());
        assertTrue(FishKey.sneaking());
        FishKey.step(0, false, true, true, true); // let go: still the minigame, a click would still be dropped
        assertTrue(FishKey.blockUse());
        assertFalse(FishKey.sneaking());
        FishKey.step(0, false, true, false, true);
        assertFalse(FishKey.blockUse());
    }

    @Test
    void onTheUseKeyHoldingThroughTheEndStaysBlockedUntilLetGo() {
        FishKey.step(1, true, true, true, true);
        FishKey.step(0, true, true, false, true); // over, button still down: the game's hold repeat must not cast
        assertTrue(FishKey.blockUse());
        assertFalse(FishKey.sneaking());
        FishKey.step(0, false, true, false, true);
        assertFalse(FishKey.blockUse());
    }

    @Test
    void theUseKeyIsNeverBlockedForOtherKeysOrWithoutARod() {
        FishKey.step(1, true, true, true, false);
        assertFalse(FishKey.blockUse());
        FishKey.step(1, true, false, true, true);
        assertFalse(FishKey.blockUse());
    }
}
