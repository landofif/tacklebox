package dev.landofif.dofisaddons.menu;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.hud.XpEstimate;
import dev.landofif.dofisaddons.util.Glyphs;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Fishonmc's power (screen/PowerScreen shows it): the sum of location levels, the compendium, collections and
 * one amount per unique fish by variant. Read passively from the /stats "Power Breakdown" item and the variant
 * lines of the compendium location pages, whenever one of them is open.
 */
public final class Power {
    public static final int MAX_LEVEL = 20;
    /** Power for reaching location level 1 to 20, the same at every location (the leveling page, l 2026-10-08). */
    private static final int[] LEVEL = {50, 75, 100, 125, 150, 175, 250, 325, 500, 750, 1250, 1750, 2500, 3750, 4000, 6000, 7750, 9250, 11250, 14000};
    /** The unique fish variants in the order of the breakdown, and the power each new one gives. */
    public static final String[] VARIANTS = {"normal", "albino", "melanistic", "trophy", "fabled", "gigantic"};
    public static final int[] PER = {50, 250, 500, 750, 1000, 150};
    /** Power every location can give through levels, all seventeen at level 20. */
    public static final int LEVEL_MAX = Arrays.stream(LEVEL).sum() * Locations.ALL.size();

    private static final Pattern SOURCE = Pattern.compile("(total power|location leveling|compendium completion|collections|total unique fish caught)\\D{0,6}?([\\d.,]+)\\s*([kmb]?)");
    /** "37 (250 per)": the variant name is a pack glyph that is dropped, so the amount per fish tells which one it is. */
    private static final Pattern COUNT = Pattern.compile("(\\d[\\d,]*)\\s*\\(\\s*([\\d.,]+)\\s*([kmb]?)\\S*\\s*per\\s*\\)");
    private static final Pattern VARIANT = Pattern.compile("(normal|albino|melanistic|trophy|fabled|gigantic)\\W+(\\d+)\\s*/\\s*(\\d+)");
    private static final Pattern REQ = Pattern.compile("requirement: \\(([\\d.,]+)\\s*([kmb]?)\\s*/\\s*([\\d.,]+)\\s*([kmb]?)\\)");
    private static final Pattern AMOUNT = Pattern.compile("^([\\d.,]+)\\s*([kmb]?)$");
    private static final String[] SOURCES = {"total", "leveling", "compendium", "collections", "unique"};

    /** One location: where its level is, what the next one gives and what fish it still has. {@code level} 0 = not read yet. */
    public record Row(Locations.Location loc, int level, double xpLeft, int gain, int headroom, int[] left, int fish, boolean known) {
        public boolean maxed() {
            return level >= MAX_LEVEL;
        }
    }

    private Power() {}

    /** Power for reaching this level (1 to 20). */
    public static int gain(int level) {
        return LEVEL[level - 1];
    }

    /** Power all levels up to this one gave. */
    public static int total(int level) {
        int sum = 0;
        for (int i = 0; i < Math.min(level, MAX_LEVEL); i++) {
            sum += LEVEL[i];
        }
        return sum;
    }

    /** 345350 as "345.35K", 1088000 as "1.09M": the server's way, two decimals, no trailing zeros. */
    public static String fmt(double v) {
        double a = Math.abs(v);
        return a >= 1_000_000 ? trim(v / 1_000_000) + "M" : a >= 1_000 ? trim(v / 1_000) + "K" : String.valueOf(Math.round(v));
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }

    public static Row row(Locations.Location loc) {
        double[] at = XpEstimate.toNext(loc.id());
        int level = at == null ? 0 : Math.min(MAX_LEVEL, (int) at[0]);
        int[] v = DofiSettings.get().variants.get(loc.id());
        int[] left = new int[VARIANTS.length];
        int fish = 0;
        for (int i = 0; v != null && i < left.length; i++) {
            left[i] = Math.max(0, v[i * 2 + 1] - v[i * 2]);
            fish += left[i] * PER[i];
        }
        boolean maxed = level >= MAX_LEVEL;
        return new Row(loc, level, at == null || maxed ? 0 : at[1], level == 0 || maxed ? 0 : gain(level + 1),
                level == 0 ? 0 : total(MAX_LEVEL) - total(level), left, fish, v != null);
    }

    public static List<Row> rows() {
        List<Row> out = new ArrayList<>();
        for (Locations.Location loc : Locations.ALL) {
            out.add(row(loc));
        }
        return out;
    }

