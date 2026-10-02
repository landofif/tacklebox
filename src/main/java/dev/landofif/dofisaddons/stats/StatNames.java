package dev.landofif.dofisaddons.stats;

import java.util.List;
import java.util.Locale;

/** Display names, order and colours for FishOnMC rarities, sizes and variants, plus number formatting. */
public final class StatNames {
    public record Kind(String id, String name, int color) {}

    public static final List<Kind> RARITIES = List.of(
            new Kind("common", "common", 0xFFFFFF),
            new Kind("rare", "rare", 0x2B85C4),
            new Kind("epic", "epic", 0x1CD832),
            new Kind("legendary", "legendary", 0xD98103),
            new Kind("mythical", "mythical", 0xC93832),
            new Kind("special", "special", 0xDD7ACF));

    public static final List<Kind> SIZES = List.of(
            new Kind("baby", "baby", 0x468CE7),
            new Kind("juvenile", "juvenile", 0x22EA08),
            new Kind("adult", "adult", 0x1C7DA0),
            new Kind("large", "large", 0xFF9000),
            new Kind("gigantic", "gigantic", 0xAF3333));

    public static final List<Kind> VARIANTS = List.of(
            new Kind("albino", "albino", 0xC6C3A1),
            new Kind("melanistic", "melanistic", 0x777777),
            new Kind("trophy", "trophy", 0xD8C13C),
            new Kind("fabled", "fabled", 0xCE2326),
            new Kind("alternate", "alternate", 0x9D7CFF),
            new Kind("spooky", "spooky", 0xFF8C1A),
            new Kind("frozen", "frozen", 0x9FE3FF));

    private StatNames() {}

    /** "largemouth_bass" -> "Largemouth Bass". */
    public static String pretty(String id) {
        if (id == null || id.isEmpty()) {
            return "?";
        }
        StringBuilder sb = new StringBuilder();
        for (String part : id.replace('-', '_').split("_")) {
            if (part.isEmpty()) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(part.toLowerCase(java.util.Locale.ROOT));
        }
        return sb.toString();
    }

    /** 1234567 -> "1,234,567". */
    public static String number(long n) {
        return String.format(Locale.ROOT, "%,d", n);
    }

    /** 38480 -> "38.5K". */
    public static String compact(double v) {
        double a = Math.abs(v);
        if (a >= 1e9) return String.format(Locale.ROOT, "%.2fb", v / 1e9);
        if (a >= 1e6) return String.format(Locale.ROOT, "%.2fm", v / 1e6);
        if (a >= 1e4) return String.format(Locale.ROOT, "%.1fk", v / 1e3);
        return String.format(Locale.ROOT, "%,.0f", v);
    }

    /** 5_025_000 ms -> "1h 23m". */
    public static String duration(long millis) {
        long minutes = millis / 60_000;
        long hours = minutes / 60;
        if (hours >= 24) {
            return (hours / 24) + "d " + (hours % 24) + "h";
        }
        if (hours > 0) {
            return hours + "h " + (minutes % 60) + "m";
        }
        return minutes + "m";
    }
}
