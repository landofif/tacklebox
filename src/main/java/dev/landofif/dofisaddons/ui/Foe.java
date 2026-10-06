package dev.landofif.dofisaddons.ui;

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
 * name, so the mod only looks right next to it (it depends on it). Widgets stay vanilla.
 */
public final class Foe {
    private static final Identifier BOX = tex("box_atlas");
    private static final Identifier SOLID = tex("box_solid_atlas");
    private static final Identifier SOLID_ALT = tex("box_solid_alt_atlas");

    public static final int TEXT = 0xFFFFFFFF;
    public static final int DIM = 0xFFAAAAAA;
    public static final int FAINT = 0xFF666666;
    /** Panel titles and headers (foecollab greens its box titles). */
    public static final int TITLE = 0xFF55FF55;
    /** Selected and hovered text, and outlines that follow the mouse. */
    public static final int ACCENT = 0xFFFFAA00;

    private static final int TEX = 15;
    private static final int SLICE = 5;

    private Foe() {}

    private static Identifier tex(String name) {
        return Identifier.of("foecollab", "textures/gui/sprites/elements/" + name + ".png");
    }

    /**
     * l's house style for everything the mod writes: no capital letters, no apostrophes. Applied here so it
     * also holds for values that come from the server or the player (wallet "38.48K", chat captures).
     */
    public static String house(String s) {
        return s.toLowerCase(Locale.ROOT).replace("'", "").replace("\u2019", "");
    }

    public static MutableText text(String s) {
        return Text.literal(house(s));
    }

    public static MutableText bold(String s) {
        return text(s).formatted(Formatting.BOLD);
    }

    /** A dark box with a brown border, {@code opacity} percent solid. */
    public static void box(DrawContext ctx, int x, int y, int w, int h, int opacity) {
        slice(ctx, BOX, x, y, w, h, opacity);
    }

    /** A brown filled box, the lighter purple one while hovered. */
    public static void button(DrawContext ctx, int x, int y, int w, int h, boolean hot) {
        slice(ctx, hot ? SOLID_ALT : SOLID, x, y, w, h, 100);
    }

    /** A title where a panel puts it, inside the top left edge. Alone it is the title of something with no box. */
    public static void title(DrawContext ctx, int x, int y, Text title) {
        if (title != null && !title.getString().isEmpty()) {
            ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, title, x + 8, y + 7, TITLE);
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
