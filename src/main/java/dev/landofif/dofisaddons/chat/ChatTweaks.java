package dev.landofif.dofisaddons.chat;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.trigger.Matcher;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Chat filters: lines that match an enabled hide rule never reach the chat box. Runs for server
 * messages and player chat. Only what you see changes; nothing is sent.
 */
public final class ChatTweaks {
    private static final Map<String, Matcher> MATCHERS = new HashMap<>();
    private static final Map<ChatFilter, Integer> HIDDEN = new IdentityHashMap<>();

    private ChatTweaks() {}

    /** @return false to hide the message */
    public static boolean allow(Text message) {
        DofiSettings st = DofiSettings.get();
        if (st != null && st.hideEmptyChat && isEmpty(message.getString())) {
            return false;
        }
        return hiddenBy(message.getString(), TriggerEngine.isOnFishOnMc(), true) == null;
    }

    /** Nothing to read: only spaces, and the server's invisible spacing glyphs (U+EEE0 to U+EEEF) or zero width characters. */
    public static boolean isEmpty(String line) {
        return line.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)
                || (c >= 0xEEE0 && c <= 0xEEEF) || (c >= 0x200B && c <= 0x200F));
    }

    /** The first enabled rule that hides {@code line}, or null. {@code count} adds to its session count. */
    public static ChatFilter hiddenBy(String line, boolean onFishOnMc, boolean count) {
        DofiSettings s = DofiSettings.get();
        if (s == null || s.chatFilters == null) {
            return null;
        }
        for (ChatFilter f : s.chatFilters) {
            if (f == null || !f.enabled || (f.fishOnMcOnly && !onFishOnMc)) {
                continue;
            }
            if (matcher(f).match(line) != null) {
                if (count) {
                    HIDDEN.merge(f, 1, Integer::sum);
                }
                return f;
            }
        }
        return null;
    }

    public static Matcher matcher(ChatFilter f) {
        return MATCHERS.computeIfAbsent(f.mode + "\u0000" + f.pattern, k -> Matcher.of(f.pattern, f.mode, true));
    }

    /** How many lines this rule hid this session. */
    public static int hidden(ChatFilter f) {
        return HIDDEN.getOrDefault(f, 0);
    }
}
