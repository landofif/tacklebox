package dev.landofif.tacklebox.input;

import net.minecraft.nbt.NbtCompound;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BaitSortTest {
    static NbtCompound item(String type, String rarity) {
        NbtCompound n = new NbtCompound();
        n.putString("type", type);
        n.putString("rarity", rarity);
        return n;
    }

    @Test
    void onlyBaitHasARankCommonFirst() {
        assertEquals(0, BaitSort.rank(item("bait", "common")));
        assertEquals(3, BaitSort.rank(item("bait", "legendary")));
        assertEquals(BaitSort.ORDER.size(), BaitSort.rank(item("bait", "weird")));
        assertEquals(-1, BaitSort.rank(item("lure", "common")));
        assertEquals(-1, BaitSort.rank(new NbtCompound()));
        assertEquals(-1, BaitSort.rank(null));
    }

    static List<Integer> apply(List<Integer> list, List<int[]> swaps) {
        List<Integer> r = new ArrayList<>(list);
        for (int[] s : swaps) {
            r.set(s[1], r.set(s[0], r.get(s[1])));
        }
        return r;
    }

    @Test
    void swapsSortLowToHigh() {
        List<Integer> ranks = List.of(3, 0, 2, 0, 1, 3);
        assertEquals(List.of(0, 0, 1, 2, 3, 3), apply(ranks, BaitSort.swaps(ranks)));
        assertTrue(BaitSort.swaps(List.of(0, 1, 1, 2)).isEmpty(), "already sorted: no clicks");
        assertTrue(BaitSort.swaps(List.of()).isEmpty());
    }

    @Test
    void equalRanksKeepTheirOrder() {
        // ranks with a tag in the tens: 1x are rare, 0x common
        List<Integer> tagged = List.of(10, 0, 11, 1, 2);
        List<Integer> ranks = tagged.stream().map(t -> t / 10).toList();
        assertEquals(List.of(0, 1, 2, 10, 11), apply(tagged, BaitSort.swaps(ranks)));
    }
}
