package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.hud.HudPos;
import dev.landofif.tacklebox.trigger.Trigger;

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
    /**
     * Where it sits: the distance to the nearest window sides (hud/HudPos), so it stays there when the window changes size.
     * null in boxes saved before that; ChatBoxes.rect makes it from x and y below, which are then not used any more.
     */
    public HudPos pos;
    /** Top left corner, in % of the screen: the first spot of a new box, and where an old one was. */
    public double x = 60;
    public double y = 40;
    /** Size in gui units: width of the box, and how many lines it shows. */
    public int width = 220;
    public int lines = 5;
    /** How many lines it shows while chat is open (never fewer than {@link #lines}). */
    public int linesOpen = 12;
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
