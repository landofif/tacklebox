package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** What the menu panel's tiles say: short names, counts, order, header summary, hover boxes, level pages, layout. */
class MenuTilesTest {
    private static final String P = " 퀃    ";

    @Test
    void shortNameDropsTheSharedWord() {
        assertEquals("basses", MenuTiles.shortName("Basses Collection"));
        assertEquals("temperate perches", MenuTiles.shortName("Temperate Perches Collection"));
        assertEquals("collections", MenuTiles.shortName("Collections")); // not a collection item
    }

    @Test
    void countIsWrittenAlikeOnBothSidesInTheUnitOfWhatIsNeeded() {
        assertEquals("43.1k/50k", MenuTiles.count("43,178/50K"));
        assertEquals("19.2k/20k", MenuTiles.count("19,249/20K"));
        assertEquals("62.1k/100k", MenuTiles.count("62.19K/100K"));
        assertEquals("196/200", MenuTiles.count("196/200"));
        assertEquals("1,603/2,500", MenuTiles.count("1,603/2.5K"));
        assertEquals("11.85m/2.32m", MenuTiles.count("11.85M/2.32M")); // the compendium past its max
        assertEquals("", MenuTiles.count(""));
    }

    @Test
    void countNeverLooksDoneBeforeItIs() {
        assertEquals("1,495/1,500", MenuTiles.count("1,495/1.5K"));
        assertEquals("19.9k/20k", MenuTiles.count("19,990/20K"));
    }

    @Test
    void closestToTheNextLevelFirstAndFinishedLast() {
        record C(String name, float progress, boolean done) {}
        List<C> in = List.of(new C("basses", .86f, false), new C("catfishes", -1, true), new C("mackerels", .997f, false),
                new C("reptiles", .26f, false), new C("gars", .98f, false), new C("cods", .98f, false));
        List<String> out = MenuTiles.byNextLevel(in, C::progress, C::done).stream().map(C::name).toList();
        assertEquals(List.of("mackerels", "gars", "cods", "basses", "reptiles", "catfishes"), out); // gars before cods: server order
    }

    @Test
    void headerSummaryLeavesOutWhatIsZero() {
        assertEquals("1 max · 7 almost there", MenuTiles.summary(1, 7));
        assertEquals("7 almost there", MenuTiles.summary(0, 7));
        assertEquals("", MenuTiles.summary(0, 0));
        assertEquals("1 max · frenzy at toledo bend", MenuTiles.line("1 max", "", "frenzy at toledo bend"));
    }

    @Test
    void collectionHoverBoxHasLevelCountAndPlaces() {
        List<String> lore = List.of(P + "Basses Collection", P + "ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ", P + "Current Level - [6]", P + "   (43,178/50K)",
                P + "Progress to Level 7", P + "Catch at:", P + "- Cypress Lake", P + "- Toledo Bend Reservoir", P + " ");
        MenuRows.Row row = MenuRows.of("Basses Collection", lore);
        assertEquals(List.of("cypress lake", "toledo bend"), MenuTiles.catchAt(lore));
        assertEquals(List.of("basses collection", "Level 6, 43.1k/50k to level 7", "Catch at cypress lake, toledo bend"),
                MenuTiles.collectionTip(row, lore));
    }

    @Test
    void aCollectionIsMaxOnlyWhenItsCountSaysMax() {
        // luna's catfishes at its top (screenshot 2026-11-01), and gigantic at 8 of its 10 levels
        List<String> catfishes = List.of(P + "Catfishes Collection", P + "ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ", P + " ", P + "Current Level - [7]",
                P + "   (30,039/MAX)", P + " ", P + "Catch at:", P + "- Cypress Lake", P + "- Nile River");
        List<String> gigantic = List.of(P + "Gigantic Collection", P + "ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ", P + "Current Level - [8]",
                P + "   (1,886/5K)", P + "Progress to Level 9");
        assertTrue(MenuTiles.maxed(catfishes));
        assertFalse(MenuTiles.maxed(gigantic));
        assertEquals(List.of("catfishes collection", "Level 7, max", "Catch at cypress lake, nile river"),
                MenuTiles.collectionTip(MenuRows.of("Catfishes Collection", catfishes), catfishes));
        assertEquals("Level 8, 1,886/5,000 to level 9", MenuTiles.collectionTip(MenuRows.of("Gigantic Collection", gigantic), gigantic).get(1));
    }

