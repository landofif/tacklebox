package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.menu.Compendium;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The server's 2026-10-11 update split every Daily Missions lore entry over two lines ({@code - 🎣 Name ✔} then {@code ┗ (3/3)}) and
 * added section headers, so foecollab's tracker, which wants {@code | Name (3/3)} on one line, finds nothing. After foecollab's own
 * tick (see {@code DailyQuestHandlerMixin}) this reads the new layout off the item and adds the unfinished missions to its list,
 * built by reflection so nothing is compiled against it.
 */
public final class FoeDailyMissions {
    private static final Pattern NAME = Pattern.compile("^\\s*-\\s+\\x{1F3A3}\\s+(?<goal>.*?)\\s*(?<done>✔)?\\s*$");
    private static final Pattern PROGRESS = Pattern.compile("^\\s*┗\\s*\\((?<p>[\\d,]+)\\s*/\\s*(?<n>[\\d,]+)\\)\\s*$");

    private static final Pattern SECTION = Pattern.compile("^\\s*(?<name>\\S.*?)\\s+Missions\\s*$");

    /** One unfinished mission, {@code goal} as shown (a glyph for the rarity missions), {@code section} the header above it ("Base Missions"...). */
    public record Mission(String section, String goal, int progress, int needed) {}

    /** Header of each mission's section as of the last read, for the hud. */
    private static volatile Map<String, String> sections = Map.of();

    private static Constructor<?> questCtor;
    private static Method updateQuest;
    private static boolean failed;

    private FoeDailyMissions() {}

    /** Unfinished missions in lore order; the tooltip also lists finished ones (✔), which foecollab does not track. */
    public static List<Mission> parse(List<String> lore) {
        List<Mission> out = new ArrayList<>();
        String goal = null;
        String section = "";
        boolean done = false;
        for (String raw : lore) {
            String line = stripFrame(raw);
            Matcher name = NAME.matcher(line);
            if (name.matches()) {
                goal = name.group("goal");
                done = name.group("done") != null;
                continue;
            }
            Matcher head = SECTION.matcher(line);
            if (head.matches() && !head.group("name").equals("Daily")) {
                section = line.strip();
                continue;
            }
            Matcher prog = PROGRESS.matcher(line);
            if (goal != null && prog.matches()) {
                int p = num(prog.group("p"));
                int n = num(prog.group("n"));
                if (!done && n > 0 && p < n && !goal.isBlank()) {
                    out.add(new Mission(section, goal, p, n));
                }
                goal = null;
            }
        }
        return out;
    }

    /** Drops the server's tooltip frame glyphs (they end in U+EEE2) in front of a lore line, keeping the indent that follows. */
    static String stripFrame(String raw) {
        return raw.substring(raw.lastIndexOf('\uEEE2') + 1);
    }

    private static int num(String s) {
        return Integer.parseInt(s.replace(",", ""));
    }

    /** The Daily Missions item of the open menu (foecollab finds it the same way), or null. */
    public static List<String> missionLore(MinecraftClient client) {
        if (client.player == null) {
            return null;
        }
        for (Slot slot : client.player.currentScreenHandler.slots) {
            if (slot.inventory == client.player.getInventory()) {
                continue;
            }
            ItemStack stack = slot.getStack();
            if (stack.isOf(Items.END_CRYSTAL) && "Daily Missions".equals(stack.getName().getString())) {
                LoreComponent lore = stack.get(DataComponentTypes.LORE);
                if (lore == null) {
                    return null;
                }
                List<String> lines = new ArrayList<>();
                for (Text t : lore.lines()) {
                    lines.add(t.getString());
                }
                return lines;
            }
        }
        return null;
    }

    /** Fills foecollab's {@code quests} list ({@code Quest(String goal, int progress, int needed, int slot)}), numbering from 1 like it does. */
    @SuppressWarnings("unchecked")
    public static void fill(List<Object> quests, MinecraftClient client) {
        if (failed || !quests.isEmpty()) {
            return;
        }
        List<String> lore = missionLore(client);
        if (lore == null) {
            return;
        }
        try {
            if (questCtor == null) {
                questCtor = Class.forName("io.github.foecollab.handler.DailyQuestHandler$Quest")
                        .getConstructor(String.class, int.class, int.class, int.class);
            }
            int slot = 1;
            Map<String, String> bySection = new HashMap<>();
            for (Mission m : parse(lore)) {
                bySection.put(m.goal(), m.section());
                quests.add(questCtor.newInstance(m.goal(), m.progress(), m.needed(), slot++));
            }
            sections = bySection;
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true; // a foecollab update changed Quest: stop quietly, its own parse still runs
            DofisAddons.LOGGER.warn("daily missions: cannot build foecollab's quests: {}", e.toString());
        }
    }

