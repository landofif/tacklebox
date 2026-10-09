package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.compat.FoeRarity;
import dev.landofif.dofisaddons.display.SlotBox;
import dev.landofif.dofisaddons.ui.Hypr;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtOps;

/**
 * Your chestplate, leggings and boots, and the reel, pole and line of your rod, as two rounded groups above the hotbar
 * (the mods own, so it needs nothing from foecollab). The rod parts are read from the rod's custom_data
 * ({@code reel}, {@code pole}, {@code line}: a list with one serialized stack) whenever a rod is in a hand, and kept after.
 */
public final class EquipmentRow {
    private static final String[] PART = {"reel", "pole", "line"};
    private static final String[] LETTER = {"ᴄ", "ʟ", "ʙ", "ʀ", "ᴘ", "ʟ"};
    private static final ItemStack[] parts = {ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY};
    private static boolean warned;

    private EquipmentRow() {}

    /** Reads the rod's parts, a few times a second. */
    public static void tick(MinecraftClient client) {
        ClientPlayerEntity player = client.player;
        if (player == null || client.world == null || player.age % 10 != 0) {
            return;
        }
        ItemStack rod = player.getMainHandStack().isOf(Items.FISHING_ROD) ? player.getMainHandStack()
                : player.getOffHandStack().isOf(Items.FISHING_ROD) ? player.getOffHandStack() : null;
        NbtComponent custom = rod == null ? null : rod.get(DataComponentTypes.CUSTOM_DATA);
        if (custom == null || custom.isEmpty()) {
            return;
        }
        NbtCompound data = custom.copyNbt();
        var ops = client.world.getRegistryManager().getOps(NbtOps.INSTANCE);
        for (int i = 0; i < PART.length; i++) {
            NbtCompound entry = data.getList(PART[i]).flatMap(l -> l.getCompound(0)).orElse(null);
            if (entry == null) {
                parts[i] = ItemStack.EMPTY;
                continue;
            }
            var decoded = ItemStack.OPTIONAL_CODEC.parse(ops, entry);
            parts[i] = decoded.result().orElse(ItemStack.EMPTY);
            if (decoded.error().isPresent() && !warned) {
                warned = true;
                DofisAddons.LOGGER.warn("equipment row: could not read the rod's {}: {}", PART[i], decoded.error().get().message());
            }
        }
    }

    public static void renderHud(DrawContext ctx) {
        DofiSettings s = DofiSettings.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        ClientPlayerEntity player = mc.player;
        if (!s.equipHud || player == null) {
            return;
        }
        ItemStack[] items = {player.getEquippedStack(EquipmentSlot.CHEST), player.getEquippedStack(EquipmentSlot.LEGS),
                player.getEquippedStack(EquipmentSlot.FEET), parts[0], parts[1], parts[2]};
        boolean any = false;
        for (ItemStack i : items) {
            any |= !i.isEmpty();
        }
        if (!any) {
            return;
        }
        int cx = ctx.getScaledWindowWidth() / 2, base = ctx.getScaledWindowHeight() - 28;
        int[] off = s.equipOffset();
        float k = s.equipScale / 100f;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx + s.hotbarX + s.equipX + off[0], base + s.hotbarY + s.equipY + off[1]);
        m.scale(k, k);
        m.translate(-cx, -base);
        int p = s.equipPitch();
        DofiSettings.BoxLook look = s.equipLook == DofiSettings.BoxLook.DEFAULT ? DofiSettings.BoxLook.HYPR : s.equipLook;
        if (look != DofiSettings.BoxLook.OFF) {
            SlotBox.draw(ctx, cx - 3 * p + 1, base - 22, cx - 1, base + 2, look);
            SlotBox.draw(ctx, cx + 1, base - 22, cx + 3 * p - 1, base + 2, look);
        }
        for (int i = 0; i < 6; i++) {
            int x = cx - 3 * p + i * p + (p - 16) / 2, y = base - 18;
            ItemStack st = items[i];
            if (st.isEmpty()) {
                ctx.drawText(mc.textRenderer, LETTER[i], x + 8 - mc.textRenderer.getWidth(LETTER[i]) / 2, y + 4, Hypr.alpha(Hypr.MUTED, 0.8f), false);
                continue;
            }
            int rgb = color(st);
            if (s.equipRecolor) {
                rgb = s.equipColor;
            }
            if (s.equipCircle && rgb >= 0) {
                FoeRarity.disc(ctx, x, y, 0x66000000 | rgb);
            } else if (rgb >= 0) { // a bar under the item in the rarity colour
                ctx.fill(x + 1, y + 18, x + 15, y + 19, 0xFF000000 | rgb);
            }
            ctx.drawItem(st, x, y);
        }
        m.popMatrix();
    }

    /** The rarity colour from the item's own custom_data, or -1 (plain). */
    private static int color(ItemStack st) {
        NbtComponent c = st.get(DataComponentTypes.CUSTOM_DATA);
        String rarity = c == null ? "" : c.copyNbt().getString("rarity").orElse("");
        return rarity.isEmpty() || rarity.equalsIgnoreCase("common") ? -1 : BaitInfo.rarityColor(rarity);
    }
}
