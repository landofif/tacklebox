package dev.landofif.tacklebox.hud;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AbbrevXpTest {
    @Test
    void decimalsStayByDefault() {
        assertEquals("367.27K", XpEstimate.abbrevXp(367_270));
        assertEquals("1.50M", XpEstimate.abbrevXp(1_500_000));
        assertEquals("999", XpEstimate.abbrevXp(999));
    }

    @Test
    void withoutDecimalsItRounds() {
        assertEquals("367K", XpEstimate.abbrevXp(367_270, false));
        assertEquals("686K", XpEstimate.abbrevXp(686_000, false));
        assertEquals("1.5M", XpEstimate.abbrevXp(1_500_000, false));
        assertEquals("3.7M", XpEstimate.abbrevXp(3_730_000, false));
        assertEquals("999", XpEstimate.abbrevXp(999, false));
    }
}
