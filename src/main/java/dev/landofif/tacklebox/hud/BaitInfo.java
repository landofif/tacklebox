package dev.landofif.tacklebox.hud;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * The bait or lure on the rod you hold (ported from fishutils' bait counter). The rod's custom_data has
 * it under {@code activeBait[0].components."minecraft:custom_data"}: {@code counter} is the live uses
 * left (the lore "uses: x/y" line lags behind), with {@code totalUses}, {@code name} and {@code rarity}.
 */
public final class BaitInfo {
    public record Bait(String name, int left, int total, String rarity) {}

    private BaitInfo() {}

    /** The bait on the rod in either hand, or null (no rod held, or nothing equipped). */
    public static Bait current() {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return null;
        }
        ItemStack rod = player.getMainHandStack().isOf(Items.FISHING_ROD) ? player.getMainHandStack()
                : player.getOffHandStack().isOf(Items.FISHING_ROD) ? player.getOffHandStack() : null;
        if (rod == null) {
            return null;
        }
        NbtComponent custom = rod.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || custom.isEmpty()) {
            return null;
        }
        NbtCompound data = custom.copyNbt().getList("activeBait")
                .flatMap(list -> list.getCompound(0))
                .flatMap(entry -> entry.getCompound("components"))
                .flatMap(components -> components.getCompound("minecraft:custom_data"))
                .orElse(null);
        if (data == null) {
            return null;
        }
        String name = data.getString("name").orElse("bait").replace('_', ' ').trim();
        return new Bait(name.isEmpty() ? "bait" : name, data.getInt("counter").orElse(0),
                data.getInt("totalUses").orElse(0), data.getString("rarity").orElse(""));
    }

    /** RGB of a fishonmc rarity name (research section 2), white if unknown. */
    public static int rarityColor(String rarity) {
        return switch (rarity.toLowerCase(Locale.ROOT)) {
            case "common" -> 0xFFFFFF;
            case "rare" -> 0x2B85C4;
            case "epic" -> 0x1CD832;
            case "legendary" -> 0xD98103;
            case "mythical" -> 0xC93832;
            default -> 0xFFFFFF;
        };
    }

    /** "13/30" in white, gold when a quarter or less is left, red when nearly out. */
    public static Text usesColored(Bait b) {
        int color = 0xFFFFFF;
        if (b.total > 0) {
            float ratio = b.left / (float) b.total;
            if (b.left <= 2 || ratio <= 0.10f) {
                color = 0xFF5555;
            } else if (ratio <= 0.25f) {
                color = 0xFFAA00;
            }
        }
        String uses = b.left + (b.total > 0 ? "/" + b.total : "");
        return Text.literal(uses).withColor(color);
    }
}
