package dev.landofif.tacklebox;

import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.menu.MenuRows;
import dev.landofif.tacklebox.menu.MenuTiles;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The tile pages from luna's dumps: the collections overview (2026-10-10, all 35), /travel (2026-10-29, Toledo Bend in a
 * frenzy) with her saved place levels, an albino collection level page and Lake Biwa's location leveling (2026-10-08).
 * The buttons are read like {@code FishMenuScreen.collect} does: bread, close and return are left out.
 */
class MenuTilesIntegrationTest {
    private static final Pattern SLOT = Pattern.compile("^slot (\\d+) minecraft:(\\S+) x\\d+$");
    private static final Pattern ESCAPE = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");

    record Item(int slot, String id, String name, List<String> lore) {
        String plain() {
            return MenuRows.clean(name).toLowerCase(Locale.ROOT);
        }

        MenuRows.Row row() {
            return MenuRows.of(name, lore);
        }
    }

    static List<Item> buttons(String file) throws Exception {
        String text;
        try (InputStream in = MenuTilesIntegrationTest.class.getResourceAsStream("/dumps/" + file)) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<Item> out = new ArrayList<>();
        Item cur = null;
        for (String line : text.split("\n")) {
            Matcher m = SLOT.matcher(line);
            if (m.matches()) {
                cur = new Item(Integer.parseInt(m.group(1)), m.group(2), "", new ArrayList<>());
                out.add(cur);
            } else if (cur != null && line.startsWith("  name=\"")) {
                cur = new Item(cur.slot, cur.id, unescape(line.substring(8, line.length() - 1)), cur.lore);
                out.set(out.size() - 1, cur);
            } else if (cur != null && line.startsWith("  lore=\"")) {
                cur.lore.add(unescape(line.substring(8, line.length() - 1)));
            }
        }
        return out.stream().filter(i -> !i.name.equals("Bread") && !i.plain().equals("close") && !i.plain().equals("return")).toList();
    }

