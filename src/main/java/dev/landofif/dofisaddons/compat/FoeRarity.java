package dev.landofif.dofisaddons.compat;

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
