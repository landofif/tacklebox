package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ParticleSwitchesTest {
    @Test
    void listHasSeenOffAndRainOnceEachByName() {
        assertEquals(List.of("minecraft:flame", "minecraft:rain", "minecraft:totem_of_undying"),
                ParticleSwitches.list(List.of("minecraft:totem_of_undying", "minecraft:rain"), List.of("minecraft:flame", "minecraft:rain")));
        assertEquals(List.of("minecraft:rain"), ParticleSwitches.list(List.of(), List.of()));
    }

    @Test
    void toggleSwitchesOffAndBackOn() {
        List<String> off = new ArrayList<>();
        ParticleSwitches.toggle(off, "minecraft:rain");
        assertEquals(List.of("minecraft:rain"), off);
        ParticleSwitches.toggle(off, "minecraft:rain");
        assertEquals(List.of(), off);
    }
}
