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

    /** The stack this mod last popped. Only that one gets {@link #spin}, so a real totem of undying stays vanilla. */
    static Object shown;
    /** Ticks vanilla's floating item animation lasts. */
    static final int TICKS = 40;
    /** How far the item keeps turning while it leaves (vanilla turns 900 back). luna: "it should spin like 300 degrees". */
    static final float LEAVE_SPIN = 300f;

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
            case ID -> pop(mc, item(spec));
            case HEAD -> pop(mc, head(spec));
            case INVENTORY -> pop(mc, find(mc, key).copyWithCount(1));
            case REMEMBERED -> {
                ItemStack look = ItemLooks.stack(key);
                pop(mc, look != null ? look : item(fallback(spec)));
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
            pop(mc, found.copyWithCount(1));
        } else if (System.currentTimeMillis() >= waitUntil) {
            waitingFor = null;
            String fallback = fallback(waitingSpec);
            pop(mc, fallback.startsWith("shard:") ? PetHeads.head(fallback) : item(fallback));
        }
    }

    private static void pop(MinecraftClient mc, ItemStack stack) {
        shown = stack;
        mc.gameRenderer.showFloatingItem(stack);
    }

    /** Vanilla's progress curve over the animation, {@code f} 0 to 1: about 0.47 by tick 12, near 0.5 until tick 30, then 1. */
    public static float curve(float f) {
        float f2 = f * f;
        float f3 = f * f2;
        return 10.25f * f3 * f2 - 24.95f * f2 * f2 + 25.5f * f3 - 13.8f * f2 + 4f * f;
    }

    /**
     * Y spin in degrees at {@code f}: vanilla's 900 * |sin| while the item comes in and holds, then {@link #LEAVE_SPIN} more the
     * same way while it leaves, so it ends at 1200 instead of turning 900 back to 0.
     */
    public static float spin(float f) {
        float j = curve(f);
        float sin = (float) Math.abs(Math.sin(j * Math.PI));
        return j <= 0.5f ? 900f * sin : 900f + LEAVE_SPIN * (1f - sin);
    }

    /**
     * The spin {@code InGameOverlayRendererMixin} draws: {@link #spin} while this mod's pop leaves, else vanilla's {@code degrees}.
     * {@code timer} is vanilla's floatingItemTimer (40 down to 0), {@code tickProgress} the frame's part of a tick.
     */
    public static float spin(Object floatingItem, int timer, float tickProgress, float degrees) {
        float f = (TICKS - timer + tickProgress) / TICKS;
        return floatingItem != null && floatingItem == shown && curve(f) > 0.5f ? spin(f) : degrees;
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
