package dev.landofif.dofisaddons.stats;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Uuids;

import java.util.Locale;
import java.util.UUID;

/**
 * What a FishOnMC fish item says about itself, read from its custom data (the server stamps every
 * caught fish with a unique id, who caught it, species, rarity, size, variant, weight and so on).
 */
public record FishInfo(UUID id, UUID catcher, String species, String rarity, String size, String variant,
                       float weight, float length, float value, float xp, String location) {

    /** Returns null for anything that isn't a real caught fish (shop previews, other items). */
    public static FishInfo from(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) {
            return null;
        }
        NbtCompound nbt = data.copyNbt();
        if (nbt.getBoolean("shopitem").orElse(false)) {
            return null;
        }
        String species = nbt.getString("fish").orElse("");
        int[] id = nbt.getIntArray("id").orElse(new int[0]);
        int[] catcher = nbt.getIntArray("catcher").orElse(new int[0]);
        if (species.isEmpty() || id.length != 4 || catcher.length != 4) {
            return null;
        }
        return new FishInfo(
                Uuids.toUuid(id),
                Uuids.toUuid(catcher),
                species,
                lower(nbt.getString("rarity").orElse("")),
                lower(nbt.getString("size").orElse("")),
                lower(nbt.getString("variant").orElse("normal")),
                nbt.getFloat("weight").orElse(0f),
                nbt.getFloat("length").orElse(0f),
                nbt.getFloat("value").orElse(0f),
                nbt.getFloat("xp").orElse(0f),
                lower(nbt.getString("location").orElse("")));
    }

    private static String lower(String s) {
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
