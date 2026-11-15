package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * The wrong pet and armor warning ({@link GearCheck} decides what it says): a hud that shows while your equipped pet or worn
 * armor does not fit the place you are at, and one chat line when you arrive there. The equipped pet is learned from the
 * server's own PETS lines and saved ({@code equippedPet}); until one came it is not known and only armor is checked.
 */
public final class GearWarning {
    private static final List<TextHud.Row> SAMPLE = List.of(
            new TextHud.Row("wrong pet", "use wolf · in inventory", GearCheck.RED, 0),
            new TextHud.Row("wrong armor", "savanna chestplate · need continental", GearCheck.RED, 0));
    private static int ticks;

    private GearWarning() {}

    /** A PETS line tells which pet is equipped. {@code s} is the normalized line; true when it changed (to be saved). */
    public static boolean onChat(String s) {
        String pet = GearCheck.equipLine(s);
        TackleboxSettings set = TackleboxSettings.get();
        if (pet == null || pet.equals(set.equippedPet)) {
            return false;
        }
        set.equippedPet = pet;
        return true;
    }

    /** Twice a second: the chat line a moment after arriving somewhere with the wrong pet or armor. */
    public static void tick(MinecraftClient client) {
        if (ticks++ % 10 != 0) {
            return;
        }
        TackleboxSettings s = TackleboxSettings.get();
        if (client.player == null || !s.gearHud.on || !TriggerEngine.isOnFishOnMc()) {
            GearCheck.reset();
            return;
        }
        GearCheck.Place here = here(client);
        if (GearCheck.arrived(here == null ? "" : here.id(), System.currentTimeMillis()) && s.gearChat) {
            String line = GearCheck.chatLine(here, s.equippedPet, hasPet(client, here), worn(client));
            if (line != null) {
                client.player.sendMessage(Foe.text("tacklebox » ").withColor(Foe.TITLE & 0xFFFFFF)
                        .append(net.minecraft.text.Text.literal(line).withColor(GearCheck.RED & 0xFFFFFF)), false);
            }
        }
    }

    /** The fishing location you are at, or null at the Cypress Lake spawn, on the crew island and in unknown places. */
    private static GearCheck.Place here(MinecraftClient client) {
        if (GearCheck.spawnHub(ServerInfo.locationId, client.player.getX(), client.player.getZ())) {
            return null;
        }
        return GearCheck.place(ServerInfo.locationId);
    }

    private static boolean hasPet(MinecraftClient client, GearCheck.Place here) {
        if (here == null) {
            return false;
        }
        for (ItemStack stack : client.player.getInventory().getMainStacks()) {
            NbtComponent c = stack.isEmpty() ? null : stack.get(DataComponentTypes.CUSTOM_DATA);
            if (c != null && here.petId().equals(GearCheck.petId(c.copyNbt().getString("pet").orElse("")))) {
                return true;
            }
        }
        return false;
    }

    private static List<GearCheck.Armor> worn(MinecraftClient client) {
        List<GearCheck.Armor> out = new ArrayList<>();
        EquipmentSlot[] slots = {EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET};
        String[] names = {"chestplate", "leggings", "boots"};
        for (int i = 0; i < slots.length; i++) {
            ItemStack stack = client.player.getEquippedStack(slots[i]);
            NbtComponent c = stack.isEmpty() ? null : stack.get(DataComponentTypes.CUSTOM_DATA);
            if (c != null) {
                out.add(new GearCheck.Armor(names[i], c.copyNbt().getString("name").orElse("")));
            }
        }
        return out;
    }

    public static void renderHud(DrawContext ctx) {
        if (TackleboxSettings.get().gearHud.on && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}, or null while all fits; a preview shows a sample then. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        TackleboxSettings s = TackleboxSettings.get();
        MinecraftClient client = MinecraftClient.getInstance();
        List<TextHud.Row> rows = List.of();
        if (client.player != null) {
            GearCheck.Place here = here(client);
            rows = GearCheck.rows(here, s.equippedPet, hasPet(client, here), worn(client));
        }
        return TextHud.draw(ctx, s.gearHud, "warning", rows.isEmpty() && preview ? SAMPLE : rows);
    }
}
