package dev.landofif.tacklebox.input;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/** The planned pickup clicks, played on an inventory with a cursor, leave the baits and lures common to legendary and the rest where it was. */
class BaitSortIntegrationTest {
    @Test
    void clicksSortBaitsAndLuresInPlace() {
        Map<Integer, NbtCompound> inv = new HashMap<>();
        inv.put(9, BaitSortTest.item("bait", "legendary"));
        inv.put(10, BaitSortTest.item("fishing_rod", "rare")); // not a bait: stays
        inv.put(14, BaitSortTest.item("lure", "common")); // lures sort with the baits
        inv.put(20, BaitSortTest.item("bait", "epic"));
        inv.put(36, BaitSortTest.item("bait", "rare")); // hotbar
        List<Integer> slots = new ArrayList<>();
        List<Integer> ranks = new ArrayList<>();
        for (int id = 0; id < 45; id++) {
            int rank = BaitSort.rank(inv.get(id));
            if (rank >= 0) {
                slots.add(id);
                ranks.add(rank);
            }
        }
        NbtCompound cursor = null;
        for (int id : BaitSort.clicks(slots, ranks)) { // vanilla pickup with different items: cursor and slot trade places
            NbtCompound was = inv.remove(id);
            if (cursor != null) {
                inv.put(id, cursor);
            }
            cursor = was;
        }
        assertNull(cursor);
        assertEquals("common", inv.get(9).getString("rarity").orElse(""));
        assertEquals("lure", inv.get(9).getString("type").orElse(""));
        assertEquals("rare", inv.get(14).getString("rarity").orElse(""));
        assertEquals("epic", inv.get(20).getString("rarity").orElse(""));
        assertEquals("legendary", inv.get(36).getString("rarity").orElse(""));
        assertEquals("fishing_rod", inv.get(10).getString("type").orElse(""));
    }
}
