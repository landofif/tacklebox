package dev.landofif.tacklebox.chat;

import java.util.List;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat stacking (setting chatStack): a line that repeats one of the last {@link #WINDOW} lines,
 * numbers ignored, takes that line's place at the bottom as "text (xN)" with the newest numbers.
 * Used by ChatHudMixin for vanilla chat and by ChatBoxes for the boxes.
 */
public final class ChatStack {
    /** How many of the newest lines a repeat is looked for in. */
    public static final int WINDOW = 20;
    private static final Pattern COUNT = Pattern.compile(" \\(x(\\d+)\\)$");
    private static final Pattern NUMBER = Pattern.compile("\\d[\\d,.]*");

    private ChatStack() {}

    /** The line as stacking compares it: no count at the end, every number as #. */
    public static String key(String plain) {
        return NUMBER.matcher(COUNT.matcher(plain).replaceFirst("")).replaceAll("#");
    }

    /** The count a stacked line shows, 1 for a line that has not stacked yet. */
    public static int count(String plain) {
        Matcher m = COUNT.matcher(plain);
        return m.find() ? Integer.parseInt(m.group(1)) : 1;
    }

    /** Index of the newest line (list newest first) within the window that the new line repeats, or -1. */
    public static <T> int find(List<T> newestFirst, Function<T, String> plain, String newPlain) {
        if (newPlain.isBlank()) {
            return -1;
        }
        String key = key(newPlain);
        for (int i = 0; i < Math.min(WINDOW, newestFirst.size()); i++) {
            if (key(plain.apply(newestFirst.get(i))).equals(key)) {
                return i;
            }
        }
        return -1;
    }
}
