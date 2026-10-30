package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.menu.MenuRows;

import java.util.List;
import java.util.Locale;

/**
 * What tacklebox's menu panel shows for /event: a row per reward threshold and per mission, made from what
 * {@link EventHud} read off the open menu (it keeps reading it while the panel is open). Formats: docs/huds/event-hud.md.
 */
public final class EventPage {
    private EventPage() {}

    /** The saved event: title, points, placement, thresholds and missions. */
    public static EventHud.State state() {
        return EventHud.state();
    }

    /** "Threshold 5", its first two rewards, and the points you have against the points it needs. */
    public static MenuRows.Row threshold(EventHud.Threshold t) {
        String sub = String.join(" · ", t.rewards().subList(0, Math.min(2, t.rewards().size())));
        float progress = t.need() > 0 ? (float) Math.min(1, t.have() / t.need()) : -1;
        return new MenuRows.Row("Threshold " + t.n(), sub, -1, progress, low(EventHud.fmt(Math.min(t.have(), t.need())) + "/" + EventHud.fmt(t.need())));
    }

    /** A mission with what it gives: per catch, repeating, once, or by the best weight. */
    public static MenuRows.Row mission(EventHud.Mission m) {
        String per = EventHud.fmt(m.per()) + " pts";
        return switch (m.kind()) {
            case "weight" -> new MenuRows.Row(m.name(), "best weight", -1, -1, low(EventHud.fmt(m.earned()) + " pts"));
            case "repeat" -> new MenuRows.Row(m.name(), per + " every " + m.max() + ", " + EventHud.fmt(m.earned()) + " pts so far", -1,
                    m.max() > 0 ? m.cur() / (float) m.max() : -1, m.cur() + "/" + m.max());
            default -> new MenuRows.Row(m.name(), (m.kind().equals("partial") ? per + " each" : per), -1,
                    m.done() ? 1 : m.max() > 0 ? m.cur() / (float) m.max() : -1, m.cur() + "/" + m.max());
        };
    }

    /** The missions in the book's order. */
    public static List<EventHud.Mission> missions() {
        return List.copyOf(EventHud.state().missions.values());
    }

    private static String low(String s) {
        return s.toLowerCase(Locale.ROOT);
    }
}
