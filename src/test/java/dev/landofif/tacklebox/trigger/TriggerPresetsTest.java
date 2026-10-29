package dev.landofif.tacklebox.trigger;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The pet drop and rare catch presets against lines from luna's logs: own drops fire, other players' broadcasts don't. */
class TriggerPresetsTest {
    private static Map<String, String> match(Trigger t, String line) {
        Matcher.Result r = Matcher.of(t).match(line);
        return r == null ? null : r.values();
    }

    @Test
    void onePetDropAndOneRareCatchPreset() {
        List<String> names = TriggerPresets.all().stream().map(TriggerPresets.Preset::name).toList();
        assertTrue(names.contains("pet drop") && names.contains("rare catch"));
        for (String gone : List.of("pet totem", "climate shard", "shard totem")) {
            assertFalse(names.contains(gone), gone);
        }
    }

    @Test
    void rareCatchMatchesOwnLines() {
        Trigger t = TriggerPresets.rareCatch();
        assertEquals("{item}", t.totem.item);
        assertTrue(t.totem.enabled && t.title.enabled && t.sound.enabled);
        assertEquals(Trigger.Animation.POP, t.title.animation);
        String[][] lines = {
                {"RARE CATCH! You pulled a 1x Subtropical Shard", "1", "Subtropical Shard"},
                {"RARE CATCH! You pulled a 1x Semi-Arid Shard", "1", "Semi-Arid Shard"},
                {"RARE CATCH! You pulled 1x Lightning in a Bottle", "1", "Lightning in a Bottle"},
                {"RARE CATCH! You pulled 1x Prospecting Amulet", "1", "Prospecting Amulet"},
                {"RARE CATCH! You pulled 1x Infusion Capsule", "1", "Infusion Capsule"},
                {"RARE CATCH! You pulled a 2x Arid Shard", "2", "Arid Shard"},
        };
        for (String[] l : lines) {
            Map<String, String> v = match(t, l[0]);
            assertNotNull(v, l[0]);
            assertEquals(l[1], v.get("n"), l[0]);
            assertEquals(l[2], v.get("item"), l[0]);
            assertEquals(l[2], TriggerEngine.fill(t.totem.item, v));
            assertEquals(l[2].toLowerCase(), TriggerEngine.fill(t.title.subtitle, v));
        }
    }

    @Test
    void rareCatchIgnoresOtherPlayers() {
        Trigger t = TriggerPresets.rareCatch();
        for (String line : List.of(
                "! [366] meowfih » RARE CATCH! You pulled 1x Prospecting Amulet",
                "[245]   Lumivix7 » RARE CATCH! You pulled 1x Lightning in a Bottle",
                "! [12]  someone » RARE CATCH! You pulled a 1x Subtropical Shard",
                "✉ To prossyclart » RARE CATCH! You pulled 1x Infusion Capsule")) {
            assertNull(match(t, line), line);
        }
    }

    @Test
    void petDropMatchesOwnLinesWithAnyRarityForm() {
        Trigger t = TriggerPresets.petDrop();
        assertEquals("pet:{pet}", t.totem.item);
        assertTrue(t.totem.enabled && t.title.enabled);
        assertEquals(Trigger.Animation.SHAKE, t.title.animation);
        String[][] lines = {
                {"PET DROP! You pulled a [COMMON] Capybara Pet", "Capybara"},
                {"PET DROP! You pulled a Legendary Duck Pet", "Duck"},
                {"PET DROP! You pulled a [RARE] Sea Turtle Pet", "Sea Turtle"},
                {"PET DROP! You pulled a  Hippo Pet", "Hippo"}, // the server's rarity glyph, dropped by normalize
                {"PET DROP! You pulled a Mythical Marsh Rabbit Pet", "Marsh Rabbit"},
        };
        for (String[] l : lines) {
            Map<String, String> v = match(t, l[0]);
            assertNotNull(v, l[0]);
            assertEquals(l[1], v.get("pet"), l[0]);
            assertEquals("pet:" + l[1], TriggerEngine.fill(t.totem.item, v));
            assertEquals(l[1].toLowerCase(), TriggerEngine.fill(t.title.subtitle, v));
        }
    }

    @Test
    void petDropIgnoresOtherPlayers() {
        Trigger t = TriggerPresets.petDrop();
        for (String line : List.of(
                "! [84]   SteamyMandu » PET DROP! You pulled a Epic Hippo Pet",
                "! [161]  pooma2 » PET DROP! You pulled a LEGENDARY Duck Pet",
                "PET DROP! Cyrinacle caught a  Hippo Pet",
                "✉ To prossyclart » PET DROP! You pulled a  Wolf Pet")) {
            assertNull(match(t, line), line);
        }
    }

    @Test
    void petMaxLevelMatchesEveryJumpTo100() {
        Trigger t = TriggerPresets.petMaxLevel();
        assertEquals("pet:{pet}", t.totem.item);
        assertTrue(t.totem.enabled && t.title.enabled && t.sound.enabled);
        String[][] lines = {
                {"PETS » Your Bullfrog Pet is now level 99 → 100 (+1)", "Bullfrog"},
                {"PETS » Your Bullfrog Pet is now level 98 → 100 (+2)", "Bullfrog"},
                {"PETS » Your Marsh Rabbit Pet is now level 97 → 100 (+3)", "Marsh Rabbit"},
                {"PETS » Your Eagle Pet is now level 99 → 100 (+1)", "Eagle"},
        };
        for (String[] l : lines) {
            Map<String, String> v = match(t, l[0]);
            assertNotNull(v, l[0]);
            assertEquals(l[1], v.get("pet"), l[0]);
            assertEquals("pet:" + l[1], TriggerEngine.fill(t.totem.item, v));
        }
    }

    @Test
    void petMaxLevelIgnoresLowerLevelsAndOtherPlayers() {
        Trigger t = TriggerPresets.petMaxLevel();
        for (String line : List.of(
                "PETS » Your Duck Pet is now level 98 → 99 (+1)",
                "PETS » Your Shark Pet is now level 9 → 10 (+1)",
                "PETS » Your Shark Pet is now level 9 → 1000 (+1)",
                "! [144]   JAH90909 » PETS » Your Duck Pet is now level 99 → 100 (+1)",
                "! [21] Tokoyami_Yuuki » is the merged pet that comes out max level or lvl 1")) {
            assertNull(match(t, line), line);
        }
    }

    @Test
    void rodNotCastMatchesTheModsOwnLine() {
        Trigger t = TriggerPresets.rodIdle();
        Map<String, String> v = match(t, "TACKLEBOX » Rod not cast for 60s");
        assertNotNull(v);
        assertEquals("60", v.get("seconds"));
        assertEquals("not cast for 60s", TriggerEngine.fill(t.title.subtitle, v));
        assertNull(match(t, "! [12] someone » rod not cast"));
        List<String> names = TriggerPresets.all().stream().map(TriggerPresets.Preset::name).toList();
        assertTrue(names.contains("pet max level") && names.contains("rod not cast"));
    }
}
