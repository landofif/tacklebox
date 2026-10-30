package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.screen.FishMenuScreen;
import dev.landofif.tacklebox.screen.PresetsScreen;
import dev.landofif.tacklebox.util.Glyphs;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Util;

import java.util.Locale;
import java.util.Set;

/**
 * The mouse wheel turns the pages of paged server menus (setting pageWheel): down clicks the menu's Next item, up its
 * Back item, the same click the player would send. Works in the chest and in the mods own menu and presets screens.
 * Taken before the screen and Mouse Tweaks see the wheel, but only when a page item was clicked.
 */
public final class MenuPages {
    private static final Set<String> NEXT = Set.of("next", "next page");
    private static final Set<String> BACK = Set.of("back", "previous", "previous page");
    /** One page per wheel turn: turns while the next page is still loading are dropped. */
    static final long COOLDOWN_MS = 250;
    private static long lastClick;

    private MenuPages() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
                ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) -> !scroll(s, vertical)));
    }

    /** Clicks the page item for one wheel turn; true when the turn was used. */
    private static boolean scroll(Screen screen, double vertical) {
        MinecraftClient client = MinecraftClient.getInstance();
        var server = client.getCurrentServerEntry();
        if (!TackleboxSettings.get().pageWheel || vertical == 0 || server == null || !server.address.toLowerCase(Locale.ROOT).contains("fishonmc")) {
            return false;
        }
        GenericContainerScreen chest = screen instanceof GenericContainerScreen c ? c : screen instanceof FishMenuScreen f ? f.original()
                : screen instanceof PresetsScreen p ? p.original() : null;
        if (chest == null || excluded(Glyphs.menuName(chest.getTitle()))) {
            return false;
        }
        ScreenHandler handler = chest.getScreenHandler();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory || !isPage(slot.getStack(), vertical < 0)) {
                continue;
            }
            long now = Util.getMeasuringTimeMs();
            if (ready(now, lastClick)) {
                lastClick = now;
                client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), (short) slot.id,
                        (byte) 0, SlotActionType.PICKUP, Int2ObjectMaps.emptyMap(), net.minecraft.screen.sync.ItemStackHash.EMPTY));
            }
            return true;
        }
        return false;
    }

    private static boolean isPage(ItemStack stack, boolean down) {
        if (stack.isEmpty()) {
            return false;
        }
        var lore = stack.get(DataComponentTypes.LORE);
        String first = lore == null || lore.lines().isEmpty() ? "" : Glyphs.plain(lore.lines().get(0));
        return isPage(Glyphs.plain(stack.getName()), first, down);
    }

    /**
     * Whether an item is the Next (down) or Back (up) page item, by its name or, where it keeps the plain item name like
     * the auction house's "Gunpowder", by the first lore line. "Return" goes to the menu before, so it never counts.
     */
    static boolean isPage(String name, String firstLore, boolean down) {
        Set<String> words = down ? NEXT : BACK;
        return words.contains(name.trim().toLowerCase(Locale.ROOT)) || words.contains(firstLore.trim().toLowerCase(Locale.ROOT));
    }

    /** Quest menus are left alone. */
    static boolean excluded(String menuName) {
        return menuName.contains("quest");
    }

    static boolean ready(long now, long last) {
        return now - last >= COOLDOWN_MS;
    }
}