    /** Foecollab's catch hook still calls {@code updateQuest} with the old goal names; the update renamed those missions. */
    public static String renameGoal(String goal) {
        return switch (goal) {
            case "Total Caught" -> "Anywhere";
            case "Mythical Caught" -> "\uF037"; // the mythical rarity glyph
            default -> goal;
        };
    }

    /** The species missions (Lumpfish, Opah...) are named after the fish, which foecollab no longer counts on its own. */
    public static void onCatch(String fishId) {
        if (failed) {
            return;
        }
        try {
            Class<?> c = Class.forName("io.github.foecollab.handler.DailyQuestHandler");
            if (updateQuest == null) {
                updateQuest = c.getMethod("updateQuest", String.class);
            }
            updateQuest.invoke(c.getMethod("instance").invoke(null), Compendium.name(fishId));
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true;
            DofisAddons.LOGGER.warn("daily missions: cannot count a catch: {}", e.toString());
        }
    }

    /** The section header a mission sits under, or null when unknown. */
    public static String sectionOf(String goal) {
        return sections.get(goal);
    }

    private static final TextColor NEEDED = TextColor.fromRgb(0x7FB8FF);
    private static Field questsField, goalField, progressField, neededField, slotField, startedField;
    private static Method instance, questDone;
    private static boolean styleFailed;

    /**
     * Rewrites the quest lines of foecollab's daily mission hud ({@code lines}: title line(s), then one line per quest) to the colours of
     * the tooltip (green name, yellow progress, blue goal) with a header whenever the section changes. Quests that are done or not
     * started keep foecollab's line.
     */
    public static void styleLines(List<Object> lines) {
        if (styleFailed) {
            return;
        }
        try {
            if (instance == null) {
                Class<?> handler = Class.forName("io.github.foecollab.handler.DailyQuestHandler");
                Class<?> quest = Class.forName("io.github.foecollab.handler.DailyQuestHandler$Quest");
                instance = handler.getMethod("instance");
                questsField = handler.getField("quests");
                goalField = quest.getField("goal");
                progressField = quest.getField("progress");
                neededField = quest.getField("needed");
                slotField = quest.getField("slot");
                startedField = quest.getField("isStarted");
                questDone = quest.getMethod("questDone");
            }
            List<?> quests = (List<?>) questsField.get(instance.invoke(null));
            int title = lines.size() - quests.size();
            if (title < 0) {
                return;
            }
            List<Object> out = new ArrayList<>(lines.subList(0, title));
            String last = null;
            for (int i = 0; i < quests.size(); i++) {
                Object q = quests.get(i);
                String goal = (String) goalField.get(q);
                String section = goal == null ? null : sectionOf(goal);
                if (section != null && !section.equals(last)) {
                    out.add(Foe.text(section).formatted(Formatting.GRAY));
                    last = section;
                }
                if (goal == null || goal.isBlank() || !(boolean) startedField.get(q) || (boolean) questDone.invoke(q)) {
                    out.add(lines.get(title + i)); // done / not started
                    continue;
                }
                out.add(Text.empty()
                        .append(Text.literal("#" + slotField.get(q) + ": ").formatted(Formatting.GRAY))
                        .append(Text.literal(goal).formatted(Formatting.GREEN))
                        .append(Text.literal(" (").formatted(Formatting.GRAY))
                        .append(Text.literal(String.valueOf(progressField.get(q))).formatted(Formatting.YELLOW))
                        .append(Text.literal("/").formatted(Formatting.GRAY))
                        .append(Text.literal(String.valueOf(neededField.get(q))).setStyle(net.minecraft.text.Style.EMPTY.withColor(NEEDED)))
                        .append(Text.literal(")").formatted(Formatting.GRAY)));
            }
            lines.clear();
            lines.addAll(out);
        } catch (ReflectiveOperationException | RuntimeException e) {
            styleFailed = true; // keep foecollab's own lines
            DofisAddons.LOGGER.warn("daily missions: cannot restyle the hud: {}", e.toString());
        }
    }
}
