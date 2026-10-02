package dev.landofif.dofisaddons.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses user-typed text with colour codes into styled segments rendered in the mono font.
 *
 * <p>Supported codes: {@code &0-&9 &a-&f} (colours), {@code &l} bold, {@code &o} italic,
 * {@code &n} underline, {@code &m} strikethrough, {@code &r} reset, and {@code &#RRGGBB} for any
 * hex colour. Use {@code &&} for a literal ampersand.
 *
 * <p>Segments are kept (rather than only a {@link Text}) so the typewriter animation can reveal
 * the text one character at a time without losing styles.
 */
public final class RichText {
    public record Segment(String text, Style style) {}

    private final List<Segment> segments;
    private final int length;

    private RichText(List<Segment> segments) {
        this.segments = segments;
        int len = 0;
        for (Segment s : segments) {
            len += s.text.length();
        }
        this.length = len;
    }

    public int length() {
        return length;
    }

    public boolean isEmpty() {
        return length == 0;
    }

    /** The full text. */
    public MutableText toText() {
        return toText(Integer.MAX_VALUE);
    }

    /** Only the first {@code maxChars} characters (for the typewriter reveal). */
    public MutableText toText(int maxChars) {
        MutableText out = Mono.text("");
        int remaining = maxChars;
        for (Segment s : segments) {
            if (remaining <= 0) {
                break;
            }
            String part = s.text.length() <= remaining ? s.text : s.text.substring(0, remaining);
            remaining -= part.length();
            out.append(Text.literal(part).setStyle(s.style));
        }
        return out;
    }

    /**
     * @param input        text with colour codes
     * @param defaultColor colour used until the first colour code (0xRRGGBB)
     */
    public static RichText parse(String input, int defaultColor) {
        Style base = Mono.style(Style.EMPTY.withColor(TextColor.fromRgb(defaultColor & 0xFFFFFF)));
        List<Segment> segments = new ArrayList<>();
        if (input == null || input.isEmpty()) {
            return new RichText(segments);
        }
        Style style = base;
        StringBuilder current = new StringBuilder();
        int i = 0;
        while (i < input.length()) {
            char c = input.charAt(i);
            if (c == '&' && i + 1 < input.length()) {
                char code = Character.toLowerCase(input.charAt(i + 1));
                if (code == '&') {
                    current.append('&');
                    i += 2;
                    continue;
                }
                if (code == '#' && i + 8 <= input.length() && isHex(input.substring(i + 2, i + 8))) {
                    flush(segments, current, style);
                    int rgb = Integer.parseInt(input.substring(i + 2, i + 8), 16);
                    // Like vanilla, a colour code resets bold/italic/etc.
                    style = base.withColor(TextColor.fromRgb(rgb));
                    i += 8;
                    continue;
                }
                Formatting f = Formatting.byCode(code);
                if (f != null) {
                    flush(segments, current, style);
                    if (f == Formatting.RESET) {
                        style = base;
                    } else if (f.isColor()) {
                        style = base.withColor(f);
                    } else {
                        style = style.withFormatting(f);
                    }
                    i += 2;
                    continue;
                }
            }
            current.append(c);
            i++;
        }
        flush(segments, current, style);
        return new RichText(segments);
    }

    /** Removes colour codes, for plain previews. */
    public static String strip(String input) {
        if (input == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        RichText rt = parse(input, 0xFFFFFF);
        for (Segment s : rt.segments) {
            sb.append(s.text);
        }
        return sb.toString();
    }

    /** A colour code lifted off the start of a line: the colour, and the line without it. */
    public record Lead(int rgb, String rest) {}

    /**
     * Takes the colour code off the start of {@code input} ({@code &a&lhi} gives green and {@code &lhi}),
     * so the colour can live in a colour box instead. A code at the start always beats the colour box,
     * which made picking a colour look broken. Returns null when there is no leading colour, or when a
     * later {@code &r} would fall back to the box colour and so change meaning.
     */
    public static Lead liftLeadColor(String input) {
        if (input == null) {
            return null;
        }
        Integer color = null;
        StringBuilder formats = new StringBuilder();
        int i = 0;
        while (i + 1 < input.length() && input.charAt(i) == '&') {
            char code = Character.toLowerCase(input.charAt(i + 1));
            if (code == '#' && i + 8 <= input.length() && isHex(input.substring(i + 2, i + 8))) {
                color = Integer.parseInt(input.substring(i + 2, i + 8), 16);
                formats.setLength(0); // a colour code resets bold and the rest
                i += 8;
                continue;
            }
            Formatting f = Formatting.byCode(code);
            if (f == null || f == Formatting.RESET) {
                break;
            }
            if (f.isColor()) {
                color = f.getColorValue();
                formats.setLength(0);
            } else {
                formats.append('&').append(code);
            }
            i += 2;
        }
        if (color == null) {
            return null;
        }
        String rest = formats + input.substring(i);
        for (int j = 0; j + 1 < rest.length(); j++) {
            if (rest.charAt(j) == '&') {
                char code = Character.toLowerCase(rest.charAt(j + 1));
                if (code == 'r') {
                    return null;
                }
                j++; // skip the code char (also covers "&&")
            }
        }
        return new Lead(color, rest);
    }

    /**
     * Parses a colour typed by the user: {@code #FF8800}, {@code FF8800}, {@code &a} or a colour
     * name like {@code gold}. Returns {@code fallback} when it can't be read.
     */
    public static int parseColor(String input, int fallback) {
        if (input == null) {
            return fallback;
        }
        String s = input.trim();
        if (s.startsWith("&#")) {
            s = s.substring(1);
        }
        if (s.startsWith("#")) {
            s = s.substring(1);
        }
        if (s.length() == 6 && isHex(s)) {
            return Integer.parseInt(s, 16);
        }
        if (s.length() == 2 && s.charAt(0) == '&') {
            Formatting f = Formatting.byCode(s.charAt(1));
            if (f != null && f.getColorValue() != null) {
                return f.getColorValue();
            }
        }
        Formatting named = Formatting.byName(s.toLowerCase().replace(' ', '_'));
        if (named != null && named.getColorValue() != null) {
            return named.getColorValue();
        }
        return fallback;
    }

    private static void flush(List<Segment> segments, StringBuilder current, Style style) {
        if (!current.isEmpty()) {
            segments.add(new Segment(current.toString(), style));
            current.setLength(0);
        }
    }

    private static boolean isHex(String s) {
        for (int i = 0; i < s.length(); i++) {
            if (Character.digit(s.charAt(i), 16) < 0) {
                return false;
            }
        }
        return true;
    }
}
