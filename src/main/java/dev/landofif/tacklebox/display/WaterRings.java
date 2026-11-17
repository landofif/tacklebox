package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Flat rings on the water at your own bobber (2026-11-17, luna: "water effects, for when a fish is coming to bite";
 * docs/display/catch-effects.md). While a fish swims in, rings at the bobber that come quicker as it nears and one bigger
 * ring on the bite; a wake of small rings behind the fish; one ring where the bobber was when you catch a fish, in its
 * rarity colour. Each ring grows and fades, nothing flashes. Pure numbers and state, no Minecraft classes:
 * {@link WaterRingRenderer} feeds it the bobber and the water top, and draws it.
 */
public final class WaterRings {
    /** A ring on the water: its middle, the water top, when it started and for how long, how it grows, its colour and strength. */
    public record Ring(double x, double y, double z, long born, long life, float from, float to, int rgb, float strength) {
        /** 0 when it starts, 1 when it is over. */
        public float progress(long now) {
            return Math.max(0, Math.min(1, (now - born) / (float) life));
        }

        /** Grows quickly at first and slows down, like a ripple. */
        public float radius(long now) {
            float p = progress(now);
            return from + (to - from) * (1 - (1 - p) * (1 - p));
        }

        /** Fades in over the first moment (no pop) and out while it grows. */
        public float alpha(long now) {
            float p = progress(now);
            return strength * Math.min(1, p / FADE_IN) * (1 - p);
        }

        public boolean over(long now) {
            return now - born >= life;
        }
    }

    /** Where your bobber floats: its x and z, the water top as y, and when it was there. */
    public record Spot(double x, double y, double z, long at) {}

    /** Receives the corners of the quads, four per segment, going round the ring. */
    @FunctionalInterface
    public interface Sink {
        void vertex(float x, float y, float z);
    }

    public static final int MAX_RINGS = 48;
    public static final int SEGMENTS = 40;
    /** The band is this wide and floats this far above the water top. */
    public static final float WIDTH = 0.06f, LIFT = 0.03f;
    static final float FADE_IN = 0.12f;

    static final long BITE_LIFE = 600;
    static final float BITE_FROM = 0.2f, BITE_TO = 1.0f, BITE_BIG_TO = 1.4f;
    /** The server re-sends BITE! while a fish is hooked: one ring per bite (like BobberTimer). */
    static final long BITE_GAP_MS = 1500;
    static final double MIN_INTERVAL = 0.25, MAX_INTERVAL = 1.0, INTERVAL_PER_SECOND = 0.35;

    static final long WAKE_LIFE = 700;
    static final float WAKE_FROM = 0.15f, WAKE_TO = 0.6f, WAKE_STRENGTH = 0.55f;
    static final double WAKE_STEP = 0.35;
    static final long WAKE_GAP_MS = 120;

    static final long CATCH_LIFE = 500;
    static final float CATCH_FROM = 0.3f, CATCH_TO = 1.5f, COMMON_STRENGTH = 0.5f;
    /** The bobber is gone the moment the fish is caught: its last spot is kept this long. */
    static final long SPOT_KEEP_MS = 2000;

    private static final List<Ring> rings = new ArrayList<>();
    private static Spot bobber;
    private static long nextBeat, lastBite, wakeAt;
    private static double wakeX = Double.NaN, wakeZ = Double.NaN;

    private WaterRings() {}

    // ---------------------------------------------------------------- numbers

    /** Seconds between the bite rings with {@code secondsLeft} until the bite: 1 s far out, 0.25 s right before it. */
    public static double interval(double secondsLeft) {
        return Math.max(MIN_INTERVAL, Math.min(MAX_INTERVAL, secondsLeft * INTERVAL_PER_SECOND));
    }

    /** The catch ring's colour: the rarity's, white for a common or a rarity we do not know. */
    public static int catchColor(String rarity) {
        int rgb = Rarity.color(rarity);
        return rgb < 0 || "common".equals(rarity) ? 0xFFFFFF : rgb;
    }

    /** A common fish (or one we do not know) gets a faint ring, half as strong. */
    public static float catchStrength(String rarity) {
        return Rarity.color(rarity) < 0 || "common".equals(rarity) ? COMMON_STRENGTH : 1;
    }

    /** Dust sparkles over the catch ring: 6 for legendary, 10 for mythical and special, none for the rest or when off. */
    public static int sparkles(String rarity) {
        TackleboxSettings s = TackleboxSettings.get();
        if (!s.waterFx || !s.waterSparkles) {
            return 0;
        }
        return switch (rarity) {
            case "legendary" -> 6;
            case "mythical", "special" -> 10;
            default -> 0;
        };
    }

    /** Whether the fish particles are wanted at all (the countdown is fed by them). */
    public static boolean wantsApproach(TackleboxSettings s) {
        return s.waterFx && (s.waterBiteRings || s.waterWake);
    }

    private static float strength(float kind) {
        return TackleboxSettings.get().waterOpacity / 100f * kind;
    }

    // ---------------------------------------------------------------- what happens

