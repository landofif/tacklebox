package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.util.Glyphs;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The menu panel's tile pages (luna, 2026-11-01): the collections overview, /travel (and the compendium's places) and
 * the level pages. What a tile and its hover box say is worked out here from item names and lore as strings, so it
 * can be tested without the game; {@code FishMenuScreen} only draws it.
 */
public final class MenuTiles {
    /** The top level of a place (20 location levels); a collection's top is in its lore ({@link #maxed}). */
    public static final int PLACE_TOP = 20;
    /** A collection at least this far to its next level is "almost there" in the header. */
    public static final float ALMOST = 0.9f;
    /** Tiles are at most {@code MAX_H} tall and get shorter down to {@code MIN_H} before the page scrolls. */
    public static final int GAP = 8, MAX_H = 52, MIN_H = 36;

    /** A level on a level page, by its concrete: green is done, yellow is the one being worked on, grey is later. */
    public enum State { DONE, NOW, LOCKED }

    /** Where the tiles go: {@code height} is all rows with their gaps, {@code scrolls} when that is more than the room. */
    public record Layout(int cols, int tw, int th, int height, boolean scrolls) {}

    private static final Pattern LEVEL_NAME = Pattern.compile("level (\\d+)");
    private static final Pattern LEVEL_OF = Pattern.compile("\\(Level \\d+\\)\\s*(.*)");
    private static final Pattern MAXED = Pattern.compile("\\(\\s*[\\d,.]+\\s*[kmb]?\\s*/\\s*max\\s*\\)", Pattern.CASE_INSENSITIVE);

    private MenuTiles() {}

    /** {@code n} tiles in {@code cols} columns in a {@code w} by {@code h} room. */
    public static Layout layout(int n, int cols, int w, int h) {
        int rows = Math.max(1, (n + cols - 1) / cols);
        int th = Math.max(MIN_H, Math.min(MAX_H, (h - (rows - 1) * GAP) / rows));
        int height = rows * th + (rows - 1) * GAP;
        return new Layout(cols, (w - (cols - 1) * GAP) / cols, th, height, height > h);
    }

    /** "Basses Collection" as "basses": every tile on the page is a collection, so the word goes. */
    public static String shortName(String name) {
        String n = MenuRows.clean(name).toLowerCase(Locale.ROOT);
        return n.endsWith(" collection") ? n.substring(0, n.length() - 11) : n;
    }

    /** Closest to the next level first (ties keep the server's order), finished ones last. */
    public static <T> List<T> byNextLevel(List<T> items, ToDoubleFunction<T> progress, Predicate<T> done) {
        List<T> out = new ArrayList<>(items);
        out.sort(Comparator.comparing((T t) -> done.test(t)).thenComparing(Comparator.comparingDouble(progress).reversed()));
        return out;
    }

    /**
     * The server's {@code 43,178/50K} with both sides written alike, in the unit of what is needed: 43.1k/50k,
     * 1,495/1,500, 11.85m/2.32m. What you have is rounded down, so a count never looks done before it is.
     */
    public static String count(String amount) {
        int slash = amount.indexOf('/');
        if (slash < 0) {
            return amount.toLowerCase(Locale.ROOT);
        }
        double have = MenuRows.number(amount.substring(0, slash)), need = MenuRows.number(amount.substring(slash + 1));
        return number(have, need, RoundingMode.FLOOR) + "/" + number(need, need, RoundingMode.HALF_UP);
    }

    /** {@code v} in the unit {@code need} is written in: plain under 10,000, k with one decimal, m and b with two. */
    static String number(double v, double need, RoundingMode mode) {
        if (need < 10_000) {
            return String.format(Locale.ROOT, "%,d", (long) (mode == RoundingMode.FLOOR ? Math.floor(v) : Math.round(v)));
        }
        double unit = need >= 1e9 ? 1e9 : need >= 1e6 ? 1e6 : 1e3;
        String s = BigDecimal.valueOf(v / unit).setScale(unit == 1e3 ? 1 : 2, mode).stripTrailingZeros().toPlainString();
        return s + (unit == 1e9 ? "b" : unit == 1e6 ? "m" : "k");
    }

    /** Header parts joined with " · ", empty ones left out. */
    public static String line(String... parts) {
        return String.join(" · ", java.util.Arrays.stream(parts).filter(p -> !p.isEmpty()).toList());
    }

    /** The collections header: "1 max · 7 almost there". */
    public static String summary(int done, int almost) {
        return line(done > 0 ? done + " max" : "", almost > 0 ? almost + " almost there" : "");
    }

