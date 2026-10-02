package dev.landofif.dofisaddons.stats;

import java.util.List;
import java.util.Locale;

/** Display names, order and colours for FishOnMC rarities, sizes and variants, plus number formatting. */
public final class StatNames {
    public record Kind(String id, String name, int color) {}

    public static final List<Kind> RARITIES = List.of(
            new Kind("common", "Common", 0xFFFFFF),
            new Kind("rare", "Rare", 0x2B85C4),
            new Kind("epic", "Epic", 0x1CD832),
            new Kind("legendary", "Legendary", 0xD98103),
            new Kind("mythical", "Mythical", 0xC93832),
            new Kind("special", "Special", 0xDD7ACF));

    public static final List<Kind> SIZES = List.of(
            new Kind("baby", "Baby", 0x468CE7),
            new Kind("juvenile", "Juvenile", 0x22EA08),
            new Kind("adult", "Adult", 0x1C7DA0),
            new Kind("large", "Large", 0xFF9000),
            new Kind("gigantic", "Gigantic", 0xAF3333));

    public static final List<Kind> VARIANTS = List.of(
            new Kind("albino", "Albino", 0xC6C3A1),
            new Kind("melanistic", "Melanistic", 0x777777),
            new Kind("trophy", "Trophy", 0xD8C13C),
            new Kind("fabled", "Fabled", 0xCE2326),
            new Kind("alternate", "Alternate", 0x9D7CFF),
            new Kind("spooky", "Spooky", 0xFF8C1A),
            new Kind("frozen", "Frozen", 0x9FE3FF));

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
            sb.append(Character.toUpperCase(part.charAt(0))).append(part.substring(1));
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
        if (a >= 1e9) return String.format(Locale.ROOT, "%.2fB", v / 1e9);
        if (a >= 1e6) return String.format(Locale.ROOT, "%.2fM", v / 1e6);
        if (a >= 1e4) return String.format(Locale.ROOT, "%.1fK", v / 1e3);
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
