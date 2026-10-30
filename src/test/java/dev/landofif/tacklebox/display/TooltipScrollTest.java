package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Where a tall tooltip sits and how far it scrolls. */
class TooltipScrollTest {
    @AfterEach
    void off() {
        TackleboxSettings.get().tooltipScroll = false;
    }

    @Test
    void tallMeansItCannotFitWithVanillasGaps() {
        assertFalse(TooltipScroll.tall(193, 200));
        assertTrue(TooltipScroll.tall(194, 200));
    }

    @Test
    void scrollStopsAtTheTopAndTheBottom() {
        assertEquals(0, TooltipScroll.clamp(-12, 300, 200));
        assertEquals(36, TooltipScroll.clamp(36, 300, 200));
        assertEquals(107, TooltipScroll.clamp(500, 300, 200)); // bottom 3 pixels above the screen edge
    }

    @Test
    void tallTooltipStartsAtTheTop() {
        TackleboxSettings.get().tooltipScroll = true;
        assertEquals(4, TooltipScroll.top(-103, 120, 300, 200)); // vanilla shows the bottom and cuts the name off
    }

    @Test
    void shortTooltipOrSettingOffIsLeftWhereVanillaPutIt() {
        TackleboxSettings.get().tooltipScroll = true;
        assertEquals(50, TooltipScroll.top(50, 120, 100, 200));
        TackleboxSettings.get().tooltipScroll = false;
        assertEquals(-103, TooltipScroll.top(-103, 120, 300, 200));
    }
}
