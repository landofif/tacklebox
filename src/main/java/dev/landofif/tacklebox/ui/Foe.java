package dev.landofif.tacklebox.ui;

import dev.landofif.tacklebox.compat.FoeCollab;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import java.util.Locale;

/**
 * The look of the mod is foecollabs: its box textures (a 15x15 atlas cut into 5px slices, stretched to
 * any size) in its colours, with the normal game font. The textures are foecollabs own, referenced by
 * name, so the mod only looks right next to it (it depends on it). Widgets stay vanilla. Text is small capitals,
 * like foecollabs labels.
 */
public final class Foe {
    private static final Identifier BOX = tex("box_atlas");

    public static final int TEXT = 0xFFFFFFFF;
    public static final int DIM = 0xFFAAAAAA;
    public static final int FAINT = 0xFF666666;
    /** Panel titles and headers (foecollab greens its box titles). */
    public static final int TITLE = 0xFF55FF55;
    /** Selected and hovered text, and outlines that follow the mouse. */
    public static final int ACCENT = 0xFFFFAA00;

    private static final int BUTTON_OFF = 0x33000000;

    private static final int TEX = 15;
    private static final int SLICE = 5;

    private Foe() {}

    private static Identifier tex(String name) {
        return Identifier.of("foecollab", "textures/gui/sprites/elements/" + name + ".png");
    }

    private static final String PLAIN = "abcdefghijklmnopqrstuvwxyz";
    private static final String SMALL = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ"; // there is no small capital x

    /** The letters as small capitals, the way foecollab writes its labels. Digits and symbols stay. */
    public static String smallCaps(String s) {
        StringBuilder out = new StringBuilder(s.length());
        for (char c : s.toLowerCase(Locale.ROOT).toCharArray()) {
            int i = PLAIN.indexOf(c);
            out.append(i < 0 ? c : SMALL.charAt(i));
        }
        return out.toString();
    }

    /** Labels, titles, headings and buttons: small capitals. */
    public static MutableText text(String s) {
        return Text.literal(smallCaps(s));
    }

    /** Descriptions and tooltips: the normal font in sentence case, which is easier to read at length. */
    public static MutableText sentence(String s) {
        return Text.literal(s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1));
    }

    public static MutableText bold(String s) {
        return text(s).formatted(Formatting.BOLD);
    }

    /** A dark box with a brown border, {@code opacity} percent solid. Without foecollab (whose texture it is) a plain see-through rounded box. */
    public static void box(DrawContext ctx, int x, int y, int w, int h, int opacity) {
        if (FoeCollab.loaded()) {
            slice(ctx, BOX, x, y, w, h, opacity);
        } else {
            Hypr.rrect(ctx, x, y, w, h, Math.min(4, Math.min(w, h) / 2), plainBox(opacity));
        }
    }

    /** The colour of the plain box: the dark of the buttons, {@code opacity} percent of its usual see-through. */
    static int plainBox(int opacity) {
        return (0xAA * Math.max(0, Math.min(100, opacity)) / 100) << 24 | 0x181825;
    }

    /** A button: a rounded see-through fill, with the accent gradient as a thin outline while hovered. */
    public static void button(DrawContext ctx, int x, int y, int w, int h, boolean hot) {
        int r = Math.min(4, h / 2);
        Hypr.rrect(ctx, x, y, w, h, r, hot ? 0xAA313244 : 0x88181825);
        if (hot) {
            Hypr.ring(ctx, x, y, w, h, r, 1, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x, w, 1), 8);
        }
    }

    /** A button that cannot be clicked, or the picked one: barely there. */
    public static void buttonOff(DrawContext ctx, int x, int y, int w, int h) {
        Hypr.rrect(ctx, x, y, w, h, Math.min(4, h / 2), BUTTON_OFF);
    }

    /** Draws a hud scaled by {@code percent} around its top left corner (x, y) and returns its rect as it ends up on the screen. */
    public static int[] scaled(DrawContext ctx, int x, int y, int percent, java.util.function.Function<DrawContext, int[]> draw) {
        float k = percent / 100f;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x, y);
        m.scale(k, k);
        m.translate(-x, -y);
        int[] r = draw.apply(ctx);
        m.popMatrix();
        return r == null ? null : new int[]{r[0], r[1], Math.round(r[2] * k), Math.round(r[3] * k)};
    }

    /** A title where a panel puts it, inside the top left edge. Alone it is the title of something with no box. */
    public static void title(DrawContext ctx, int x, int y, Text title) {
        if (title != null && !title.getString().isEmpty()) {
            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, title, x + 8, y + 7, Hypr.acc2());
        }
    }

    /** The same title lined up inside a box {@code boxW} wide, with the same 8 pixel edge. */
    public static void title(DrawContext ctx, int x, int y, int boxW, dev.landofif.tacklebox.TackleboxSettings.HudAlign align, Text title) {
        if (title != null && !title.getString().isEmpty()) {
            var tr = MinecraftClient.getInstance().textRenderer;
            ctx.drawTextWithShadow(tr, title, align.x(x + 8, boxW - 16, tr.getWidth(title)), y + 7, Hypr.acc2());
        }
    }

    /** A box with its title inside the top edge. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, Text title, int opacity) {
        box(ctx, x, y, w, h, opacity);
        title(ctx, x, y, title);
    }

    /** The atlas as a 3x3 grid: corners as they are, edges and the middle stretched. */
    private static void slice(DrawContext ctx, Identifier tex, int x, int y, int w, int h, int opacity) {
        int color = (255 * opacity / 100) << 24 | 0xFFFFFF;
        int[] u = {0, SLICE, TEX - SLICE};
        int[] xs = {x, x + SLICE, x + w - SLICE};
        int[] ys = {y, y + SLICE, y + h - SLICE};
        int[] ws = {SLICE, w - SLICE * 2, SLICE};
        int[] hs = {SLICE, h - SLICE * 2, SLICE};
        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 3; col++) {
                if (ws[col] > 0 && hs[row] > 0) {
                    ctx.drawTexture(RenderPipelines.GUI_TEXTURED, tex, xs[col], ys[row], (float) u[col], (float) u[row],
                            ws[col], hs[row], SLICE, SLICE, TEX, TEX, color);
                }
            }
        }
    }
}
