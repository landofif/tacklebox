package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.trigger.Matcher;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Locale;
import java.util.function.Predicate;

/** The vanilla totem of undying animation (the floating item only, no particles or sound: triggers have their own sound) with any item. */
public final class TotemPop {
    private TotemPop() {}

    /** Size multiplier of the pop being shown, read by {@code InGameOverlayRendererMixin}. */
    public static float scale = 1f;

    /** How long a pop waits for a caught item to land in the inventory. */
    static final long WAIT_MS = 2000;
    private static String waitingFor;
    private static String waitingSpec;
    private static long waitUntil;
    private static String last = "";

    /** Where the item of a pop comes from, in the order they are tried. */
    public enum Source {
        /** An item id, or the totem for an empty spec. */
        ID,
        /** {@code pet:<type>} or {@code shard:<climate>}: a head from {@link PetHeads}. */
        HEAD,
        /** An item name: a copy of the stack in the inventory, so the server pack draws it as the real thing. */
        INVENTORY,
        /** An item name: its look from {@link ItemLooks}. */
        REMEMBERED,
        /** An item name in neither: wait for it to land, then {@link #fallback}. */
        WAIT
    }

    /** The spec of the last pop asked for (tests). */
    public static String last() {
        return last;
    }

    /** {@code spec} is an item id, {@code pet:<type>}, {@code shard:<climate>} or an item name ("Lightning in a Bottle"). */
    public static void show(String spec, double scale) {
        last = spec = spec.trim();
        waitingFor = null; // a newer pop replaces one still waiting
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc == null || mc.player == null) {
            return;
        }
        TotemPop.scale = (float) scale;
        String key = ItemLooks.key(spec);
        switch (source(spec, TotemPop::isItem, k -> find(mc, k) != null, ItemLooks::has)) {
            case ID -> mc.gameRenderer.showFloatingItem(item(spec));
            case HEAD -> mc.gameRenderer.showFloatingItem(head(spec));
            case INVENTORY -> mc.gameRenderer.showFloatingItem(find(mc, key).copyWithCount(1));
            case REMEMBERED -> {
                ItemStack look = ItemLooks.stack(key);
                mc.gameRenderer.showFloatingItem(look != null ? look : item(fallback(spec)));
            }
            case WAIT -> {
                waitingFor = key;
                waitingSpec = spec;
                waitUntil = System.currentTimeMillis() + WAIT_MS;
            }
        }
    }

    public static Source source(String spec, Predicate<String> isItem, Predicate<String> inInventory, Predicate<String> remembered) {
        String lower = spec.trim().toLowerCase(Locale.ROOT);
        if (lower.startsWith("pet:") || lower.startsWith("shard:")) {
            return Source.HEAD;
        }
        String key = ItemLooks.key(spec);
        if (key.isEmpty() || isItem.test(lower)) {
            return Source.ID;
        }
        if (inInventory.test(key)) {
            return Source.INVENTORY;
        }
        return remembered.test(key) ? Source.REMEMBERED : Source.WAIT;
    }

    /** What an item name pops when it never landed: the bundled head for a shard ("Semi-Arid Shard" -> shard:semi-arid), else the totem. */
    public static String fallback(String spec) {
        String name = Matcher.normalize(spec).toLowerCase(Locale.ROOT);
        return name.endsWith(" shard") ? "shard:" + name.substring(0, name.length() - " shard".length()) : "minecraft:totem_of_undying";
    }

    /** Pops a waiting item once it is in the inventory, or the fallback after {@link #WAIT_MS}. Called every client tick. */
    public static void tick(MinecraftClient mc) {
        if (waitingFor == null) {
            return;
        }
        if (mc.player == null) {
            waitingFor = null;
            return;
        }
        ItemStack found = find(mc, waitingFor);
        if (found != null) {
            waitingFor = null;
            mc.gameRenderer.showFloatingItem(found.copyWithCount(1));
        } else if (System.currentTimeMillis() >= waitUntil) {
            waitingFor = null;
            String fallback = fallback(waitingSpec);
            mc.gameRenderer.showFloatingItem(fallback.startsWith("shard:") ? PetHeads.head(fallback) : item(fallback));
        }
    }

    private static boolean isItem(String id) {
        Identifier parsed = Identifier.tryParse(id);
        return parsed != null && Registries.ITEM.containsId(parsed);
    }

    private static ItemStack item(String id) {
        Identifier parsed = Identifier.tryParse(id.toLowerCase(Locale.ROOT));
        return parsed != null && Registries.ITEM.containsId(parsed) ? new ItemStack(Registries.ITEM.get(parsed)) : new ItemStack(Items.TOTEM_OF_UNDYING);
    }

    private static ItemStack head(String spec) {
        return spec.toLowerCase(Locale.ROOT).startsWith("pet:") ? PetHeads.head(spec.substring(4)) : PetHeads.head(spec);
    }

    /** The first inventory stack whose name has {@code key}. */
    private static ItemStack find(MinecraftClient mc, String key) {
        var inventory = mc.player.getInventory();
        for (int i = 0; i < inventory.size(); i++) {
            ItemStack stack = inventory.getStack(i);
            if (!stack.isEmpty() && ItemLooks.key(stack.getName().getString()).equals(key)) {
                return stack;
            }
        }
        return null;
    }
}
