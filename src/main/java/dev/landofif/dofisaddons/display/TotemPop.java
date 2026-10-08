package dev.landofif.dofisaddons.display;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** The vanilla totem of undying animation (the floating item only, no particles or sound: triggers have their own sound) with any item. */
public final class TotemPop {
    private TotemPop() {}

    /** {@code spec} is an item id or {@code pet:<type>}; anything else shows the totem. */
    public static void show(String spec) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        spec = spec.trim();
        ItemStack stack;
        if (spec.toLowerCase(Locale.ROOT).startsWith("pet:")) {
            stack = PetHeads.head(spec.substring(4));
        } else {
            Identifier id = Identifier.tryParse(spec.toLowerCase(Locale.ROOT));
            stack = id != null && Registries.ITEM.containsId(id) ? new ItemStack(Registries.ITEM.get(id)) : new ItemStack(Items.TOTEM_OF_UNDYING);
        }
        mc.gameRenderer.showFloatingItem(stack);
    }
}
