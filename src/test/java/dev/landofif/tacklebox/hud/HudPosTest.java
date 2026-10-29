package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Window sizes are gui pixels at gui scale 2: a 1920x1080 fullscreen is 960x540, a half width split window 480x540. */
class HudPosTest {
    @Test
    void stuckToTheRightEdgeStaysThereWhenTheWindowNarrows() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 575, 1, 215, 40); // 170 from the right edge
        assertEquals(575, p.left(960));
        assertEquals(480 - 170 - 215, p.left(480));
    }

    @Test
    void flushRightStaysFlushRight() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 745, 1, 215, 40);
        assertEquals(0, p.x);
        assertEquals(480 - 215, p.left(480));
        assertEquals(745, p.left(960));
    }

    @Test
    void rightEdgeStaysPutWhenTheHudGetsWider() { // the level hud's width follows the location name
        HudPos p = new HudPos();
        p.moveTo(960, 540, 745, 1, 215, 40);
        p.measure(960, 540, 260, 40);
        assertEquals(960 - 260, p.left(960));
    }

    @Test
    void topLeftHudStaysInTheTopLeft() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 4, 70, 120, 50);
        assertFalse(p.right);
        assertFalse(p.bottom);
        assertEquals(4, p.left(480));
        assertEquals(70, p.top(270));
    }

    @Test
    void bottomHudKeepsItsDistanceFromTheBottomEdge() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 10, 400, 100, 100); // 40 from the bottom
        assertTrue(p.bottom);
        assertEquals(400, p.top(540));
        assertEquals(270 - 40 - 100, p.top(270));
    }

    @Test
    void neverEndsUpOutsideTheWindow() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 575, 1, 215, 40); // 170 from the right
        assertEquals(0, p.left(300)); // 300 - 170 - 215 is negative
        HudPos wide = new HudPos();
        wide.moveTo(960, 540, 700, 10, 215, 40);
        assertEquals(0, wide.left(100)); // wider than the window
        assertEquals(0, wide.top(20)); // higher than the window
    }

    @Test
    void draggingPastTheEdgeStopsAtTheEdge() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 5000, -30, 215, 40);
        assertEquals(960 - 215, p.left(960));
        assertEquals(0, p.top(540));
        assertTrue(p.x >= 0 && p.y >= 0);
    }

    @Test
    void aSpotSavedTheOldWayIsPinnedToTheNearestSidesOnTheFirstDraw() {
        HudPos p = new HudPos(745, 1); // a plain top left corner
        assertFalse(p.pinned);
        assertEquals(745, p.left(960));
        p.measure(960, 540, 215, 40);
        assertTrue(p.pinned);
        assertTrue(p.right);
        assertEquals(745, p.left(960));
        assertEquals(480 - 215, p.left(480));
    }

    @Test
    void aSpotSavedOutsideTheCurrentWindowIsBroughtBackToItsEdge() {
        HudPos p = new HudPos(789, 103); // saved in a wider window than this one
        p.measure(480, 540, 171, 60);
        assertTrue(p.right);
        assertEquals(480 - 171, p.left(480));
    }

    @Test
    void measuringAPinnedSpotOnlyRemembersTheSize() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 575, 1, 215, 40);
        int x = p.x;
        p.measure(480, 270, 215, 40); // a different window does not re-pin it
        assertEquals(x, p.x);
        assertTrue(p.right);
    }

    @Test
    void drawGivesTheCornerAndRemembersTheSize() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 745, 1, 215, 40);
        int[] r = p.draw(480, 540, (x, y) -> new int[]{x, y, 230, 40});
        assertEquals(480 - 215, r[0]); // drawn where the old size put it
        assertEquals(480 - 230, p.left(480)); // the next frame uses the new size
    }

    @Test
    void aHudThatShowsNothingKeepsItsSize() {
        HudPos p = new HudPos();
        p.moveTo(960, 540, 745, 1, 215, 40);
        assertNull(p.draw(960, 540, (x, y) -> null));
        assertEquals(745, p.left(960));
    }
}
