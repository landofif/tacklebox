package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.BigCatch.Kind;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BigCatchTest {
    private static final long T = 2_000_000;

    @BeforeEach
    void on() {
        TackleboxSettings s = TackleboxSettings.get();
        s.bigCatch = true;
        s.bigFabled = s.bigTrophy = s.bigMythical = s.bigGigantic = true;
        s.bigGlow = s.bigShine = s.bigSound = true;
        s.bigGlowOpacity = 35;
        BigCatch.clear();
    }

    @AfterEach
    void off() {
        TackleboxSettings s = TackleboxSettings.get();
        s.bigCatch = false;
        s.bigFabled = s.bigTrophy = s.bigMythical = s.bigGigantic = true;
        s.bigGlow = s.bigShine = s.bigSound = true;
        s.bigGlowOpacity = 35;
        BigCatch.clear();
        SoundQueue.stopAll();
    }

    private static Kind kind(String rarity, String variant, String size) {
        return BigCatch.kind(rarity, variant, size, TackleboxSettings.get());
    }

    @Test
    void whichFishCountAndInWhatOrder() {
        assertEquals(Kind.FABLED, kind("epic", "fabled", "adult"));
        assertEquals(Kind.TROPHY, kind("rare", "trophy", "baby"));
        assertEquals(Kind.MYTHICAL, kind("mythical", "normal", "large"));
        assertEquals(Kind.GIGANTIC, kind("common", "normal", "gigantic"));
        assertEquals(Kind.FABLED, kind("mythical", "fabled", "gigantic"), "fabled before the rest");
        assertEquals(Kind.TROPHY, kind("mythical", "trophy", "gigantic"), "trophy before mythical and gigantic");
        assertEquals(Kind.MYTHICAL, kind("mythical", "normal", "gigantic"), "mythical before gigantic");
    }

    @Test
    void aNormalFishGetsNothing() {
        assertNull(kind("legendary", "normal", "large"));
        assertNull(kind("special", "albino", "adult"), "albino and melanistic are not big catches");
        assertNull(kind("epic", "melanistic", "juvenile"));
        assertNull(BigCatch.onCatch("legendary", "normal", "large", T));
        assertNull(BigCatch.current());
        assertEquals(0, BigCatch.glowAlpha(T + 200));
    }

    @Test
    void aKindSwitchedOffHandsOverToTheNextOne() {
        TackleboxSettings s = TackleboxSettings.get();
        s.bigFabled = false;
        assertEquals(Kind.MYTHICAL, kind("mythical", "fabled", "adult"));
        assertNull(kind("epic", "fabled", "adult"));
        s.bigMythical = false;
        assertEquals(Kind.GIGANTIC, kind("mythical", "fabled", "gigantic"));
        s.bigCatch = false;
        assertNull(BigCatch.onCatch("mythical", "fabled", "gigantic", T), "the group's switch is off");
    }

    @Test
    void theGlowFadesInHoldsAndFadesOut() {
        assertEquals(0, BigCatch.glow(-1, 0.35f));
        assertEquals(0, BigCatch.glow(0, 0.35f), 1e-6, "starts clear: no flash");
        assertEquals(0.175f, BigCatch.glow(75, 0.35f), 1e-6, "halfway in");
        assertEquals(0.35f, BigCatch.glow(150, 0.35f), 1e-6);
        assertEquals(0.35f, BigCatch.glow(299, 0.35f), 1e-6, "holds");
        assertEquals(0.175f, BigCatch.glow(450, 0.35f), 1e-6, "halfway out");
        assertEquals(0, BigCatch.glow(600, 0.35f));
        float last = 0;
        for (long t = 0; t <= 150; t += 5) {
            assertTrue(BigCatch.glow(t, 0.35f) >= last, "only rises while fading in");
            last = BigCatch.glow(t, 0.35f);
        }
        for (long t = 300; t < 600; t += 5) {
            assertTrue(BigCatch.glow(t, 0.35f) <= last, "only falls while fading out");
            last = BigCatch.glow(t, 0.35f);
        }
        for (long t = 0; t < 600; t += 25) {
            assertTrue(Math.abs(BigCatch.glow(t + 25, 0.35f) - BigCatch.glow(t, 0.35f)) < 0.11f, "no jump at " + t);
        }
    }

    @Test
    void theGlowFollowsTheMomentAndItsSwitches() {
        TackleboxSettings s = TackleboxSettings.get();
        assertEquals(Kind.MYTHICAL, BigCatch.onCatch("mythical", "normal", "adult", T));
        assertEquals(Kind.MYTHICAL, BigCatch.current());
        assertEquals(0.35f, BigCatch.glowAlpha(T + 200), 1e-6);
        assertEquals(0, BigCatch.glowAlpha(T + 600));
        s.bigGlowOpacity = 80;
        assertEquals(0.8f, BigCatch.glowAlpha(T + 200), 1e-6);
        s.bigGlow = false;
        assertEquals(0, BigCatch.glowAlpha(T + 200));
    }

    @Test
    void theShineCrossesTheNameOnce() {
        assertEquals(0, BigCatch.shine(0.5f, -1));
        assertEquals(0, BigCatch.shine(0f, 0), 1e-6, "starts off the left end");
        assertEquals(0.9f, BigCatch.shine(0.5f, 350), 1e-6, "the middle of the name at half time");
        assertEquals(0, BigCatch.shine(1f, 700), 1e-6, "ends off the right end");
        assertEquals(0, BigCatch.shine(0.5f, 701));
        assertTrue(BigCatch.shine(0.1f, 150) > BigCatch.shine(0.9f, 150), "left first");
        assertTrue(BigCatch.shine(0.9f, 550) > BigCatch.shine(0.1f, 550), "right last");
        assertEquals(0, BigCatch.shine(0.9f, 150), "a narrow band, not the whole line");
        long peakAt = -1;
        float best = -1;
        for (long t = 0; t <= 700; t += 5) {
            if (BigCatch.shine(0.75f, t) > best) {
                best = BigCatch.shine(0.75f, t);
                peakAt = t;
            }
        }
        assertEquals(475, peakAt, 5, "a letter at three quarters is lit brightest at about 0.48 s");
    }

    @Test
    void theShineBelongsToItsOwnCatchTitle() {
        TackleboxSettings s = TackleboxSettings.get();
        assertEquals(-1, BigCatch.shineStart(T - 50), "no big catch yet");
        BigCatch.onCatch("rare", "trophy", "adult", T);
        assertEquals(T, BigCatch.shineStart(T - 50));
        assertEquals(-1, BigCatch.shineStart(T + 10), "a later title is another catch");
        assertEquals(0xD5BF3B, Kind.TROPHY.rgb);
        int c = BigCatch.shineColor();
        assertTrue((c >> 16 & 0xFF) > 0xD5 && (c >> 8 & 0xFF) > 0xBF && (c & 0xFF) > 0x3B, "lighter than the trophy gold");
        s.bigShine = false;
        assertEquals(-1, BigCatch.shineStart(T - 50));
    }

    @Test
    void eachKindHasItsSound() {
        assertEquals(List.of("minecraft:block.conduit.activate"), ids(Kind.FABLED));
        assertEquals(3, BigCatch.sound(Kind.TROPHY).size(), "a bell chord");
        assertTrue(ids(Kind.TROPHY).stream().allMatch("minecraft:block.note_block.bell"::equals));
        assertEquals(List.of(1.0f, 1.26f, 1.5f), BigCatch.sound(Kind.TROPHY).stream().map(BigCatch.Note::pitch).toList());
        assertEquals(List.of("minecraft:entity.player.levelup"), ids(Kind.MYTHICAL));
        assertEquals(List.of("minecraft:block.amethyst_block.chime"), ids(Kind.GIGANTIC));
        for (Kind k : Kind.values()) {
            for (BigCatch.Note n : BigCatch.sound(k)) {
                assertTrue(n.volume() > 0 && n.volume() <= 1 && n.pitch() >= 0.5f && n.pitch() <= 2f, k + " " + n);
                assertEquals(n.id(), String.valueOf(SoundQueue.parseId(n.id())), "a valid id");
            }
        }
    }

    private static List<String> ids(Kind k) {
        return BigCatch.sound(k).stream().map(BigCatch.Note::id).toList();
    }

    @Test
    void aFullInventoryGoesByTheSubtitleBadges() {
        String mythical = " Atlantic Cod";
        assertEquals("mythical", CatchTitle.rarityOf(mythical));
        assertEquals("normal", CatchTitle.variantOf(mythical));
        assertEquals(Kind.MYTHICAL, kind(CatchTitle.rarityOf(mythical), CatchTitle.variantOf(mythical), ""));
        String fabled = "  Kenai Salmon";
        assertEquals("epic", CatchTitle.rarityOf(fabled));
        assertEquals(Kind.FABLED, kind(CatchTitle.rarityOf(fabled), CatchTitle.variantOf(fabled), ""));
        assertEquals(Kind.TROPHY, kind(CatchTitle.rarityOf(" Pike"), CatchTitle.variantOf(" Pike"), ""));
        assertNull(kind(CatchTitle.rarityOf(" Pike"), CatchTitle.variantOf(" Pike"), ""), "a legendary is not big");
        assertEquals("special", CatchTitle.rarityOf(" Ghost Fish"));
        assertEquals("", CatchTitle.rarityOf("Atlantic Cod"));
        assertFalse(CatchTitle.variantOf(" Albino Cod").equals("fabled"));
    }
}
