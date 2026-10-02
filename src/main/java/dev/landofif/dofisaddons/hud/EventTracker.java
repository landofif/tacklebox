package dev.landofif.dofisaddons.hud;

import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;

/**
 * Weather and world events from their chat announcements (ported from fishutils' event timer, strings in
 * research section 4). The server sends no countdown or end message for weather, so each event counts
 * from its announcement against a typical length and drops off at that estimate; moons end on their
 * "has set" line instead.
 */
public final class EventTracker {
    private static final long GRACE_MS = 3_000L;

    public enum Event {
        RAIN("rain", "RAIN » A rain shower has begun!", 600, false),
        THUNDERSTORM("thunderstorm", "STORM » A thunderstorm has formed!", 600, false),
        SUPERCELL("supercell", "STORM » A supercell has formed!", 600, false),
        RAINBOW("rainbow", "RAINBOW » A Rainbow has appeared!", 420, false),
        GOLD_RUSH("gold_rush", "GOLD RUSH » A Gold Rush has begun!", 960, false),
        BLOOMING_OASIS("blooming_oasis", "BLOOMING OASIS » A Blooming Oasis has appeared!", 600, false),
        FULL_MOON("full_moon", "FULL MOON » A Full Moon has risen", 600, true),
        BLUE_MOON("blue_moon", "BLUE MOON » A Blue Moon has risen", 600, true),
        SUPER_MOON("super_moon", "SUPER MOON » A Super Moon has risen", 600, true),
        BLOOD_MOON("blood_moon", "BLOOD MOON » A Blood Moon has risen", 600, true);

        public final String id;
        final String announcement;
        final int durationSec;
        final boolean moon;

        Event(String id, String announcement, int durationSec, boolean moon) {
            this.id = id;
            this.announcement = announcement;
            this.durationSec = durationSec;
            this.moon = moon;
        }
    }

    private static final Map<Event, Long> ACTIVE = new EnumMap<>(Event.class);

    private EventTracker() {}

    /** Every incoming server chat line. */
    public static synchronized void onChat(String raw) {
        if (raw == null || raw.isEmpty()) {
            return;
        }
        for (Event e : Event.values()) {
            if (raw.contains(e.announcement)) {
                ACTIVE.put(e, System.currentTimeMillis());
                return;
            }
        }
        if (raw.contains("Moon has set")) {
            ACTIVE.keySet().removeIf(e -> e.moon);
        }
    }

    public static synchronized void clear() {
        ACTIVE.clear();
    }

    /** Estimated seconds left for an event by id, or -1 if it isnt on. */
    public static synchronized int secondsLeft(String id) {
        long now = System.currentTimeMillis();
        ACTIVE.entrySet().removeIf(en -> now - en.getValue() > en.getKey().durationSec * 1000L + GRACE_MS);
        for (Map.Entry<Event, Long> en : ACTIVE.entrySet()) {
            if (en.getKey().id.equals(id)) {
                return Math.max(0, en.getKey().durationSec - (int) ((now - en.getValue()) / 1000L));
            }
        }
        return -1;
    }

    /** True if any tracked event is on. */
    public static synchronized boolean any() {
        for (Event e : Event.values()) {
            if (secondsLeft(e.id) >= 0) {
                return true;
            }
        }
        return false;
    }

    public static String clock(int seconds) {
        return String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60);
    }
}
