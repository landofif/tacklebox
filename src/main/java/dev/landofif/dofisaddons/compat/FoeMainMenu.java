package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofisAddons;

import java.lang.reflect.Field;

/**
 * Foecollab knows the main menu is open from the title of the screen ({@code 할}) and then reads the Daily Missions item for its
 * tracker. {@code FishMenuScreen} replaces that screen and has no such title, so this sets foecollab's flag for it by reflection.
 */
public final class FoeMainMenu {
    public static final String TITLE = "할";
    private static Field state;
    private static Object handler;
    private static boolean failed;

    private FoeMainMenu() {}

    public static void open(boolean open) {
        if (failed) {
            return;
        }
        try {
            if (state == null) {
                Class<?> c = Class.forName("io.github.foecollab.handler.DailyQuestHandler");
                handler = c.getMethod("instance").invoke(null);
                state = c.getField("questMenuState");
            }
            state.setBoolean(handler, open);
        } catch (ReflectiveOperationException | RuntimeException e) {
            failed = true; // foecollab changed or is missing: its tracker keeps its own logic
            DofisAddons.LOGGER.warn("daily missions: cannot tell foecollab the menu is open: {}", e.toString());
        }
    }
}
