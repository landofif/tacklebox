package dev.landofif.tacklebox.hud;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * What can be caught where right now, and when the next chance comes (luna, 2026-11-01): the cryptids of moons, rainbows and
 * the in-game clock, lightning in a bottle in storms, and the fabled fish. Pure, so the tests run it: {@link CryptidHud} feeds it
 * the chat state, the clock and the time.
 */
public final class Cryptids {
    /** A moon rises every 160 minutes to the second and lasts 600 seconds (luna's logs, 75 moons 2026-09-05 to 11-01). */
    public static final long CYCLE = 160 * 60_000L, MOON_LENGTH = 600_000L;
    /** The last moonrise in luna's logs (2026-11-01 18:42:55 Middle European time), until the mod sees one itself. */
    public static final long SEEN_RISE = 1_793_554_975_000L;
    /** Real milliseconds per in-game minute: a game day is 20 real minutes, like vanilla (assumed; 8 days are the 160 minute cycle). */
    static final double MS_PER_GAME_MINUTE = 1_200_000 / 1440.0;

    public static final int MOON = 0xFFDD88FF, BLOOD = 0xFFFF5555, RAINBOW = 0xFFFF7BE0, STORM = 0xFFFFD83D, FABLED = 0xFFE0413A, CLOCK = 0xFF7FD18B;

    /**
     * Where to fish and what for: "toledo bend", "rougarou 1/300", "blood/claw" (the drop is "" when the catch is the item).
     * {@code key} names its dry streak, {@code loc} is the location id catches must be at ("" anywhere), {@code items} the
     * drop names (lowercase) that end the streak.
     */
    public record Spot(String key, String loc, String place, String what, String drop, List<String> items) {
        String brief() {
            return place + " " + what;
        }
    }

    /** One row of the hud: a coloured label and a grey value; {@code head} rows name an event, the others are its spots. */
    public record Row(String label, String value, int color, boolean head) {}

    /** A cryptid that bites between two in-game hours. */
    record Window(String name, int from, int to, Spot spot) {}

    static final Spot ROUGAROU = new Spot("rougarou", "toledobend", "toledo bend", "rougarou 1/300", "blood/claw", List.of("rougarou blood", "rougarou claw"));
    static final Spot BESSIE_MOON = new Spot("bessie", "greatlakes", "great lakes", "bessie 1/500", "silver scale", List.of("silver scale"));
    static final Spot CAPSULE = new Spot("capsule", "", "anywhere", "infusion capsule 1/300", "", List.of("infusion capsule"));
    static final Spot GARABONCIAS = new Spot("garaboncias", "danube", "danube", "garaboncias 1/250", "concoction/essence", List.of("vile concoction", "purifying essence"));
    static final List<Window> WINDOWS = List.of(
            new Window("bigfoot", 1, 3, new Spot("bigfoot", "spawn", "cypress lake", "bigfoot 1/300", "tooth/fur", List.of("bigfoot tooth", "bigfoot fur"))),
            new Window("titanoboa", 2, 4, new Spot("titanoboa", "amazon", "amazon", "titanoboa 1/1500", "titan's fang", List.of("titan"))),
            new Window("bessie", 4, 6, new Spot("bessie", "greatlakes", "great lakes", "bessie 1/1000", "silver scale", List.of("silver scale"))));
    /** Every spot whose drop ends a dry streak, for {@link #dropKey}; the fabled fish ends by its catch instead. */
    private static final List<Spot> DROPS = List.of(ROUGAROU, BESSIE_MOON, CAPSULE, GARABONCIAS, WINDOWS.get(0).spot, WINDOWS.get(1).spot, lightning("1/500"));

    static Spot lightning(String odds) {
        return new Spot("lightning", "", "anywhere", "lightning bottle " + odds, "", List.of("lightning in a bottle"));
    }

    private Cryptids() {}

    /** What is on, as the hud and the popups see it. {@code moon} is "Blood Moon" or "", {@code storm} "Thunderstorm", "Supercell" or "", {@code fabled} the place or "". */
    public record State(long now, String moon, long moonRise, String storm, boolean rainbow, String fabled, int clock, java.util.Set<String> off,
                        Map<String, Integer> dry) {}

