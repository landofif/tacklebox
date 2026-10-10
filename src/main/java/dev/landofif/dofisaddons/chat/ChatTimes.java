package dev.landofif.dofisaddons.chat;

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

    public static synchronized void record(Text message) {
        if (TIMES.put(message, System.currentTimeMillis()) == null) {
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
}
