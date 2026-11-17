package dev.landofif.tacklebox.trigger;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.PetRating;
import dev.landofif.tacklebox.display.SoundQueue;
import dev.landofif.tacklebox.display.TitleOverlay;
import dev.landofif.tacklebox.display.TotemPop;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Your own pet drops and rare catches are built in; each kind of drop can lose its title, totem pop or sound in the settings. */
class RareDropsTest {
    private int sounds;
    /** The engine's cooldowns outlive a test, so each one starts a minute later. */
    private static long clock = 900_000;
    private final long[] now = {clock += 60_000};

    @BeforeEach
    void setUp() {
        TriggerEngine.clock = () -> now[0];
        PetRating.clock = () -> now[0];
        PetRating.reset();
        TriggerEngine.playSound = s -> sounds++;
        TriggerEngine.assumeFishOnMc = true;
        TriggerEngine.forgetFires();
        reset();
        TriggerStore.all().clear();
        TitleOverlay.clear();
    }

    @AfterEach
    void tearDown() {
        TriggerEngine.clock = System::currentTimeMillis;
        PetRating.clock = System::currentTimeMillis;
        PetRating.reset();
        TriggerEngine.playSound = SoundQueue::play;
        TriggerEngine.assumeFishOnMc = false;
        reset();
        TriggerStore.all().clear();
        TitleOverlay.clear();
    }

    private static void reset() {
        TackleboxSettings s = TackleboxSettings.get();
        s.rareDrops = true;
        s.dropsMigrated = false;
        s.dropTitleOff.clear();
        s.dropPopOff.clear();
        s.dropSoundOff.clear();
    }

    private static void chat(String line) {
        TriggerEngine.onGameMessage(Text.literal(line), false);
    }

    private static Matcher.Result match(Trigger t, String line) {
        return Matcher.of(t).match(line);
    }

    private static String row(String line) {
        for (Trigger t : RareDrops.triggers()) {
            Matcher.Result r = match(t, line);
            if (r != null) {
                return RareDrops.row(t, r);
            }
        }
        return null;
    }

    @Test
    void everyKindOfDropHasItsRow() {
        assertEquals(RareDrops.PET, row("PET DROP! You pulled a [COMMON] Capybara Pet"));
        assertEquals(RareDrops.SHARD, row("RARE CATCH! You pulled a 1x Subtropical Shard"));
        assertEquals(RareDrops.SHARD, row("RARE CATCH! You pulled a 3x Arctic Shard"));
        assertEquals(RareDrops.BOTTLE, row("RARE CATCH! You pulled 1x Lightning in a Bottle"));
        assertEquals(RareDrops.CAPSULE, row("RARE CATCH! You pulled 1x Infusion Capsule"));
        assertEquals(RareDrops.AMULET, row("RARE CATCH! You pulled 1x Prospecting Amulet"));
        assertEquals(RareDrops.BIGFOOT, row("RARE CATCH! You pulled 1x Bigfoot Tooth"));
        assertEquals(RareDrops.OTHER, row("RARE CATCH! You pulled 1x Something New"));
        assertNull(row("! [366] meowfih » RARE CATCH! You pulled 1x Prospecting Amulet"), "other players' drops never show");
        assertNull(row("! [366] meowfih » PET DROP! You pulled a Capybara Pet"));
        assertEquals(RareDrops.ROWS.size(), 7);
        assertEquals(RareDrops.PET, row(" +  Lynx Pet"), "a quest's pet reward is a pet drop too");
        assertNull(row(" + 2x Mediterranean Shard (x3)"));
        assertNull(row(" - 🎣 Lynx Pet  \n    ┗ (0/1) ∞ x5\n"));
    }

    @Test
    void everythingOnByDefault() {
        chat("RARE CATCH! You pulled a 1x Subtropical Shard");
        assertEquals(List.of("+ Subtropical Shard"), titles());
        assertEquals("Subtropical Shard", TotemPop.last());
        assertEquals(1, sounds, "one sound request, the queue repeats it");
    }

