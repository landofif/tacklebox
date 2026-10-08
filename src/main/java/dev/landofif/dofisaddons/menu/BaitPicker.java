package dev.landofif.dofisaddons.menu;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.landofif.dofisaddons.DofisAddons;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * The bait odds model behind screen/BaitScreen, ported from l's Bait Picker artifact (rules: zeb of fishonmc staff
 * and the wiki tacklebox page). The rarity is rolled first from the luck odds, then the bait's likelihood is checked:
 * a pool fish of the rolled rarity, else the nearest lower one that has one. A roll below the whole pool gets a normal
 * fish, because the real chance of climbing is unpublished, so the bait odds are a lower bound. A bait never raises
 * the rolled rarity by itself. Fish and bait data: {@code data/dofisaddons/baitpicker.json}.
 */
public final class BaitPicker {
    public static final String[] RARITY = {"common", "rare", "epic", "legendary", "mythical"};
    public static final String RL = "CRELM";
    public static final String[] QUESTS = {"adult", "large", "rare", "epic", "legendary"};

    public record Fish(String name, int rarity, String group, String depth) {}

    public record Loc(String name, char water, List<Fish> fish) {}

    public record Bait(String name, boolean lure, int rarity, String water, int likelihood, List<String> targets) {}

    /** A bait that beats no bait: its chance per cast, the fish it pulls per rarity, and baits with the same pool. */
    public record Row(Bait bait, int[] pool, List<List<String>> fish, double chance, List<String> same) {}

    private static final Map<String, Integer> LURE_USES = new LinkedHashMap<>();
    private static final String[] LURE_KEYS = {"Crankbait (Tiger", "Roostertail", "Crankbait Bluegill", "Spinner Bait Chartreuse",
            "Frog Leopard", "Spinnerbait White Shad", "Spinner Bait Golden", "Frog Rainforest", "Glide Bait", "Diving Plug", "Popper",
            "Bucktail", "Jerkbait", "Saltwater Spoon", "Marlin Lure Blue Green", "Marlin Lure", "Frog Dark", "Spinner Bait Lime"};
    private static final int[] LURE_USE = {30, 20, 30, 50, 30, 50, 50, 30, 50, 40, 40, 25, 30, 25, 50, 30, 30, 50};

    /** Climate of each location (docs/fishonmc-research.md section 7; the Nile is a guess), armour stats are "climate_luck". */
    private static final Map<String, String> CLIMATE = Map.ofEntries(
            Map.entry("cypress", "subtropical"), Map.entry("kenai", "subarctic"), Map.entry("biwa", "subtropical"),
            Map.entry("murray", "semiarid"), Map.entry("everglades", "savanna"), Map.entry("keywest", "savanna"),
            Map.entry("toledobend", "subtropical"), Map.entry("greatlakes", "continental"), Map.entry("danube", "continental"),
            Map.entry("oilrig", "savanna"), Map.entry("amazon", "rainforest"), Map.entry("mediterranean", "mediterranean"),
            Map.entry("capecod", "oceanic"), Map.entry("hawaii", "savanna"), Map.entry("lofoten", "subarctic"),
            Map.entry("cairns", "monsoon"), Map.entry("nile", "arid"));

    private static List<Loc> locs;
    private static List<Bait> baits;

    private BaitPicker() {}

    public static List<Loc> locs() {
        load();
        return locs;
    }

