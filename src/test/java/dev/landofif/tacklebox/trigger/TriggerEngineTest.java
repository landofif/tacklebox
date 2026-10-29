package dev.landofif.tacklebox.trigger;

import dev.landofif.tacklebox.display.SoundQueue;
import dev.landofif.tacklebox.display.TitleOverlay;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;

/** A trigger fires on the line that matches; more lines of it within 250 ms change that title, with no second sound or firing. */
class TriggerEngineTest {
    private final long[] now = {50_000};
    private int sounds;
    private Trigger quest;

    @BeforeEach
    void setUp() {
        TriggerEngine.clock = () -> now[0];
        TriggerEngine.playSound = s -> sounds++;
        TitleOverlay.clear();
        quest = TriggerPresets.questComplete();
        quest.fishOnMcOnly = false;
        quest.title.subtitle = "{matches} line{s}";
        TriggerStore.all().clear();
        TriggerStore.all().add(quest);
    }

    @AfterEach
    void tearDown() {
        TriggerEngine.clock = System::currentTimeMillis;
        TriggerEngine.playSound = SoundQueue::play;
        TriggerStore.all().clear();
        TitleOverlay.clear();
    }

    private static boolean chat(String line) {
        return TriggerEngine.onGameMessage(Text.literal(line), false);
    }

    private static List<String> titles() {
        return TitleOverlay.shown().stream().map(s -> s.title().getString()).toList();
    }

    @Test
    void firesOnTheLineWithoutWaiting() {
        chat("QUESTS » Quest #3 has been completed!");
        assertEquals(List.of("quest 3 complete"), titles());
        assertEquals("1 line", TitleOverlay.shown().get(0).subtitle().getString());
        assertEquals(1, sounds);
        assertEquals(1, TriggerEngine.count(quest));
    }

    @Test
    void laterLinesWithin250msJoinTheTitleOnScreen() {
        chat("QUESTS » Quest #3 has been completed!");
        TitleOverlay.Shown first = TitleOverlay.shown().get(0);
        now[0] += 120;
        TriggerEngine.tick();
        chat("QUESTS » Quest #5 has been completed!");

        assertEquals(List.of("quests 3 & 5 complete"), titles());
        assertSame(first, TitleOverlay.shown().get(0));
        assertEquals("2 lines", first.subtitle().getString());
        assertEquals(1, sounds, "no second sound");
        assertEquals(1, TriggerEngine.count(quest), "no second firing");

        now[0] += 100;
        chat("QUESTS » Quest #6 has been completed!");
        assertEquals(List.of("quests 3, 5 & 6 complete"), titles());
        assertEquals(1, sounds);
    }

    @Test
    void afterTheMergeWindowTheCooldownStillHolds() {
        chat("QUESTS » Quest #3 has been completed!");
        now[0] += 400;
        TriggerEngine.tick();
        chat("QUESTS » Quest #5 has been completed!"); // cooldown is 1 s
        assertEquals(List.of("quest 3 complete"), titles());
        assertEquals(1, sounds);

        now[0] += 700;
        TriggerEngine.tick();
        chat("QUESTS » Quest #5 has been completed!");
        assertEquals(List.of("quest 3 complete", "quest 5 complete"), titles(), "a new firing stacks under the first");
        assertEquals(2, sounds);
        assertEquals(2, TriggerEngine.count(quest));
    }

    @Test
    void mergedLinesAreHiddenToo() {
        quest.hideMessage = true;
        assertFalse(chat("QUESTS » Quest #3 has been completed!"));
        assertFalse(chat("QUESTS » Quest #5 has been completed!"));
    }
}
