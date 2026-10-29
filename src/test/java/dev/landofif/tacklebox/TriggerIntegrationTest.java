package dev.landofif.tacklebox;

import dev.landofif.tacklebox.display.TitleOverlay;
import dev.landofif.tacklebox.display.TotemPop;
import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.trigger.TriggerPresets;
import dev.landofif.tacklebox.trigger.TriggerStore;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Chat lines from luna's logs through the trigger engine with the presets, as the game hands them over: each title is on screen
 * in the same call, quest lines that come together share one title, rare catches stack, another trigger replaces them, and the
 * totem pop gets the item's name. The colours are made up (the server's are not logged); the text is real.
 */
class TriggerIntegrationTest {
    @AfterEach
    void tearDown() {
        TriggerStore.all().clear();
        TitleOverlay.clear();
    }

    private static Trigger add(Trigger t) {
        t.fishOnMcOnly = false; // no server in tests
        TriggerStore.all().add(t);
        return t;
    }

    private static Text colored(String start, String item) {
        MutableText t = Text.literal(start).styled(s -> s.withColor(0xFFFFFF));
        return t.append(Text.literal(item).styled(s -> s.withColor(0x55FF55)));
    }

    private static List<String> lines(List<TitleOverlay.Shown> shown) {
        return shown.stream().map(s -> s.title().getString() + " / " + s.subtitle().getString()).toList();
    }

    @Test
    void chatLinesToTitlesAndPops() throws InterruptedException {
        TriggerStore.all().clear();
        Trigger quest = add(TriggerPresets.questComplete());
        Trigger rare = add(TriggerPresets.rareCatch());
        Trigger pet = add(TriggerPresets.petDrop());

        // two quests done by one catch: one title, changed in place by the second line
        assertTrue(TriggerEngine.onGameMessage(Text.literal("QUESTS » Quest #3 has been completed!"), false));
        assertEquals(List.of("quest 3 complete / type /quests to claim it"), lines(TitleOverlay.shown()));
        TitleOverlay.Shown first = TitleOverlay.shown().get(0);
        TriggerEngine.onGameMessage(Text.literal("QUESTS » Quest #5 has been completed!"), false);
        assertEquals(List.of("quests 3 & 5 complete / type /quests to claim it"), lines(TitleOverlay.shown()));
        assertSame(first, TitleOverlay.shown().get(0));
        assertEquals(1, TriggerEngine.count(quest));

        // a rare catch is another trigger: the quest title goes, the shard shows alone, with the shard's chat colour
        TriggerEngine.onGameMessage(colored("RARE CATCH! You pulled a 1x ", "Subtropical Shard"), false);
        assertEquals(List.of("rare catch! / subtropical shard"), lines(TitleOverlay.shown()));
        assertEquals(rare.id, TitleOverlay.shown().get(0).trigger());
        assertEquals("Subtropical Shard", TotemPop.last());
        assertEquals(TotemPop.Source.WAIT, TotemPop.source(TotemPop.last(), id -> false, key -> false, key -> false));
        assertEquals("shard:subtropical", TotemPop.fallback(TotemPop.last()));

        // another player's rare catch changes nothing
        TriggerEngine.onGameMessage(Text.literal("! [366] meowfih » RARE CATCH! You pulled 1x Prospecting Amulet"), false);
        assertEquals(List.of("rare catch! / subtropical shard"), lines(TitleOverlay.shown()));

        // the next own rare catch, past the merge window and the 1 s cooldown, stacks under the first
        Thread.sleep(1100);
        TriggerEngine.tick();
        TriggerEngine.onGameMessage(Text.literal("RARE CATCH! You pulled 1x Lightning in a Bottle"), false);
        assertEquals(List.of("rare catch! / subtropical shard", "rare catch! / lightning in a bottle"), lines(TitleOverlay.shown()));
        assertEquals(List.of(1, 0), TitleOverlay.shown().stream().map(TitleOverlay.Shown::place).toList());
        assertEquals("Lightning in a Bottle", TotemPop.last());
        assertEquals(TotemPop.Source.INVENTORY,
                TotemPop.source(TotemPop.last(), id -> false, key -> key.equals("lightninginabottle"), key -> false));
        assertEquals("minecraft:totem_of_undying", TotemPop.fallback(TotemPop.last()));

        // a pet drop replaces both, and pops the pet's head
        TriggerEngine.onGameMessage(Text.literal("PET DROP! You pulled a [COMMON] Capybara Pet"), false);
        assertEquals(List.of("pet drop! / capybara"), lines(TitleOverlay.shown()));
        assertEquals(pet.id, TitleOverlay.shown().get(0).trigger());
        assertEquals("pet:Capybara", TotemPop.last());
        assertEquals(TotemPop.Source.HEAD, TotemPop.source(TotemPop.last(), id -> false, key -> true, key -> true));
    }

    @Test
    void petAtMaxLevelPopsItsHead() {
        Trigger max = add(TriggerPresets.petMaxLevel());
        add(TriggerPresets.petDrop());
        assertTrue(TriggerEngine.onGameMessage(Text.literal("PETS » Your Duck Pet is now level 98 → 99 (+1)"), false));
        assertEquals(List.of(), lines(TitleOverlay.shown()));
        TriggerEngine.onGameMessage(Text.literal("PETS » Your Marsh Rabbit Pet is now level 97 → 100 (+3)"), false);
        assertEquals(List.of("marsh rabbit max level! / level 100"), lines(TitleOverlay.shown()));
        assertEquals(max.id, TitleOverlay.shown().get(0).trigger());
        assertEquals("pet:Marsh Rabbit", TotemPop.last());
    }
}
