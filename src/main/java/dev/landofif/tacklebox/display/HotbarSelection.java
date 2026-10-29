package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.ui.Hypr;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Util;

/** The held hotbar slot as a rounded gradient outline that glides to the new slot when you scroll, like hyprland's focus border. */
public final class HotbarSelection {
    private static float pos = -1;
    private static long last;

    private HotbarSelection() {}

    /** {@code slot} 0 to 8; {@code mid} the screen middle, {@code pitch} the slot distance, {@code y} the top of the outline. */
    public static void draw(DrawContext ctx, int slot, int mid, int pitch, int y) {
        long now = Util.getMeasuringTimeMs();
        float dt = Math.min(0.1f, (now - last) / 1000f);
        last = now;
        pos = pos < 0 || dt >= 0.1f ? slot : Hypr.approach(pos, slot, dt, 22);
        int x = mid + Math.round((pos - 4) * pitch) - 11;
        double angle = Math.toRadians(45) + (now % 10000) / 10000.0 * Math.PI * 2;
        Hypr.rrect(ctx, x, y, 22, 22, 5, Hypr.alpha(Hypr.acc1(), 0.16f));
        Hypr.ring(ctx, x, y, 22, 22, 5, 1, Hypr.angled(Hypr.acc1(), Hypr.acc2(), x, y, 22, 22, angle, 1), 8);
    }
}
