package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofisAddons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;
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

    /** One unfinished mission, {@code goal} as shown (a glyph for the rarity missions). */
    public record Mission(String goal, int progress, int needed) {}

    private static Constructor<?> questCtor;
    private static boolean failed;

    private FoeDailyMissions() {}

    /** Unfinished missions in lore order; the tooltip also lists finished ones (✔), which foecollab does not track. */
    public static List<Mission> parse(List<String> lore) {
        List<Mission> out = new ArrayList<>();
        String goal = null;
        boolean done = false;
        for (String raw : lore) {
            String line = stripFrame(raw);
            Matcher name = NAME.matcher(line);
            if (name.matches()) {
                goal = name.group("goal");
                done = name.group("done") != null;
                continue;
            }
            Matcher prog = PROGRESS.matcher(line);
            if (goal != null && prog.matches()) {
                int p = num(prog.group("p"));
                int n = num(prog.group("n"));
                if (!done && n > 0 && p < n && !goal.isBlank()) {
                    out.add(new Mission(goal, p, n));
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
            for (Mission m : parse(lore)) {
                quests.add(questCtor.newInstance(m.goal(), m.progress(), m.needed(), slot++));
            }
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true; // a foecollab update changed Quest: stop quietly, its own parse still runs
            DofisAddons.LOGGER.warn("daily missions: cannot build foecollab's quests: {}", e.toString());
        }
    }
}
