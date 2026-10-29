package dev.landofif.tacklebox.hud;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** The chat lines of the fabled event and the fish frenzy, as plain string parsing (formats in docs/fishonmc-research.md). */
final class EventLines {
    // FISH FRENZY ALERT / Fish at Kenai River for 1 hour of buffed rates
    private static final Pattern FRENZY_START = Pattern.compile("^Fish at (.+?) for (\\d+) hours? of buffed rates");
    // FISH FRENZY » A Fish Frenzy is active at Kenai River (for 16m 56s)
    private static final Pattern FRENZY_LEFT = Pattern.compile("FISH FRENZY [>»] A Fish Frenzy is active at (.+?) \\(for ((?:\\d+h ?)?(?:\\d+m ?)?(?:\\d+s)?)\\)");
    private static final Pattern FRENZY_END = Pattern.compile("^[^A-Za-z0-9]*FRENZY [>»] The Fish Frenzy has ended");
    private static final Pattern PART = Pattern.compile("(\\d+)([hms])");
    private static final Pattern FABLED_START = Pattern.compile("^Visit the (.+?) to\\b");

    record Frenzy(String place, long remainingMs) {}

    private EventLines() {}

    /** The alert's body line or a "is active" reminder; null for anything else. */
    static Frenzy frenzy(String s) {
        Matcher m = FRENZY_START.matcher(s);
        if (m.find()) {
            return new Frenzy(m.group(1), Long.parseLong(m.group(2)) * 3_600_000L);
        }
        m = FRENZY_LEFT.matcher(s);
        if (m.find()) {
            long ms = 0;
            Matcher p = PART.matcher(m.group(2));
            while (p.find()) {
                ms += Long.parseLong(p.group(1)) * switch (p.group(2)) {
                    case "h" -> 3_600_000L;
                    case "m" -> 60_000L;
                    default -> 1000L;
                };
            }
            return new Frenzy(m.group(1), ms);
        }
        return null;
    }

    static boolean frenzyEnded(String s) {
        return FRENZY_END.matcher(s).find();
    }

    /** "FABLED FISH EVENT": the line under it says whether the event started or ended. */
    static boolean fabledHeader(String s) {
        return s.replaceAll("^[^A-Za-z0-9]*", "").trim().equals("FABLED FISH EVENT");
    }

    /** The location of "Visit the Cypress Lake to", or null. */
    static String fabledStart(String s) {
        Matcher m = FABLED_START.matcher(s);
        return m.find() ? m.group(1) : null;
    }

    static boolean fabledEnded(String s) {
        return s.startsWith("Caught by ");
    }
}
