package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.trigger.Trigger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Lines copied from l's logs of 2026-10-09. */
class ChatBoxesTest {
    private static ChatBox crew() {
        return ChatBoxes.defaults().stream().filter(b -> b.name.equals("crew")).findFirst().orElseThrow();
    }

    private static boolean takes(ChatBox box, String line) {
        return box.rules.stream().anyMatch(f -> ChatTweaks.matcher(f).match(line) != null);
    }

    @Test
    void crewBoxTakesCrewsTagLines() {
        ChatBox crew = crew();
        assertTrue(takes(crew, "CREWS » FoE |  Doggertie joined the server"));
        assertTrue(takes(crew, "CREWS » FoE |  Doggertie left the server"));
        assertTrue(takes(crew, "CREWS » Mission completed! Catch 3x Gigantic → + 50Kxp"));
        assertTrue(takes(crew, "CREWS » Teleporting to your crew island..."));
        assertTrue(takes(crew, "[Crew Chat] Someone » hello"));
    }

    @Test
    void crewBoxLeavesOtherLinesAlone() {
        ChatBox crew = crew();
        assertFalse(takes(crew, "! [368]   Realstranden » CREWS  Mission completed! Catch 533x Rare  + 25Kxp"));
        assertFalse(takes(crew, "FISH FRENZY » A Fish Frenzy is active at Kenai River (for 1m 32s)"));
        assertFalse(takes(crew, "✉ From someone » hi"));
    }

    @Test
    void savedCrewBoxWithOnlyTheOldRuleGetsTheCrewsRule() {
        ChatBox old = new ChatBox();
        old.name = "crew";
        old.rules.add(new ChatFilter("^\\[crew chat\\] ", Trigger.MatchMode.REGEX));
        List<ChatBox> saved = new ArrayList<>(List.of(old));
        ChatBoxes.addCrewsRule(saved);
        assertEquals(2, old.rules.size());
        assertTrue(takes(old, "CREWS » Teleporting to your crew island."));
        ChatBoxes.addCrewsRule(saved); // not added twice
        assertEquals(2, old.rules.size());
    }

    /** F11 and split screen change the window size; a box 50 px from the right edge has to stay 50 px from it. */
    @Test
    void boxKeepsItsDistanceFromTheRightEdgeWhenTheWindowNarrows() {
        ChatBox box = new ChatBox();
        box.width = 180;
        box.x = (960 - 180 - 50) * 100.0 / 960;
        box.y = 20;
        int[] full = ChatBoxes.rect(box, 960, 540, false);
        assertEquals(50, 960 - (full[0] + full[2]));
        int[] half = ChatBoxes.rect(box, 480, 540, false);
        assertEquals(50, 480 - (half[0] + half[2]));
    }

    @Test
    void boxAtTheBottomKeepsItsDistanceFromTheBottomEdge() {
        ChatBox box = new ChatBox();
        box.width = 180;
        box.lines = 7;
        int h = ChatBoxes.heightOf(7);
        box.x = 5.0 / 960 * 100;
        box.y = (540 - h - 30) * 100.0 / 540; // 30 from the bottom
        assertEquals(30, 540 - (ChatBoxes.rect(box, 960, 540, false)[1] + h));
        assertEquals(30, 270 - (ChatBoxes.rect(box, 960, 270, false)[1] + h));
    }

    @Test
    void openChatGrowsAPinnedBoxUpwardFromItsBottomEdge() {
        ChatBox box = new ChatBox();
        box.width = 180;
        box.x = 50;
        box.y = 80;
        int[] closed = ChatBoxes.rect(box, 960, 540, false);
        int[] open = ChatBoxes.rect(box, 960, 540, true);
        assertEquals(closed[1] + closed[3], open[1] + open[3]);
        assertTrue(open[3] > closed[3]);
    }

    @Test
    void editedCrewBoxIsLeftAlone() {
        ChatBox edited = new ChatBox();
        edited.name = "crew";
        edited.rules.add(new ChatFilter("^something else", Trigger.MatchMode.REGEX));
        ChatBoxes.addCrewsRule(new ArrayList<>(List.of(edited)));
        assertEquals(1, edited.rules.size());
    }
}
