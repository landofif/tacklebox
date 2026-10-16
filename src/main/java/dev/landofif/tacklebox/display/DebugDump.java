package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.screen.FishMenuScreen;
import dev.landofif.tacklebox.util.Glyphs;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.text.TextCodecs;
import net.minecraft.util.Formatting;
import com.mojang.serialization.JsonOps;

/**
 * /tacklebox debug: prints server data to chat (and the log) for working out new features, ported from
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
                say("lore: " + escape(line.getString()), Formatting.GRAY);
            }
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        String snbt = custom == null || custom.isEmpty() ? "(none)" : custom.copyNbt().toString();
        for (int i = 0; i < snbt.length(); i += 180) {
            say("custom_data: " + snbt.substring(i, Math.min(snbt.length(), i + 180)), Formatting.GRAY);
        }
        saveStyled(MinecraftClient.getInstance(), stack);
        return 1;
    }

    /**
     * The held item again, but with every Text as JSON (styles, colours, fonts, siblings) and the tooltip as the
     * client builds it, after other mods' tooltip callbacks. The chat lines above only carry plain strings.
     * Saved to config/tacklebox/guidump and copied to the clipboard.
     */
    private static void saveStyled(MinecraftClient client, ItemStack stack) {
        StringBuilder out = new StringBuilder();
        out.append("item ").append(Registries.ITEM.getId(stack.getItem())).append(" x").append(stack.getCount()).append('\n');
        out.append("custom_name=").append(json(client, stack.get(DataComponentTypes.CUSTOM_NAME))).append('\n');
        out.append("item_name=").append(json(client, stack.get(DataComponentTypes.ITEM_NAME))).append('\n');
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                out.append("lore=").append(json(client, line)).append('\n');
            }
        }
        out.append("\ntooltip (basic, as the client builds it)\n");
        for (Text line : stack.getTooltip(Item.TooltipContext.create(client.world), client.player, TooltipType.BASIC)) {
            out.append("tooltip=").append(json(client, line)).append('\n');
        }
        NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (custom != null && !custom.isEmpty()) {
            out.append("\ncustom_data=").append(escape(custom.copyNbt().toString())).append('\n');
        }
        String text = out.toString();
        client.keyboard.setClipboard(text);
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve(Tacklebox.MOD_ID).resolve("guidump");
            Files.createDirectories(dir);
            Path file = dir.resolve("item-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
            Files.writeString(file, text);
            Tacklebox.LOGGER.info("[guidump] saved {}", file);
            say("styled dump copied, saved to guidump/" + file.getFileName(), Formatting.AQUA);
        } catch (IOException e) {
            Tacklebox.LOGGER.warn("[guidump] could not save", e);
            say("styled dump copied to the clipboard (saving failed)", Formatting.RED);
        }
    }

    /** A Text as JSON with non-ascii characters escaped, so fonts and glyphs show up in the dump. */
    private static String json(MinecraftClient client, Text text) {
        if (text == null) {
            return "(none)";
        }
        return TextCodecs.CODEC.encodeStart(client.world.getRegistryManager().getOps(JsonOps.INSTANCE), text).result()
                .map(json -> escape(json.toString())).orElse("(could not encode) " + escape(text.getString()));
    }

    /** Private-use and other non-ascii characters as \\uXXXX, so glyphs show up in a dump. */
    private static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(c < 0x20 || c > 0x7E ? String.format(java.util.Locale.ROOT, "\\u%04X", (int) c) : String.valueOf(c));
        }
        return sb.toString();
    }

    /**
     * Everything about the open screen worth sending along: class, title (raw and with codepoints) and each
     * container slot's item, name, lore and custom_data. Saved to config/tacklebox/guidump and copied to
     * the clipboard. Works in the mods own menu screens too, because they sit on the servers real chest.
     */
    public static void gui(Screen screen) {
        MinecraftClient client = MinecraftClient.getInstance();
        StringBuilder out = new StringBuilder();
        Text title = screen instanceof FishMenuScreen menu ? menu.original().getTitle() : screen.getTitle();
        out.append("screen ").append(screen.getClass().getName()).append('\n');
        if (screen instanceof FishMenuScreen) {
            out.append("shown as the mods menu screen over the chest above\n");
        }
        out.append("title text=\"").append(escape(title.getString())).append("\"\n");
        out.append("title plain=\"").append(Glyphs.plain(title)).append("\" menuName=").append(Glyphs.menuName(title)).append('\n');
        ScreenHandler handler = client.player == null ? null : client.player.currentScreenHandler;
        if (handler != null && handler != client.player.playerScreenHandler) {
            out.append("handler ").append(handler.getClass().getSimpleName()).append(" syncId=").append(handler.syncId).append('\n');
            for (Slot slot : handler.slots) {
                if (slot.inventory instanceof PlayerInventory || slot.getStack().isEmpty()) {
                    continue;
                }
                ItemStack stack = slot.getStack();
                if (stack.isOf(net.minecraft.item.Items.BREAD) && (stack.get(DataComponentTypes.LORE) == null || stack.get(DataComponentTypes.LORE).lines().isEmpty()) && "Bread".equals(stack.getName().getString())) {
                    continue; // the server's placeholder for an empty slot
                }
                out.append("\nslot ").append(slot.id).append(' ').append(Registries.ITEM.getId(stack.getItem())).append(" x").append(stack.getCount()).append('\n');
                out.append("  name=\"").append(escape(stack.getName().getString())).append("\"\n");
                LoreComponent lore = stack.get(DataComponentTypes.LORE);
                if (lore != null) {
                    for (Text line : lore.lines()) {
                        out.append("  lore=\"").append(escape(line.getString())).append("\"\n");
                    }
                }
                NbtComponent custom = stack.get(DataComponentTypes.CUSTOM_DATA);
                if (custom != null && !custom.isEmpty()) {
                    out.append("  custom_data=").append(custom.copyNbt()).append('\n');
                }
            }
        }
        String text = out.toString();
        client.keyboard.setClipboard(text);
        try {
            Path dir = FabricLoader.getInstance().getConfigDir().resolve(Tacklebox.MOD_ID).resolve("guidump");
            Files.createDirectories(dir);
            Path file = dir.resolve(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".txt");
            Files.writeString(file, text);
            Tacklebox.LOGGER.info("[guidump] saved {}", file);
            toast(client, "gui dump copied, saved to " + dir.getFileName() + "/" + file.getFileName());
        } catch (IOException e) {
            Tacklebox.LOGGER.warn("[guidump] could not save", e);
            toast(client, "gui dump copied to the clipboard (saving failed)");
        }
    }

    private static void toast(MinecraftClient client, String line) {
        client.getToastManager().add(new net.minecraft.client.toast.SystemToast(net.minecraft.client.toast.SystemToast.Type.PERIODIC_NOTIFICATION,
                Foe.text("gui dump"), Text.literal(line)));
    }

    private static void say(String line, Formatting color) {
        Tacklebox.LOGGER.info("[debug] {}", line);
        ClientPlayerEntity self = MinecraftClient.getInstance().player;
        if (self != null) {
            self.sendMessage(Text.literal(line).formatted(color), false);
        }
    }
}
