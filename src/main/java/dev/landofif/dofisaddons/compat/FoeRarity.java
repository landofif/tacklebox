package dev.landofif.dofisaddons.compat;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** An items rarity colour as foecollab works it out (its FOMCItem.getRarity), by reflection. -1 if the server
 * gave it no rarity, which is every filler, button and arrow of a menu. */
public final class FoeRarity {
    private static Method rarity;
    private static Field color;
    private static Object none;
    private static boolean tried;

    private FoeRarity() {}

    /** A see-through circle in the items rarity colour, 16x16 at x, y, if the item has a rarity. */
    public static void circle(DrawContext ctx, int x, int y, ItemStack stack) {
        int rgb = color(stack);
        if (rgb < 0) {
            return;
        }
        int argb = 0x66 << 24 | rgb;
        for (int row = 0; row < 16; row++) {
            double dy = row + 0.5 - 8;
            int half = (int) Math.round(Math.sqrt(64 - dy * dy));
            ctx.fill(x + 8 - half, y + row, x + 8 + half, y + row + 1, argb);
        }
    }

    public static int color(ItemStack stack) {
        if (!tried) {
            tried = true;
            try {
                rarity = Class.forName("io.github.foecollab.FOMC.Types.FOMCItem").getMethod("getRarity", ItemStack.class);
                color = rarity.getReturnType().getField("COLOR");
                none = rarity.getReturnType().getField("DEFAULT").get(null);
            } catch (ReflectiveOperationException | LinkageError e) {
                rarity = null;
            }
        }
        if (rarity == null || stack.isEmpty()) {
            return -1;
        }
        try {
            Object r = rarity.invoke(null, stack);
            return r == null || r == none ? -1 : color.getInt(r) & 0xFFFFFF;
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }
}
