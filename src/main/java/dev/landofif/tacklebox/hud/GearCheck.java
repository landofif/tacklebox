package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.menu.Locations;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Wrong pet and wrong armor for the place you are at (luna, 2026-11-15). Pure, so the tests run it: {@link GearWarning} feeds
 * it the location, the chat and what you wear. Every location has one pet of its own, and armor fits a climate; the table and
 * the idea come from FOE+ (itshaxr/FOE-Plus, FEATURES.md and its gear warning), which builds on FOE-R (DannyPX) and FOECollab
 * (Kimberlyow, whose own warning this stands in for). No code was copied.
 */
public final class GearCheck {
    /** A fishing location: its id (as in the boss bar table and item data), name, climate id, and its pet's id and name. */
    public record Place(String id, String name, String climate, String petId, String pet) {}

    public static final List<Place> PLACES = List.of(
            new Place("spawn", "cypress lake", "subtropical", "bullfrog", "bullfrog"),
            new Place("kenai", "kenai river", "subarctic", "bear", "bear"),
            new Place("biwa", "lake biwa", "subtropical", "fox", "fox"),
            new Place("murray", "murray river", "semi-arid", "kangaroo", "kangaroo"),
            new Place("everglades", "everglades", "savanna", "marshrabbit", "marsh rabbit"),
            new Place("keywest", "key west", "savanna", "seaturtle", "sea turtle"),
            new Place("toledobend", "toledo bend", "subtropical", "duck", "duck"),
            new Place("greatlakes", "great lakes", "continental", "eagle", "eagle"),
            new Place("danube", "danube river", "continental", "wolf", "wolf"),
            new Place("oilrig", "oil rig", "savanna", "pelican", "pelican"),
            new Place("amazon", "amazon river", "rainforest", "capybara", "capybara"),
            new Place("mediterranean", "mediterranean sea", "mediterranean", "lynx", "lynx"),
            new Place("capecod", "cape cod", "oceanic", "shark", "shark"),
            new Place("hawaii", "hawaii", "savanna", "dolphin", "dolphin"),
            new Place("cairns", "cairns", "monsoon", "koala", "koala"),
            new Place("lofotenislands", "lofoten islands", "subarctic", "sheep", "sheep"),
            new Place("nile", "nile river", "arid", "hippo", "hippo"));

    /** The climate ids armor carries in its {@code name}. */
    static final List<String> CLIMATES = List.of("subtropical", "subarctic", "semi-arid", "savanna", "continental", "rainforest",
            "mediterranean", "oceanic", "monsoon", "arid");

    public static final int RED = 0xFFFF5555;
    /** The boss bar and the items settle a moment after you arrive, so the chat line waits this long. */
    static final long ARRIVAL_DELAY = 3000;

    // "PETS » Equipped your Marsh Rabbit Pet", "PETS » Your Duck Pet is now level 12 → 13 (+1)", "PETS » Pet unequipped!" after Matcher.normalize
    private static final Pattern EQUIPPED = Pattern.compile("^pets > (?:equipped your|your) (.+?) pet(?: is now level\\b.*)?$", Pattern.CASE_INSENSITIVE);
    private static final Pattern UNEQUIPPED = Pattern.compile("^pets > pet unequipped", Pattern.CASE_INSENSITIVE);

    private static String lastPlace = "";
    private static long checkAt;

    private GearCheck() {}

    /** The fishing location with that id, or null (crew island, unknown places). */
    public static Place place(String locationId) {
        for (Place p : PLACES) {
            if (p.id().equals(locationId)) {
                return p;
            }
        }
        return null;
    }

    /** Cypress Lake's spawn area (x 95..145, z -58..38): no warning until you get to the water. */
    public static boolean spawnHub(String locationId, double x, double z) {
        return "spawn".equals(locationId) && x >= 95 && x <= 145 && z >= -58 && z <= 38;
    }

    /** A pet's id from its name in chat or its item data: "Marsh Rabbit" is "marshrabbit". */
    public static String petId(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    /**
     * What a normalized chat line says about your equipped pet: its id for an equip or level line (only the equipped pet levels),
     * "" for an unequip, null for any other line.
     */
    public static String equipLine(String line) {
        if (UNEQUIPPED.matcher(line).find()) {
            return "";
        }
        Matcher m = EQUIPPED.matcher(line);
        return m.find() ? petId(m.group(1)) : null;
    }

    /** A worn armor piece: which one ("chestplate") and the climate id from its item data. */
    public record Armor(String piece, String climate) {}

    /**
     * The hud rows for what does not fit at {@code here}; empty when all is fine, when {@code here} is null, or when no pet is
     * known and the armor fits.
     *
     * @param pet      the equipped pet's id, "" when none or not known yet
     * @param hasRight whether the place's own pet is in the inventory
     */
    public static List<TextHud.Row> rows(Place here, String pet, boolean hasRight, List<Armor> worn) {
        List<TextHud.Row> rows = new ArrayList<>();
        if (here == null) {
            return rows;
        }
        if (!pet.isEmpty() && !pet.equals(here.petId())) {
            rows.add(new TextHud.Row("wrong pet", "use " + here.pet() + (hasRight ? " · in inventory" : " · not in inventory"), RED, 0));
        }
        List<String> pieces = wrongArmor(here, worn);
        if (!pieces.isEmpty()) {
            rows.add(new TextHud.Row("wrong armor", String.join(", ", pieces) + " · need " + here.climate(), RED, 0));
        }
        return rows;
    }

    /** The worn pieces of another climate than the place's, like "savanna chestplate". Pieces without a known climate are left out. */
    static List<String> wrongArmor(Place here, List<Armor> worn) {
        List<String> out = new ArrayList<>();
        for (Armor a : worn) {
            String c = a.climate() == null ? "" : a.climate().toLowerCase(Locale.ROOT).trim();
            if (CLIMATES.contains(c) && !c.equals(here.climate())) {
                out.add(c + " " + a.piece());
            }
        }
        return out;
    }

    /** The one chat line for an arrival, or null when all fits: "wrong pet at danube river: use a wolf pet (in inventory) · wrong armor: ...". */
    public static String chatLine(Place here, String pet, boolean hasRight, List<Armor> worn) {
        if (here == null) {
            return null;
        }
        List<String> parts = new ArrayList<>();
        if (!pet.isEmpty() && !pet.equals(here.petId())) {
            parts.add("wrong pet at " + Locations.shown(here.name()) + ": use a " + here.pet() + " pet (" + (hasRight ? "in inventory" : "not in inventory") + ")");
        }
        List<String> pieces = wrongArmor(here, worn);
        if (!pieces.isEmpty()) {
            parts.add("wrong armor: " + String.join(", ", pieces) + ", " + Locations.shown(here.name()) + " is " + here.climate());
        }
        return parts.isEmpty() ? null : String.join(" · ", parts);
    }

    /**
     * Called a few times a second with the place you are at ("" for none, the spawn hub or crew island): true once,
     * {@link #ARRIVAL_DELAY} after the place changed to a fishing location.
     */
    public static boolean arrived(String placeId, long now) {
        if (!placeId.equals(lastPlace)) {
            lastPlace = placeId;
            checkAt = placeId.isEmpty() ? 0 : now + ARRIVAL_DELAY;
        }
        if (checkAt != 0 && now >= checkAt) {
            checkAt = 0;
            return true;
        }
        return false;
    }

    /** For tests and on leaving the server: the next place is an arrival again. */
    public static void reset() {
        lastPlace = "";
        checkAt = 0;
    }
}
