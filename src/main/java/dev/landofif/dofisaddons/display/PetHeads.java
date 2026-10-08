package dev.landofif.dofisaddons.display;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.landofif.dofisaddons.DofiSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.ProfileComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;

import java.util.Locale;

/**
 * The head of each pet type, for the totem pop. The server gives pet items a player head with a texture per pet, and the
 * chat line only names the type, so the texture is remembered from any pet item that shows up in the inventory or an open
 * menu (pets menu, vaults, and the vaults saved earlier). Pets wearing a skin are skipped, their head is the skin's.
 */
public final class PetHeads {
    private static int ticks;
    private static boolean vaultsRead;

    private PetHeads() {}

    /** "Sea Turtle" and "seaturtle" are the same pet. */
    private static String key(String type) {
        return type.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || ++ticks % 20 != 0) {
            return;
        }
        boolean dirty = false;
        if (!vaultsRead) { // pets sitting in vaults you opened before
            vaultsRead = true;
            for (int n = 1; n <= dev.landofif.dofisaddons.menu.Vaults.COUNT; n++) {
                var items = dev.landofif.dofisaddons.menu.Vaults.get(n);
                for (int i = 0; items != null && i < items.size(); i++) {
                    dirty |= learn(items.get(i));
                }
            }
        }
        for (var slot : client.player.currentScreenHandler.slots) {
            dirty |= learn(slot.getStack());
        }
        if (dirty) {
            DofiSettings.save();
        }
    }

    private static boolean learn(ItemStack stack) {
        var cd = stack.get(DataComponentTypes.CUSTOM_DATA);
        var profile = stack.get(DataComponentTypes.PROFILE);
        if (cd == null || profile == null) {
            return false;
        }
        var nbt = cd.copyNbt();
        String type = nbt.getString("type", "");
        String pet = key(nbt.getString("pet", ""));
        if (!type.equals("pet") || pet.isEmpty() || nbt.contains("skin")) {
            return false;
        }
        var json = ProfileComponent.CODEC.encodeStart(JsonOps.INSTANCE, profile).result();
        if (json.isEmpty() || json.get().toString().equals(DofiSettings.get().petHeads.get(pet))) {
            return false;
        }
        DofiSettings.get().petHeads.put(pet, json.get().toString());
        return true;
    }

    private static java.util.Map<String, String> bundled;

    /** The heads shipped with the mod ({@code data/dofisaddons/petheads.json}), so nobody has to meet every pet type first. */
    private static java.util.Map<String, String> bundled() {
        if (bundled == null) {
            bundled = new java.util.HashMap<>();
            try (var in = PetHeads.class.getResourceAsStream("/data/dofisaddons/petheads.json")) {
                JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8))
                        .getAsJsonObject().entrySet().forEach(e -> bundled.put(e.getKey(), e.getValue().getAsString()));
            } catch (Exception e) {
                dev.landofif.dofisaddons.DofisAddons.LOGGER.error("Could not read the bundled pet heads", e);
            }
        }
        return bundled;
    }

    /** A player head with the pets texture, the bundled one, or a plain player head for a type that is in neither (a new pet). */
    public static ItemStack head(String type) {
        ItemStack stack = new ItemStack(Items.PLAYER_HEAD);
        String saved = DofiSettings.get().petHeads.get(key(type));
        if (saved == null) {
            saved = bundled().get(key(type));
        }
        if (saved != null) {
            ProfileComponent.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(saved)).result()
                    .ifPresent(p -> stack.set(DataComponentTypes.PROFILE, p));
        }
        return stack;
    }
}
