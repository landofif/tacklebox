package dev.landofif.tacklebox.hud;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.function.LongFunction;
import java.util.regex.Pattern;

/**
 * The server's shop clocks and your votes (pure, the tests run it). The idea and the cosmetic store and vote facts come from
 * FOE+ (itshaxr/FOE-Plus, StoreTimerHandler); the tackle shop grid is checked against luna's logs.
 * <p>
 * Tackle shop: restocks every 4 hours at 23, 3, 7, 11, 15 and 19 UTC. All 52 {@code TACKLE SHOP » » Tackle items restocked!} lines
 * in luna's logs (2026-10-07 to 11-14) are on that grid, through both the EU and the US clock change, so it is a fixed UTC grid
 * and needs no saved anchor. Cosmetic store: resets at midnight UTC (from FOE+, no chat line for it, not seen in game yet).
 * Votes: every {@code VOTE » You received ...} line is one vote and counts for 24 hours; 7 sites (from FOE+).
 */
public final class Timers {
    public static final long TACKLE_PERIOD = 4L * 60 * 60 * 1000;
    static final long TACKLE_ANCHOR = Instant.parse("2026-11-05T23:00:00Z").toEpochMilli();
    public static final long DAY = 24L * 60 * 60 * 1000;
    public static final int VOTE_SITES = 7;

    // "TACKLE SHOP » » Tackle items restocked! Check the Fish Pro Shop ..." after Matcher.normalize; a player quoting it has "name >" in front
    private static final Pattern RESTOCK = Pattern.compile("^tackle shop >(?: >)? tackle items restocked", Pattern.CASE_INSENSITIVE);
    private static final Pattern VOTE = Pattern.compile("^vote > you received\\b", Pattern.CASE_INSENSITIVE);

    /** The last cosmetic store reset seen this session (0 = none yet), so each reset gives one title. */
    private static long cosmeticSeen;

    private Timers() {}

    /** A title to show: its big line and the line under it. */
    public record Popup(String title, String subtitle) {}

    public static boolean restockLine(String normalized) {
        return RESTOCK.matcher(normalized).find();
    }

    public static boolean voteLine(String normalized) {
        return VOTE.matcher(normalized).find();
    }

    /** Milliseconds until the next tackle shop restock. */
    public static long tackleIn(long now) {
        return TACKLE_PERIOD - Math.floorMod(now - TACKLE_ANCHOR, TACKLE_PERIOD);
    }

    /** Milliseconds until the cosmetic store resets (midnight UTC). */
    public static long cosmeticIn(long now) {
        return DAY - Math.floorMod(now, DAY);
    }

    /** Adds a vote and drops the ones older than a day; at most {@link #VOTE_SITES} are kept. */
    public static void addVote(List<Long> votes, long now) {
        votes.removeIf(t -> t == null || now - t >= DAY);
        votes.add(now);
        while (votes.size() > VOTE_SITES) {
            votes.remove(0);
        }
    }

    public static int votes(List<Long> votes, long now) {
        return (int) Math.min(VOTE_SITES, votes.stream().filter(t -> t != null && now - t < DAY).count());
    }

    /** The title for a restock line. */
    public static Popup restocked() {
        return new Popup("tackle shop", "restocked");
    }

    /**
     * Called while in game: the title for a cosmetic store reset, once per reset, or null. The first call only remembers the last
     * reset, so joining after one gives no title.
     */
    public static Popup cosmeticReset(long now) {
        long last = now - Math.floorMod(now, DAY);
        Popup p = cosmeticSeen != 0 && last > cosmeticSeen ? new Popup("cosmetic store", "reset") : null;
        cosmeticSeen = last;
        return p;
    }

    /** Leaving the server: the next join starts over. */
    public static void reset() {
        cosmeticSeen = 0;
    }

    /** The hud rows: tackle shop, cosmetic store and votes. {@code clock} writes a countdown. */
    public static List<TextHud.Row> rows(long now, List<Long> votes, boolean showVotes, LongFunction<String> clock) {
        List<TextHud.Row> rows = new ArrayList<>();
        rows.add(new TextHud.Row("tackle shop", clock.apply(tackleIn(now))));
        rows.add(new TextHud.Row("cosmetic store", clock.apply(cosmeticIn(now))));
        if (showVotes) {
            rows.add(new TextHud.Row("votes", votes(votes, now) + "/" + VOTE_SITES));
        }
        return rows;
    }
}
