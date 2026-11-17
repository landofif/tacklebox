package dev.landofif.tacklebox.display;

import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalDouble;
import java.util.function.LongSupplier;

/**
 * The rating of a pet you just got, at the end of its drop title in the rating's colour ("+ Capybara Pet 93.4%"). The chat line
 * has no stats, so the title waits for a pet of that kind to turn up new in the inventory and reads the rating from the item.
 */
public final class PetRating {
    /** How long a title waits for its pet, and how long before the chat line a pet may have arrived. */
    static final long WINDOW_MS = 3000;
    public static LongSupplier clock = System::currentTimeMillis;

    /** A pet item in the inventory: its id, its kind ("lynx") and its rating in percent. */
    public record Item(String id, String pet, double rating) {}

    private static final class Known {
        final long since;
        boolean used;

        Known(long since) {
            this.since = since;
        }
    }

    private record Waiting(TitleOverlay.Shown title, String pet, long at) {}

    /** The pets in the inventory by id, with when each one turned up. */
    private static final Map<String, Known> KNOWN = new HashMap<>();
    private static final List<Waiting> WAITING = new ArrayList<>();
    /** False until the first look at the inventory: the pets that are there then are not new. */
    private static boolean primed;

    private PetRating() {}

    /** "sickly" to "perfect", the server's names for a rating in percent (the steps are foecollab's {@code Pet.getConstantFromPercent}). */
    public static String word(double percent) {
        if (percent <= 20) return "sickly";
        if (percent < 30) return "bad";
        if (percent < 40) return "below average";
        if (percent < 50) return "average";
        if (percent < 60) return "good";
        if (percent < 80) return "great";
        if (percent < 90) return "excellent";
        if (percent < 100) return "amazing";
        return "perfect";
    }

    /** The colour the server writes that name in. */
    static int color(double percent) {
        return switch (word(percent)) {
            case "sickly" -> 0x74403B;
            case "bad" -> 0xFF5555;
            case "below average" -> 0xFCFC54;
            case "average" -> 0xFCA800;
            case "good" -> 0x54FC54;
            case "great" -> 0x00A800;
            case "excellent" -> 0x54FCFC;
            case "amazing" -> 0xFC54FC;
            default -> 0xA800A8;
        };
    }

    /** What goes at the end of the title: " 93.4%" in the rating's colour, bold like the rest of the title. */
    static String percent(double rating) {
        return String.format(Locale.ROOT, "&#%06x&l %.1f%%", color(rating), rating);
    }

    /** "Sea Turtle" and "seaturtle" are the same pet. */
    private static String key(String pet) {
        return pet == null ? "" : pet.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    /** A pet drop title was shown: its rating follows when the pet is in the inventory. */
    public static void await(TitleOverlay.Shown title, String pet) {
        if (title != null && !key(pet).isEmpty()) {
            WAITING.add(new Waiting(title, key(pet), clock.getAsLong()));
        }
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null) {
            reset();
            return;
        }
        List<Item> items = new ArrayList<>();
        var inventory = client.player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            var cd = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (cd == null || stack.get(DataComponentTypes.PROFILE) == null) {
                continue; // a pet is a head
            }
            NbtCompound nbt = cd.copyNbt();
            OptionalDouble rating = PetSale.rating(nbt);
            if (!nbt.getString("type", "").equals("pet") || rating.isEmpty()) {
                continue;
            }
            NbtElement id = nbt.get("id");
            items.add(new Item(id == null ? nbt.toString() : id.toString(), nbt.getString("pet", ""), rating.getAsDouble()));
        }
        scan(items);
    }

    /** The pets in the inventory now: remembers the new ones, and gives a waiting title the rating of its pet. */
    public static void scan(List<Item> items) {
        long now = clock.getAsLong();
        Map<String, Known> before = new HashMap<>(KNOWN);
        KNOWN.clear();
        for (Item item : items) {
            Known known = before.get(item.id());
            KNOWN.put(item.id(), known != null ? known : new Known(primed ? now : Long.MIN_VALUE));
        }
        primed = true;
        WAITING.removeIf(w -> now - w.at() > WINDOW_MS);
        WAITING.removeIf(w -> {
            Item found = null;
            for (Item item : items) {
                Known known = KNOWN.get(item.id());
                if (!known.used && known.since >= w.at() - WINDOW_MS && key(item.pet()).equals(w.pet())
                        && (found == null || known.since > KNOWN.get(found.id()).since)) {
                    found = item;
                }
            }
            if (found == null) {
                return false;
            }
            KNOWN.get(found.id()).used = true;
            TitleOverlay.append(w.title(), percent(found.rating()));
            return true;
        });
    }

    /** Left the server (and tests): nothing known, nothing waiting. */
    public static void reset() {
        KNOWN.clear();
        WAITING.clear();
        primed = false;
    }
}
