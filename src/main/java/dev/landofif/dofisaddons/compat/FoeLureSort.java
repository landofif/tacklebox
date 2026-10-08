package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.util.Glyphs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Foecollabs bait sorting helper for lures: lures that are the same type, colour and size (every tooltip line except
 * the uses line, which differs between a fresh and a used lure) get the same coloured square, shown when foecollabs own
 * helper is (always, or while its key is held). Foecollab is reached by reflection only.
 */
public final class FoeLureSort {
    private static Field only, held;
    private static Object config, keys, visibility;
    private static boolean tried;
    private static long countedAt;
    private static Map<String, Integer> counts = new HashMap<>();

    private FoeLureSort() {}

    /** Draws the square on a lure that has an exact match in another slot of the open screen. */
    public static void render(DrawContext ctx, ItemStack stack, int x, int y) {
        if (!isLure(stack) || !active()) {
            return;
        }
        String key = key(stack);
        if (counts().getOrDefault(key, 0) > 1) {
            int hue = Math.floorMod(key.hashCode(), 360);
            ctx.fill(x, y, x + 16, y + 16, 0x99 << 24 | java.awt.Color.HSBtoRGB(hue / 360f, 0.7f, 1f) & 0xFFFFFF);
        }
    }

    private static boolean isLure(ItemStack stack) {
        var cd = stack.get(DataComponentTypes.CUSTOM_DATA);
        return cd != null && cd.copyNbt().getString("type", "").equalsIgnoreCase("lure");
    }

    /** The name and every lore line but the uses ("Uses: 30/30" or "Remaining Uses: 120"). */
    private static String key(ItemStack stack) {
        StringBuilder b = new StringBuilder(Glyphs.plain(stack.getName()));
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                String s = Glyphs.plain(line).toLowerCase(Locale.ROOT);
                if (!s.contains("uses")) {
                    b.append('\n').append(s);
                }
            }
        }
        return b.toString();
    }

    private static Map<String, Integer> counts() {
        long now = Util.getMeasuringTimeMs();
        if (now - countedAt > 150) { // once per slot per frame is far too often
            countedAt = now;
            counts = new HashMap<>();
            if (MinecraftClient.getInstance().currentScreen instanceof HandledScreen<?> screen) {
                for (var slot : screen.getScreenHandler().slots) {
                    ItemStack st = slot.getStack();
                    if (!st.isEmpty() && isLure(st)) {
                        counts.merge(key(st), 1, Integer::sum);
                    }
                }
            }
        }
        return counts;
    }

    private static boolean active() {
        if (!tried) {
            tried = true;
            try {
                Object cfg = Class.forName("io.github.foecollab.config.FOEConfig").getMethod("getConfig").invoke(null);
                visibility = cfg.getClass().getField("baitSortingHelperVisibility").get(cfg);
                only = visibility.getClass().getField("showOnlyWhilePressingKeybind");
                keys = Class.forName("io.github.foecollab.handler.KeybindHandler").getMethod("instance").invoke(null);
                held = keys.getClass().getField("visualizeBaitSorting");
            } catch (ReflectiveOperationException | LinkageError e) {
                only = null;
            }
        }
        try {
            return only != null && (!only.getBoolean(visibility) || held.getBoolean(keys));
        } catch (ReflectiveOperationException e) {
            return false;
        }
    }
}
