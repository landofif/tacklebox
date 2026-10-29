package dev.landofif.tacklebox.menu;

import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Lines and item texts from luna's /quests dumps of 2026-10-29 (glyph prefixes as in the dump). */
class QuestRedoTest {
    static final String PREFIX = " 퀃    ";

    static List<Text> questLore(String difficulty, int count, String tier) {
        return List.of(Text.literal(" 퀁    Fishing Quest #3"),
                Text.literal(PREFIX + difficulty + " ꞯᴜᴇsᴛ"),
                Text.literal(PREFIX + "Catch " + count + "x " + tier + " fish"),
                Text.literal(PREFIX + "at Murray River"),
                Text.literal(PREFIX + "ɪɴ ᴘʀᴏɢʀᴇss (70/71)"));
    }

    static final String HARD = "ʜᴀʀᴅ", EASY = "ᴇᴀsʏ";

    @BeforeEach
    void reset() {
        QuestRedo.reset();
    }

    @Test
    void readsDifficultyAndTierFromAQuestItem() {
        assertEquals(new QuestRedo.Quest("hard", "rare"), QuestRedo.parse(questLore(HARD, 71, "Rare")));
        assertEquals(new QuestRedo.Quest("easy", "adult"), QuestRedo.parse(questLore(EASY, 12, "Adult")));
        assertNull(QuestRedo.parse(List.of(Text.literal("Click to start."))));
    }

    @Test
    void questNumberFromNameElseSlot() {
        assertEquals(3, QuestRedo.number("Fishing Quest #3", 0));
        assertEquals(1, QuestRedo.number("Fishing Quest", 12));
        assertEquals(4, QuestRedo.number("Fishing Quest", 20));
        assertEquals(8, QuestRedo.number("Fishing Quest", 24));
        assertEquals(0, QuestRedo.number("Close", 40));
        assertEquals(0, QuestRedo.number("MEDIUM", 13)); // the difficulty menu shares the title and slot 13
    }

    @Test
    void clickedQuestMarksItsDifficultyThenItsTier() {
        QuestRedo.see(3, questLore(HARD, 71, "Rare"));
        QuestRedo.see(4, questLore(EASY, 20, "Epic"));
        QuestRedo.click(3, List.of()); // the claimed slot no longer says what it was
        List<Text> hardLore = List.of(Text.literal("Click to start a hard quest."));
        assertTrue(QuestRedo.marks("HARD", hardLore));
        assertFalse(QuestRedo.marks("EASY", List.of(Text.literal("Click to start an easy quest."))));
        List<Text> tierLore = List.of(Text.literal("Click to start."));
        assertTrue(QuestRedo.marks("Rare", tierLore));
        assertFalse(QuestRedo.marks("Epic", tierLore));
        assertFalse(QuestRedo.marks("Return", List.of())); // other buttons in the same menus
    }

    @Test
    void startedChatLineIsTheFallback() {
        QuestRedo.onChat(Text.literal("QUEST » Started a new Medium fishing quest to catch 29x Epic at Cypress Lake"));
        QuestRedo.click(5, List.of());
        assertTrue(QuestRedo.marks("MEDIUM", List.of(Text.literal("Click to start a medium quest."))));
        assertTrue(QuestRedo.marks("Epic", List.of(Text.literal("Click to start."))));
    }

    @Test
    void nothingKnownMarksNothing() {
        QuestRedo.click(2, List.of());
        assertFalse(QuestRedo.marks("HARD", List.of(Text.literal("Click to start a hard quest."))));
    }
}
