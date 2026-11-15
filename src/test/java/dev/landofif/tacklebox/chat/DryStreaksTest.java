package dev.landofif.tacklebox.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class DryStreaksTest {
    private final Map<String, Integer> dry = new HashMap<>();
    private final Map<String, String> text = new HashMap<>();
    private final Set<String> on = new HashSet<>(List.of("pet", "variant", "gigantic", "shard", "capsule", "bottle"));

    @BeforeEach
    void setUp() {
        DryStreaks.reset();
        for (DryStreaks.Kind k : DryStreaks.KINDS) {
            text.put(k.key(), k.key() + " %item% %streak%");
        }
    }

    private List<String> fish(long now) {
        return DryStreaks.onCatch(dry, on, text, "bass", "normal", "adult", false, false, now);
    }

    @Test
    void parsesTheServersOwnDropLines() {
        assertEquals(new DryStreaks.Drop("pet", "Eagle Pet", 1, "common"), DryStreaks.parse("PET DROP! You pulled a [COMMON] Eagle Pet"));
        assertEquals(new DryStreaks.Drop("pet", "Marsh Rabbit Pet", 1, ""), DryStreaks.parse("PET DROP! You pulled a Marsh Rabbit Pet"));
        assertEquals(new DryStreaks.Drop("shard", "Subtropical Shard", 3, ""), DryStreaks.parse("RARE CATCH! You pulled a 3x Subtropical Shard"));
        assertEquals(new DryStreaks.Drop("shard", "Semi-Arid Shard", 1, ""), DryStreaks.parse("RARE CATCH! You pulled a 1x Semi-Arid Shard (x2)"));
        assertEquals(new DryStreaks.Drop("capsule", "Infusion Capsule", 1, ""), DryStreaks.parse("RARE CATCH! You pulled 1x Infusion Capsule"));
        assertEquals(new DryStreaks.Drop("bottle", "Lightning in a Bottle", 1, ""), DryStreaks.parse("RARE CATCH! You pulled a Lightning in a Bottle!"));
    }

    @Test
    void otherLinesAreNoDrop() {
        assertNull(DryStreaks.parse("RARE CATCH! You pulled 1x Prospecting Amulet")); // no streak of its own
        assertNull(DryStreaks.parse("PET DROP! Wolfpack5200 caught a Duck Pet"));
        assertNull(DryStreaks.parse("[382] Realstranden > PET DROP! You pulled a Common Bear Pet"));
        assertNull(DryStreaks.parse("! [276] meowfih > RARE CATCH! You pulled 1x Prospecting Amulet"));
    }

    @Test
    void aDropAfterItsCatchCountsThatCatchOnce() {
        for (int i = 0; i < 4; i++) {
            fish(10_000L * (i + 1));
        }
        fish(100_000);
        assertEquals("pet Eagle Pet 5", DryStreaks.onDrop(dry, on, text, new DryStreaks.Drop("pet", "Eagle Pet", 1, ""), 100_050));
        assertEquals(0, dry.get("pet"));
        assertEquals(5, dry.get("shard"));
    }

    @Test
    void aDropBeforeItsCatchCountsThatCatchOnce() {
        for (int i = 0; i < 4; i++) {
            fish(10_000L * (i + 1));
        }
        assertEquals("pet Eagle Pet 5", DryStreaks.onDrop(dry, on, text, new DryStreaks.Drop("pet", "Eagle Pet", 1, ""), 100_000));
        fish(100_050); // the catch that brought it
        assertEquals(0, dry.get("pet"));
        assertEquals(5, dry.get("shard"));
        fish(120_000);
        assertEquals(1, dry.get("pet"));
    }

    @Test
    void aDropWhoseCatchNeverShowsDoesNotEatALaterCatch() {
        assertEquals("shard Arid Shard 1", DryStreaks.onDrop(dry, on, text, new DryStreaks.Drop("shard", "Arid Shard", 1, ""), 100_000));
        fish(100_000 + DryStreaks.NEAR + 1);
        assertEquals(1, dry.get("shard"));
    }

    @Test
    void capsulesCountOnlyInABloodMoonAndBottlesOnlyInAStorm() {
        fish(10_000);
        DryStreaks.onCatch(dry, on, text, "bass", "normal", "adult", true, false, 20_000);
        DryStreaks.onCatch(dry, on, text, "bass", "normal", "adult", true, false, 30_000);
        DryStreaks.onCatch(dry, on, text, "bass", "normal", "adult", false, true, 40_000);
        assertEquals(2, dry.get("capsule"));
        assertEquals(1, dry.get("bottle"));
        assertEquals(4, dry.get("pet"));
    }

    @Test
    void aVariantOrGiganticFishSaysItsOwnStreak() {
        fish(10_000);
        fish(20_000);
        assertEquals(List.of("variant bass 3"), DryStreaks.onCatch(dry, on, text, "bass", "albino", "adult", false, false, 30_000));
        assertEquals(0, dry.get("variant:albino"));
        assertEquals(3, dry.get("variant:trophy"));
        assertEquals(List.of("variant cod 4", "gigantic cod 4"), DryStreaks.onCatch(dry, on, text, "cod", "Trophy", "gigantic", false, false, 40_000));
        assertEquals(0, dry.get("gigantic"));
        assertEquals(1, dry.get("variant:albino"));
        assertTrue(DryStreaks.onCatch(dry, on, text, "cod", "spooky", "large", false, false, 50_000).isEmpty()); // event variants have no streak
    }

    @Test
    void aSwitchedOffKindStillCountsButSaysNothing() {
        on.clear();
        fish(10_000);
        assertNull(DryStreaks.onDrop(dry, on, text, new DryStreaks.Drop("pet", "Fox Pet", 1, ""), 10_050));
        assertEquals(0, dry.get("pet"));
        assertTrue(DryStreaks.onCatch(dry, on, text, "bass", "albino", "gigantic", false, false, 20_000).isEmpty());
        assertEquals(0, dry.get("gigantic"));
    }

    @Test
    void fillsEveryPlaceholderAndGroupsThousands() {
        assertEquals("3x Arid Shard 1,234 albino rare",
                DryStreaks.fill("%count%x %item% %streak% %variant% %rarity%", "Arid Shard", 1234, 3, "albino", "rare"));
    }

    @Test
    void aBlankOrMissingTextIsTheDefault() {
        text.put("pet", "  ");
        text.remove("shard");
        assertEquals(DryStreaks.KINDS.get(0).template(), DryStreaks.template(text, "pet"));
        assertTrue(DryStreaks.template(text, "shard").contains("%streak%"));
        assertEquals("capsule %item% %streak%", DryStreaks.template(text, "capsule"));
    }
}