    /**
     * A finished collection: its count reads {@code (30,039/MAX)} instead of the next level's need (luna, 2026-11-01).
     * Not a level number: species collections end at 7, gigantic, albino, fabled and the rarities at 10.
     */
    public static boolean maxed(List<String> lore) {
        return lore.stream().anyMatch(line -> MAXED.matcher(MenuRows.clean(line)).find());
    }

    /** The places under "Catch at:" in a collection's lore ("- Toledo Bend Reservoir"), by their short names. */
    public static List<String> catchAt(List<String> lore) {
        List<String> out = new ArrayList<>();
        boolean in = false;
        for (String raw : lore) {
            String line = MenuRows.clean(raw);
            if (line.equals("Catch at:")) {
                in = true;
            } else if (in && line.startsWith("- ")) {
                Locations.Location loc = Locations.byName(line);
                out.add(loc != null ? loc.name() : line.substring(2).trim().toLowerCase(Locale.ROOT));
            }
        }
        return out;
    }

    /** The hover box of a collection: its name, the level with the count to the next, where to catch them. */
    public static List<String> collectionTip(MenuRows.Row row, List<String> lore) {
        List<String> out = new ArrayList<>();
        out.add(row.name().toLowerCase(Locale.ROOT));
        if (maxed(lore)) {
            out.add("Level " + row.level() + ", max");
        } else if (row.level() >= 0) {
            out.add("Level " + row.level() + (row.amount().isEmpty() ? "" : ", " + count(row.amount()) + " to level " + (row.level() + 1)));
        }
        List<String> places = catchAt(lore);
        if (!places.isEmpty()) {
            out.add("Catch at " + String.join(", ", places));
        }
        return out;
    }

    /** The word for a /travel mark: "frenzy", "fabled" or "". */
    public static String markWord(int mark) {
        return mark == Locations.FRENZY ? "frenzy" : mark == Locations.FABLED ? "fabled" : "";
    }

    /** The hover box of a place: its name and mark, the level, the climate line, the bait noted for it. */
    public static List<String> placeTip(Locations.Location loc, String mark, int level, float progress, String climate) {
        List<String> out = new ArrayList<>();
        out.add(line(loc.name(), mark));
        if (level >= PLACE_TOP) {
            out.add("Level " + level + ", max");
        } else if (level >= 0) {
            out.add("Level " + level + (progress >= 0 ? ", " + (int) (progress * 100) + "% to level " + (level + 1) : ""));
        } else {
            out.add("Level not known yet: open the compendium once");
        }
        if (!climate.isEmpty()) {
            out.add(climate);
        }
        out.add("");
        List<Locations.Strategy> notes = Locations.strategies(loc);
        if (notes.isEmpty()) {
            out.add("No bait noted for this place yet");
        }
        for (Locations.Strategy s : notes) {
            out.add(String.join(", ", java.util.stream.Stream.of(s.bait() + (s.rarity().isEmpty() ? "" : " (" + s.rarity() + ")"),
                    s.quest().isEmpty() ? "" : s.quest() + " quest", s.targets(), s.extra()).filter(p -> !p.isEmpty()).toList()));
        }
        return out;
    }

    /** n from a plain item name "level n", otherwise -1. */
    public static int level(String plainName) {
        Matcher m = LEVEL_NAME.matcher(plainName);
        return m.matches() ? Integer.parseInt(m.group(1)) : -1;
    }

    /** A level page (a collection's levels, a place's location leveling): five or more items named "level n". */
    public static boolean isLevelPage(List<String> plainNames) {
        return plainNames.stream().filter(n -> level(n) > 0).count() >= 5;
    }

    public static State state(String itemPath) {
        return itemPath.equals("green_concrete") ? State.DONE : itemPath.equals("yellow_concrete") ? State.NOW : State.LOCKED;
    }

    /** What a level page is about, from a level's lore: "lake biwa" ({@code (Level 1) Lake Biwa}) or "albino collection" (its small caps line). */
    public static String levelPageTitle(List<String> lore) {
        for (String raw : lore) {
            String line = MenuRows.clean(raw);
            Matcher m = LEVEL_OF.matcher(line);
            if (m.find() && !m.group(1).isEmpty()) {
                return m.group(1).toLowerCase(Locale.ROOT);
            }
            String plain = Glyphs.fromSmallCaps(line).toLowerCase(Locale.ROOT);
            if (plain.endsWith(" collection")) {
                return plain;
            }
        }
        return "";
    }
}
