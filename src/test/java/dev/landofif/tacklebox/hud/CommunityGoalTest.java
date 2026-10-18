package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.trigger.Matcher;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** The bar and chat lines are the ones in luna's latest.log (2026-10-18). */
class CommunityGoalTest {
    @Test
    void parsesAbbreviatedNumbers() {
        assertEquals(21280, CommunityGoal.parse("21.28K"));
        assertEquals(100000, CommunityGoal.parse("100k"));
        assertEquals(1234, CommunityGoal.parse("1,234"));
        assertEquals(-1, CommunityGoal.parse("."));
    }

    @Test
    void barCountingResetAndChatSync() {
        CommunityGoal.restore(-1, -1, 0);
        assertFalse(CommunityGoal.up(1000));
        CommunityGoal.onCatch(1000);
        assertEquals(0, CommunityGoal.mine(), "no bar, no counting");

        assertTrue(CommunityGoal.onBar("Community Goal: 21.28K/100k Catches", 1000));
        assertEquals(21280, CommunityGoal.current());
        assertEquals(100000, CommunityGoal.goal());
        CommunityGoal.onCatch(2000);
        assertEquals(1, CommunityGoal.mine());
        assertFalse(CommunityGoal.up(5000), "bar not seen for 3s");

        CommunityGoal.onChat(Matcher.normalize("Community Catches: 21.21K/100k"));
        CommunityGoal.onChat(Matcher.normalize("Your Catches Contribution: 304"));
        assertEquals(304, CommunityGoal.mine());

        assertTrue(CommunityGoal.onBar("Community Goal: 150/100k Catches", 6000));
        assertEquals(0, CommunityGoal.mine(), "goal went down: new goal");
    }

    @Test
    void ignoresOtherBars() {
        assertFalse(CommunityGoal.onBar("12:30 PM 21° Kenai River", 0));
    }
}
