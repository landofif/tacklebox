package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.trigger.Trigger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Titles never wait: the same trigger stacks (newest at its height, at most 3, each with its own timer), another trigger replaces. */
class TitleOverlayTest {
    private final long[] now = {10_000};
    private final Trigger.TitleSettings settings = new Trigger.TitleSettings(); // 0.3 s in, 2.5 s stay, 0.6 s out

    @BeforeEach
    void clock() {
        TitleOverlay.clock = () -> now[0];
        TitleOverlay.clear();
    }

    @AfterEach
    void reset() {
        TitleOverlay.clock = System::currentTimeMillis;
        TitleOverlay.clear();
    }

    private TitleOverlay.Shown show(String trigger, String title) {
        return TitleOverlay.show(trigger, settings, title, "");
    }

    private static List<String> titles() {
        return TitleOverlay.shown().stream().map(s -> s.title().getString()).toList();
    }

    @Test
    void sameTriggerStacksNewestAtItsHeight() {
        show("rare", "one");
        now[0] += 400;
        show("rare", "two");
        now[0] += 400;
        show("rare", "three");
        assertEquals(List.of("one", "two", "three"), titles());
        assertEquals(List.of(2, 1, 0), TitleOverlay.shown().stream().map(TitleOverlay.Shown::place).toList());

        now[0] += 400;
        show("rare", "four"); // at most three: the oldest goes
        assertEquals(List.of("two", "three", "four"), titles());
        assertEquals(List.of(2, 1, 0), TitleOverlay.shown().stream().map(TitleOverlay.Shown::place).toList());
        assertEquals(List.of(10_400L, 10_800L, 11_200L), TitleOverlay.shown().stream().map(TitleOverlay.Shown::startedAt).toList());
    }

    @Test
    void eachStackedTitleKeepsItsOwnTimer() {
        show("rare", "one");
        now[0] += 1000;
        show("rare", "two");
        now[0] = 10_000 + 3400; // 0.3 + 2.5 + 0.6 s: "one" is over, "two" is not
        show("rare", "three");
        assertEquals(List.of("two", "three"), titles());
        assertEquals(1, TitleOverlay.shown().get(0).place());
    }

    @Test
    void olderTitleSlidesUpInsteadOfJumping() {
        TitleOverlay.Shown first = show("rare", "one");
        show("rare", "two");
        assertEquals(0f, TitleOverlay.place(first, now[0]), 1e-6);
        float halfway = TitleOverlay.place(first, now[0] + 75);
        assertTrue(halfway > 0f && halfway < 1f, "on its way: " + halfway);
        assertEquals(1f, TitleOverlay.place(first, now[0] + 150), 1e-6);
    }

    @Test
    void anotherTriggerClearsTheScreenAndShowsAlone() {
        show("rare", "one");
        show("rare", "two");
        show("quest", "quest 3 complete");
        assertEquals(List.of("quest 3 complete"), titles());
        assertEquals("quest", TitleOverlay.shown().get(0).trigger());
        assertEquals(0, TitleOverlay.shown().get(0).place());
    }

    @Test
    void updateChangesTheTextAndKeepsTheTimer() {
        TitleOverlay.Shown shown = TitleOverlay.show("quest", settings, "quest 3 complete", "claim it");
        now[0] += 200;
        TitleOverlay.update(shown, "quests 3 & 5 complete", "claim them");
        assertSame(shown, TitleOverlay.shown().get(0));
        assertEquals(1, TitleOverlay.shown().size());
        assertEquals("quests 3 & 5 complete", shown.title().getString());
        assertEquals("claim them", shown.subtitle().getString());
        assertEquals(10_000L, shown.startedAt());
    }

    @Test
    void entranceIsVisibleFromTheFirstFrame() {
        assertTrue(TitleOverlay.alpha(0, 300, 2500, 600) >= 0.4f);
        assertEquals(1f, TitleOverlay.alpha(300, 300, 2500, 600), 1e-6);
        assertEquals(1f, TitleOverlay.alpha(0, 0, 2500, 600), 1e-6);
        assertEquals(0f, TitleOverlay.alpha(3400, 300, 2500, 600), 1e-6); // total time stays in + stay + out
        assertTrue(TitleOverlay.alpha(3100, 300, 2500, 600) < 0.6f);

        assertTrue(TitleOverlay.popScale(0f) >= 0.7f);
        assertEquals(1f, TitleOverlay.popScale(1f), 1e-6);
        float peak = 0f;
        for (int i = 0; i <= 100; i++) {
            peak = Math.max(peak, TitleOverlay.popScale(i / 100f));
        }
        assertTrue(peak > 1f && peak < 1.1f, "a small overshoot: " + peak);
    }
}
