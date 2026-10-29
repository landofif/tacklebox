package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static dev.landofif.tacklebox.display.ScreenshotCard.*;
import static org.junit.jupiter.api.Assertions.*;

class ScreenshotCardTest {
    private static final int SCREEN_W = 480;
    private static final long T = 1_000_000;

    private static ScreenshotCard card() {
        return new ScreenshotCard(Path.of("2026-10-28_12.00.00.png"), 1920, 1080, true, T);
    }

    @Test
    void slidesInStaysAndSlidesOut() {
        ScreenshotCard c = card();
        assertEquals(0, c.in(T));
        double half = c.in(T + SLIDE_MS / 2);
        assertTrue(half > 0.5 && half < 1, "eases out, so more than half way at half time: " + half);
        assertEquals(1, c.in(T + SLIDE_MS));
        assertEquals(1, c.in(T + SLIDE_MS + SHOW_MS));
        assertTrue(c.in(T + SLIDE_MS + SHOW_MS + SLIDE_MS / 2) < 1);
        assertEquals(0, c.in(T + SLIDE_MS * 2 + SHOW_MS));
        assertFalse(c.gone(T + SLIDE_MS * 2 + SHOW_MS - 1));
        assertTrue(c.gone(T + SLIDE_MS * 2 + SHOW_MS));
    }

    @Test
    void sitsInTheTopRightCornerAndStartsOutsideTheWindow() {
        ScreenshotCard c = card();
        assertEquals(SCREEN_W - MARGIN - PICTURE_W, c.x(SCREEN_W, T + SLIDE_MS));
        assertEquals(SCREEN_W, c.x(SCREEN_W, T));
    }

    @Test
    void pictureKeepsTheScreenshotsShape() {
        assertEquals(68, card().pictureH()); // 120 * 1080 / 1920 = 67.5
        assertEquals(PICTURE_W, new ScreenshotCard(Path.of("a.png"), 500, 500, true, T).pictureH());
    }

    @Test
    void openButtonUnderTheRightEndOfThePicture() {
        ScreenshotCard c = card();
        long now = T + 1000;
        int[] b = c.button(SCREEN_W, now);
        assertArrayEquals(new int[]{SCREEN_W - MARGIN - BUTTON_W, MARGIN + 68 + 2, BUTTON_W, BUTTON_H}, b);
        assertTrue(c.onButton(b[0], b[1], SCREEN_W, now));
        assertTrue(c.onButton(b[0] + b[2] - 1, b[1] + b[3] - 1, SCREEN_W, now));
        assertFalse(c.onButton(b[0] - 1, b[1], SCREEN_W, now));
        assertFalse(c.onButton(b[0], b[1] - 1, SCREEN_W, now), "the picture above it is not the button");
        assertFalse(c.onButton(b[0], b[1], SCREEN_W, T), "nothing to click before it slid in");
    }

    @Test
    void holdingKeepsItInUntilTheMouseLeaves() {
        ScreenshotCard c = card();
        long late = T + SLIDE_MS + SHOW_MS - 100; // about to slide out
        ScreenshotCard held = c.held(late);
        assertEquals(1, held.in(late + LINGER_MS));
        assertTrue(held.in(late + LINGER_MS + SLIDE_MS / 2) < 1);

        long early = T + 1000; // plenty of time left: nothing changes
        assertSame(c, c.held(early));

        long sliding = T + SLIDE_MS + SHOW_MS + SLIDE_MS / 2; // already on its way out: comes back in
        assertEquals(1, c.held(sliding).in(sliding));
    }

    @Test
    void cardCoversThePictureAndTheRowUnderIt() {
        ScreenshotCard c = card();
        long now = T + 1000;
        int x = c.x(SCREEN_W, now);
        assertTrue(c.onCard(x, MARGIN, SCREEN_W, now));
        assertTrue(c.onCard(x + PICTURE_W - 1, MARGIN + 68 + 2 + BUTTON_H - 1, SCREEN_W, now));
        assertFalse(c.onCard(x - 1, MARGIN, SCREEN_W, now));
        assertFalse(c.onCard(x, MARGIN + 68 + 2 + BUTTON_H, SCREEN_W, now));
    }
}