    @Test
    void placeHoverBoxHasMarkLevelClimateAndBait() {
        Locations.Location toledo = Locations.byName("toledo bend reservoir");
        List<String> tip = MenuTiles.placeTip(toledo, "frenzy", 13, .287f, "Subtropical · Freshwater · 13°C");
        assertEquals("toledo bend · frenzy", tip.get(0));
        assertEquals("Level 13, 28% to level 14", tip.get(1));
        assertEquals("Subtropical · Freshwater · 13°C", tip.get(2));
        assertTrue(tip.contains("dough balls (common), epic quest, 1r 1e"));
        assertTrue(tip.contains("sweet corn (rare), legendary quest, 1r 2e 1l, high luck (+1000)"));

        Locations.Location oil = Locations.byName("oil rig");
        assertEquals("Level 20, max", MenuTiles.placeTip(oil, "", 20, 1, "").get(1));
        List<String> unknown = MenuTiles.placeTip(oil, "", -1, -1, "");
        assertEquals("oil rig", unknown.get(0));
        assertEquals("Level not known yet: open the compendium once", unknown.get(1));
        assertEquals("No bait noted for this place yet", unknown.get(unknown.size() - 1));
    }

    @Test
    void markWords() {
        assertEquals("frenzy", MenuTiles.markWord(Locations.FRENZY));
        assertEquals("fabled", MenuTiles.markWord(Locations.FABLED));
        assertEquals("", MenuTiles.markWord(0));
    }

    @Test
    void levelPagesAndTheirLevels() {
        assertEquals(8, MenuTiles.level("level 8"));
        assertEquals(-1, MenuTiles.level("leveling bonuses"));
        assertEquals(-1, MenuTiles.level("location leveling data"));
        assertTrue(MenuTiles.isLevelPage(List.of("level 1", "level 2", "level 3", "level 4", "level 5", "return")));
        assertFalse(MenuTiles.isLevelPage(List.of("level 300 colors", "fishing profile", "collections")));
        assertEquals(MenuTiles.State.DONE, MenuTiles.state("green_concrete"));
        assertEquals(MenuTiles.State.NOW, MenuTiles.state("yellow_concrete"));
        assertEquals(MenuTiles.State.LOCKED, MenuTiles.state("gray_concrete"));
    }

    @Test
    void levelPageTitleFromTheLore() {
        assertEquals("lake biwa", MenuTiles.levelPageTitle(List.of(P + "✔ (Level 1) Lake Biwa", P + "ʟᴏᴄᴀᴛɪᴏɴ ᴄᴏʟʟᴇᴄᴛɪᴏɴs")));
        assertEquals("albino collection", MenuTiles.levelPageTitle(List.of(P + "✔ (Level 1)", P + "ᴀʟʙɪɴᴏ ᴄᴏʟʟᴇᴄᴛɪᴏɴ")));
        assertEquals("", MenuTiles.levelPageTitle(List.of(P + "Rewards:")));
    }

    @Test
    void layoutShrinksTilesBeforeItScrolls() {
        // luna's panel at gui scale 2 on 1920x1080: 580 by 462 for the tiles
        MenuTiles.Layout collections = MenuTiles.layout(35, 5, 580, 462);
        assertEquals(52, collections.th());
        assertEquals(109, collections.tw());
        assertFalse(collections.scrolls());
        MenuTiles.Layout shorter = MenuTiles.layout(35, 5, 580, 380);
        assertEquals(47, shorter.th());
        assertFalse(shorter.scrolls());
        MenuTiles.Layout scale3 = MenuTiles.layout(35, 5, 580, 282); // gui scale 3: 36 tall tiles still do not fit
        assertEquals(MenuTiles.MIN_H, scale3.th());
        assertTrue(scale3.scrolls());
        MenuTiles.Layout travel = MenuTiles.layout(17, 4, 580, 462);
        assertEquals(52, travel.th());
        assertEquals(139, travel.tw());
        assertEquals(5 * 52 + 4 * MenuTiles.GAP, travel.height());
    }
}
