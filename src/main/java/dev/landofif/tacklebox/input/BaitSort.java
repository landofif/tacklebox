package dev.landofif.tacklebox.input;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * The bait sort key (luna 2026-10-30, tacklebox's own, also without foecollab): in your inventory, the baits (custom_data
 * {@code type} "bait") are put in rarity order, common first, legendary last, in the slots the baits already take. Swaps are
 * three pickup clicks, one click every 2 ticks; it stops if the inventory closes or something is on the cursor.
 */
public final class BaitSort {
    static final List<String> ORDER = List.of("common", "rare", "epic", "legendary", "mythical", "special");
    private static final int EVERY = 2;

    private static final Deque<Integer> clicks = new ArrayDeque<>();
    private static int syncId = -1;
    private static int wait;

    private BaitSort() {}

    /** Where a bait's rarity goes in the order; unknown rarities last. -1 for anything that is not a bait. */
    static int rank(NbtCompound data) {
        if (data == null || !"bait".equals(data.getString("type").orElse(""))) {
            return -1;
        }
        int i = ORDER.indexOf(data.getString("rarity").orElse(""));
        return i < 0 ? ORDER.size() : i;
    }

    /** Swaps (positions in {@code ranks}) that sort the ranks low to high, equal ranks keeping their order. */
    static List<int[]> swaps(List<Integer> ranks) {
        List<Integer> r = new ArrayList<>(ranks);
        List<int[]> out = new ArrayList<>();
        for (int i = 0; i < r.size(); i++) {
            int min = i;
            for (int j = i + 1; j < r.size(); j++) {
                if (r.get(j) < r.get(min)) {
                    min = j;
                }
            }
            if (min != i) {
                // move the smallest to i and shift the rest along by swapping down, so equal ranks keep their order
                for (int j = min; j > i; j--) {
                    out.add(new int[]{j - 1, j});
                    r.set(j, r.set(j - 1, r.get(j)));
                }
            }
        }
        return out;
    }

    /** The slot ids to pickup-click, in order: each swap picks up one bait, puts it in the other's slot and brings that one back. */
    static List<Integer> clicks(List<Integer> slots, List<Integer> ranks) {
        List<Integer> out = new ArrayList<>();
        for (int[] s : swaps(ranks)) {
            int a = slots.get(s[0]), b = slots.get(s[1]);
            out.add(a);
            out.add(b);
            out.add(a);
        }
        return out;
    }

    /** The pressed key in {@code screen}: plans the clicks if it is your inventory. */
    public static void start(Screen screen) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (!(screen instanceof InventoryScreen inv) || mc.player == null || !clicks.isEmpty()) {
            return;
        }
        ScreenHandler handler = inv.getScreenHandler();
        if (!handler.getCursorStack().isEmpty()) {
            return;
        }
        List<Integer> slots = new ArrayList<>();
        List<Integer> ranks = new ArrayList<>();
        for (Slot slot : handler.slots) {
            int rank = slot.inventory instanceof PlayerInventory && slot.getIndex() < 36 ? rank(data(slot.getStack())) : -1;
            if (rank >= 0) {
                slots.add(slot.id);
                ranks.add(rank);
            }
        }
        clicks.addAll(clicks(slots, ranks));
        syncId = handler.syncId;
        wait = 0;
    }

    public static void tick(MinecraftClient mc) {
        if (clicks.isEmpty()) {
            return;
        }
        if (mc.player == null || mc.interactionManager == null || !(mc.currentScreen instanceof InventoryScreen inv)
                || inv.getScreenHandler().syncId != syncId) {
            clicks.clear();
            return;
        }
        if (--wait > 0) {
            return;
        }
        wait = EVERY;
        mc.interactionManager.clickSlot(syncId, clicks.poll(), 0, SlotActionType.PICKUP, mc.player);
    }

    private static NbtCompound data(ItemStack stack) {
        NbtComponent c = stack.isEmpty() ? null : stack.get(DataComponentTypes.CUSTOM_DATA);
        return c == null ? null : c.copyNbt();
    }
}
