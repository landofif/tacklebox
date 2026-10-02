package dev.landofif.dofisaddons.stats;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * One set of counters. The tracker keeps two: this session (in memory) and all-time (saved per
 * player in {@code config/dofisaddons/stats/<uuid>.json}). Plain fields so Gson can save it as-is.
 */
public class StatBook {
    public long fish;
    public double value;
    public double xp;
    /** Time spent actively fishing; pauses when no fish has been caught for a while. */
    public long activeMillis;

    public Map<String, Long> rarity = new LinkedHashMap<>();
    public Map<String, Long> size = new LinkedHashMap<>();
    public Map<String, Long> variant = new LinkedHashMap<>();
    public Map<String, Long> species = new LinkedHashMap<>();
    public Map<String, Long> location = new LinkedHashMap<>();

    public long pets;
    public long shards;
    public long lightningBottles;
    public long infusionCapsules;
    public long questsCompleted;
    /** Money from selling fish ("SELL » You just earned $..."). */
    public double moneyEarned;

    /** Heaviest fish per species. */
    public Map<String, Best> heaviest = new LinkedHashMap<>();
    /**
     * Fish count at the moment something was last caught, keyed like "rarity:mythical" or "pet".
     * "Fish since last X" (dry streak) = fish - this.
     */
    public Map<String, Long> lastAt = new LinkedHashMap<>();

    public static class Best {
        public float weight;
        public float length;
        public String rarity;
        public String variant;
        public long when;
    }

    public void addFish(FishInfo f, long now) {
        fish++;
        value += f.value();
        xp += f.xp();
        bump(rarity, f.rarity());
        bump(size, f.size());
        bump(variant, f.variant());
        bump(species, f.species());
        bump(location, f.location());
        mark("rarity:" + f.rarity());
        mark("size:" + f.size());
        mark("variant:" + f.variant());

        Best best = heaviest.get(f.species());
        if (best == null || f.weight() > best.weight) {
            best = new Best();
            best.weight = f.weight();
            best.length = f.length();
            best.rarity = f.rarity();
            best.variant = f.variant();
            best.when = now;
            heaviest.put(f.species(), best);
        }
    }

    /** Remembers that X was just caught, for the "fish since last X" counter. */
    public void mark(String key) {
        lastAt.put(key, fish);
    }

    /** Fish caught since the last X, or -1 if X was never seen. */
    public long since(String key) {
        Long at = lastAt.get(key);
        return at == null ? -1 : Math.max(0, fish - at);
    }

    public long count(Map<String, Long> map, String key) {
        return map.getOrDefault(key, 0L);
    }

    /** Fish per hour of active time, or 0 before there's enough data. */
    public double perHour() {
        if (activeMillis < 60_000) {
            return 0;
        }
        return fish / (activeMillis / 3_600_000.0);
    }

    void sanitize() {
        if (rarity == null) rarity = new LinkedHashMap<>();
        if (size == null) size = new LinkedHashMap<>();
        if (variant == null) variant = new LinkedHashMap<>();
        if (species == null) species = new LinkedHashMap<>();
        if (location == null) location = new LinkedHashMap<>();
        if (heaviest == null) heaviest = new LinkedHashMap<>();
        if (lastAt == null) lastAt = new LinkedHashMap<>();
    }

    private static void bump(Map<String, Long> map, String key) {
        if (key == null || key.isEmpty()) {
            return;
        }
        map.merge(key, 1L, Long::sum);
    }
}
