package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.OrderedText;
import org.joml.Vector2f;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Lets foecollab's huds step aside for chat windows (setting {@code hideBehindChat}). A foecollab hud does not say
 * where it is, so while one draws, every text it draws is added up into a rectangle ({@link #text}); next frame the
 * hud is skipped when that rectangle overlaps a chat window or the player list ({@link HudCover}). A skipped hud
 * keeps its old rectangle, so it comes back as soon as the chat moves away. See {@code FoeHudsMixin}.
 */
public final class FoeCover {
    private static final int PAD = 3;
    private static final Map<Object, int[]> BOUNDS = new IdentityHashMap<>();
    private static int[] cur;

    private FoeCover() {}

    /** Draws a foecollab hud through {@code draw}, or skips it when it sat under a chat window last time. */
    public static void run(Object hud, Runnable draw) {
        TackleboxSettings s = TackleboxSettings.get();
        if (s == null || !s.hideBehindChat || cur != null) {
            draw.run();
            return;
        }
        int[] b = BOUNDS.get(hud);
        if (b != null && HudCover.covered(b[0], b[1], b[2], b[3])) {
            return;
        }
        cur = new int[]{Integer.MAX_VALUE, Integer.MAX_VALUE, Integer.MIN_VALUE, Integer.MIN_VALUE};
        try {
            draw.run();
        } finally {
            int[] c = cur;
            cur = null;
            if (c[2] > c[0]) {
                BOUNDS.put(hud, new int[]{c[0] - PAD, c[1] - PAD, c[2] - c[0] + PAD * 2, c[3] - c[1] + PAD * 2});
            }
        }
    }

    /** Called for every text drawn; counts only while a foecollab hud is drawing. */
    public static void text(DrawContext ctx, TextRenderer tr, OrderedText text, int x, int y) {
        if (cur == null) {
            return;
        }
        Vector2f a = ctx.getMatrices().transformPosition(x, y, new Vector2f());
        Vector2f b = ctx.getMatrices().transformPosition(x + tr.getWidth(text), y + 9, new Vector2f());
        cur[0] = Math.min(cur[0], (int) Math.floor(a.x));
        cur[1] = Math.min(cur[1], (int) Math.floor(a.y));
        cur[2] = Math.max(cur[2], (int) Math.ceil(b.x));
        cur[3] = Math.max(cur[3], (int) Math.ceil(b.y));
    }
}