    @Test
    void titleIsPlusTheItemInItsChatColour() {
        TriggerEngine.onGameMessage(TriggerColorsTest.line("RARE CATCH! ", 0xFFAA00, "You pulled a 1x ", 0xFFFFFF, "Subtropical Shard", 0x55FF55), false);
        TitleOverlay.Shown shown = TitleOverlay.shown().get(0);
        assertEquals("", shown.subtitle().getString());
        List<String> chars = TriggerColorsTest.chars(shown.title());
        assertEquals("+:ffaa00:b", chars.get(0));
        assertEquals("S:55ff55:b", chars.get(2));
    }

    @Test
    void countShowsOnlyWhenMoreThanOne() {
        chat("RARE CATCH! You pulled a 3x Arctic Shard");
        assertEquals(List.of("+3 Arctic Shard"), titles());
        assertEquals("Arctic Shard", TotemPop.last());
        TitleOverlay.clear();
        now[0] += 60_000;
        chat("RARE CATCH! You pulled a 1x Arctic Shard");
        assertEquals(List.of("+ Arctic Shard"), titles());
    }

    private static List<String> titles() {
        return TitleOverlay.shown().stream().map(s -> s.title().getString()).toList();
    }

    @Test
    void titleOffKeepsThePopAndTheSound() {
        TackleboxSettings.get().dropTitleOff.add(RareDrops.SHARD);
        chat("RARE CATCH! You pulled a 1x Subtropical Shard");
        assertEquals(List.of(), titles());
        assertEquals("Subtropical Shard", TotemPop.last());
        assertTrue(sounds > 0);
    }

    @Test
    void popOffKeepsTheTitle() {
        TackleboxSettings.get().dropPopOff.add(RareDrops.PET);
        String before = TotemPop.last();
        chat("PET DROP! You pulled a [COMMON] Capybara Pet");
        assertEquals(List.of("+ Capybara Pet"), titles());
        assertEquals(before, TotemPop.last(), "no pop for the pet");
    }

    @Test
    void soundOffIsSilent() {
        TackleboxSettings.get().dropSoundOff.add(RareDrops.BOTTLE);
        chat("RARE CATCH! You pulled 1x Lightning in a Bottle");
        assertEquals(List.of("+ Lightning in a Bottle"), titles());
        assertEquals(0, sounds);
    }

    @Test
    void oneKindOffLeavesTheOthers() {
        TackleboxSettings.get().dropTitleOff.add(RareDrops.SHARD);
        chat("RARE CATCH! You pulled 1x Lightning in a Bottle");
        assertEquals(List.of("+ Lightning in a Bottle"), titles());
    }

    @Test
    void allThreeOffMeansNothingFiresAndNoCooldownIsUsed() {
        TackleboxSettings s = TackleboxSettings.get();
        s.dropTitleOff.add(RareDrops.AMULET);
        s.dropPopOff.add(RareDrops.AMULET);
        s.dropSoundOff.add(RareDrops.AMULET);
        assertNull(RareDrops.apply(RareDrops.triggers().get(1), match(RareDrops.triggers().get(1), "RARE CATCH! You pulled 1x Prospecting Amulet")));
        chat("RARE CATCH! You pulled 1x Prospecting Amulet");
        assertEquals(List.of(), titles());
        assertEquals(0, sounds);
        chat("RARE CATCH! You pulled 1x Infusion Capsule");
        assertEquals(1, titles().size(), "the next drop is not blocked by a cooldown");
    }

    @Test
    void mainSwitchOffRemovesTheBuiltIns() {
        TackleboxSettings.get().rareDrops = false;
        assertTrue(RareDrops.triggers().isEmpty());
        chat("PET DROP! You pulled a [COMMON] Capybara Pet");
        assertEquals(List.of(), titles());
    }