    private static void load() {
        if (locs != null) {
            return;
        }
        locs = new ArrayList<>();
        baits = new ArrayList<>();
        try (var in = BaitPicker.class.getResourceAsStream("/data/dofisaddons/baitpicker.json")) {
            JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            for (JsonElement e : root.getAsJsonArray("locs")) {
                JsonObject o = e.getAsJsonObject();
                List<Fish> fish = new ArrayList<>();
                for (JsonElement f : o.getAsJsonArray("fish")) {
                    JsonArray a = f.getAsJsonArray();
                    fish.add(new Fish(a.get(0).getAsString(), a.get(1).getAsInt(), a.get(2).getAsString(), a.get(3).getAsString()));
                }
                locs.add(new Loc(o.get("name").getAsString(), o.get("water").getAsString().charAt(0), fish));
            }
            for (JsonElement e : root.getAsJsonArray("baits")) {
                JsonObject o = e.getAsJsonObject();
                List<String> targets = new ArrayList<>();
                o.getAsJsonArray("targets").forEach(t -> targets.add(t.getAsString()));
                baits.add(new Bait(o.get("name").getAsString(), o.get("kind").getAsString().equals("lure"),
                        RL.indexOf(o.get("rar").getAsString()), o.get("water").getAsString(), o.get("lik").getAsInt(), targets));
            }
        } catch (Exception e) {
            DofisAddons.LOGGER.error("Could not read the bait data", e);
        }
    }

    /** The wiki location this mods location belongs to (their names are the same, lowercased). */
    public static Loc loc(Locations.Location l) {
        for (Loc c : locs()) {
            if (c.name().equalsIgnoreCase(l.name())) {
                return c;
            }
        }
        return null;
    }

    public static String climate(Locations.Location l) {
        return CLIMATE.getOrDefault(l.id(), "");
    }

    /** How many casts a lure lasts, or 0 for baits. */
    public static int uses(Bait b) {
        if (b.lure()) {
            for (int i = 0; i < LURE_KEYS.length; i++) {
                if (b.name().startsWith(LURE_KEYS[i])) {
                    return LURE_USE[i];
                }
            }
        }
        return 0;
    }

    // ---- odds ----

    /** Chance of each rarity per cast at this luck (wiki base 55/30/10/4/1; the in-game tooltip's 65 is a bug). */
    public static double[] odds(double luck) {
        double[] p = {55 - luck * .02, 30 + luck * .002, 10 + luck * .004, 4 + luck * .006, 1 + luck * .008};
        double sum = 0;
        for (int i = 0; i < 5; i++) {
            p[i] = Math.max(0, p[i]);
            sum += p[i];
        }
        for (int i = 0; i < 5; i++) {
            p[i] /= sum;
        }
        return p;
    }

    /** Chance of baby, juvenile, adult, large and gigantic per cast at this scale (a bait does not change it). */
    public static double[] sizeOdds(double s) {
        double baby, juvenile, adult;
        if (s <= 1052) {
            baby = 20 - s * .019;
            juvenile = 40 - s * .008;
            adult = 30 + s * .008;
        } else if (s <= 2222) {
            baby = 0;
            juvenile = 60 - s * .027;
            adult = 30 + s * .008;
        } else {
            baby = 0;
            juvenile = 0;
            adult = 90 - s * .019;
        }
        double[] a = {baby, juvenile, adult, 9.98 + s * .018, .02 + s * .001};
        double sum = 0;
        for (int i = 0; i < 5; i++) {
            a[i] = Math.max(0, a[i]);
            sum += a[i];
        }
        for (int i = 0; i < 5; i++) {
            a[i] /= sum;
        }
        return a;
    }

    /** Chance per cast of getting a fish of rarity {@code x} with this pool (fish per rarity) and likelihood. */
    private static double chance(int[] pool, int likelihood, double[] p, int x) {
        double l = likelihood / 100.0, c = 0;
        for (int r = 0; r < 5; r++) {
            int res = r;
            while (res >= 0 && pool[res] == 0) {
                res--;
            }
            if (res < 0) {
                c += r == x ? p[r] : 0;
            } else {
                c += res == x ? l * p[r] : 0;
                c += r == x ? (1 - l) * p[r] : 0;
            }
        }
        return c;
    }

    private static boolean matches(String target, Fish f) {
        if (target.equals("*")) {
            return true;
        }
        return target.charAt(0) == '@' ? f.depth().equals(target.substring(1))
                : target.equals(f.group()) || target.equals(f.name());
    }

