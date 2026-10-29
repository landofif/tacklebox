package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.trigger.Trigger;

/**
 * One "hide this from chat" rule, saved in settings.json. Patterns work like trigger patterns: contains
 * with {@code *} wildcards, the whole line, or a regular expression.
 */
public class ChatFilter {
    public String pattern = "";
    public Trigger.MatchMode mode = Trigger.MatchMode.SIMPLE;
    public boolean enabled = true;
    /** Only hide while on fishonmc. */
    public boolean fishOnMcOnly = true;

    public ChatFilter() {}

    public ChatFilter(String pattern, Trigger.MatchMode mode) {
        this.pattern = pattern;
        this.mode = mode;
    }
}
