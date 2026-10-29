package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Starting a quest again in /quests (luna 2026-10-30): the difficulty and tier buttons that the quest clicked last had are marked,
 * so picking the same quest again is two clicks on the marked buttons. The quest list is read while it is drawn (lore
 * {@code ʜᴀʀᴅ ǫᴜᴇsᴛ} and {@code Catch 71x Rare fish}), the start chat line is the fallback. Display only, nothing is clicked.
 */
public final class QuestRedo {
    /** Difficulty (easy, medium, hard) and tier (adult, large, rare, epic, legendary), lowercase. */
    public record Quest(String difficulty, String tier) {}

    private static final Pattern NUMBER = Pattern.compile("Quest #(\\d)");
    private static final Pattern DIFFICULTY = Pattern.compile("\\b(easy|medium|hard) \\S*uest\\b");
    private static final Pattern TIER = Pattern.compile("\\bcatch \\d+x (adult|large|rare|epic|legendary)\\b");
    // QUEST » Started a new Hard fishing quest to catch 29x Epic at Cypress Lake
    private static final Pattern STARTED = Pattern.compile("QUEST [>»] Started a new (Easy|Medium|Hard) fishing quest to catch \\d+x (Adult|Large|Rare|Epic|Legendary)\\b");
    /** The quest list's slots in order (dump 2026-10-29), for a quest item whose name has no number. */
    private static final List<Integer> SLOTS = List.of(12, 13, 14, 20, 21, 22, 23, 24);

    private static final Map<Integer, Quest> SEEN = new HashMap<>();
    private static int chosen;
    private static Quest lastStarted;

    private QuestRedo() {}

    /** The quest an item's lore describes, or null when it names no difficulty and tier. */
    public static Quest parse(List<Text> lore) {
        StringBuilder all = new StringBuilder();
        for (Text line : lore) {
            all.append(Glyphs.fromSmallCaps(line.getString())).append('\n');
        }
        String s = all.toString().toLowerCase(Locale.ROOT);
        Matcher d = DIFFICULTY.matcher(s), t = TIER.matcher(s);
        return d.find() && t.find() ? new Quest(d.group(1), t.group(1)) : null;
    }

    /** The quest number of a quest list item (1 to 8), 0 for anything else (difficulty and tier buttons, Close). */
    public static int number(String name, int slot) {
        Matcher m = NUMBER.matcher(name);
        if (m.find()) {
            return Integer.parseInt(m.group(1));
        }
        return name.toLowerCase(Locale.ROOT).contains("quest") ? SLOTS.indexOf(slot) + 1 : 0;
    }

    /** A quest list item as it is drawn. */
    public static void see(int number, List<Text> lore) {
        Quest q = number > 0 ? parse(lore) : null;
        if (q != null) {
            SEEN.put(number, q);
        }
    }

    /** The player clicked a quest list item: its difficulty and tier get marked in the next two menus. */
    public static void click(int number, List<Text> lore) {
        if (number <= 0) {
            return;
        }
        see(number, lore);
        chosen = number;
    }

    public static void onChat(Text message) {
        Matcher m = STARTED.matcher(message.getString());
        if (m.find()) {
            lastStarted = new Quest(m.group(1).toLowerCase(Locale.ROOT), m.group(2).toLowerCase(Locale.ROOT));
            if (chosen > 0) {
                SEEN.put(chosen, lastStarted);
            }
        }
    }

    /** Whether a button of the difficulty menu ({@code HARD}, lore "Click to start a hard quest.") or the tier menu ({@code Rare}, "Click to start.") is the one to pick. */
    public static boolean marks(String name, List<Text> lore) {
        Quest q = SEEN.getOrDefault(chosen, lastStarted);
        if (q == null || lore.isEmpty()) {
            return false;
        }
        String first = lore.get(0).getString().trim().toLowerCase(Locale.ROOT);
        String n = name.trim().toLowerCase(Locale.ROOT);
        if (first.equals("click to start.")) {
            return n.equals(q.tier());
        }
        return n.equals(q.difficulty()) && first.matches("click to start an? " + q.difficulty() + " quest\\.");
    }

    public static void reset() {
        SEEN.clear();
        chosen = 0;
        lastStarted = null;
    }
}
