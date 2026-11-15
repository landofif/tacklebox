package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * "Slots left" (luna, 2026-11-15): how many of your 36 inventory slots (hotbar and the 27 above it) are empty, shown once that
 * is at or under a number you set ({@code slotsLeftAt}; 36 shows it always). Red at 0.
 */
public final class SlotsLeft {
    public static final int SLOTS = 36;
    private static final int RED = 0xFFFF5555;

    private SlotsLeft() {}

    /** The row for {@code free} empty slots, or nothing while there are more than {@code at} (a preview always shows). */
    static List<TextHud.Row> rows(int free, int at, boolean preview) {
        if (free > at && !preview) {
            return List.of();
        }
        return List.of(new TextHud.Row("slots left", Integer.toString(free), 0, free == 0 ? RED : 0));
    }

    static int free(MinecraftClient mc) {
        int free = 0;
        if (mc.player != null) {
            for (int i = 0; i < SLOTS; i++) {
                if (mc.player.getInventory().getStack(i).isEmpty()) {
                    free++;
                }
            }
        }
        return free;
    }

    public static void renderHud(DrawContext ctx) {
        if (TackleboxSettings.get().slotsLeftHud.on && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}, or null while it does not show. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        TackleboxSettings s = TackleboxSettings.get();
        return TextHud.draw(ctx, s.slotsLeftHud, "inventory", rows(free(MinecraftClient.getInstance()), s.slotsLeftAt, preview));
    }
}
