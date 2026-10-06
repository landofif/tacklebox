package dev.landofif.dofisaddons.chat;

import dev.landofif.dofisaddons.trigger.Trigger;

import java.util.ArrayList;
import java.util.List;

/**
 * A separate chat container, saved in settings.json. Lines matching one of its rules go here instead
 * of the main chat (or as well, with {@link #alsoInChat}). Each box is its own panel you can move.
 */
public class ChatBox {
    public String name = "";
    public boolean enabled = true;
    public List<ChatFilter> rules = new ArrayList<>();
    /** Keep matching lines in the main chat too. */
    public boolean alsoInChat = false;
    /** Top left corner, in % of the screen (like custom huds). */
    public double x = 60;
    public double y = 40;
    /** Size in gui units: width of the box, and how many lines it shows. */
    public int width = 220;
    public int lines = 5;
    /** Hide the box this many seconds after its last line while chat is closed (-1 = always show). */
    public int fadeSeconds = 10;

    public ChatBox() {}

    ChatBox(String name, double x, double y, String regex) {
        this.name = name;
        this.x = x;
        this.y = y;
        rules.add(new ChatFilter(regex, Trigger.MatchMode.REGEX));
    }
}
