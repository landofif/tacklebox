package dev.landofif.tacklebox.hud;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.function.LongFunction;

/**
 * The fish frenzy cycles (luna's sheet, in Middle European time): a cycle starts every 3 hours at 01, 04, ... 22 o'clock, a frenzy
 * can spawn in the first 2 hours of it and then lasts 1 hour from its own spawn, so the next window always opens on time.
 */
final class FrenzySchedule {
    static final ZoneId ZONE = ZoneId.of("Europe/Berlin");
    private static final int WINDOW_HOURS = 2, CYCLE_HOURS = 3;

    /** {@code open}: inside the spawn window, {@code ms} is the time until it closes; else until the next window opens. */
    record State(boolean open, long ms) {}

    private FrenzySchedule() {}

    static State at(Instant now) {
        LocalDateTime local = now.atZone(ZONE).toLocalDateTime();
        LocalDateTime hour = local.truncatedTo(ChronoUnit.HOURS);
        LocalDateTime start = hour.minusHours((hour.getHour() + 2) % CYCLE_HOURS); // hours 1, 4, 7 ... are cycle starts
        Instant windowEnd = start.plusHours(WINDOW_HOURS).atZone(ZONE).toInstant();
        if (now.isBefore(windowEnd)) {
            return new State(true, windowEnd.toEpochMilli() - now.toEpochMilli());
        }
        return new State(false, start.plusHours(CYCLE_HOURS).atZone(ZONE).toInstant().toEpochMilli() - now.toEpochMilli());
    }

    /** The value of the server hud line: the frenzy the chat announced, else the window state. {@code clock} writes a duration. */
    static String describe(long now, long activeLeft, String place, LongFunction<String> clock) {
        if (activeLeft > 0) {
            return place + " " + clock.apply(activeLeft);
        }
        State s = at(Instant.ofEpochMilli(now));
        return s.open() ? "may spawn, " + clock.apply(s.ms()) + " left" : "next window in " + clock.apply(s.ms());
    }
}
