package dev.landofif.tacklebox.menu;

import net.minecraft.nbt.NbtCompound;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The 17 fishing locations (docs/fishonmc-research.md section 7, minus crew island) and the bait strategies
 * luna and a friend noted down. The pictures are the items the server puts in the menu.
 */
public final class Locations {
    /** {@code key} is matched against a menu item's name; {@code wiki} is the wiki table's spelling. */
    public record Location(String id, String name, String key, String wiki) {
    }

    /** One row of the notes: what to fish with, and what you get out of it. */
    public record Strategy(String bait, String rarity, String quest, String targets, String extra) {}

    public static final List<Location> ALL = List.of(
            new Location("cypress", "cypress lake", "cypress", "cypress lake"),
            new Location("kenai", "kenai river", "kenai", "kenai river"),
            new Location("biwa", "lake biwa", "biwa", "lake biwa"),
            new Location("murray", "murray river", "murray", "murray river"),
            new Location("everglades", "everglades", "everglades", "everglades"),
            new Location("keywest", "key west", "key west", "key west"),
            new Location("toledobend", "toledo bend", "toledo", "toledo bend"),
            new Location("greatlakes", "great lakes", "great lakes", "great lakes"),
            new Location("danube", "danube river", "danube", "danube river"),
            new Location("oilrig", "oil rig", "oil rig", "oil rig"),
            new Location("amazon", "amazon river", "amazon", "amazon river"),
            new Location("mediterranean", "mediterranean sea", "mediterranean", "med sea"),
            new Location("capecod", "cape cod", "cape cod", "cape cod"),
            new Location("hawaii", "hawaii", "hawaii", "hawaii"),
            new Location("lofoten", "lofoten islands", "lofoten", "lofoten islands"),
            new Location("cairns", "cairns", "cairns", "cairns"),
            new Location("nile", "nile river", "nile", "nile river"));

    /** The location a menu item is named after, or null. */
    public static Location byName(String plainName) {
        String n = plainName.toLowerCase(Locale.ROOT);
        for (Location l : ALL) {
            if (n.contains(l.key())) {
                return l;
            }
        }
        return null;
    }

    /** The /travel colours: the server adds "*FRENZY!*" to the name of a place in a frenzy; the fabled event's place is from chat. */
    public static final int FRENZY = 0xFF2E8B3A, FABLED = 0xFFE0413A;

    /** The colour of a /travel item by its full name (all fonts), 0 for none. A frenzy goes before the fabled event (luna 2026-10-29). */
    public static int mark(String name, String fabledPlace) {
        if (name.toLowerCase(Locale.ROOT).contains("frenzy")) {
            return FRENZY;
        }
        Location loc = byName(name);
        return loc != null && loc == byName(fabledPlace) ? FABLED : 0;
    }

    /** A /travel location item: its custom_data is {@code {fastTravel:1b}}. */
    public static boolean isTravelItem(NbtCompound customData) {
        return customData.contains("fastTravel");
    }

    public static List<Strategy> strategies(Location l) {
        List<Strategy> out = new ArrayList<>();
        for (String line : NOTES.split("\n")) {
            String[] c = line.split("\t", -1);
            if (c.length >= 5 && c[0].equals(l.wiki())) {
                out.add(new Strategy(c[1], c[2], c[3], c[4], c.length > 5 ? c[5] : ""));
            }
        }
        return out;
    }

    // location, bait, bait rarity, best quest, targets, extra. tab separated; blank = not noted.
    private static final String NOTES = """
            cypress lake\tbread\tcommon\trare\t1c 1r\t
            cypress lake\tdough balls\tcommon\trare\t1c 1r\t
            cypress lake\tcrank\trare\trare / epic\t1r 2e\t
            cypress lake\tfrogs\tepic\trare / epic\t1r 2e\t
            lake biwa\tbread\tcommon\trare\t1c 2r\t
            lake biwa\tdough balls\tcommon\trare\t1c 2r\t
            lake biwa\tsalmon\tcommon\tlegendary\t1r 1e 2l\thigh luck
            lake biwa\tcrank\trare\trare / epic\t1r 1e\t
            lake biwa\troostertail - fire tiger\trare\trare / epic\t2r 2e\t
            lake biwa\ttrout dough\trare\trare / epic\t1r 1e\t
            murray river\tcatfish\t\trare\t2c 2r\tluck 600-700 ?
            murray river\tworms\tcommon\tlegendary\t\t????
            key west\tclams\tcommon\trare\t4c 3r\t
            key west\tsand worms\t\t\t\t
            key west\tred jerkbait\tepic\trare\t1r\t
            toledo bend\tdough balls\tcommon\tepic\t1r 1e\t
            toledo bend\tbread\tcommon\tepic\t1r 1e\t
            toledo bend\tminnow live bait\trare\trare\t1c 5r 1e\t
            toledo bend\tsweet corn\trare\tlegendary\t1r 2e 1l\thigh luck (+1000)
            great lakes\tbread\tcommon\trare\t1r\t
            great lakes\tsweet corn\trare\trare\t1r\t
            great lakes\tbasses\t\trare\t2r\t
            great lakes\tcut roach\tepic\tepic\t1e\t
            danube river\tcut roach\tepic\tepic\t1e\t
            amazon river\tfrog - dark rainforest\tepic\trare\t2r\t
            med sea\tmenhaden live bait\trare\tepic\t2c 1e\t1k+ luck?
            med sea\tsmall chunk of bonito\trare\tepic\t1c 1e\t?? test
            med sea\tmedium cut squid\trare\tepic\t2c 2e\t?? test
            med sea\tcut mullet\tepic\tepic\t2c 1r 3e\tabt 900+ luck is good
            cape cod\tsand shrimp\tcommon\trare\t1c 1r 1l\t
            cape cod\tsand shrimp\tcommon\tlegendary\t1c 1r 1l\twith really high luck ???
            cape cod\therring live bait\trare\trare\t1c 4r\t?? test
            hawaii\tclams\tcommon\trare\t5c 4r\t
            hawaii\tscallops\tcommon\trare\t5c 4r\t
            hawaii\tmussels\trare\trare\t5c 4r\t
            hawaii\tblue jerkbait\tepic\tepic\t1e\t
            hawaii\tmullet jerkbait\tlegendary\tepic\t1e\t
            cairns\tsand flea\tcommon\trare\t1c 1r\t
            """;

    private Locations() {}
}
