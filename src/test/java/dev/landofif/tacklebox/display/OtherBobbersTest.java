package dev.landofif.tacklebox.display;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OtherBobbersTest {
    private final Object me = new Object();
    private final Object other = new Object();

    @Test
    void hidesOthersOnlyWhenOn() {
        assertTrue(OtherBobbers.hides(true, other, me));
        assertFalse(OtherBobbers.hides(false, other, me));
    }

    @Test
    void keepsOwnBobberAndWorksWithoutPlayer() {
        assertFalse(OtherBobbers.hides(true, me, me));
        assertFalse(OtherBobbers.hides(true, other, null));
    }
}