    /** Your bobber is out, its water top at {@code y}; called every tick. */
    public static void onBobber(double x, double y, double z, long now) {
        bobber = new Spot(x, y, z, now);
    }

    /** Your bobber's last spot, null when it is older than {@link #SPOT_KEEP_MS}. */
    public static Spot bobber(long now) {
        return bobber != null && now - bobber.at() <= SPOT_KEEP_MS ? bobber : null;
    }

    /** Every tick: drops the rings that are over and, while a fish swims to your bobber, adds a bite ring when one is due. */
    public static void tick(long now, boolean approaching, double secondsLeft) {
        if (!TackleboxSettings.get().waterFx) {
            rings.clear();
            return;
        }
        rings.removeIf(r -> r.over(now));
        if (approaching) {
            beat(secondsLeft, now);
        }
    }

    /** The countdown runs: a bite ring at the bobber when the last one is {@link #interval} old. */
    static void beat(double secondsLeft, long now) {
        TackleboxSettings s = TackleboxSettings.get();
        Spot at = bobber(now);
        if (!s.waterFx || !s.waterBiteRings || at == null || now < nextBeat) {
            return;
        }
        add(new Ring(at.x(), at.y(), at.z(), now, BITE_LIFE, BITE_FROM, BITE_TO, s.waterColor, strength(1)));
        nextBeat = now + Math.round(interval(secondsLeft) * 1000);
    }

    /** The BITE! title: one bigger ring at the bobber; the re-sends of the same bite add none. */
    public static void onBite(long now) {
        TackleboxSettings s = TackleboxSettings.get();
        boolean fresh = now - lastBite > BITE_GAP_MS;
        lastBite = now;
        Spot at = bobber(now);
        if (fresh && s.waterFx && s.waterBiteRings && at != null) {
            add(new Ring(at.x(), at.y(), at.z(), now, BITE_LIFE, BITE_FROM, BITE_BIG_TO, s.waterColor, strength(1)));
        }
    }

    /** Your fish's particle at {@code x, z} (water top {@code y}): a small ring once it moved {@link #WAKE_STEP} from the last one. */
    public static void onWake(double x, double y, double z, long now) {
        TackleboxSettings s = TackleboxSettings.get();
        if (!s.waterFx || !s.waterWake || now - wakeAt < WAKE_GAP_MS) {
            return;
        }
        double dx = x - wakeX, dz = z - wakeZ;
        if (!Double.isNaN(dx) && dx * dx + dz * dz < WAKE_STEP * WAKE_STEP) {
            return;
        }
        add(new Ring(x, y, z, now, WAKE_LIFE, WAKE_FROM, WAKE_TO, s.waterColor, strength(WAKE_STRENGTH)));
        wakeX = x;
        wakeZ = z;
        wakeAt = now;
    }

    /**
     * You caught a fish of this rarity: one ring where the bobber was, if its switch is on. Returns that spot (for the
     * sparkles), null when water effects are off or the bobber was not seen in the last {@link #SPOT_KEEP_MS}.
     */
    public static Spot onCatch(String rarity, long now) {
        TackleboxSettings s = TackleboxSettings.get();
        Spot at = bobber(now);
        if (!s.waterFx || at == null) {
            return null;
        }
        if (s.waterCatchRing) {
            add(new Ring(at.x(), at.y(), at.z(), now, CATCH_LIFE, CATCH_FROM, CATCH_TO, catchColor(rarity), strength(catchStrength(rarity))));
        }
        return at;
    }

    /** Adds a ring; with {@link #MAX_RINGS} alive the oldest goes. */
    static void add(Ring ring) {
        if (rings.size() >= MAX_RINGS) {
            rings.remove(0);
        }
        rings.add(ring);
    }

    /** The rings as the ticks left them, oldest first; drawing only reads them. */
    public static List<Ring> rings() {
        return Collections.unmodifiableList(rings);
    }

    /** Forgets everything (leaving the server, tests). */
    public static void reset() {
        rings.clear();
        bobber = null;
        nextBeat = lastBite = wakeAt = 0;
        wakeX = wakeZ = Double.NaN;
    }

    // ---------------------------------------------------------------- shape

    /** A flat ring around {@code (cx, y, cz)}: {@link #SEGMENTS} quads, {@link #WIDTH} wide, centred on {@code radius}. */
    public static void emit(float cx, float y, float cz, float radius, Sink sink) {
        float in = Math.max(0, radius - WIDTH / 2), out = radius + WIDTH / 2;
        for (int i = 0; i < SEGMENTS; i++) {
            double a = Math.PI * 2 * i / SEGMENTS, b = Math.PI * 2 * (i + 1) / SEGMENTS;
            float ca = (float) Math.cos(a), sa = (float) Math.sin(a), cb = (float) Math.cos(b), sb = (float) Math.sin(b);
            sink.vertex(cx + ca * in, y, cz + sa * in);
            sink.vertex(cx + ca * out, y, cz + sa * out);
            sink.vertex(cx + cb * out, y, cz + sb * out);
            sink.vertex(cx + cb * in, y, cz + sb * in);
        }
    }
}
