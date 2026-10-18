package dev.landofif.tacklebox.hud;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The server's community goal: all players' catches towards a goal (boss bar {@code Community Goal: 21.28K/100k Catches}) and
 * how many of them the player made. The mod counts the player's own catches while the bar is up, and takes the server's number from the {@code /communityevent} reply:
 * {@code Community Catches: 21.21K/100k} then {@code Your Catches Contribution: 304}. Pure, so it is unit tested.
 */
public final class CommunityGoal {
    private static final String NUM = "([\\d.,]+\\s*[kKmM]?)";
    private static final Pattern BAR = Pattern.compile("community goal:\\s*" + NUM + "\\s*/\\s*" + NUM, Pattern.CASE_INSENSITIVE);
    private static final Pattern CATCHES = Pattern.compile("community catches:\\s*" + NUM + "\\s*/\\s*" + NUM, Pattern.CASE_INSENSITIVE);
    private static final Pattern MINE = Pattern.compile("your catches contribution:\\s*([\\d,]+)", Pattern.CASE_INSENSITIVE);
    /** The bar is read every half second; it counts as up while it was seen this recently. */
    private static final long BAR_MS = 3000;

    private static long current = -1, goal = -1, mine, seenAt;

    private CommunityGoal() {}

    /** "21.28K" as 21280, "100k" as 100000, "1,234" as 1234; -1 when it is not a number. */
    static long parse(String s) {
        String t = s.replace(",", "").replaceAll("\\s", "").toLowerCase(java.util.Locale.ROOT);
        long factor = 1;
        if (t.endsWith("k") || t.endsWith("m")) {
            factor = t.endsWith("k") ? 1000 : 1_000_000;
            t = t.substring(0, t.length() - 1);
        }
        try {
            return Math.round(Double.parseDouble(t) * factor);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    /** A boss bar title; true when it was the community goal bar. A goal below the last one seen is a new goal: the player's catches start over. */
    public static boolean onBar(String title, long now) {
        Matcher m = BAR.matcher(title);
        if (!m.find()) {
            return false;
        }
        long c = parse(m.group(1)), g = parse(m.group(2));
        if (c < 0 || g < 0) {
            return false;
        }
        current = c;
        goal = g;
        seenAt = now;
        return true;
    }

    /** A chat line (as {@code Matcher.normalize} writes it): the /communityevent reply sets the player's catches. */
    public static void onChat(String line) {
        Matcher m = MINE.matcher(line);
        if (m.find()) {
            mine = parse(m.group(1));
            return;
        }
        m = CATCHES.matcher(line);
        if (m.find()) {
            long c = parse(m.group(1));
            if (c > current) {
                current = c;
            }
            long g = parse(m.group(2));
            if (g >= 0) {
                goal = g;
            }
        }
    }

    /** One catch of the player; only counted while the goal bar is up. */
    public static void onCatch(long now) {
        if (up(now)) {
            mine++;
        }
    }

    public static boolean up(long now) {
        return current >= 0 && now - seenAt < BAR_MS;
    }

    public static long current() {
        return current;
    }

    public static long goal() {
        return goal;
    }

    public static long mine() {
        return mine;
    }

    /** Back what the last session saved; the bar is not up until it is seen again. */
    public static void restore(long savedCurrent, long savedGoal, long savedMine) {
        current = savedCurrent;
        goal = savedGoal;
        mine = savedMine;
        seenAt = 0;
    }
}
