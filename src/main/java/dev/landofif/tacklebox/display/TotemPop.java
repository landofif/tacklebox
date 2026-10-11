package dev.landofif.tacklebox.display;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;

/** The vanilla totem of undying animation (the floating item only, no particles or sound: triggers have their own sound) with any item. */
public final class TotemPop {
    private TotemPop() {}

    /** Size multiplier of the pop being shown, read by {@code InGameOverlayRendererMixin}. */
    public static float scale = 1f;

    /** {@code spec} is an item id, {@code pet:<type>} or {@code shard:<climate>}; anything else shows the totem. */
    public static void show(String spec, double scale) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }
        TotemPop.scale = (float) scale;
        spec = spec.trim();
        ItemStack stack;
        if (spec.toLowerCase(Locale.ROOT).startsWith("pet:")) {
            stack = PetHeads.head(spec.substring(4));
        } else if (spec.toLowerCase(Locale.ROOT).startsWith("shard:")) {
            stack = PetHeads.head(spec);
        } else {
            Identifier id = Identifier.tryParse(spec.toLowerCase(Locale.ROOT));
            stack = id != null && Registries.ITEM.containsId(id) ? new ItemStack(Registries.ITEM.get(id)) : new ItemStack(Items.TOTEM_OF_UNDYING);
        }
        mc.gameRenderer.showFloatingItem(stack);
    }
}
