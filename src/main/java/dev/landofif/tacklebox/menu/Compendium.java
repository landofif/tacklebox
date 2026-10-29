package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which species of each compendium location you have, per variant: read from the compendium species pages (a location, then
 * its filter and pages; luna's dumps 2026-10-09) and kept up to date by the catches foecollab reports, so a catch that is new
 * for the compendium can fire a trigger and the counters stay right between visits. Reading only, nothing is clicked.
 */
public final class Compendium {
    private static final Pattern PROGRESS = Pattern.compile("(normal|gigantic|albino|melanistic|trophy|fabled) progression:\\s*\\((\\d+)\\s*/\\s*(\\d+)\\)");
    private static final Pattern HOME = Pattern.compile("^(.+?) compendium$");
    private static final Pattern TOTAL = Pattern.compile("^total:\\s*\\d");
    /** The species page marks a missing species with this in front of its name (and a gray dye for the item). */
    private static final String MISSING = "✖";

    private static final Set<String> UNMATCHED = new HashSet<>();

    private Compendium() {}

    /** "Atlantic Chub Mackerel" and "atlantic_chub_mackerel" are the same species. */
    static String key(String name) {
        return name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    /** Reads a species page when one is open: every species item with its caught or missing state. */
    public static void scan(HandledScreen<?> screen) {
        Locations.Location loc = null;
        String variant = null;
        int have = -1, total = -1;
        Map<String, Boolean> page = new LinkedHashMap<>();
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || slot.inventory instanceof PlayerInventory) {
                continue;
            }
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) {
                continue;
            }
            List<String> lines = lore.lines().stream().map(Glyphs::plain).toList();
            for (String line : lines) {
                Matcher p = PROGRESS.matcher(line);
                if (variant == null && p.find()) {
                    variant = p.group(1);
                    have = Integer.parseInt(p.group(2));
                    total = Integer.parseInt(p.group(3));
                }
                Matcher h = HOME.matcher(line);
                if (loc == null && h.find()) {
                    loc = Locations.byName(h.group(1));
                }
            }
            if (lines.stream().anyMatch(l -> TOTAL.matcher(l).find())) {
                String name = stack.getName().getString();
                String key = key(name);
                if (!key.isEmpty()) {
                    page.put(key, !name.contains(MISSING));
                }
            }
        }
        if (loc == null || variant == null || page.isEmpty()) {
            return;
        }
        TackleboxSettings s = TackleboxSettings.get();
        Map<String, Boolean> kept = s.species.computeIfAbsent(loc.id() + "|" + variant, k -> new LinkedHashMap<>());
        boolean changed = !kept.entrySet().containsAll(page.entrySet());
        kept.putAll(page);
        int[] v = s.variants.get(loc.id());
        int i = Arrays.asList(Power.VARIANTS).indexOf(variant);
        if (v != null && (v[i * 2] != have || v[i * 2 + 1] != total)) {
            v[i * 2] = have;
            v[i * 2 + 1] = total;
            changed = true;
        }
        if (changed) {
            TackleboxSettings.save();
        }
    }

    /** Foecollab's id of a location to ours (cypress lake is "spawn" there), or null. */
    private static Locations.Location location(String foecollabId) {
        String id = switch (foecollabId) {
            case "spawn", "spawnhub" -> "cypress";
            case "lofotenislands" -> "lofoten";
            default -> foecollabId;
        };
        for (Locations.Location l : Locations.ALL) {
            if (l.id().equals(id)) {
                return l;
            }
        }
        return null;
    }

    /**
     * One catch of yours, from foecollab: counts it, and when the species was missing in that list it is new for the compendium,
     * so the counters move and the trigger line "COMPENDIUM » ..." goes through the trigger engine (not into chat). A gigantic
     * normal fish counts for the gigantic list only, and a variant outside the six (spooky, frozen) for none; both are guesses
     * that can only miss a new entry, never invent one.
     */
    public static void onCatch(String fishId, String variantId, String sizeId, String locationId) {
        Locations.Location loc = location(locationId);
        if (loc == null) {
            return;
        }
        TackleboxSettings s = TackleboxSettings.get();
        long[] d = s.dex.get(loc.id());
        if (d != null) {
            d[0]++;
        }
        String list = switch (variantId) {
            case "normal" -> sizeId.equals("gigantic") ? "gigantic" : "normal";
            case "albino", "melanistic", "trophy", "fabled" -> variantId;
            default -> null;
        };
        Map<String, Boolean> known = list == null ? null : s.species.get(loc.id() + "|" + list);
        if (known == null) {
            return; // that list was never read
        }
        String key = key(fishId);
        Boolean have = known.get(key);
        if (have == null) {
            if (UNMATCHED.add(loc.id() + "|" + key)) {
                Tacklebox.LOGGER.info("compendium: no species '{}' on the {} pages of {}", fishId, list, loc.id());
            }
            return;
        }
        if (have) {
            return;
        }
        known.put(key, true);
        int[] v = s.variants.get(loc.id());
        int i = Arrays.asList(Power.VARIANTS).indexOf(list);
        if (v != null && v[i * 2] < v[i * 2 + 1]) {
            v[i * 2]++;
        }
        TackleboxSettings.save();
        TriggerEngine.onGameMessage(Text.literal("COMPENDIUM » New " + list + " fish: " + name(fishId) + " at " + loc.name()), false);
    }

    private static String name(String fishId) {
        StringBuilder b = new StringBuilder();
        for (String w : fishId.replace('_', ' ').split(" ")) {
            if (!w.isEmpty()) {
                b.append(b.isEmpty() ? "" : " ").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
        }
        return b.toString();
    }
}
