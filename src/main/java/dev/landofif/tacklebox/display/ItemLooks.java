package dev.landofif.tacklebox.display;

import com.google.gson.JsonParser;
import com.mojang.serialization.JsonOps;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.Matcher;
import net.minecraft.component.ComponentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * How the server's own items look, by name, so the totem pop can show a rare catch that is no longer in the inventory.
 * The server draws them as vanilla items with a model from its pack (Bigfoot Tooth is a stick with custom_model_data 2),
 * so only the item and the components that change the look are kept. Learned in {@link PetHeads}' slot pass.
 */
public final class ItemLooks {
    private ItemLooks() {}

    /** "Lightning in a Bottle" and "lightninginabottle" are the same item: letters and digits of the normalized name. */
    public static String key(String name) {
        return Matcher.normalize(name).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    /**
     * Remembers the look of a server item (custom_data with a type; fish have none, pets have their own heads). The first
     * stack of a name in one pass ({@code seen}) wins, so two looks of one name don't take turns being saved.
     * @return true if something changed
     */
    static boolean learn(ItemStack stack, Set<String> seen) {
        var cd = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (cd == null) {
            return false;
        }
        String type = cd.copyNbt().getString("type", "");
        String key = key(stack.getName().getString());
        if (type.isEmpty() || type.equals("pet") || key.isEmpty() || !seen.add(key)) {
            return false;
        }
        ItemStack look = new ItemStack(stack.getItem());
        for (ComponentType<?> t : List.of(DataComponentTypes.ITEM_MODEL, DataComponentTypes.CUSTOM_MODEL_DATA, DataComponentTypes.PROFILE,
                DataComponentTypes.DYED_COLOR, DataComponentTypes.ENCHANTMENT_GLINT_OVERRIDE)) {
            copy(stack, look, t);
        }
        var json = ItemStack.CODEC.encodeStart(JsonOps.INSTANCE, look).result();
        if (json.isEmpty() || json.get().toString().equals(TackleboxSettings.get().itemLooks.get(key))) {
            return false;
        }
        TackleboxSettings.get().itemLooks.put(key, json.get().toString());
        return true;
    }

    private static <T> void copy(ItemStack from, ItemStack to, ComponentType<T> type) {
        T value = from.get(type);
        if (value != null) {
            to.set(type, value);
        }
    }

    public static boolean has(String key) {
        return TackleboxSettings.get().itemLooks.containsKey(key);
    }

    /** The remembered look of {@code key}, or null. */
    public static ItemStack stack(String key) {
        String saved = TackleboxSettings.get().itemLooks.get(key);
        return saved == null ? null : ItemStack.CODEC.parse(JsonOps.INSTANCE, JsonParser.parseString(saved)).result().orElse(null);
    }
}