    /** The switches in the settings, one per kind of event; each hides it from the hud and the popups. */
    public static final List<String> KINDS = List.of("moons", "rainbow", "storms", "fabled", "bigfoot", "titanoboa", "bessie");

    /** The switch an event name belongs to: "blood moon" is "moons", "supercell" is "storms". */
    static String kind(String name) {
        if (name.endsWith(" moon")) return "moons";
        if (name.equals("thunderstorm") || name.equals("supercell")) return "storms";
        return name;
    }

    /** The next moonrise after {@code now}, from one seen rise; -1 when none was seen. */
    public static long nextMoon(long rise, long now) {
        if (rise <= 0) {
            return -1;
        }
        long k = Math.floorDiv(now - rise, CYCLE) + 1;
        return rise + k * CYCLE;
    }

    /** "2:53", "AM" as minutes since midnight; -1 when the bar was not read. 12 AM is midnight, 12 PM noon. */
    public static int clockMinutes(String time, String ampm) {
        int colon = time == null ? -1 : time.indexOf(':');
        if (colon < 0 || ampm == null || ampm.isEmpty()) {
            return -1;
        }
        try {
            int h = Integer.parseInt(time.substring(0, colon)) % 12, m = Integer.parseInt(time.substring(colon + 1));
            return (ampm.equalsIgnoreCase("PM") ? h + 12 : h) * 60 + m;
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** The events on now, each with its spots, by name: the hud's top rows and the popups' lines. */
    static Map<String, List<Spot>> now(State s) {
        Map<String, List<Spot>> out = new LinkedHashMap<>();
        if (!s.moon.isEmpty()) {
            List<Spot> spots = new ArrayList<>(List.of(ROUGAROU, BESSIE_MOON));
            if (s.moon.toLowerCase(Locale.ROOT).startsWith("blood")) {
                spots.add(CAPSULE);
            }
            out.put(s.moon.toLowerCase(Locale.ROOT), spots);
        }
        if (s.rainbow) {
            out.put("rainbow", List.of(GARABONCIAS));
        }
        if (!s.storm.isEmpty()) {
            boolean supercell = s.storm.equalsIgnoreCase("supercell");
            // joining during one gives "A Thunderstorm is ongoing", which the server hud keeps as plain "Storm"
            out.put(supercell ? "supercell" : "thunderstorm", List.of(lightning(supercell ? "1/250" : "1/500")));
        }
        if (!s.fabled.isEmpty()) {
            String id = ServerInfo.LOCATIONS.entrySet().stream().filter(e -> e.getValue().equalsIgnoreCase(s.fabled)).map(Map.Entry::getKey).findFirst().orElse("?");
            out.put("fabled", List.of(new Spot("fabled", id, s.fabled.toLowerCase(Locale.ROOT), "fabled fish 1/3000", "", List.of())));
        }
        for (Window w : WINDOWS) {
            if (open(w, s.clock)) {
                out.put(w.name, List.of(w.spot));
            }
        }
        out.keySet().removeIf(name -> s.off.contains(kind(name)));
        return out;
    }

    static boolean open(Window w, int clock) {
        return clock >= w.from * 60 && clock < w.to * 60;
    }

    /** Real milliseconds from the in-game time {@code clock} to {@code minute}, going forward. */
    static long gameMs(int clock, int minute) {
        return Math.round(Math.floorMod(minute - clock, 1440) * MS_PER_GAME_MINUTE);
    }

    /** The hud: what is on, with its spots and time left, then what comes next, soonest first. */
    public static List<Row> rows(State s, java.util.function.LongFunction<String> clock) {
        List<Row> out = new ArrayList<>();
        for (Map.Entry<String, List<Spot>> e : now(s).entrySet()) {
            out.add(new Row(e.getKey(), left(s, e.getKey(), clock), color(e.getKey()), true));
            for (Spot spot : e.getValue()) {
                out.add(new Row(spot.place, spot.what + (spot.drop.isEmpty() ? "" : " · " + spot.drop) + " (" + s.dry.getOrDefault(spot.key, 0) + ")", 0, false));
            }
        }
        List<long[]> order = new ArrayList<>(); // {ms until, index}: -1 is the moon, else a window
        long moon = nextMoon(s.moonRise, s.now);
        if (s.moon.isEmpty() && moon > 0 && !s.off.contains("moons")) {
            order.add(new long[]{moon - s.now, -1});
        }
        if (s.clock >= 0) {
            for (int i = 0; i < WINDOWS.size(); i++) {
                if (!open(WINDOWS.get(i), s.clock) && !s.off.contains(WINDOWS.get(i).name)) {
                    order.add(new long[]{gameMs(s.clock, WINDOWS.get(i).from * 60), i});
                }
            }
        }
        order.sort((a, b) -> Long.compare(a[0], b[0]));
        if (!order.isEmpty()) {
            out.add(new Row("next", "", 0, true));
        }
        for (long[] o : order) {
            String name = o[1] < 0 ? "moon · " + ROUGAROU.place + ", " + BESSIE_MOON.place : WINDOWS.get((int) o[1]).name + " · " + WINDOWS.get((int) o[1]).spot.place;
            out.add(new Row(name, clock.apply(o[0]), o[1] < 0 ? MOON : CLOCK, false));
        }
        return out;
    }

    /** Time left of an event: a moon from its rise, a window from the clock; rainbows, storms and the fabled event end by chat, so "". */
    private static String left(State s, String name, java.util.function.LongFunction<String> clock) {
        if (name.endsWith(" moon") && s.moonRise > 0 && s.now - s.moonRise < MOON_LENGTH) {
            return clock.apply(s.moonRise + MOON_LENGTH - s.now);
        }
        for (Window w : WINDOWS) {
            if (w.name.equals(name)) {
                return clock.apply(gameMs(s.clock, w.to * 60));
            }
        }
        return "";
    }

    static int color(String name) {
        if (name.startsWith("blood")) return BLOOD;
        if (name.endsWith(" moon")) return MOON;
        if (name.equals("rainbow")) return RAINBOW;
        if (name.equals("thunderstorm") || name.equals("supercell")) return STORM;
        if (name.equals("fabled")) return FABLED;
        return CLOCK;
    }

    /** A popup: the event as the title, where to go as the subtitle ("danube garaboncias 1/250"), in the event's colour. */
    public record Popup(String title, String subtitle, int color) {}

    static Popup popup(String name, List<Spot> spots) {
        List<String> parts = new ArrayList<>();
        for (Spot s : spots) {
            parts.add(s.brief());
        }
        return new Popup(name, String.join(" · ", parts), color(name));
    }

    /** The dry streaks a catch at {@code location} adds one to: every spot of an event that is on, at that place or anywhere. */
    public static java.util.Set<String> counting(State s, String location) {
        java.util.Set<String> out = new java.util.LinkedHashSet<>();
        for (List<Spot> spots : now(s).values()) {
            for (Spot spot : spots) {
                if (location != null && location.startsWith(spot.loc)) { // "" is anywhere; cypress lake is "spawn" or "spawnhub"
                    out.add(spot.key);
                }
            }
        }
        return out;
    }

    private static final java.util.regex.Pattern OWN_DROP = java.util.regex.Pattern.compile("^rare catch! you (?:pulled|caught) ", java.util.regex.Pattern.CASE_INSENSITIVE);

    /** The dry streak an own rare catch line ends ("RARE CATCH! You pulled 1x Rougarou Claw" is "rougarou"), or null. {@code line} is normalized. */
    public static String dropKey(String line) {
        if (!OWN_DROP.matcher(line).find()) {
            return null;
        }
        String l = line.toLowerCase(Locale.ROOT);
        for (Spot spot : DROPS) {
            for (String item : spot.items) {
                if (l.contains(item)) {
                    return spot.key;
                }
            }
        }
        return null;
    }

    private static java.util.Set<String> shown = new java.util.HashSet<>();

    /** The popups of events that started since the last call (each once while it lasts). */
    public static List<Popup> started(State s) {
        Map<String, List<Spot>> on = now(s);
        List<Popup> out = new ArrayList<>();
        for (Map.Entry<String, List<Spot>> e : on.entrySet()) {
            if (!shown.contains(e.getKey())) {
                out.add(popup(e.getKey(), e.getValue()));
            }
        }
        shown = new java.util.HashSet<>(on.keySet());
        return out;
    }

    /** For tests and on leaving: forget which popups were shown. */
    public static void reset() {
        shown = new java.util.HashSet<>();
    }
}