    // ---- reading ----

    /** Looks for the breakdown item (lore "Total Power: ...") and the compendium location pages (items named after a location with variant lines). */
    public static void scan(HandledScreen<?> screen) {
        DofiSettings s = DofiSettings.get();
        boolean changed = false;
        Map<String, String> found = new java.util.LinkedHashMap<>();
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
            if (lines.stream().anyMatch(l -> l.contains("total power"))) {
                Map<String, Double> read = breakdown(lines);
                if (read.containsKey("total")) {
                    s.powerAt = System.currentTimeMillis();
                    if (!read.equals(s.power)) {
                        s.power = new java.util.LinkedHashMap<>(read);
                        changed = true;
                    }
                }
            }
            collection(Glyphs.plain(stack.getName()), lines, found);
            Locations.Location loc = Locations.byName(Glyphs.plain(stack.getName()));
            int[] v = loc == null ? null : variants(lines);
            if (v != null && !Arrays.equals(v, s.variants.get(loc.id()))) {
                s.variants.put(loc.id(), v);
                changed = true;
            }
        }
        for (var e : found.entrySet()) {
            if (!e.getValue().equals(s.collections.put(e.getKey(), e.getValue()))) {
                changed = true;
            }
        }
        if (changed) {
            DofiSettings.save();
        }
    }

    /** A collections level item ("Level 8", lore "albino collection", "Requirement: (60/80)", a line with only the power): keeps the lowest unfinished level per category. */
    static void collection(String name, List<String> lines, Map<String, String> found) {
        Matcher level = Pattern.compile("^level (\\d+)$").matcher(name);
        String cat = lines.stream().filter(l -> l.endsWith(" collection")).findFirst().map(l -> l.substring(0, l.length() - 11)).orElse(null);
        Matcher req = lines.stream().map(REQ::matcher).filter(Matcher::find).findFirst().orElse(null);
        Matcher amount = lines.stream().map(AMOUNT::matcher).filter(Matcher::matches).findFirst().orElse(null);
        if (cat == null || req == null || amount == null || !level.find()) {
            return;
        }
        String old = found.get(cat);
        if (old == null || Integer.parseInt(old.substring(0, old.indexOf('|'))) > Integer.parseInt(level.group(1))) {
            found.put(cat, level.group(1) + "|" + (long) XpEstimate.num(req.group(1), req.group(2)) + "|" + (long) XpEstimate.num(req.group(3), req.group(4))
                    + "|" + (long) XpEstimate.num(amount.group(1), amount.group(2)));
        }
    }

    /** The lines of the Power Breakdown item to its numbers: the five sources, and "n.normal" and so on for the unique fish counts. */
    static Map<String, Double> breakdown(List<String> lines) {
        Map<String, Double> out = new java.util.LinkedHashMap<>();
        for (String line : lines) {
            Matcher m = SOURCE.matcher(line);
            if (m.find()) {
                String key = switch (m.group(1)) {
                    case "total power" -> SOURCES[0];
                    case "location leveling" -> SOURCES[1];
                    case "compendium completion" -> SOURCES[2];
                    case "collections" -> SOURCES[3];
                    default -> SOURCES[4];
                };
                out.put(key, XpEstimate.num(m.group(2), m.group(3)));
            }
            Matcher c = COUNT.matcher(line);
            while (c.find()) {
                int per = (int) Math.round(XpEstimate.num(c.group(2), c.group(3)));
                for (int i = 0; i < PER.length; i++) {
                    if (PER[i] == per) {
                        out.put("n." + VARIANTS[i], XpEstimate.num(c.group(1), ""));
                    }
                }
            }
        }
        return out;
    }

    /** Caught and total per variant from a compendium location item, or null unless all six lines are there. */
    static int[] variants(List<String> lines) {
        int[] out = new int[VARIANTS.length * 2];
        boolean[] seen = new boolean[VARIANTS.length];
        for (String line : lines) {
            Matcher m = VARIANT.matcher(line);
            if (m.find()) {
                int i = Arrays.asList(VARIANTS).indexOf(m.group(1));
                out[i * 2] = Integer.parseInt(m.group(2));
                out[i * 2 + 1] = Integer.parseInt(m.group(3));
                seen[i] = true;
            }
        }
        for (boolean b : seen) {
            if (!b) {
                return null;
            }
        }
        return out;
    }
}
