package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntityTallyTest {
    @Test
    void mostCommonFirstTiesByName() {
        List<String> ids = List.of("b", "a", "item_display", "item_display", "item_display", "a");
        assertEquals(List.of("item_display 3", "a 2", "b 1"), EntityTally.lines(ids));
    }

    @Test
    void emptyGivesNothing() {
        assertEquals(List.of(), EntityTally.lines(List.of()));
    }
}
