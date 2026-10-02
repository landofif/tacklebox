package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * /dofi debug: prints server data to chat (and the log) for working out new features, ported from
 * fishutils. Read only.
 */
public final class DebugDump {
    private DebugDump() {}

    /** The held item's id, lore and raw custom_data (where fishonmc keeps bait, fish and pet data). */
    public static int heldItem() {
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self == null) {
            return 0;
        }
        ItemStack stack = self.getMainHandStack();
        if (stack.isEmpty()) {
            say("hold an item in your main hand first.", Formatting.RED);
            return 0;
        }
        say("item " + Registries.ITEM.getId(stack.getItem()) + " x" + stack.getCount(), Formatting.AQUA);
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                say("lore: " + ShortTooltips.escape(line.getString()), Formatting.GRAY);
            }
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        String snbt = custom == null || custom.isEmpty() ? "(none)" : custom.copyNbt().toString();
        for (int i = 0; i < snbt.length(); i += 180) {
            say("custom_data: " + snbt.substring(i, Math.min(snbt.length(), i + 180)), Formatting.GRAY);
        }
        return 1;
    }

    /** Arms a one-shot print of the next tooltip you hover, before and after shortening. */
    public static int nextTooltip() {
        ShortTooltips.dumpNext = true;
        say("hover an item: its tooltip will be printed line by line.", Formatting.AQUA);
        return 1;
    }

    private static void say(String line, Formatting color) {
        DofisAddons.LOGGER.info("[debug] {}", line);
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self != null) {
            self.sendMessage(Mono.text(line).formatted(color), false);
        }
    }
}
