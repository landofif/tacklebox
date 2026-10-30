package dev.landofif.tacklebox;

import dev.landofif.tacklebox.hud.EventPage;
import dev.landofif.tacklebox.hud.EventPanelFixture;
import dev.landofif.tacklebox.menu.MenuRows;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * luna, 2026-10-30: /event gets tacklebox's panel like the other menus. Its title (art only, {@code U+EEE4 U+D568}) is
 * named "event", and the items of her Halloween 2026 dump become threshold and mission rows.
 */
class EventPanelIntegrationTest {
    @Test
    void eventMenuIsNamedAndReadIntoRows() {
        assertEquals("event", Glyphs.menuName(Text.literal("함")));
        EventPanelFixture.readDump();

        var state = EventPage.state();
        assertEquals("halloween 2026", state.title);
        assertEquals("#22", state.placement);
        List<MenuRows.Row> steps = state.thresholds.stream().map(EventPage::threshold).toList();
        assertEquals(List.of("Threshold 1", "Threshold 2", "Threshold 3"), steps.stream().map(MenuRows.Row::name).toList());
        assertEquals("220/220", steps.get(0).amount());
        assertEquals("1.5k/1.5k", steps.get(2).amount());
        assertEquals("$80k · 50x cut carp", steps.get(2).sub());

        List<MenuRows.Row> missions = EventPage.missions().stream().map(EventPage::mission).toList();
        assertEquals(3, missions.size());
        assertEquals(new MenuRows.Row("amazon river", "400 pts", -1, 40 / 1500f, "40/1500"), missions.get(2));
        EventPanelFixture.clear();
    }
}
