package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

/** An items rarity colour from its own custom_data {@code rarity} (fishing rods excepted, like foecollab). -1 if the server
 * gave it no rarity, which is every filler, button and arrow of a menu. Read by tacklebox itself since 2026-10-30. */
public final class Rarity {
    private Rarity() {}

    /** A see-through circle in the items rarity colour, 16x16 at x, y, if the item has a rarity. */
    public static void circle(DrawContext ctx, int x, int y, ItemStack stack) {
        int rgb = color(stack);
        if (rgb < 0) {
            return;
        }
        disc(ctx, x, y, alpha() << 24 | rgb);
    }

    /** The rarity circles setting's mark behind an item at x, y: the circle, or a square around it. With the gradient
     * on, the square is clear at its top and reaches the opacity setting at its bottom. */
    public static void mark(DrawContext ctx, int x, int y, ItemStack stack, boolean square) {
        if (!square) {
            circle(ctx, x, y, stack);
            return;
        }
        int rgb = color(stack);
        if (rgb >= 0) {
            int[] r = markSquare(x, y);
            if (TackleboxSettings.get().rarityGradient) {
                ctx.fillGradient(r[0], r[1], r[2], r[3], rgb, alpha() << 24 | rgb);
            } else {
                ctx.fill(r[0], r[1], r[2], r[3], alpha() << 24 | rgb);
            }
        }
    }

    /** The square around a 16x16 item at x, y, as {x1, y1, x2, y2}: 14x14 and centred on the item, so in the 18 pixel
     * slot grid neighbouring squares keep a 4 pixel gap. */
    public static int[] markSquare(int x, int y) {
        return new int[]{x + 1, y + 1, x + 15, y + 15};
    }

    /** The opacity setting in alpha (0-255), 40% gives the old 0x66. */
    private static int alpha() {
        return TackleboxSettings.get().rarityOpacity * 255 / 100;
    }

    /** A filled circle, 16x16 at x, y. */
    public static void disc(DrawContext ctx, int x, int y, int argb) {
        for (int row = 0; row < 16; row++) {
            double dy = row + 0.5 - 8;
            int half = (int) Math.round(Math.sqrt(64 - dy * dy));
            ctx.fill(x + 8 - half, y + row, x + 8 + half, y + row + 1, argb);
        }
    }

    public static int color(ItemStack stack) {
        if (stack.isEmpty() || stack.isOf(Items.FISHING_ROD)) {
            return -1;
        }
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data == null ? -1 : color(data.copyNbt().getString("rarity").orElse(""));
    }

    /** RGB of a rarity id from custom_data (research section 2), -1 for none or one we do not know. */
    public static int color(String rarity) {
        return switch (rarity) {
            case "common", "rare", "epic", "legendary", "mythical" -> dev.landofif.tacklebox.hud.BaitInfo.rarityColor(rarity);
            case "special" -> 0xDD7ACF;
            default -> -1;
        };
    }
}
