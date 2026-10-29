package dev.landofif.tacklebox.compat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Method;

/**
 * Foecollab forgets the equipped pet when its login scan misses (the pet has to be in the saved slot and its entity
 * has to show up within 5 seconds, else "Did not find Pet" and the hud says no pet until you re-equip). It does keep
 * the pet it saved, so this finds that pet (by id) anywhere in the inventory and puts it back. All by reflection.
 */
public final class FoePet {
    private static int ticks;

    private FoePet() {}

    /** Called at the end of foecollab's pet tick with its PetEquipHandler. */
    public static void restore(Object handler, MinecraftClient client) {
        if (client.player == null || ++ticks % 20 != 0) {
            return;
        }
        try {
            Class<?> handlerClass = handler.getClass();
            Object status = handlerClass.getField("petStatus").get(handler);
            if (!((Enum<?>) status).name().equals("NO_PET")) {
                return;
            }
            Object profiles = Class.forName("io.github.foecollab.handler.ProfileDataHandler").getMethod("instance").invoke(null);
            Object data = profiles.getClass().getField("profileData").get(profiles);
            Object saved = data.getClass().getField("equippedPet").get(data);
            if (saved == null || data.getClass().getField("equippedPetSlot").getInt(data) < 0) {
                return; // really unequipped
            }
            Object id = saved.getClass().getField("id").get(saved);
            Class<?> petClass = saved.getClass();
            Method getPet = petClass.getMethod("getPet", ItemStack.class);
            var inventory = client.player.getInventory();
            for (int i = 0; i < inventory.size(); i++) {
                ItemStack stack = inventory.getStack(i);
                Object pet = stack.isEmpty() ? null : getPet.invoke(null, stack);
                if (pet == null || !id.equals(petClass.getField("id").get(pet))) {
                    continue;
                }
                profiles.getClass().getMethod("updatePet", petClass, int.class).invoke(profiles, pet, i);
                handlerClass.getField("currentPetItem").set(handler, stack.copy());
                Method items = handlerClass.getDeclaredMethod("updateCurrentPetItemItem", ItemStack.class);
                items.setAccessible(true);
                items.invoke(handler, stack.copy());
                @SuppressWarnings({"unchecked", "rawtypes"})
                Object has = Enum.valueOf((Class) status.getClass(), "HAS_PET");
                handlerClass.getField("petStatus").set(handler, has);
                return;
            }
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            // foecollab changed; leave its hud alone
        }
    }
}