    private static String unescape(String s) {
        return ESCAPE.matcher(s).replaceAll(r -> Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(r.group(1), 16))));
    }

    @Test
    void collectionsOverviewClosestToTheNextLevelFirst() throws Exception {
        List<Item> items = buttons("collections-20261010.txt");
        assertEquals(35, items.size());
        assertEquals("collections", FishMenu.crewTitleMenu(items.stream().map(Item::plain).toList()));

        List<Item> order = MenuTiles.byNextLevel(items, i -> i.row().progress(), i -> MenuTiles.maxed(i.lore));
        List<String> names = order.stream().map(i -> MenuTiles.shortName(i.name)).toList();
        // mackerels 1,495/1.5K, gars 196/200, tunas 970/1K, carps 19,249/20K, seabreams 1,414/1.5K
        assertEquals(List.of("mackerels", "gars", "tunas", "carps", "seabreams"), names.subList(0, 5));
        assertEquals("reptiles", names.get(34)); // 33/125
        assertTrue(names.contains("temperate perches") && names.contains("eurasian minnows"));
        assertTrue(names.stream().noneMatch(n -> n.contains("collection")));

        long almost = items.stream().filter(i -> i.row().progress() >= MenuTiles.ALMOST).count();
        assertEquals("7 almost there", MenuTiles.summary(0, (int) almost));

        Item mackerels = order.get(0);
        assertEquals("1,495/1,500", MenuTiles.count(mackerels.row().amount()));
        Item basses = items.get(0);
        assertEquals(List.of("basses collection", "Level 6, 43.1k/50k to level 7",
                "Catch at cypress lake, lake biwa, everglades, toledo bend, great lakes"), MenuTiles.collectionTip(basses.row(), basses.lore));
    }

    @Test
    void aFinishedCollectionGoesLastWithMaxAndGiganticAtEightDoesNot() throws Exception {
        // the dump is from before catfishes reached level 7: its lore is put in from luna's screenshot of 2026-11-01, and
        // gigantic (10 levels; red block, at 8 in her screenshot) is added in the species' format, which a dump has to confirm
        String p = " 퀃    ";
        List<Item> items = new ArrayList<>(buttons("collections-20261010.txt"));
        Item cat = items.stream().filter(i -> i.plain().equals("catfishes collection")).findFirst().orElseThrow();
        items.set(items.indexOf(cat), new Item(cat.slot, cat.id, cat.name, List.of(p + "Catfishes Collection", p + "ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ",
                p + "Current Level - [7]", p + "                        (30,039/MAX)", p + "Catch at:", p + "- Cypress Lake")));
        items.add(new Item(36, "red_concrete", "Gigantic Collection", List.of(p + "Gigantic Collection", p + "ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ",
                p + "Current Level - [8]", p + "                        (1,886/5K)", p + "Progress to Level 9")));

        List<Item> order = MenuTiles.byNextLevel(items, i -> i.row().progress(), i -> MenuTiles.maxed(i.lore));
        assertEquals("catfishes", MenuTiles.shortName(order.get(35).name));
        assertEquals(1, items.stream().filter(i -> MenuTiles.maxed(i.lore)).count());
        assertEquals("Level 7, max", MenuTiles.collectionTip(order.get(35).row(), order.get(35).lore).get(1));
        Item gigantic = order.stream().filter(i -> i.plain().equals("gigantic collection")).findFirst().orElseThrow();
        assertTrue(order.indexOf(gigantic) < 34);
        assertEquals("Level 8, 1,886/5,000 to level 9", MenuTiles.collectionTip(gigantic.row(), gigantic.lore).get(1));
    }

    @Test
    void everyCollectionNameFitsAsWholeWordsInLunasWindow() throws Exception {
        // her game window on 2026-11-01: 941 px at gui scale 2, so the grid is 434 wide and a tile 80; the name gets 68
        List<Item> items = buttons("collections-20261010.txt");
        MenuTiles.Layout layout = MenuTiles.layout(items.size() + 1, 5, 434, 433);
        assertEquals(80, layout.tw());
        assertEquals(47, layout.th());
        assertEquals(2, MenuTiles.nameRows(layout.th())); // so temperate perches keeps both lines above the bar
        java.util.function.ToIntFunction<String> w = s -> s.length() * 6 - (int) s.chars().filter(c -> c == ' ').count() * 2;
        for (Item i : items) {
            String name = MenuTiles.shortName(i.name);
            List<String> lines = MenuTiles.nameLines(name, layout.tw() - 12, 2, w);
            assertEquals(name, String.join(" ", lines), name);
        }
        assertEquals(List.of("eurasian", "minnows"), MenuTiles.nameLines("eurasian minnows", 68, 2, w));
        assertEquals(List.of("gigantic"), MenuTiles.nameLines("gigantic", 68, 2, w));
    }

    @Test
    void travelPlacesInServerOrderInFourColumns() throws Exception {
        List<Item> items = buttons("travel-20261029.txt");
        assertEquals(17, items.size());
        List<Locations.Location> places = items.stream().map(i -> Locations.byName(i.plain())).toList();
        assertTrue(places.stream().allMatch(java.util.Objects::nonNull));
        assertEquals("cypress lake", places.get(0).name());
        assertEquals("cairns", places.get(16).name());

        MenuTiles.Layout layout = MenuTiles.layout(items.size(), 4, 580, 462);
        assertEquals(5 * 52 + 4 * MenuTiles.GAP, layout.height());
        assertFalse(layout.scrolls());

        Item toledo = items.get(6);
        String mark = MenuTiles.markWord(Locations.mark(toledo.name, ""));
        assertEquals("frenzy", mark);
        // luna's saved level for toledo bend, 13|0.28695336
        List<String> tip = MenuTiles.placeTip(places.get(6), mark, 13, 0.28695336f, toledo.row().sub());
        assertEquals(List.of("toledo bend · frenzy", "Level 13, 28% to level 14", "Subtropical · Freshwater · 13°C", ""), tip.subList(0, 4));
        assertTrue(tip.contains("bread (common), epic quest, 1r 1e"));

        // lake biwa at 20 (11.85M/2.32M) is the one max; the header also names the frenzy
        String summary = MenuTiles.line("1 max", mark + " at " + places.get(6).name());
        assertEquals("1 max · frenzy at toledo bend", summary);
        assertEquals("Level 20, max", MenuTiles.placeTip(places.get(2), "", 20, 1, items.get(2).row().sub()).get(1));
    }

    @Test
    void collectionLevelPage() throws Exception {
        List<Item> items = buttons("collection-levels-20261008.txt");
        assertTrue(MenuTiles.isLevelPage(items.stream().map(Item::plain).toList()));
        assertEquals(10, items.size());
        assertEquals("albino collection", MenuTiles.levelPageTitle(items.get(0).lore));
        assertEquals(MenuTiles.State.DONE, MenuTiles.state(items.get(6).id));
        assertEquals(MenuTiles.State.NOW, MenuTiles.state(items.get(7).id));
        assertEquals(MenuTiles.State.LOCKED, MenuTiles.state(items.get(9).id));
        assertEquals("60/80", MenuTiles.count(items.get(7).row().amount()));
        assertEquals("60/500", MenuTiles.count(items.get(9).row().amount()));
        assertEquals(10, MenuTiles.level(items.get(9).plain()));
        long done = items.stream().filter(i -> MenuTiles.state(i.id) == MenuTiles.State.DONE).count();
        assertEquals("7/10 done", done + "/" + items.size() + " done");
    }

    @Test
    void locationLevelingPageKeepsItsOtherButtonsAsRows() throws Exception {
        List<Item> items = buttons("location-leveling-20261008.txt");
        List<String> plain = items.stream().map(Item::plain).toList();
        assertTrue(MenuTiles.isLevelPage(plain));
        List<Item> levels = items.stream().filter(i -> MenuTiles.level(i.plain()) > 0).toList();
        List<Item> rest = items.stream().filter(i -> MenuTiles.level(i.plain()) <= 0).toList();
        assertEquals(20, levels.size());
        assertEquals(List.of("location leveling data", "leveling bonuses"), rest.stream().map(Item::plain).toList());
        assertEquals("lake biwa", MenuTiles.levelPageTitle(levels.get(0).lore));
        assertTrue(levels.stream().allMatch(i -> MenuTiles.state(i.id) == MenuTiles.State.DONE));
        assertNotNull(MenuTiles.layout(20, 5, 580, 462 - ROW_PAIR));
        assertFalse(MenuTiles.layout(20, 5, 580, 462 - ROW_PAIR).scrolls());
    }

    /** One row of the two other buttons under the grid, with the gap above it. */
    private static final int ROW_PAIR = 22 + MenuTiles.GAP;

    @Test
    void crewAndQuestMenusStayRows() {
        assertFalse(MenuTiles.isLevelPage(List.of("empty crew display", "crew info", "crew leveling", "meowfih (online)")));
        assertFalse(MenuTiles.isLevelPage(List.of("fishing quest #1", "fishing quest #2", "fishing quest #3")));
    }
}
