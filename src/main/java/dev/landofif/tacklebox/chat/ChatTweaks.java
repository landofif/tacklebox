package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.Matcher;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Chat filters: lines that match an enabled hide rule never reach the chat box. Runs for server
 * messages, player chat and the lines mods add to the chat themselves. Only what you see changes; nothing is sent.
 */
public final class ChatTweaks {
    private static final Map<String, Matcher> MATCHERS = new HashMap<>();
    private static final Map<ChatFilter, Integer> HIDDEN = new IdentityHashMap<>();

    /** What the server says at every login. The fishing simulator line is its tagline (in small capitals), and only the exact lines match. */
    private static final java.util.regex.Pattern LOGIN = java.util.regex.Pattern.compile(
            "(?is)connected to instance\\d+ \\(an undefined server\\)"
                    + "|you are now using the (fish|shader) pack!"
                    + "|welcome to fishonmc!"
                    + "|a fishing simulator|ᴀ ꜰɪꜱʜɪɴɢ ꜱɪᴍᴜʟᴀᴛᴏʀ"
                    + "|type /help or /wiki to learn about the server!"
                    + "|read up on the /rules and utilize /report when needed\\."
                    + "|type /discord to join our discord!");

    private ChatTweaks() {}

    /** @return false to hide the message */
    public static boolean allow(Text message) {
        TackleboxSettings st = TackleboxSettings.get();
        if (st != null && st.hideLoginChat && LOGIN.matcher(message.getString().strip()).matches()) {
            return false;
        }
        if (st != null && st.hideEmptyChat && isEmpty(message.getString())) {
            return false;
        }
        return hiddenBy(message.getString(), TriggerEngine.isOnFishOnMc(), true) == null;
    }

    /**
     * A line on its way into the chat hud, from the server or from a mod. Lines a mod writes itself (foecollab's dry
     * streaks) never pass {@link #allow}, so the hide rules are asked here too.
     * @return false to hide the line
     */
    public static boolean allowAdded(String line) {
        return hiddenBy(line, TriggerEngine.isOnFishOnMc(), true) == null;
    }

    /** Nothing to read: only spaces, and the server's invisible spacing glyphs (U+EEE0 to U+EEEF) or zero width characters. */
    public static boolean isEmpty(String line) {
        return line.codePoints().allMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c)
                || (c >= 0xEEE0 && c <= 0xEEEF) || (c >= 0x200B && c <= 0x200F));
    }

    /** The first enabled rule that hides {@code line}, or null. {@code count} adds to its session count. */
    public static ChatFilter hiddenBy(String line, boolean onFishOnMc, boolean count) {
        TackleboxSettings s = TackleboxSettings.get();
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
