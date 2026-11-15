package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.item.ItemStack;

/**
 * Your inventory on the hud, without opening it (luna, 2026-11-15): the 27 slots above the hotbar as three rows of nine, items
 * with their counts. Display only, nothing can be clicked. Hidden while a chest or inventory screen is open, which shows it anyway.
 */
public final class InventoryHud {
    public static final int COLS = 9, ROWS = 3, CELL = 18;
    /** The first inventory slot above the hotbar. */
    static final int FIRST = 9;

    private InventoryHud() {}

    /** The top left corner of cell {@code i} (0 to 26) inside the grid. */
    static int[] cell(int i) {
        return new int[]{i % COLS * CELL, i / COLS * CELL};
    }

    static int[] size() {
        return new int[]{COLS * CELL, ROWS * CELL};
    }

    public static void renderHud(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (TackleboxSettings.get().inventoryHud.on && TriggerEngine.isOnFishOnMc() && !(mc.currentScreen instanceof HandledScreen<?>)) {
            draw(ctx);
        }
    }

    /** Draws the grid at its saved spot and returns {x, y, w, h}. */
    public static int[] draw(DrawContext ctx) {
        TackleboxSettings s = TackleboxSettings.get();
        HudSpot spot = s.inventoryHud;
        return spot.pos.draw(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(),
                (x, y) -> Foe.scaled(ctx, x, y, spot.scale, c -> drawAt100(c, x, y, s.inventoryHudCells)));
    }

    private static int[] drawAt100(DrawContext ctx, int x, int y, boolean cells) {
        MinecraftClient mc = MinecraftClient.getInstance();
        for (int i = 0; i < COLS * ROWS; i++) {
            int[] at = cell(i);
            int cx = x + at[0], cy = y + at[1];
            if (cells) {
                ctx.fill(cx + 1, cy + 1, cx + CELL - 1, cy + CELL - 1, 0x40000000);
            }
            ItemStack stack = mc.player == null ? ItemStack.EMPTY : mc.player.getInventory().getStack(FIRST + i);
            if (!stack.isEmpty()) {
                ctx.drawItem(stack, cx + 1, cy + 1);
                ctx.drawStackOverlay(mc.textRenderer, stack, cx + 1, cy + 1);
            }
        }
        int[] size = size();
        return new int[]{x, y, size[0], size[1]};
    }
}
