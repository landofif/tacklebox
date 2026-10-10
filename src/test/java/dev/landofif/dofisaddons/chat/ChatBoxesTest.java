package dev.landofif.dofisaddons.chat;

import dev.landofif.dofisaddons.trigger.Trigger;
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

    @Test
    void editedCrewBoxIsLeftAlone() {
        ChatBox edited = new ChatBox();
        edited.name = "crew";
        edited.rules.add(new ChatFilter("^something else", Trigger.MatchMode.REGEX));
        ChatBoxes.addCrewsRule(new ArrayList<>(List.of(edited)));
        assertEquals(1, edited.rules.size());
    }
}
