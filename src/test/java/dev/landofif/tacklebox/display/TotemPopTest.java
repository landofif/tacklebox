package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** A totem pop of an item name: the inventory stack, else its remembered look, else wait for it, else a shard head or the totem. */
class TotemPopTest {
    private static TotemPop.Source source(String spec, boolean item, boolean inInventory, boolean remembered) {
        return TotemPop.source(spec, id -> item, key -> inInventory, key -> remembered);
    }

    @Test
    void nameKeysIgnoreCaseSpacesGlyphsAndPunctuation() {
        assertEquals("lightninginabottle", ItemLooks.key("Lightning in a Bottle"));
        assertEquals("lightninginabottle", ItemLooks.key("  LIGHTNING   in a  bottle "));
        assertEquals("semiaridshard", ItemLooks.key(" Semi-Arid Shard"));
        assertEquals("bigfoottooth", ItemLooks.key("Bigfoot Tooth"));
        assertEquals("", ItemLooks.key(" "));
    }

    @Test
    void idsAndHeadsComeFirst() {
        List<String> asked = new ArrayList<>();
        assertEquals(TotemPop.Source.ID, TotemPop.source(" Minecraft:Stick ", id -> asked.add(id), k -> true, k -> true));
        assertEquals(List.of("minecraft:stick"), asked);
        assertEquals(TotemPop.Source.HEAD, source("pet:Capybara", false, true, true));
        assertEquals(TotemPop.Source.HEAD, source("shard:subtropical", false, true, true));
        assertEquals(TotemPop.Source.ID, source("", false, false, false)); // nothing typed: the totem
    }

    @Test
    void itemNameLooksInTheInventoryThenRememberedThenWaits() {
        assertEquals(TotemPop.Source.INVENTORY, source("Lightning in a Bottle", false, true, true));
        assertEquals(TotemPop.Source.REMEMBERED, source("Lightning in a Bottle", false, false, true));
        assertEquals(TotemPop.Source.WAIT, source("Lightning in a Bottle", false, false, false));
    }

    @Test
    void inventoryAndMemoryAreAskedByNameKey() {
        List<String> keys = new ArrayList<>();
        TotemPop.source("Infusion Capsule", id -> false, k -> keys.add("inventory " + k) && false, k -> keys.add("remembered " + k) && false);
        assertEquals(List.of("inventory infusioncapsule", "remembered infusioncapsule"), keys);
    }

    @Test
    void fallbackIsTheShardHeadOrTheTotem() {
        assertEquals("shard:subtropical", TotemPop.fallback("Subtropical Shard"));
        assertEquals("shard:semi-arid", TotemPop.fallback("Semi-Arid Shard"));
        assertEquals("minecraft:totem_of_undying", TotemPop.fallback("Lightning in a Bottle"));
        assertEquals("minecraft:totem_of_undying", TotemPop.fallback("Prospecting Amulet"));
    }

    @Test
    void showWithoutAGameDoesNothingButRemembersTheSpec() {
        TotemPop.show("  Subtropical Shard ", 1);
        assertEquals("Subtropical Shard", TotemPop.last());
    }

    private static float vanilla(float f) {
        return 900f * (float) Math.abs(Math.sin(TotemPop.curve(f) * Math.PI));
    }

    @Test
    void curveRisesHoldsNearTheMiddleThenEnds() {
        assertEquals(0f, TotemPop.curve(0f), 1e-6f);
        assertEquals(0.469f, TotemPop.curve(12 / 40f), 1e-3f);
        assertEquals(0.5f, TotemPop.curve(21 / 40f), 2e-3f);
        assertEquals(0.533f, TotemPop.curve(30 / 40f), 1e-3f);
        assertEquals(1f, TotemPop.curve(1f), 1e-4f);
    }

    @Test
    void comingInAndHoldingIsVanilla() {
        for (int tick = 0; tick <= 21; tick++) {
            float f = tick / 40f;
            assertEquals(vanilla(f), TotemPop.spin(f), 1e-3f, "tick " + tick);
        }
        assertEquals(895.8f, TotemPop.spin(12 / 40f), 0.1f);
    }

    @Test
    void leavingTurns300MoreTheSameWay() {
        assertEquals(1200f, TotemPop.spin(1f), 0.5f);
        assertEquals(900f + 300f * (1f - 636.7f / 900f), TotemPop.spin(37 / 40f), 0.5f);
        float before = TotemPop.spin(0.5f);
        for (int i = 501; i <= 1000; i++) {
            float now = TotemPop.spin(i / 1000f);
            assertTrue(now >= before - 1e-3f, "turns back at f " + i / 1000f);
            assertTrue(now - before < 5f, "jumps at f " + i / 1000f);
            before = now;
        }
    }

    @Test
    void sizeIsOnlyForTheModsOwnPop() {
        Object ours = new Object();
        TotemPop.shown = ours;
        TotemPop.scale = 0.5f;
        try {
            assertEquals(0.5f, TotemPop.size(ours));
            assertEquals(1f, TotemPop.size(new Object()));
            assertEquals(1f, TotemPop.size(null));
        } finally {
            TotemPop.shown = null;
            TotemPop.scale = 1f;
        }
    }
}
