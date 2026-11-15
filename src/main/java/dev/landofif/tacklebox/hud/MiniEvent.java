package dev.landofif.tacklebox.hud;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The mini event (luna, 2026-11-15): what the server answers to /minievent, kept for the mini event hud. Pure, so the tests
 * run it: {@link MiniEventHud} feeds it the chat and the catches and sends the command when {@link #due} says so. The idea and
 * the timing (every 10 minutes, shortly after a catch, the answer hidden) come from FOE+ (itshaxr/FOE-Plus, MiniEventHandler),
 * which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow); the answer's format is from luna's logs. No code was copied.
 * <p>
 * An answer is a header ({@code MED SEA MINI EVENT » Ends in 2d 1h 13m 26s}), {@code Daily missions reroll in 18h 13m 26s},
 * then one message per mission, two lines each: {@code  - 🎣 Mediterranean Sea  ✔} and {@code     ┗ (59/100) ∞ x25}
 * (the marks are {@code ∞ xN}, {@code One Timer} and {@code Daily}; a variant is a badge glyph in the name).
 */
public final class MiniEvent {
    /** How often the command is sent while an event is on, and while none is. */
    public static final long REFRESH = 10 * 60_000L, IDLE_REFRESH = 30 * 60_000L;
    /** The first send after joining, the send after a catch, and the least time between two sends. */
    static final long FIRST = 40_000L, CATCH_DELAY = 5_000L, CATCH_GAP = 60_000L;
    /** How long after a send the answer is hidden, and how long after a header its lines still belong to it. */
    static final long HIDE = 3_000L, CAPTURE = 2_000L;

    public static final int GREEN = 0xFF55FF55;

    private static final Pattern HEADER = Pattern.compile("^([A-Z][A-Za-z ]*MINI EVENT) » Ends in (.+)$");
    private static final Pattern REROLL = Pattern.compile("^Daily missions reroll in (.+)$");
    private static final Pattern COMPLETE = Pattern.compile("^[A-Z][A-Za-z ]*MINI EVENT » MISSION COMPLETE");
    private static final Pattern MISSION = Pattern.compile("^\\s*-\\s*\\S+\\s+([^\\n]+?)\\s*(✔)?\\s*\\n\\s*┗\\s*\\(([\\d,]+)/([\\d,]+)\\)\\s*([^\\n]*?)\\s*$");
    private static final Pattern UNIT = Pattern.compile("(\\d+)\\s*([dhms])");

    private static final String SMALL = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ", PLAIN = "abcdefghijklmnopqrstuvwxyz";
    /** The server pack's variant and rarity badges as words (research section 2). */
    private static final Map<Character, String> BADGES = Map.ofEntries(Map.entry('', "albino"), Map.entry('', "melanistic"),
            Map.entry('', "trophy"), Map.entry('', "fabled"), Map.entry('', "common"), Map.entry('', "rare"),
            Map.entry('', "epic"), Map.entry('', "legendary"), Map.entry('', "mythical"));
    private static final List<String> SIZES = List.of("baby", "juvenile", "adult", "large", "gigantic");
    private static final List<String> VARIANTS = List.of("albino", "melanistic", "trophy", "fabled");
    private static final List<String> RARITIES = List.of("common", "rare", "epic", "legendary", "mythical");

    /** A mission: its name as plain lowercase words, the count, and its mark ("∞ x25", "one timer", "daily"). */
    public static final class Mission {
        public final String name, mark;
        public int cur, max;

        Mission(String name, int cur, int max, String mark) {
            this.name = name;
            this.cur = cur;
            this.max = max;
            this.mark = mark;
        }

        /** A mission that starts over when it is done ("∞ x25"). */
        public boolean repeats() {
            return mark.startsWith("∞");
        }

        String kind() {
            return repeats() ? "∞" : mark;
        }
    }

    private static String title = "";
    private static long endsAt, rerollAt;
    private static final List<Mission> missions = new ArrayList<>();
    private static long answerAt = Long.MIN_VALUE / 2, completeAt = Long.MIN_VALUE / 2;
    private static boolean hidingAnswer;
    private static long hideUntil, nextAsk, lastAsk = Long.MIN_VALUE / 2;

    private MiniEvent() {}