    @Test
    void theBuiltInsAreNotAffectedByTheCopy() {
        Trigger pet = RareDrops.triggers().get(0);
        TackleboxSettings.get().dropTitleOff.add(RareDrops.PET);
        Trigger copy = RareDrops.apply(pet, match(pet, "PET DROP! You pulled a [COMMON] Capybara Pet"));
        assertNotNull(copy);
        assertFalse(copy.title.enabled);
        assertTrue(pet.title.enabled);
        assertEquals(pet.id, copy.id);
    }

    @Test
    void oldPresetTriggersAreSwitchedOffOnceAndEditedOnesStay() {
        Trigger oldPet = TriggerPresets.petDrop();
        Trigger oldRare = TriggerPresets.rareCatch();
        Trigger edited = TriggerPresets.rareCatch();
        edited.pattern = edited.pattern + "x";
        Trigger quest = TriggerPresets.questComplete();
        TriggerStore.all().addAll(List.of(oldPet, oldRare, edited, quest));

        assertTrue(RareDrops.migrate());
        assertFalse(oldPet.enabled);
        assertFalse(oldRare.enabled);
        assertTrue(edited.enabled);
        assertTrue(quest.enabled);
        assertTrue(TackleboxSettings.get().dropsMigrated);

        oldPet.enabled = true; // luna switches it back on herself: not touched again
        assertFalse(RareDrops.migrate());
        assertTrue(oldPet.enabled);
    }

    private static String subtitle() {
        return TitleOverlay.shown().get(0).subtitle().getString();
    }

    /** The whole way: the chat line shows the title, the pet turns up in the inventory a moment later, its percent goes at the end of the title. */
    @Test
    void petDropTitleGetsTheRatingOfThePetThatArrives() {
        PetRating.scan(List.of(new PetRating.Item("old", "capybara", 12.5)));
        chat("PET DROP! You pulled a [COMMON] Capybara Pet");
        assertEquals(List.of("+ Capybara Pet"), titles(), "the capybara that was there already is not the drop");
        now[0] += 100;
        PetRating.scan(List.of(new PetRating.Item("old", "capybara", 12.5), new PetRating.Item("new", "capybara", 93.4)));
        assertEquals(List.of("+ Capybara Pet 93.4%"), titles(), "the percent, not the rating's word");
        assertEquals("", subtitle(), "no subtitle");
        List<String> chars = TriggerColorsTest.chars(TitleOverlay.shown().get(0).title());
        assertEquals("+:fd95f6:b", chars.get(0), "the title keeps its own colour");
        assertEquals("9:fc54fc:b", chars.get(chars.size() - 5), "the percent in the rating's colour, bold like the title");
        assertEquals("%:fc54fc:b", chars.get(chars.size() - 1));
    }

    @Test
    void questPetShowsLikeAPetDropWithItsRating() {
        chat(" + $5.4K (x3)");
        chat(" + 2x Mediterranean Shard (x3)");
        assertEquals(List.of(), titles());
        PetRating.scan(List.of());
        chat(" +  Lynx Pet");
        assertEquals(List.of("+ Lynx Pet"), titles());
        assertEquals(1, sounds);
        PetRating.scan(List.of(new PetRating.Item("a", "lynx", 53.25)));
        assertEquals(List.of("+ Lynx Pet 53.3%"), titles());
    }

    @Test
    void questPetFollowsThePetRowsSwitches() {
        TackleboxSettings s = TackleboxSettings.get();
        s.dropTitleOff.add(RareDrops.PET);
        s.dropSoundOff.add(RareDrops.PET);
        chat(" +  Lynx Pet");
        assertEquals(List.of(), titles());
        assertEquals(0, sounds);
        PetRating.scan(List.of(new PetRating.Item("a", "lynx", 53.25))); // no title: nothing to add to, nothing breaks
    }
}
