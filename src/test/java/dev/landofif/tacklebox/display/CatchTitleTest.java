package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.CatchTitle.Info;
import dev.landofif.tacklebox.display.CatchTitle.Seg;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchTitleTest {
    static final Text FISH = Text.literal("");
    static final Text SUBTITLE = Text.literal(" Atlantic Cod");
    static final Info COD = new Info("Atlantic Cod", "legendary", "trophy", "adult", 12.44f, 31.66f);

    private final TackleboxSettings s = TackleboxSettings.get();

    static NbtCompound cod() {
        NbtCompound n = new NbtCompound();
        n.putString("fish", "atlantic_cod");
        n.putString("rarity", "legendary");
        n.putString("variant", "trophy");
        n.putString("size", "adult");
        n.putFloat("weight", 12.44f);
        n.putFloat("length", 31.66f);
        return n;
    }

    private static String text(List<Seg> line) {
        return line.stream().map(Seg::text).collect(Collectors.joining());
    }

    @BeforeEach
    void defaults() {
        TackleboxSettings d = new TackleboxSettings();
        s.catchTitle = true;
        s.catchImage = d.catchImage;
        s.catchRarity = d.catchRarity;
        s.catchVariant = d.catchVariant;
        s.catchName = d.catchName;
        s.catchNameRarity = d.catchNameRarity;
        s.catchSize = d.catchSize;
        s.catchWeight = d.catchWeight;
        s.catchLength = d.catchLength;
        s.catchColor = d.catchColor;
        s.catchSeconds = d.catchSeconds;
        s.catchHideTitle = d.catchHideTitle;
        CatchTitle.clear();
    }

    @AfterEach
    void off() {
        s.catchTitle = false;
        CatchTitle.clear();
    }

    @Test
    void offByDefaultLikeEveryFeature() {
        assertFalse(new TackleboxSettings().catchTitle);
    }

    @Test
    void readsTheFishFromItsItem() {
        assertEquals(COD, CatchTitle.read(cod(), "Atlantic Cod"));
        assertEquals("Atlantic Cod", CatchTitle.read(cod(), "  Atlantic  Cod ").name(), "pack pictures in the name are left out");
        assertEquals("atlantic cod", CatchTitle.read(cod(), "").name(), "no item name: the species id");
    }

    @Test
    void lineOneIsTheRarityBadgeThenTheName() {
        List<Seg> line = CatchTitle.line1(COD, s);
        assertEquals(new Seg("", 0xFFFFFF, true), line.get(0));
        assertEquals(new Seg(" ᴀᴛʟᴀɴᴛɪᴄ ᴄᴏᴅ", 0xFFFFFF, false), line.get(1));
        assertEquals(2, line.size(), "the variant badge is off by default");
    }

    @Test
    void theVariantBadgeSitsBetweenRarityAndNameWhenOn() {
        s.catchVariant = true;
        assertEquals("  ᴀᴛʟᴀɴᴛɪᴄ ᴄᴏᴅ", text(CatchTitle.line1(COD, s)));
        Info normal = new Info("Pike", "common", "normal", "baby", 1, 2);
        assertEquals(" ᴘɪᴋᴇ", text(CatchTitle.line1(normal, s)), "a normal fish has no variant badge");
    }

    @Test
    void everyPartOfLineOneSwitchesOff() {
        s.catchRarity = false;
        assertEquals("ᴀᴛʟᴀɴᴛɪᴄ ᴄᴏᴅ", text(CatchTitle.line1(COD, s)));
        s.catchRarity = true;
        s.catchName = false;
        assertEquals("", text(CatchTitle.line1(COD, s)));
        s.catchRarity = false;
        assertTrue(CatchTitle.line1(COD, s).isEmpty());
    }

    @Test
    void theNameTakesTheTextColourOrTheRarityColour() {
        s.catchColor = 0x112233;
        assertEquals(0x112233, CatchTitle.line1(COD, s).get(1).rgb());
        s.catchNameRarity = true;
        assertEquals(0xD98103, CatchTitle.line1(COD, s).get(1).rgb());
        assertEquals(0xFFFFFF, CatchTitle.line1(COD, s).get(0).rgb(), "a badge keeps its own colours");
    }

    @Test
    void lineTwoIsSizeWeightAndLengthWithOneDecimal() {
        List<Seg> line = CatchTitle.line2(COD, s);
        assertEquals(new Seg("ᴀᴅᴜʟᴛ", 0x1C7DA0, false), line.get(0));
        assertEquals(new Seg(" 12.4ʟʙ / 31.7ɪɴ", 0xFFFFFF, false), line.get(1));
        assertEquals("0.2", CatchTitle.number(0.17f));
        assertEquals("7.0", CatchTitle.number(7f));
    }

    @Test
    void everyPartOfLineTwoSwitchesOff() {
        s.catchSize = false;
        assertEquals("12.4ʟʙ / 31.7ɪɴ", text(CatchTitle.line2(COD, s)));
        s.catchWeight = false;
        assertEquals("31.7ɪɴ", text(CatchTitle.line2(COD, s)));
        s.catchWeight = true;
        s.catchLength = false;
        assertEquals("12.4ʟʙ", text(CatchTitle.line2(COD, s)));
        s.catchWeight = false;
        assertTrue(CatchTitle.line2(COD, s).isEmpty());
        s.catchSize = true;
        assertEquals("ᴀᴅᴜʟᴛ", text(CatchTitle.line2(COD, s)));
    }

    @Test
    void badgesAreTheServersTagGlyphs() {
        assertEquals(List.of("", "", "", "", "", "", ""),
                List.of("common", "rare", "epic", "legendary", "mythical", "special", "shiny").stream().map(CatchTitle::rarityBadge).toList());
        assertEquals(List.of("", "", "", "", "", "", "", ""),
                List.of("normal", "albino", "melanistic", "trophy", "fabled", "alternate", "spooky", "frozen").stream().map(CatchTitle::variantBadge).toList());
        assertEquals(0xAF3333, CatchTitle.sizeColor("gigantic", 0));
        assertEquals(0x123456, CatchTitle.sizeColor("huge", 0x123456));
    }

    @Test
    void rowsStackPictureThenTheTwoLines() {
        assertArrayEquals(new float[]{0, 34, 44, 53}, CatchTitle.layout(32, true, true));
        assertArrayEquals(new float[]{-1, 0, 10, 19}, CatchTitle.layout(0, true, true), "no picture: the lines move up");
        assertArrayEquals(new float[]{0, -1, 34, 43}, CatchTitle.layout(32, false, true));
        assertArrayEquals(new float[]{0, -1, -1, 32}, CatchTitle.layout(32, false, false), "only the picture until the fish is read");
    }

    @Test
    void onlyACatchTitleStartsItAndHidesTheServers() {
        assertFalse(CatchTitle.onTitle(Text.literal("BITE!"), 1000));
        assertNull(CatchTitle.view(1000));
        assertTrue(CatchTitle.onTitle(FISH, 1000));
        assertSame(FISH, CatchTitle.view(1000).image());
        assertNull(CatchTitle.view(1000).info(), "the picture alone until the fish item is read");
    }

    @Test
    void theServersTitleStaysWhenHidingIsOff() {
        s.catchHideTitle = false;
        assertFalse(CatchTitle.onTitle(FISH, 1000));
        assertNotNull(CatchTitle.view(1000), "ours still shows");
        assertFalse(CatchTitle.onSubtitle(SUBTITLE, 1010));
    }

    @Test
    void switchedOffItDoesNothing() {
        s.catchTitle = false;
        assertFalse(CatchTitle.onTitle(FISH, 1000));
        assertFalse(CatchTitle.onSubtitle(SUBTITLE, 1010));
        CatchTitle.onFish(cod(), "Atlantic Cod", 1050);
        s.catchTitle = true;
        assertNull(CatchTitle.view(1100));
    }

    @Test
    void theFishItemFillsTheLines() {
        CatchTitle.onTitle(FISH, 1000);
        CatchTitle.onFish(cod(), "Atlantic Cod", 1050);
        assertEquals(COD, CatchTitle.view(1060).info());
        assertNull(CatchTitle.view(2000).fallback(), "the subtitle is not needed once the fish is known");
    }

    @Test
    void itEndsAfterItsSeconds() {
        s.catchSeconds = 2;
        CatchTitle.onTitle(FISH, 1000);
        assertNotNull(CatchTitle.view(3000));
        assertNull(CatchTitle.view(3001));
    }

    @Test
    void aFullInventoryFallsBackToTheServersSubtitle() {
        CatchTitle.onTitle(FISH, 1000);
        assertTrue(CatchTitle.onSubtitle(SUBTITLE, 1010), "the subtitle after the catch title is hidden too");
        assertNull(CatchTitle.view(1400).fallback(), "the fish item gets half a second to turn up");
        assertSame(SUBTITLE, CatchTitle.view(1500).fallback());
    }

    @Test
    void aSubtitleSentBeforeTheTitleCountsToo() {
        assertFalse(CatchTitle.onSubtitle(SUBTITLE, 990), "nothing to hide yet, the title clears it");
        CatchTitle.onTitle(FISH, 1000);
        assertSame(SUBTITLE, CatchTitle.view(1500).fallback());
    }

    @Test
    void otherSubtitlesAreLeftAlone() {
        CatchTitle.onTitle(FISH, 1000);
        assertFalse(CatchTitle.onSubtitle(Text.literal("you leveled up"), 1010), "no rarity badge: not the catch's");
        assertFalse(CatchTitle.onSubtitle(SUBTITLE, 2500), "too long after the catch title");
        assertNull(CatchTitle.view(2600).fallback());
    }

    @Test
    void anOldSubtitleIsNotReusedForALaterCatch() {
        CatchTitle.onSubtitle(SUBTITLE, 1000);
        CatchTitle.onTitle(FISH, 5000);
        assertNull(CatchTitle.view(5600).fallback());
    }

    @Test
    void aNewCatchForgetsTheLastFish() {
        CatchTitle.onTitle(FISH, 1000);
        CatchTitle.onFish(cod(), "Atlantic Cod", 1050);
        CatchTitle.onTitle(Text.literal(""), 2000);
        assertNull(CatchTitle.view(2010).info());
    }

    @Test
    void theAnimationIsTheBiteAlertsAndEnds() {
        assertNull(BobberTimer.anim(TackleboxSettings.BiteAnim.POP, 3.01f, 3));
        assertArrayEquals(new float[]{1, 1, 0, 0}, BobberTimer.anim(TackleboxSettings.BiteAnim.POP, 1f, 3), 1e-4f);
        assertTrue(BobberTimer.anim(TackleboxSettings.BiteAnim.POP, 0.05f, 3)[1] < 1, "it pops in from small");
        assertEquals(0.5f, BobberTimer.anim(TackleboxSettings.BiteAnim.FADE, 2.875f, 3)[0], 1e-3f, "and fades out over the last quarter second");
        assertEquals(1f, BobberTimer.anim(TackleboxSettings.BiteAnim.NONE, 2.99f, 3)[0], "none just ends");
        assertEquals(-24f, BobberTimer.anim(TackleboxSettings.BiteAnim.SLIDE, 0f, 3)[3], 1e-4f);
    }
}
