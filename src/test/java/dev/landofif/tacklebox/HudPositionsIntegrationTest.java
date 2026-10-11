package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.chat.ChatBox;
import dev.landofif.tacklebox.chat.ChatBoxes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Settings saved before huds were stuck to the window sides (the numbers are l's settings.json of 2026-10-11, saved in
 * fullscreen at gui scale 2, so 960x540) go through load, a first draw, a save and a load again, in windows of other sizes.
 */
class HudPositionsIntegrationTest {
    private static final String OLD = """
            {"levelBars": true, "levelBarsX": 745, "levelBarsY": 1,
             "boostsHud": true, "boostsX": 787, "boostsY": 306,
             "serverHud": true, "serverHudX": 789, "serverHudY": 103,
             "nowPlaying": true, "nowPlayingX": 0, "nowPlayingY": 4,
             "chatX": 0, "chatY": -177,
             "chatBoxes": [
               {"name": "dms", "enabled": true, "x": 61.458333333333336, "y": 74.07407407407408, "width": 180, "lines": 7},
               {"name": "crew", "enabled": true, "x": 26.25, "y": 20.55888223552894, "width": 181, "lines": 5}]}
            """;

    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsStayAgainstTheirEdgesInAnyWindow() {
        TackleboxSettings s = load(OLD);
        s.levelBarsPos.measure(960, 540, 215, 40);
        s.boostsPos.measure(960, 540, 173, 60);
        s.serverHudPos.measure(960, 540, 171, 90);
        s.nowPlayingPos.measure(960, 540, 160, 50);

        assertEquals(745, s.levelBarsPos.left(960)); // nothing moved in the window it was saved in
        assertEquals(787, s.boostsPos.left(960));
        assertEquals(789, s.serverHudPos.left(960));
        assertEquals(0, s.nowPlayingPos.left(960));

        assertEquals(480 - 215, s.levelBarsPos.left(480)); // split screen: still against the right edge
        assertEquals(480 - 173, s.boostsPos.left(480));
        assertEquals(480 - 171, s.serverHudPos.left(480));
        assertEquals(0, s.nowPlayingPos.left(480)); // the one on the left stays on the left
        assertEquals(1, s.levelBarsPos.top(270)); // and a hud at the top stays at the top
        assertEquals(306, s.boostsPos.top(540));
    }

    @Test
    void chatBoxesFromOldSettingsStayAgainstTheirEdgesInAnyWindow() {
        TackleboxSettings s = load(OLD);
        ChatBox dms = s.chatBoxes.get(0);
        int[] full = ChatBoxes.rect(dms, 960, 540, false);
        assertEquals(590, full[0]); // 61.458 percent of 960, as before
        int[] half = ChatBoxes.rect(dms, 480, 540, false);
        assertEquals(960 - (590 + 180), 480 - (half[0] + half[2])); // the same distance from the right edge
        assertEquals(540 - (full[1] + full[3]), 540 - (half[1] + half[3]));
    }

    @Test
    void pinnedPositionsSurviveSavingAndLoading() {
        TackleboxSettings s = load(OLD);
        s.levelBarsPos.measure(960, 540, 215, 40);
        ChatBoxes.rect(s.chatBoxes.get(0), 960, 540, false);
        String saved = new Gson().toJson(s);

        TackleboxSettings again = load(saved);
        assertTrue(again.levelBarsPos.pinned);
        assertTrue(again.levelBarsPos.right);
        again.levelBarsPos.measure(480, 540, 215, 40); // the next start is in a half width window
        assertEquals(480 - 215, again.levelBarsPos.left(480));
        assertEquals(745, again.levelBarsPos.left(960)); // and back
        int[] box = ChatBoxes.rect(again.chatBoxes.get(0), 960, 540, false);
        assertEquals(590, box[0]);
    }

    @Test
    void aFreshInstallStartsInTheDefaultCorners() {
        TackleboxSettings s = load("{}");
        s.questsPos.measure(960, 540, 120, 50);
        s.levelBarsPos.measure(960, 540, 215, 40);
        assertEquals(4, s.questsPos.left(480));
        assertEquals(70, s.questsPos.top(270));
        assertEquals(4, s.levelBarsPos.left(480));
        assertEquals(40, s.levelBarsPos.top(270));
    }
}
