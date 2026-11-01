package dev.landofif.tacklebox;

import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.menu.StatsPage;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
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
 * luna's whole /stats dump (2026-10-30, F8 while tacklebox's screen was open) read into the profile layout, as her
 * own page and as someone else's ({@code /stats <player>}, only the "Profile of" line differs). The dump leaves out
 * the server's filler bread (lore-less "Bread" in every free slot), so it is put back here as the real chest has it.
 */
class StatsPageIntegrationTest {
    private static final Pattern SLOT = Pattern.compile("^slot (\\d+) \\S+ x\\d+$");
    private static final Pattern ESCAPE = Pattern.compile("\\\\u([0-9A-Fa-f]{4})");

    /**
     * The dump's slots as pieces: {@code name="..."}, {@code lore="..."} (java escapes), {@code custom_data={...}} (snbt),
     * and filler bread in the other slots of the 54.
     */
    static List<StatsPage.Piece> dump(String owner) throws Exception {
        String text;
        try (InputStream in = StatsPageIntegrationTest.class.getResourceAsStream("/dumps/stats-20261030.txt")) {
            text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
        List<StatsPage.Piece> out = new ArrayList<>();
        int slot = -1;
        String name = "";
        List<String> lore = new ArrayList<>();
        NbtCompound data = new NbtCompound();
        for (String line : (text + "\nslot -1 end x1").split("\n")) {
            Matcher m = SLOT.matcher(line);
            if (m.matches() || line.equals("slot -1 end x1")) {
                if (slot >= 0) {
                    out.add(new StatsPage.Piece(slot, name, lore, data));
                }
                slot = m.matches() ? Integer.parseInt(m.group(1)) : -1;
                name = "";
                lore = new ArrayList<>();
                data = new NbtCompound();
            } else if (line.startsWith("  name=\"")) {
                name = unescape(line.substring(8, line.length() - 1));
            } else if (line.startsWith("  lore=\"")) {
                lore.add(unescape(line.substring(8, line.length() - 1)).replace("Profile of meowfih", "Profile of " + owner));
            } else if (line.startsWith("  custom_data=")) {
                data = StringNbtReader.readCompound(line.substring(14));
            }
        }
        List<Integer> used = out.stream().map(StatsPage.Piece::slot).toList();
        for (int free = 0; free < 54; free++) {
            if (!used.contains(free)) {
                out.add(new StatsPage.Piece(free, "Bread", List.of(), new NbtCompound()));
            }
        }
        out.sort(java.util.Comparator.comparingInt(StatsPage.Piece::slot));
        return out;
    }

    private static String unescape(String s) {
        return ESCAPE.matcher(s).replaceAll(r -> Matcher.quoteReplacement(String.valueOf((char) Integer.parseInt(r.group(1), 16))));
    }

    private static List<List<String>> plainLore(List<StatsPage.Piece> pieces) {
        return pieces.stream().map(p -> p.lore().stream()
                .map(l -> Glyphs.fromSmallCaps(dev.landofif.tacklebox.menu.MenuRows.clean(l)).toLowerCase(Locale.ROOT)).toList()).toList();
    }

    @Test
    void ownStatsPage() throws Exception {
        List<StatsPage.Piece> pieces = dump("meowfih");
        assertEquals(54, pieces.size()); // the dump's 24 items and 30 filler breads
        StatsPage.Stats s = StatsPage.read(pieces);
        assertNotNull(s);
        assertEquals("meowfih", s.owner());
        assertEquals(287, s.level());
        assertEquals("3.02M/4.22M", s.amount());

        // gear by its custom_data, in the dump's slots
        assertEquals(10, s.hat());
        assertEquals(-1, s.helmet());
        assertEquals(19, s.chest());
        assertEquals(28, s.legs());
        assertEquals(37, s.boots());
        assertEquals(25, s.reel());
        assertEquals(34, s.pole());
        assertEquals(43, s.line());
        assertEquals(146, s.lineHave());
        assertEquals(400, s.lineMax());
        assertEquals(new StatsPage.Pet(7, "Kangaroo Pet", 100, 1f, "246.69M/MAX", "Wade's Amulet"), s.pet());

        assertEquals(new StatsPage.Tile(22, "425.18k", "power"), s.power());
        assertEquals(new StatsPage.Tile(32, "202", "runes"), s.runes());
        assertEquals(new StatsPage.Tile(23, "210.54k", "catches"), s.catches());
        assertEquals(new StatsPage.Tile(16, "433x", "Small Chunk Of Meat"), s.bait());

        // the "Empty Profile Display" breads have lore, so they stay showcase slots
        assertEquals(List.of(2, 3, 4, 5, 6), s.displays());
        // fishing menu, collections, power breakdown, level 300 colors, admiral colors; return and close are left to esc,
        // the filler bread is no button
        assertEquals(List.of(1, 21, 31, 48, 50), s.buttons());
        assertFalse(FishMenu.othersProfile(plainLore(pieces), "meowfih"));
    }

    @Test
    void someoneElsesStatsPage() throws Exception {
        List<StatsPage.Piece> pieces = dump("freddy_2005");
        StatsPage.Stats s = StatsPage.read(pieces);
        assertEquals("freddy_2005", s.owner());
        assertEquals(19, s.chest()); // their armour is drawn on the model
        assertEquals(37, s.boots());
        assertTrue(FishMenu.othersProfile(plainLore(pieces), "meowfih"));
    }
}
