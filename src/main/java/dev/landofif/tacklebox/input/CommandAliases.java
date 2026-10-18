package dev.landofif.tacklebox.input;

import dev.landofif.tacklebox.TackleboxSettings;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;

import java.util.List;

/**
 * Command aliases: a command you type (or a key command runs) whose first word is an alias name is sent as the
 * alias's command instead, with the rest of what you typed appended. One pass only, an alias never expands another.
 */
public final class CommandAliases {
    /** An alias name and the command it stands for, both without the slash. */
    public static class Entry {
        public String name = "";
        public String command = "";
        public boolean enabled = true;
    }

    private CommandAliases() {}

    public static void register() {
        ClientSendMessageEvents.MODIFY_COMMAND.register(command -> {
            TackleboxSettings s = TackleboxSettings.get();
            return s == null ? command : expand(command, s.aliases);
        });
    }

    /** {@code command} is what is about to be sent, without the slash. Returns it unchanged when no alias matches. */
    public static String expand(String command, List<Entry> aliases) {
        if (command == null || aliases == null || aliases.isEmpty()) {
            return command;
        }
        int end = 0;
        while (end < command.length() && !Character.isWhitespace(command.charAt(end))) {
            end++;
        }
        String word = command.substring(0, end);
        for (Entry e : aliases) {
            String name = clean(e.name);
            String target = clean(e.command);
            if (e.enabled && !name.isEmpty() && !target.isEmpty() && name.equalsIgnoreCase(word)) {
                return target + command.substring(end);
            }
        }
        return command;
    }

    private static String clean(String s) {
        return s == null ? "" : s.strip().replaceFirst("^/+", "");
    }
}