    /**
     * One chat message as the server sent it (pack glyphs and line breaks kept).
     *
     * @return true when it is part of the answer to the mod's own quiet /minievent and is to be hidden
     */
    public static boolean onChat(String raw, long now) {
        String s = raw.strip();
        Matcher m = HEADER.matcher(s);
        if (m.find()) {
            title = m.group(1).toLowerCase(Locale.ROOT);
            endsAt = now + duration(m.group(2));
            rerollAt = 0;
            missions.clear();
            answerAt = now;
            hidingAnswer = now <= hideUntil;
            hideUntil = 0;
            if (hidingAnswer && nextAsk > lastAsk + REFRESH) {
                nextAsk = lastAsk + REFRESH; // the send was planned as if no event were on
            }
            return hidingAnswer;
        }
        boolean inAnswer = now - answerAt <= CAPTURE;
        m = REROLL.matcher(s);
        if (m.find() && inAnswer) {
            rerollAt = now + duration(m.group(1));
            return hidingAnswer;
        }
        if (COMPLETE.matcher(s).find()) {
            completeAt = now;
            return false;
        }
        m = MISSION.matcher(raw);
        if (m.find()) {
            Mission read = new Mission(words(m.group(1)), number(m.group(3)), number(m.group(4)), m.group(5).toLowerCase(Locale.ROOT));
            if (inAnswer) {
                missions.add(read);
                return hidingAnswer;
            }
            if (now - completeAt <= CAPTURE) { // the mission under a MISSION COMPLETE line: its new count
                completeAt = Long.MIN_VALUE / 2;
                for (Mission old : missions) {
                    if (old.name.equals(read.name) && old.kind().equals(read.kind())) {
                        missions.set(missions.indexOf(old), read);
                        break;
                    }
                }
            }
            return false;
        }
        if (now <= hideUntil && isNoEventReply(s)) {
            hideUntil = 0;
            clear();
            return true;
        }
        return false;
    }

    /**
     * Any other answer to the quiet command: a server line about the mini event that is none of the known announcements.
     * The server's wording with no event on is not in luna's logs, so this is a guess.
     */
    private static boolean isNoEventReply(String s) {
        String l = s.toLowerCase(Locale.ROOT);
        return l.contains("mini event") && !s.startsWith("[") && !s.startsWith("!") && !l.contains("mission complete")
                && !l.contains("completed every") && !l.contains("now available");
    }

    /** "2d 1h 13m 26s" in milliseconds. */
    static long duration(String s) {
        long ms = 0;
        Matcher m = UNIT.matcher(s);
        while (m.find()) {
            long n = Long.parseLong(m.group(1));
            ms += n * switch (m.group(2)) {
                case "d" -> 86_400_000L;
                case "h" -> 3_600_000L;
                case "m" -> 60_000L;
                default -> 1000L;
            };
        }
        return ms;
    }

    private static int number(String s) {
        return Integer.parseInt(s.replace(",", ""));
    }

    /** A mission name as plain lowercase words: small capitals as letters, variant and rarity badges as their words, other pack glyphs gone. */
    static String words(String name) {
        StringBuilder out = new StringBuilder();
        for (char c : name.toCharArray()) {
            int small = SMALL.indexOf(c);
            if (small >= 0) {
                out.append(PLAIN.charAt(small));
            } else if (BADGES.containsKey(c)) {
                out.append(' ').append(BADGES.get(c)).append(' ');
            } else if (c < '' || c > '') {
                out.append(c);
            }
        }
        return out.toString().toLowerCase(Locale.ROOT).trim().replaceAll("\\s+", " ");
    }

    /** Whether an event is on, as far as the last answer says. */
    public static boolean on(long now) {
        return !title.isEmpty() && now < endsAt;
    }

    public static List<Mission> missions() {
        return missions;
    }

    // ---------------------------------------------------------------- catches

    /** What a caught fish is: the location id, variant, size, rarity and fish group from its item data, and its name. */
    public record Fish(String location, String variant, String size, String rarity, String group, String name) {}

    /** An own catch: every unfinished mission it counts for goes up by one, until the server's next answer has the exact numbers. */
    public static void onCatch(Fish f, long now) {
        if (!on(now)) {
            return;
        }
        for (Mission mi : missions) {
            if (mi.cur < mi.max && counts(mi.name, f)) {
                mi.cur++;
            }
        }
    }

