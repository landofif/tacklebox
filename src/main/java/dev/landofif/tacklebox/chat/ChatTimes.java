package dev.landofif.tacklebox.chat;

import net.minecraft.text.Text;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * When each chat line arrived, for the hover tooltip. Vanilla chat only keeps a game tick per line,
 * so the wall clock time is remembered here by message object, for as many lines as the chat keeps.
 */
public final class ChatTimes {
    private static final int KEEP = 2100;
    private static final DateTimeFormatter FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss").withZone(ZoneId.systemDefault());
    private static final Map<Text, Long> TIMES = new IdentityHashMap<>();
    private static final ArrayDeque<Text> ORDER = new ArrayDeque<>();

    private ChatTimes() {}

    public static void record(Text message) {
        record(message, System.currentTimeMillis());
    }

    /** With a time of its own: lines chat/ChatRestore brings back. */
    public static synchronized void record(Text message, long millis) {
        if (TIMES.put(message, millis) == null) {
            ORDER.addLast(message);
            while (ORDER.size() > KEEP) {
                TIMES.remove(ORDER.removeFirst());
            }
        }
    }

    /** Arrival time in millis, or -1 if unknown. */
    public static synchronized long at(Text message) {
        Long t = TIMES.get(message);
        return t == null ? -1 : t;
    }

    /** 24 hour clock, local time. */
    public static String format(long millis) {
        return FORMAT.format(Instant.ofEpochMilli(millis));
    }

    /** True when some part of the message has hover text, which the game shows itself. */
    public static boolean hasHover(Text message) {
        AtomicBoolean found = new AtomicBoolean();
        message.visit((style, s) -> {
            if (style.getHoverEvent() != null) {
                found.set(true);
            }
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return found.get();
    }

    private static final DateTimeFormatter SHORT = DateTimeFormatter.ofPattern("HH:mm").withZone(ZoneId.systemDefault());

    /** The line without the "[HH:mm:ss] " or "[HH:mm] " stamp {@link #rewrite} puts in front. */
    public static String bare(String s) {
        int end = s.indexOf(']');
        return s.length() > 3 && s.charAt(0) == '[' && s.charAt(3) == ':' && end > 0 && end <= 9 && s.startsWith(" ", end + 1) ? s.substring(end + 2) : s;
    }

    /** The server's "!" in front of player chat, like "! [113] name ». Removed with one space after it. */
    private static int bangAt(String s) {
        int i = 0;
        while (i < s.length() && (Character.isWhitespace(s.charAt(i)) || (s.charAt(i) >= '\uE000' && s.charAt(i) <= '\uF8FF'))) {
            i++;
        }
        if (i >= s.length() || s.charAt(i) != '!') {
            return -1;
        }
        int j = i + 1;
        while (j < s.length() && s.charAt(j) == ' ') {
            j++;
        }
        return j < s.length() && s.charAt(j) == '[' ? i : -1;
    }

    /** Message without the "!" and, with {@code stamp}, with a gray time in front. Same object if nothing changes. */
    public static Text rewrite(Text message, int mode, long millis) {
        boolean stamp = mode > 0;
        int bang = bangAt(message.getString());
        if (bang < 0 && !stamp) {
            return message;
        }
        net.minecraft.text.MutableText out = Text.empty();
        if (stamp) {
            out.append(Text.literal("[" + (mode == 2 ? SHORT.format(Instant.ofEpochMilli(millis)) : format(millis)) + "] ").formatted(net.minecraft.util.Formatting.GRAY));
        }
        int[] pos = {0};
        message.visit((style, s) -> {
            StringBuilder sb = new StringBuilder();
            for (int k = 0; k < s.length(); k++, pos[0]++) {
                if (bang >= 0 && pos[0] == bang) {
                    if (k + 1 < s.length() && s.charAt(k + 1) == ' ') {
                        k++;
                        pos[0]++;
                    }
                    continue;
                }
                sb.append(s.charAt(k));
            }
            if (!sb.isEmpty()) {
                out.append(Text.literal(sb.toString()).setStyle(style));
            }
            return java.util.Optional.empty();
        }, net.minecraft.text.Style.EMPTY);
        return out;
    }
}
