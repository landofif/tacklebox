package dev.landofif.tacklebox.screen;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SettingsNavTest {
    @Test
    void aNewPageShowsItsFirstGroup() {
        SettingsNav nav = new SettingsNav(8);
        assertEquals(0, nav.group());
        assertTrue(nav.page(3));
        assertEquals(3, nav.page);
        assertEquals(0, nav.group());
    }

    @Test
    void everyPageRemembersItsGroup() {
        SettingsNav nav = new SettingsNav(8);
        nav.group(4);
        nav.page(2);
        nav.group(1);
        nav.page(0);
        assertEquals(4, nav.group());
        nav.page(2);
        assertEquals(1, nav.group());
    }

    @Test
    void pagesOutsideTheListAreRefused() {
        SettingsNav nav = new SettingsNav(8);
        assertFalse(nav.page(-1));
        assertFalse(nav.page(8));
        assertFalse(nav.page(0), "the page already shown");
        assertEquals(0, nav.page);
    }

    @Test
    void columnsAtGuiScaleTwo() {
        int[] c = SettingsNav.columns(960, 6);
        assertArrayEquals(new int[]{100, 120, 190, 438}, c);
    }

    @Test
    void columnsFillTheScreenAndStayOnIt() {
        for (int width : new int[]{320, 480, 519, 520, 640, 960, 1920}) {
            int[] c = SettingsNav.columns(width, 6);
            assertTrue(c[0] >= 10, "left edge at " + width);
            assertTrue(c[0] + c[1] + c[2] + c[3] + 12 <= width - 10, "right edge at " + width);
            assertTrue(c[3] >= 120, "room for a setting at " + width + ": " + c[3]);
        }
    }

    @Test
    void narrowScreensShowPagesAsIcons() {
        assertEquals(28, SettingsNav.columns(480, 6)[1]);
        assertEquals(120, SettingsNav.columns(640, 6)[1]);
    }
}
