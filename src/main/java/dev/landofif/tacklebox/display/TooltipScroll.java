package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.util.Util;

/**
 * A tooltip taller than the screen starts with its top on screen and the mouse wheel scrolls it (setting tooltipScroll).
 * The scroll starts over when a tooltip of another size shows or none was drawn for a moment. The wheel goes to the
 * tooltip before the menu page wheel, the screen and Mouse Tweaks, but only while a tall tooltip is up.
 */
public final class TooltipScroll {
    /** Pixels per wheel turn, a bit more than one line. */
    static final int STEP = 12;
    /** A tall tooltip not drawn for this long is gone. */
    static final long GONE_MS = 150;
    private static int scroll, lastWidth, lastHeight;
    private static long lastDrawn;

    private TooltipScroll() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
                ScreenMouseEvents.allowMouseScroll(screen).register((s, x, y, horizontal, vertical) -> !wheel(vertical)));
    }

    private static boolean wheel(double vertical) {
        if (!TackleboxSettings.get().tooltipScroll || vertical == 0 || Util.getMeasuringTimeMs() - lastDrawn > GONE_MS) {
            return false;
        }
        scroll -= (int) Math.signum(vertical) * STEP; // wheel down shows more of the bottom
        return true;
    }

    /** The top of a tooltip {@code height} tall on a screen {@code screenHeight} tall; {@code y} is where vanilla put it. */
    public static int top(int y, int width, int height, int screenHeight) {
        if (!TackleboxSettings.get().tooltipScroll || !tall(height, screenHeight)) {
            return y;
        }
        long now = Util.getMeasuringTimeMs();
        if (width != lastWidth || height != lastHeight || now - lastDrawn > GONE_MS) {
            scroll = 0;
            lastWidth = width;
            lastHeight = height;
        }
        lastDrawn = now;
        scroll = clamp(scroll, height, screenHeight);
        return 4 - scroll;
    }

    /** Taller than the screen with vanillas 4 pixel gap above and 3 below. */
    static boolean tall(int height, int screenHeight) {
        return height + 7 > screenHeight;
    }

    /** Between the top on screen (0) and the bottom on screen. */
    static int clamp(int scroll, int height, int screenHeight) {
        return Math.max(0, Math.min(height + 7 - screenHeight, scroll));
    }
}
