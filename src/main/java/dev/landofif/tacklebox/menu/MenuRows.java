package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.util.Glyphs;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * What the mods menu screen shows for one server button: its name, one line about it and a progress bar when
 * the lore has an {@code (a/b)}. Works on the lore as strings (from {@code Text.getString()}), so the server's
 * glyph prefixes ({@code  퀃 ...}) are cleaned out here. Lore formats: docs/fishonmc-research.md.
 */
public final class MenuRows {
    /** One row: {@code progress} is 0..1 or -1 without a bar, {@code amount} the "a/b" as the server wrote it. */
    public record Row(String name, String sub, int level, float progress, String amount) {
        public boolean done() {
            return progress >= 1;
        }
    }

    private static final Pattern FRACTION = Pattern.compile("\\(\\s*([\\d,.]+[kKmM]?)\\s*/\\s*([\\d,.]+[kKmM]?)\\s*\\)");
    private static final Pattern LEVEL = Pattern.compile("\\[(\\d+)]");

    private MenuRows() {}

    /** The line without the server's art glyphs, emoji, formatting codes and extra spaces; case is kept. */
    public static String clean(String line) {
        StringBuilder out = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < line.length(); ) {
            int cp = line.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '§') {
                i = Math.min(line.length(), i + 1);
                continue;
            }
            if (Glyphs.isArt(cp) || cp > 0xFFFF || Character.isISOControl(cp)) {
                continue;
            }
            if (Character.isWhitespace(cp)) {
                space = out.length() > 0;
                continue;
            }
            if (space) {
                out.append(' ');
                space = false;
            }
            out.appendCodePoint(cp);
        }
        return out.toString();
    }

    /**
     * A line written in small capitals, like {@code ᴄʟɪᴄᴋ ᴛᴏ ᴠɪᴇᴡ}: the server's hints, not a description. Its s is a
     * plain {@code s} ({@code ʜᴀʀᴅ ꞯᴜᴇsᴛ}), so only capital ascii letters rule it out.
     */
    static boolean smallCapsOnly(String line) {
        boolean caps = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                return false;
            }
            caps |= c >= 128 && Character.isLetter(c);
        }
        return caps;
    }

    /** Builds the row of a button from its name and its raw lore lines. */
    public static Row of(String name, List<String> lore) {
        name = clean(name);
        float progress = -1;
        String amount = "";
        int level = -1;
        List<String> lines = new ArrayList<>();
        for (String raw : lore) {
            String line = clean(raw);
            Matcher f = FRACTION.matcher(line);
            if (progress < 0 && f.find()) {
                double have = number(f.group(1)), need = number(f.group(2));
                progress = need > 0 ? (float) Math.min(1, have / need) : -1; // the compendium can go past the max
                amount = f.group(1) + "/" + f.group(2);
                line = (line.substring(0, f.start()) + line.substring(f.end())).trim();
            }
            Matcher l = LEVEL.matcher(line);
            if (level < 0 && l.find()) {
                level = Integer.parseInt(l.group(1));
            }
            if (!line.isEmpty() && !line.equalsIgnoreCase(name) && !smallCapsOnly(line)) {
                lines.add(line);
            }
        }
        return new Row(name, sub(lines, lore), level, progress, amount);
    }

    /** The one line about a button: the climate of a place, or the first line that says something. */
    private static String sub(List<String> lines, List<String> lore) {
        String travel = travel(lines);
        if (!travel.isEmpty()) {
            return travel;
        }
        if (lines.isEmpty()) {
            return "";
        }
        String first = lines.get(0);
        if (lines.size() > 1 && lines.get(1).startsWith("at ")) {
            first += " " + lines.get(1); // a quest: "Catch 70x Rare fish" "at Murray River"
        }
        return first;
    }

    /** A /travel item as "Subtropical · Freshwater · -5°C", or "" when the lore has no climate. */
    static String travel(List<String> lines) {
        String climate = "", water = "", now = "";
        for (String line : lines) {
            if (line.startsWith("Climate:")) {
                climate = line.substring(8).trim();
            } else if (line.startsWith("Water:")) {
                water = line.substring(6).trim();
            } else if (line.startsWith("It is currently ")) {
                now = line.substring(16).replaceAll("\\.$", "").trim();
            }
        }
        if (climate.isEmpty()) {
            return "";
        }
        return String.join(" · ", java.util.stream.Stream.of(climate, water, now).filter(s -> !s.isEmpty()).toList());
    }

    /** 43,178 or 1.18M or 50K or 1.2B as a number. */
    static double number(String s) {
        s = s.replace(",", "").toLowerCase(Locale.ROOT);
        double k = s.endsWith("k") ? 1_000 : s.endsWith("m") ? 1_000_000 : s.endsWith("b") ? 1_000_000_000 : 1;
        if (k > 1) {
            s = s.substring(0, s.length() - 1);
        }
        try {
            return Double.parseDouble(s) * k;
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