    /**
     * Whether a catch counts for a mission: everything the name says has to fit the fish, the place ("mediterranean sea"), the
     * size ("gigantic"), the variant, the rarity, and what is left is the fish's name or its group ("rays"). Pet missions and
     * anything else that cannot be told from the fish are left to the server's numbers.
     */
    static boolean counts(String mission, Fish f) {
        String w = mission;
        String at = f.location().startsWith("spawn") ? "spawn" : f.location();
        place:
        for (GearCheck.Place p : GearCheck.PLACES) {
            for (String name : List.of(ServerInfo.LOCATIONS.get(p.id()).toLowerCase(Locale.ROOT), p.name())) {
                if (w.equals(name) || w.startsWith(name + " ")) {
                    if (!p.id().equals(at)) {
                        return false;
                    }
                    w = w.substring(name.length()).trim();
                    break place;
                }
            }
        }
        String[] is = {f.size(), f.variant(), "mythic".equals(f.rarity()) ? "mythical" : f.rarity()};
        int i = 0;
        for (List<String> words : List.of(SIZES, VARIANTS, RARITIES)) {
            for (String word : words) {
                if ((" " + w + " ").contains(" " + word + " ")) {
                    if (!word.equalsIgnoreCase(is[i])) {
                        return false;
                    }
                    w = (" " + w + " ").replace(" " + word + " ", " ").trim();
                }
            }
            i++;
        }
        if (w.isEmpty() || w.equalsIgnoreCase(f.name())) {
            return true;
        }
        String g = letters(f.group()), m = letters(w);
        return !g.isEmpty() && (m.equals(g) || m.equals(g + "s") || (m + "s").equals(g));
    }

    private static String letters(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
    }

    // ---------------------------------------------------------------- when to ask

    /**
     * Called every tick while the hud is on: true when /minievent is to be sent now. The first send is {@link #FIRST} after
     * joining, then every {@link #REFRESH} ({@link #IDLE_REFRESH} with no event), never while a screen is open.
     */
    public static boolean due(long now, boolean screenOpen) {
        if (nextAsk == 0) {
            nextAsk = now + FIRST;
        }
        if (now < nextAsk || screenOpen) {
            return false;
        }
        lastAsk = now;
        nextAsk = now + (on(now) ? REFRESH : IDLE_REFRESH);
        hideUntil = now + HIDE;
        return true;
    }

    /** A catch during an event: ask again in {@link #CATCH_DELAY}, but not within {@link #CATCH_GAP} of the last send. */
    public static void askAfterCatch(long now) {
        if (!on(now)) {
            return;
        }
        long at = Math.max(now + CATCH_DELAY, lastAsk + CATCH_GAP);
        if (nextAsk == 0 || at < nextAsk) {
            nextAsk = at;
        }
    }

    // ---------------------------------------------------------------- the hud

    /** The hud's title: the event as the server names it, "mini event" before the first answer. */
    public static String title() {
        return title.isEmpty() ? "mini event" : title;
    }

    /** The hud rows: when it ends, when the daily missions reroll, then the missions that are not done and the repeating ones. Empty with no event. */
    public static List<TextHud.Row> rows(long now) {
        List<TextHud.Row> rows = new ArrayList<>();
        if (!on(now)) {
            return rows;
        }
        rows.add(new TextHud.Row("ends in", clock(endsAt - now)));
        if (rerollAt > now) {
            rows.add(new TextHud.Row("daily reroll", clock(rerollAt - now)));
        }
        for (Mission m : missions) {
            if (m.cur >= m.max && !m.repeats()) {
                continue;
            }
            rows.add(new TextHud.Row(m.name, m.cur + "/" + m.max + " · " + m.mark, 0, m.cur >= m.max ? GREEN : 0));
        }
        return rows;
    }

    /** A countdown: "2d 1h", "18h 13m", "4:05". */
    static String clock(long ms) {
        long s = Math.max(0, ms / 1000);
        if (s >= 86_400) {
            return s / 86_400 + "d " + s % 86_400 / 3600 + "h";
        }
        if (s >= 3600) {
            return s / 3600 + "h " + String.format(Locale.ROOT, "%02d", s % 3600 / 60) + "m";
        }
        return String.format(Locale.ROOT, "%d:%02d", s / 60, s % 60);
    }

    private static void clear() {
        title = "";
        endsAt = 0;
        rerollAt = 0;
        missions.clear();
    }

    /** For tests, on leaving the server and while the hud is off: forget the event and start asking over. */
    public static void reset() {
        clear();
        answerAt = completeAt = lastAsk = Long.MIN_VALUE / 2;
        hidingAnswer = false;
        hideUntil = nextAsk = 0;
    }
}
