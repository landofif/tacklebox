package dev.landofif.tacklebox.trigger;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Turns a trigger's pattern into a compiled regex and matches chat lines against it.
 *
 * <p>Chat lines are normalised first: FishOnMC's private-use icon glyphs are removed, runs of
 * whitespace collapse to one space, and {@code »} is treated the same as {@code >} so people can
 * type {@code QUESTS > Quest} on a normal keyboard.
 */
public final class Matcher {
    /** Result of a match: the captured values by name (plus "message"), and where each one is in the normalized line ({start, end}). */
    public record Result(Map<String, String> values, Map<String, int[]> spans) {}

    private final Pattern compiled;
    private final String error;

    private Matcher(Pattern compiled, String error) {
        this.compiled = compiled;
        this.error = error;
    }

    /** Human-readable problem with the pattern, or null if it's fine. */
    public String error() {
        return error;
    }

    public static Matcher of(Trigger trigger) {
        return of(trigger.pattern, trigger.mode, trigger.ignoreCase);
    }

    public static Matcher of(String pattern, Trigger.MatchMode mode, boolean ignoreCase) {
        if (pattern == null || pattern.isBlank()) {
            return new Matcher(null, "type the chat text to look for");
        }
        int flags = ignoreCase ? Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE : 0;
        try {
            String regex = switch (mode == null ? Trigger.MatchMode.SIMPLE : mode) {
                case SIMPLE -> simpleToRegex(normalize(pattern), false);
                case EXACT -> "^" + simpleToRegex(normalize(pattern), true) + "$";
                case REGEX -> pattern;
            };
            return new Matcher(Pattern.compile(regex, flags), null);
        } catch (PatternSyntaxException e) {
            return new Matcher(null, "pattern error: " + e.getDescription());
        }
    }

    public Result match(String rawLine) {
        if (compiled == null) {
            return null;
        }
        String line = normalize(rawLine);
        java.util.regex.Matcher m = compiled.matcher(line);
        if (!m.find()) {
            return null;
        }
        Map<String, String> values = new LinkedHashMap<>();
        Map<String, int[]> spans = new java.util.HashMap<>();
        values.put("message", line);
        spans.put("message", new int[]{0, line.length()});
        for (Map.Entry<String, Integer> group : m.namedGroups().entrySet()) {
            capture(line, m, group.getValue(), group.getKey(), values, spans);
        }
        for (int i = 1; i <= m.groupCount(); i++) {
            if (!values.containsKey(String.valueOf(i))) {
                capture(line, m, i, String.valueOf(i), values, spans);
            }
        }
        return new Result(values, spans);
    }

    /** A group's text without the spaces around it (like {@link String#trim}), and where that text is in the line. */
    private static void capture(String line, java.util.regex.Matcher m, int group, String name, Map<String, String> values, Map<String, int[]> spans) {
        if (m.group(group) == null) {
            values.put(name, "");
            return;
        }
        int start = m.start(group);
        int end = m.end(group);
        while (start < end && line.charAt(start) <= ' ') {
            start++;
        }
        while (end > start && line.charAt(end - 1) <= ' ') {
            end--;
        }
        values.put(name, line.substring(start, end));
        spans.put(name, new int[]{start, end});
    }

    /** Strips server icon glyphs and tidies whitespace so typed patterns line up with chat. */
    public static String normalize(String s) {
        return normalize(s, null);
    }

    /** {@link #normalize(String)}, and in {@code from} (if given, at least as long as {@code s}) the index in {@code s} of each character kept. */
    static String normalize(String s, int[] from) {
        if (s == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder(s.length());
        boolean lastSpace = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '' && c <= '') {
                continue; // FishOnMC resource-pack icons (rarity tags, ranks, spacers)
            }
            if (c == '»') {
                c = '>';
            }
            if (Character.isWhitespace(c)) {
                if (!lastSpace && !sb.isEmpty()) {
                    if (from != null) {
                        from[sb.length()] = i;
                    }
                    sb.append(' ');
                }
                lastSpace = true;
                continue;
            }
            lastSpace = false;
            if (from != null) {
                from[sb.length()] = i;
            }
            sb.append(c);
        }
        int end = sb.length();
        while (end > 0 && sb.charAt(end - 1) == ' ') {
            end--;
        }
        return sb.substring(0, end);
    }

    /**
     * {@code *} becomes "anything", {@code {name}} becomes a named capture, everything else is
     * literal. A capture at the very end of the pattern takes the rest of the line.
     */
    static String simpleToRegex(String pattern, boolean anchored) {
        StringBuilder out = new StringBuilder();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        int autoName = 1;
        java.util.Set<String> used = new java.util.HashSet<>();
        while (i < pattern.length()) {
            char c = pattern.charAt(i);
            if (c == '*') {
                flushLiteral(out, literal);
                out.append(".*?");
                i++;
                continue;
            }
            if (c == '{') {
                int close = pattern.indexOf('}', i);
                if (close > i) {
                    String name = pattern.substring(i + 1, close).replaceAll("[^A-Za-z0-9]", "");
                    if (name.isEmpty() || !Character.isLetter(name.charAt(0))) {
                        name = "v" + autoName++;
                    }
                    // Java regex forbids reusing a group name: a repeated {n} becomes n2, n3, ...
                    String unique = name;
                    for (int k = 2; !used.add(unique); k++) {
                        unique = name + k;
                    }
                    name = unique;
                    flushLiteral(out, literal);
                    boolean last = close == pattern.length() - 1;
                    out.append("(?<").append(name).append(">").append(last && !anchored ? ".+" : ".+?").append(")");
                    i = close + 1;
                    continue;
                }
            }
            literal.append(c);
            i++;
        }
        flushLiteral(out, literal);
        return out.toString();
    }

    private static void flushLiteral(StringBuilder out, StringBuilder literal) {
        if (!literal.isEmpty()) {
            out.append(Pattern.quote(literal.toString()));
            literal.setLength(0);
        }
    }
}
