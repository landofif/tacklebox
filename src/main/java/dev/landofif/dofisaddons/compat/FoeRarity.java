package dev.landofif.dofisaddons.compat;

import net.minecraft.item.ItemStack;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** An items rarity colour as foecollab works it out (its FOMCItem.getRarity), by reflection. -1 if none. */
public final class FoeRarity {
    private static Method rarity;
    private static Field color;
    private static boolean tried;

    private FoeRarity() {}

    public static int color(ItemStack stack) {
        if (!tried) {
            tried = true;
            try {
                rarity = Class.forName("io.github.foecollab.FOMC.Types.FOMCItem").getMethod("getRarity", ItemStack.class);
                color = rarity.getReturnType().getField("COLOR");
            } catch (ReflectiveOperationException | LinkageError e) {
                rarity = null;
            }
        }
        if (rarity == null || stack.isEmpty()) {
            return -1;
        }
        try {
            Object r = rarity.invoke(null, stack);
            return r == null ? -1 : color.getInt(r) & 0xFFFFFF;
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }
}
