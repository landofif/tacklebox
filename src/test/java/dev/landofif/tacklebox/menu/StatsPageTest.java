package dev.landofif.tacklebox.menu;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Pieces of the /stats page, lore and custom_data from luna's dump 2026-10-30. */
class StatsPageTest {
    private static final String P = " 퀃    ";

    static StatsPage.Piece piece(int slot, String name, String data, String... lore) {
        try {
            NbtCompound nbt = data.isEmpty() ? new NbtCompound() : StringNbtReader.readCompound(data);
            List<String> lines = new ArrayList<>();
            for (String l : lore) {
                lines.add(P + l);
            }
            return new StatsPage.Piece(slot, name, lines, nbt);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    static List<StatsPage.Piece> base() {
        return new ArrayList<>(List.of(
                piece(22, "Fishing Profile", "", "Profile of meowfih", "Fishing Level [287]", "                        (3.02M/4.22M)",
                        "🎣 Time Fishing 4d 15h 35m 37s", " Power 425.18K", "Bite Speed 683"),
                piece(23, "General Statistics", "", "ᴄᴀᴛᴄʜ ʀᴀᴛᴇ: 99.36%", "ᴡᴏʀʟᴅ ʀᴇᴄᴏʀᴅꜱ: 0", "ꜰɪꜱʜ ᴄᴀᴜɢʜᴛ: 210,542")));
    }

    @Test
    void notAStatsPageWithoutProfileAndGeneralStatistics() {
        assertNull(StatsPage.read(List.of(piece(22, "Fishing Profile", "", "Profile of meowfih"))));
        assertNull(StatsPage.read(List.of(piece(23, "General Statistics", ""))));
        assertNull(StatsPage.read(List.of()));
    }

    @Test
    void headerAndNumbers() {
        List<StatsPage.Piece> pieces = base();
        pieces.add(piece(32, "Runes Breakdown", "", "Total Runes Discovered: 202", "+ Prospect 202"));
        pieces.add(piece(16, "Small Chunk Of Meat", "{type:\"bait\",rarity:\"rare\",counter:433}", "", "ʙᴀɪᴛ ɪᴛᴇᴍ", "Stock: 433x"));
        StatsPage.Stats s = StatsPage.read(pieces);
        assertEquals("meowfih", s.owner());
        assertEquals(287, s.level());
        assertEquals("3.02M/4.22M", s.amount());
        assertEquals(3.02 / 4.22, s.progress(), 1e-4);
        assertEquals(new StatsPage.Tile(22, "425.18k", "power"), s.power());
        assertEquals(new StatsPage.Tile(32, "202", "runes"), s.runes());
        assertEquals(new StatsPage.Tile(23, "210,542", "catches"), s.catches());
        assertEquals(new StatsPage.Tile(16, "433x", "Small Chunk Of Meat"), s.bait());
    }

    @Test
    void armourByTypeAndPieceNotByName() {
        List<StatsPage.Piece> pieces = base();
        pieces.add(piece(10, "Purple Flower Hat", "{type:\"hat\"}"));
        pieces.add(piece(19, "Rainforest Chestplate", "{type:\"armor\",piece:\"leather chestplate\",quality:93,tier:4}"));
        pieces.add(piece(28, "Rainforest Leggings", "{type:\"armor\",piece:\"leather leggings\"}"));
        pieces.add(piece(37, "Some Boots Named Helmet", "{type:\"armor\",piece:\"leather boots\"}"));
        StatsPage.Stats s = StatsPage.read(pieces);
        assertEquals(10, s.hat());
        assertEquals(-1, s.helmet());
        assertEquals(19, s.chest());
        assertEquals(28, s.legs());
        assertEquals(37, s.boots());
    }

    @Test
    void lineDurabilityAndMissingParts() {
        List<StatsPage.Piece> pieces = base();
        pieces.add(piece(43, "Branwell's Freshwater III Line", "{type:\"line\",curDurability:146,maxDurability:400}"));
        StatsPage.Stats s = StatsPage.read(pieces);
        assertEquals(43, s.line());
        assertEquals(146, s.lineHave());
        assertEquals(400, s.lineMax());
        assertEquals(-1, s.reel());
        assertEquals(-1, s.pole());
        assertNull(s.pet());
        assertNull(s.runes());
        assertNull(s.bait());

        pieces.set(2, piece(43, "Old Line", "{type:\"line\"}"));
        assertEquals(-1, StatsPage.read(pieces).lineMax());
    }

    @Test
    void petAtMaxIsAFullBar() {
        StatsPage.Pet pet = StatsPage.pet(piece(7, "Kangaroo Pet", "{type:\"pet\"}", "", "sᴇᴍɪ₋ᴀʀɪᴅ ᴘᴇᴛ",
                "ᴘᴇᴛ ʟᴇᴠᴇʟ: [100]", "                       (246.69M/MAX)", "Equipped Skin: Default", "Equipped Item: Wade's Amulet"));
        assertEquals(new StatsPage.Pet(7, "Kangaroo Pet", 100, 1f, "246.69M/MAX", "Wade's Amulet"), pet);

        StatsPage.Pet young = StatsPage.pet(piece(7, "Otter Pet", "{type:\"pet\"}", "ᴘᴇᴛ ʟᴇᴠᴇʟ: [12]", "(1.5M/6M)", "Equipped Item: Empty"));
        assertEquals(0.25f, young.progress(), 1e-4);
        assertEquals("", young.item());
    }

    @Test
    void showcaseSlotsGiveThePlacementAndTheRestAreButtons() {
        List<StatsPage.Piece> pieces = base();
        pieces.add(piece(2, "Empty Profile Display", "", "ᴄʟɪᴄᴋ ᴛᴏ sᴇʟᴇᴄᴛ"));
        pieces.add(piece(4, "Christmas Event 2025", "", "ᴘʀᴏꜰɪʟᴇ ᴅɪsᴘʟᴀʏ", "Placement: #23", "Points: 23145pts"));
        pieces.add(piece(21, "Collections", "", "View all information for fishing"));
        pieces.add(piece(46, "Return", ""));
        pieces.add(piece(49, "Close", ""));
        StatsPage.Stats s = StatsPage.read(pieces);
        assertEquals("#23", s.placement());
        assertEquals("Christmas Event 2025", s.event());
        assertEquals(List.of(2, 4), s.displays());
        assertEquals(List.of(21), s.buttons());
        assertTrue(s.buttons().stream().noneMatch(id -> id == 22 || id == 23));
    }
}