    private static boolean usable(Bait b, Loc loc) {
        return b.water().equals("F") || b.water().equals("S") ? loc.water() == 'B' || b.water().charAt(0) == loc.water() : b.water().equals(loc.name());
    }

    /** Baits that can pull rarity {@code x} here, best first; baits with the same pool and likelihood are folded into one row. */
    public static List<Row> rows(Loc loc, double luck, int x, boolean[] tiers, boolean lures) {
        load();
        double[] p = odds(luck);
        List<Row> out = new ArrayList<>();
        Map<String, Row> seen = new LinkedHashMap<>();
        for (Bait b : baits) {
            if (!usable(b, loc) || b.likelihood() == 0 || !tiers[b.rarity()] || b.lure() && !lures) {
                continue;
            }
            int[] pool = new int[5];
            List<List<String>> names = List.of(new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
            for (Fish f : loc.fish()) {
                if (b.targets().stream().anyMatch(t -> matches(t, f))) {
                    pool[f.rarity()]++;
                    names.get(f.rarity()).add(f.name());
                }
            }
            if (java.util.Arrays.stream(pool).sum() == 0) {
                continue;
            }
            String key = java.util.Arrays.toString(pool) + b.likelihood() + b.rarity() + b.lure();
            Row twin = seen.get(key);
            if (twin != null) {
                twin.same().add(b.name());
                continue;
            }
            Row row = new Row(b, pool, names, chance(pool, b.likelihood(), p, x), new ArrayList<>());
            seen.put(key, row);
            out.add(row);
        }
        out.sort((a, b) -> b.chance() != a.chance() ? Double.compare(b.chance(), a.chance())
                : a.bait().rarity() != b.bait().rarity() ? Integer.compare(a.bait().rarity(), b.bait().rarity())
                : a.bait().name().compareTo(b.bait().name()));
        return out;
    }

    // ---- quests ----

    private static double xpMax(int level) {
        return Math.min(31.62, 21.714 + 0.2093 * level + 0.0266 * level * level);
    }

    /** Catches a hard quest of this kind asks for at this location level: fitted from 16 dumps, about 4% off, each quest varies a further 12-17%. */
    public static int catches(int quest, int level) {
        double[][] k = {{3.665, 0.92}, {2.675, 0.92}, {1.303, 1.2}, {0.553, 1.2}, {0.738, 1}};
        double n = Math.round(k[quest][0] * Math.pow(xpMax(level), k[quest][1]));
        return (int) (quest == 4 ? Math.min(20, n) : n);
    }

    // ---- armour ----

    /** The luck and scale entries ("subtropical_luck") of the armour pieces' {@code base} stats, summed. */
    public static Map<String, Double> armorStats(Iterable<ItemStack> pieces) {
        Map<String, Double> sum = new TreeMap<>();
        for (ItemStack st : pieces) {
            var cd = st.get(DataComponentTypes.CUSTOM_DATA);
            if (cd == null) {
                continue;
            }
            cd.copyNbt().getList("base").ifPresent(list -> {
                for (int i = 0; i < list.size(); i++) {
                    list.getCompound(i).ifPresent(e -> {
                        String id = e.getString("id", "");
                        if (id.endsWith("_luck") || id.endsWith("_scale")) {
                            sum.merge(id, e.getDouble("cur", 0), Double::sum);
                        }
                    });
                }
            });
        }
        return sum;
    }

    /** What the stats give at this location: its climate's, its own and the global ones. {@code stat} is "luck" or "scale". */
    public static int stat(Map<String, Double> stats, Locations.Location l, String stat) {
        double sum = 0;
        for (String area : new String[]{climate(l), l.id(), "global"}) {
            sum += stats.getOrDefault(area + "_" + stat, 0.0);
        }
        return (int) Math.round(sum);
    }
}
