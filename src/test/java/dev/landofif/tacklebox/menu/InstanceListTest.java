package dev.landofif.tacklebox.menu;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** luna, 2026-10-30: the instance list (it shares the crew title) stays the server's chest; names from her screenshot. */
class InstanceListTest {
    @Test
    void instanceItemsMakeItTheInstanceList() {
        assertTrue(FishMenu.isInstanceList(List.of("instance 1 (14/75)", "instance 2 (15/75)", "instance 4 (14/75)")));
        assertTrue(FishMenu.isInstanceList(List.of("close", "instance 5 (33/75)")));
    }

    @Test
    void crewAndOtherMenusAreNot() {
        assertFalse(FishMenu.isInstanceList(List.of("basses collection", "location leveling data", "level 3")));
        assertFalse(FishMenu.isInstanceList(List.of("instances")));
        assertFalse(FishMenu.isInstanceList(List.of()));
    }
}
