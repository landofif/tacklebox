package dev.landofif.tacklebox.display;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Uuids;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatchWatchTest {
    static final UUID ME = UUID.fromString("00000000-0000-0000-0000-00000000000a");
    static final UUID OTHER = UUID.fromString("00000000-0000-0000-0000-00000000000b");

    static NbtCompound fish(UUID id, UUID catcher, String species) {
        NbtCompound n = new NbtCompound();
        n.putIntArray("id", Uuids.toIntArray(id));
        n.putIntArray("catcher", Uuids.toIntArray(catcher));
        n.putString("fish", species);
        n.putString("rarity", "common");
        n.putString("variant", "normal");
        n.putString("size", "adult");
        n.putString("location", "kenai");
        return n;
    }

    @BeforeEach
    void reset() {
        CatchWatch.check(List.of(), ME, Long.MAX_VALUE); // clears a pending catch
    }

    @Test
    void catchTitleIsOneFishGlyph() {
        assertTrue(CatchWatch.isCatchTitle(""));
        assertFalse(CatchWatch.isCatchTitle(""));
        assertFalse(CatchWatch.isCatchTitle("A"));
        assertFalse(CatchWatch.isCatchTitle(""));
    }

    @Test
    void readsOnlyYourOwnFish() {
        UUID id = UUID.randomUUID();
        assertEquals(new CatchWatch.Fish(id, "sockeyesalmon", "normal", "adult", "kenai"), CatchWatch.read(fish(id, ME, "sockeyesalmon"), ME));
        assertNull(CatchWatch.read(fish(id, OTHER, "sockeyesalmon"), ME));
        NbtCompound bait = new NbtCompound();
        bait.putString("rarity", "rare");
        assertNull(CatchWatch.read(bait, ME));
    }

    @Test
    void theNewFishAfterTheTitleIsTheCatchOnce() {
        NbtCompound old = fish(UUID.randomUUID(), ME, "pike");
        NbtCompound caught = fish(UUID.randomUUID(), ME, "sockeyesalmon");
        CatchWatch.onTitle("", List.of(old), ME, 1000);
        assertNull(CatchWatch.check(List.of(old), ME, 1050));
        assertEquals("sockeyesalmon", CatchWatch.check(List.of(old, caught), ME, 1100).fish());
        assertNull(CatchWatch.check(List.of(old, caught), ME, 1150));
    }

    @Test
    void noTitleOrTooLateIsNoCatch() {
        NbtCompound caught = fish(UUID.randomUUID(), ME, "sockeyesalmon");
        assertNull(CatchWatch.check(List.of(caught), ME, 1000)); // a fish taken out of a chest
        CatchWatch.onTitle("", List.of(), ME, 1000);
        assertNull(CatchWatch.check(List.of(caught), ME, 1000 + CatchWatch.WINDOW + 1));
        CatchWatch.onTitle("not a fish", List.of(), ME, 5000);
        assertNull(CatchWatch.check(List.of(caught), ME, 5001));
    }

    @Test
    void someoneElsesFishDoesNotCount() {
        CatchWatch.onTitle("", List.of(), ME, 1000);
        assertNull(CatchWatch.check(List.of(fish(UUID.randomUUID(), OTHER, "pike")), ME, 1100));
    }
}
