package dev.landofif.tacklebox;

import dev.landofif.tacklebox.menu.QuestRedo;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * luna's /quests round of 2026-10-29 (dumps 23:55 to 23:58): quest #3, hard rare at Murray River, is done; she opens it, picks
 * HARD, then Rare. The same steps here with every button of the two menus as the dumps list them: only those two get marked.
 */
class QuestRedoIntegrationTest {
    private static final String PREFIX = " 퀃    ";
    private static final String HARD = "ʜᴀʀᴅ", MEDIUM = "ᴍᴇᴅɪᴜᴍ";

    private static List<Text> quest(int n, String difficulty, int count, String tier) {
        return List.of(Text.literal(" 퀁    Fishing Quest #" + n),
                Text.literal(PREFIX + difficulty + " ꞯᴜᴇsᴛ"),
                Text.literal(PREFIX + "Catch " + count + "x " + tier + " fish"),
                Text.literal(PREFIX + "at Murray River"));
    }

    /** Slot name to lore of the difficulty menu (dump 23:57:53) and the tier menu (dump 23:57:57). */
    private static final Map<String, List<Text>> DIFFICULTY = new LinkedHashMap<>(), TIER = new LinkedHashMap<>();

    static {
        DIFFICULTY.put("EASY", List.of(Text.literal("Click to start an easy quest.")));
        DIFFICULTY.put("MEDIUM", List.of(Text.literal("Click to start a medium quest.")));
        DIFFICULTY.put("HARD", List.of(Text.literal("Click to start a hard quest.")));
        DIFFICULTY.put("Return", List.of());
        DIFFICULTY.put("Close", List.of());
        for (String t : List.of("Adult", "Large", "Rare", "Epic", "Legendary")) {
            TIER.put(t, List.of(Text.literal("Click to start.")));
        }
        TIER.put("Return", List.of());
        TIER.put("Close", List.of());
    }

    private static List<String> marked(Map<String, List<Text>> menu) {
        return menu.entrySet().stream().filter(e -> QuestRedo.marks(e.getKey(), e.getValue())).map(Map.Entry::getKey).toList();
    }

    @BeforeEach
    void reset() {
        QuestRedo.reset();
    }

    @Test
    void finishedQuestIsPickedAgainButtonByButton() {
        int[] slots = {12, 13, 14, 20, 21, 22, 23, 24};
        for (int i = 0; i < slots.length; i++) { // the list as drawn: every quest seen, #3 medium epic, the rest hard rare
            int n = QuestRedo.number("Fishing Quest #" + (i + 1), slots[i]);
            QuestRedo.see(n, i == 2 ? quest(3, MEDIUM, 71, "Epic") : quest(n, HARD, 80, "Rare"));
        }
        QuestRedo.click(QuestRedo.number("Fishing Quest #3", 14), quest(3, MEDIUM, 71, "Epic")); // claim the green one
        QuestRedo.click(QuestRedo.number("Fishing Quest #3", 14), List.of(Text.literal("Click to start a quest."))); // now empty

        assertEquals(List.of("MEDIUM"), marked(DIFFICULTY));
        assertEquals(List.of("Epic"), marked(TIER));

        QuestRedo.onChat(Text.literal("QUEST » Started a new Medium fishing quest to catch 78x Epic at Murray River"));
        QuestRedo.click(QuestRedo.number("Fishing Quest #4", 20), quest(4, HARD, 83, "Rare"));
        assertEquals(List.of("HARD"), marked(DIFFICULTY));
        assertEquals(List.of("Rare"), marked(TIER));
    }
}
