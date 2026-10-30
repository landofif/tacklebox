package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The fishonmc /stats page (your own and {@code /stats <player>}) read into the pieces of the profile layout.
 * Gear is told apart by its custom_data {@code type} ({@code hat}, {@code armor} with {@code piece}, {@code reel},
 * {@code pole}, {@code line}, {@code pet}, {@code bait}); the buttons have no custom_data, so by their name; the
 * five showcase items by their slots 2-6. Slot map: docs/fishonmc-research.md (luna's dump 2026-10-30).
 */
public final class StatsPage {
    /** One chest item: its slot, raw name, raw lore lines and custom_data (empty when it has none). */
    public record Piece(int slot, String name, List<String> lore, NbtCompound data) {}

    /** A big number tile: the slot it clicks, the number, and a dim line under it. */
    public record Tile(int slot, String value, String sub) {}

    /** The pet card: its xp bar is full at MAX. */
    public record Pet(int slot, String name, int level, float progress, String amount, String item) {}

    /** A slot of the page is -1 when the menu has no such item. */
    public record Stats(String owner, int level, float progress, String amount, String event, String placement,
                        int hat, int helmet, int chest, int legs, int boots,
                        int reel, int pole, int line, int lineHave, int lineMax, Pet pet,
                        Tile power, Tile runes, Tile catches, Tile bait,
                        List<Integer> displays, List<Integer> buttons) {}

    private static final int DISPLAY_FIRST = 2, DISPLAY_LAST = 6;
    private static final Pattern NUMBER = Pattern.compile("([\\d,.]+[kmb]?)");
    private static final Pattern LEVEL = Pattern.compile("\\[(\\d+)]");
    private static final Pattern XP = Pattern.compile("\\(\\s*([\\d,.]+[kKmMbB]?)\\s*/\\s*([\\d,.]+[kKmMbB]?|MAX)\\s*\\)");

    private StatsPage() {}

    /** The page, or null when these items are not a /stats page (it has the Fishing Profile and General Statistics). */
    public static Stats read(List<Piece> pieces) {
        Piece profile = named(pieces, "fishing profile"), general = named(pieces, "general statistics");
        if (profile == null || general == null) {
            return null;
        }
        MenuRows.Row row = MenuRows.of(profile.name, profile.lore);
        String owner = "";
        for (String line : profile.lore) {
            String l = MenuRows.clean(line);
            if (l.startsWith("Profile of ")) {
                owner = l.substring(11).trim();
            }
        }
        String event = "", placement = "";
        List<Integer> displays = new ArrayList<>();
        int hat = -1, helmet = -1, chest = -1, legs = -1, boots = -1, reel = -1, pole = -1, line = -1, lineHave = -1, lineMax = -1;
        Pet pet = null;
        Tile bait = null;
        List<Integer> used = new ArrayList<>();
        for (Piece p : pieces) {
            String type = p.data.getString("type", "");
            if (p.slot >= DISPLAY_FIRST && p.slot <= DISPLAY_LAST) {
                displays.add(p.slot);
                String place = find(p.lore, "placement:");
                if (placement.isEmpty() && !place.isEmpty()) {
                    placement = place;
                    event = MenuRows.clean(p.name);
                }
            } else if (type.equals("hat")) {
                hat = p.slot;
            } else if (type.equals("armor")) {
                String piece = p.data.getString("piece", "");
                if (piece.endsWith("helmet")) {
                    helmet = p.slot;
                } else if (piece.endsWith("chestplate")) {
                    chest = p.slot;
                } else if (piece.endsWith("leggings")) {
                    legs = p.slot;
                } else if (piece.endsWith("boots")) {
                    boots = p.slot;
                } else {
                    continue;
                }
            } else if (type.equals("reel")) {
                reel = p.slot;
            } else if (type.equals("pole")) {
                pole = p.slot;
            } else if (type.equals("line")) {
                line = p.slot;
                lineHave = p.data.getInt("curDurability", -1);
                lineMax = p.data.getInt("maxDurability", -1);
            } else if (type.equals("pet")) {
                pet = pet(p);
            } else if (type.equals("bait")) {
                bait = new Tile(p.slot, find(p.lore, "stock:"), MenuRows.clean(p.name));
            } else {
                continue;
            }
            used.add(p.slot);
        }
        Piece runes = named(pieces, "runes breakdown");
        Tile power = new Tile(profile.slot, find(profile.lore, "power"), "power");
        Tile runesTile = runes == null ? null : new Tile(runes.slot, find(runes.lore, "total runes discovered:"), "runes");
        Tile catches = new Tile(general.slot, find(general.lore, "fish caught:"), "catches");
        used.add(profile.slot);
        used.add(general.slot);
        if (runes != null) {
            used.add(runes.slot);
        }
        List<Integer> buttons = new ArrayList<>();
        for (Piece p : pieces) {
            String name = plain(p.name);
            if (!used.contains(p.slot) && !displays.contains(p.slot) && !name.equals("close") && !name.equals("return")) {
                buttons.add(p.slot);
            }
        }
        return new Stats(owner, row.level(), row.progress(), row.amount(), event, placement, hat, helmet, chest, legs, boots,
                reel, pole, line, lineHave, lineMax, pet, power, runesTile, catches, bait, displays, buttons);
    }

    /** Name, {@code ᴘᴇᴛ ʟᴇᴠᴇʟ: [100]}, {@code (246.69M/MAX)} and {@code Equipped Item: Wade's Amulet}. */
    static Pet pet(Piece p) {
        int level = -1;
        float progress = -1;
        String amount = "", item = "";
        for (String raw : p.lore) {
            String l = MenuRows.clean(raw);
            String low = plain(l);
            Matcher lv = LEVEL.matcher(low);
            if (level < 0 && low.startsWith("pet level") && lv.find()) {
                level = Integer.parseInt(lv.group(1));
            }
            Matcher xp = XP.matcher(l);
            if (amount.isEmpty() && xp.find()) {
                amount = xp.group(1) + "/" + xp.group(2);
                double need = xp.group(2).equals("MAX") ? 0 : MenuRows.number(xp.group(2));
                progress = need > 0 ? (float) Math.min(1, MenuRows.number(xp.group(1)) / need) : 1;
            }
            if (l.startsWith("Equipped Item:")) {
                item = l.substring(14).trim();
            }
        }
        return new Pet(p.slot, MenuRows.clean(p.name), level, progress, amount, item.equalsIgnoreCase("empty") ? "" : item);
    }

    /** The number after {@code label} in the first lore line that starts with it ({@code "Power 425.18K"} gives 425.18K), or "". */
    static String find(List<String> lore, String label) {
        for (String raw : lore) {
            String l = MenuRows.clean(raw);
            if (plain(l).startsWith(label)) {
                Matcher m = NUMBER.matcher(l.substring(label.length()).toLowerCase(Locale.ROOT));
                if (m.find()) {
                    String n = m.group(1);
                    String rest = l.substring(label.length()).trim();
                    return rest.startsWith("#") ? "#" + n : rest.contains(n + "x") ? n + "x" : shorten(n);
                }
            }
        }
        return "";
    }

    /** A whole number of 1000 or more like the servers own: {@code 210,542} gives 210.54k, {@code 1,547,700} gives 1.55m. */
    static String shorten(String n) {
        if (!n.matches("[\\d,]+")) {
            return n;
        }
        double v = MenuRows.number(n);
        if (v < 1000) {
            return n;
        }
        String s = String.format(Locale.ROOT, "%.2f", v >= 1_000_000 ? v / 1_000_000 : v / 1000);
        return s.replaceAll("\\.?0+$", "") + (v >= 1_000_000 ? "m" : "k");
    }

    private static Piece named(List<Piece> pieces, String name) {
        for (Piece p : pieces) {
            if (plain(p.name).equals(name)) {
                return p;
            }
        }
        return null;
    }

    /** Cleaned, small capitals as plain letters, lower case. */
    private static String plain(String s) {
        return Glyphs.fromSmallCaps(MenuRows.clean(s)).toLowerCase(Locale.ROOT);
    }
}
