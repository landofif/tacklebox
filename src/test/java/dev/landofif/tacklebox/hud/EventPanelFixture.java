package dev.landofif.tacklebox.hud;

import java.util.List;

/** Feeds {@link EventHud} the /event items (plain, as {@code Glyphs.plain} gives them) of luna's Halloween 2026 dumps. */
public final class EventPanelFixture {
    private EventPanelFixture() {}

    public static void readDump() {
        EventHud.clear();
        EventHud.read(List.of(
                new EventHud.Item("threshold 1 ✔", List.of("threshold 1 ✔", "", "[2.61k pts|220 pts]", "", "$25k", "20x mozzarella cube")),
                new EventHud.Item("threshold 2 ✔", List.of("threshold 2 ✔", "", "[2.61k pts|840 pts]", "", "$50k", "50x chicken nugget")),
                new EventHud.Item("threshold 3 ✔", List.of("threshold 3 ✔", "", "[2.61k pts|1.5k pts]", "", "$80k", "50x cut carp", "tag:")),
                new EventHud.Item("event missions → 2.61k pts", List.of("event missions → 2.61k pts", "",
                        "- 🎣 river monsters", "┗ (1/10) [24 pts|2 pts] ∞",
                        "- 🎣 wels catfish", "┗ (3/10) [150 pts|50 pts] partial",
                        "- 🎣 amazon river", "┗ (40/1500) [0 pts|400 pts]")),
                new EventHud.Item("welcome to halloween 2026!", List.of("welcome to halloween 2026!", "check out the event missions!")),
                new EventHud.Item("event leaderboards", List.of("event leaderboards", "your placement: #22", "event points: 2.61k pts"))));
    }

    public static void clear() {
        EventHud.clear();
    }
}
