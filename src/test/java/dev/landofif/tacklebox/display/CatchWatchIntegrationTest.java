package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Uuids;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/** A catch read by tacklebox itself, without foecollab: title, then the new fish of yours, then the compendium count. */
class CatchWatchIntegrationTest {
    @Test
    void aCatchReachesTheCompendium() {
        UUID me = UUID.randomUUID();
        TackleboxSettings.get().dex.put("keywest", new long[3]);
        NbtCompound fish = new NbtCompound();
        fish.putIntArray("id", Uuids.toIntArray(UUID.randomUUID()));
        fish.putIntArray("catcher", Uuids.toIntArray(me));
        fish.putString("fish", "sockeye_salmon");
        fish.putString("variant", "normal");
        fish.putString("size", "adult");
        fish.putString("location", "keywest");

        CatchWatch.onTitle("", List.of(), me, 10_000);
        CatchWatch.Fish caught = CatchWatch.check(List.of(fish), me, 10_400);
        assertNotNull(caught);
        CatchWatch.onCatch(caught, 10_400);
        assertEquals(1, TackleboxSettings.get().dex.get("keywest")[0]);
    }
}
