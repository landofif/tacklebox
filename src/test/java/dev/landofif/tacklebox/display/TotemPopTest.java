package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

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
}
