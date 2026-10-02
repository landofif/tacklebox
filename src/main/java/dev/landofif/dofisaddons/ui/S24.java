package dev.landofif.dofisaddons.ui;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

/**
 * The mod's look, taken from l's system24 Discord theme (~/.config/equibop/themes/system24.theme.css):
 * flat neutral greys, thin borders that turn accent purple on hover, panels with their label set into
 * the top border, square corners. Colours are the theme's oklch values converted to sRGB.
 */
public final class S24 {
    // text-1 .. text-5
    public static final int TEXT_1 = 0xFFEEEEEE;
    public static final int TEXT_2 = 0xFFCECECE;
    public static final int TEXT_3 = 0xFFAEAEAE;
    public static final int TEXT_4 = 0xFF808080;
    public static final int TEXT_5 = 0xFF484848;
    // bg-1 (pressed) .. bg-4 (main background)
    public static final int BG_1 = 0xFF303030;
    public static final int BG_2 = 0xFF262626;
    public static final int BG_3 = 0xFF1D1D1D;
    public static final int BG_4 = 0xFF141414;
    /** --border (54% grey at 20%) as it lands on bg-4. */
    public static final int BORDER = 0xFF2A2A2A;
    /** accent-2 (purple-2): hovered borders and small accent elements. */
    public static final int ACCENT = 0xFFB589D6;
    /** accent-4 (purple-4). */
    public static final int ACCENT_DIM = 0xFF966AB5;

    /** How far a panel's label rises above its top border. */
    public static final int LABEL_RISE = 5;

    private S24() {}

    /** True while one of the mod's own screens is open (the widget mixins only restyle those). */
    public static boolean active() {
        return MinecraftClient.getInstance().currentScreen instanceof DofiScreen;
    }

    /** True when vanilla widgets, lists, backgrounds and tooltips should be drawn in this style right now. */
    public static boolean everywhere() {
        DofiSettings s = StatsTracker.settings();
        return active() || (s != null && s.s24Everywhere);
    }

    /** True when chests, the inventory and server menus get the flat panel look. */
    public static boolean menus() {
        DofiSettings s = StatsTracker.settings();
        return s != null && s.s24Menus;
    }

    /** True when the hotbar gets flat boxes. */
    public static boolean hotbar() {
        DofiSettings s = StatsTracker.settings();
        return s != null && s.s24Hotbar;
    }

    /** {@code rgb} at the given alpha (0-255). */
    public static int alpha(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** A panel: bg-4 fill, thin border, and (if given) a muted label set into the top border. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, Text label) {
        ctx.fill(x, y, x + w, y + h, BG_4);
        outline(ctx, x, y, w, h, BORDER);
        if (label != null && !label.getString().isEmpty()) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int lw = tr.getWidth(label);
            ctx.fill(x + 6, y, x + 12 + lw, y + 1, BG_4); // gap in the border
            ctx.drawText(tr, label, x + 9, y - 4, TEXT_4, false);
        }
    }

    /**
     * A vanilla button anywhere in the game. In the mod's own screens an inactive button is the selected
     * tab (accent underline); everywhere else it is a disabled button, so it is just dimmed.
     */
    public static void button(DrawContext ctx, int x, int y, int w, int h, boolean hot, boolean active) {
        if (active || active()) {
            box(ctx, x, y, w, h, hot, active);
        } else {
            ctx.fill(x, y, x + w, y + h, BG_3);
            outline(ctx, x, y, w, h, BORDER);
        }
    }

    /** A horizontal border line with a label set into it, like a panel title (splits one panel in two). */
    public static void divider(DrawContext ctx, int x, int y, int w, Text label) {
        ctx.fill(x, y, x + w, y + 1, BORDER);
        if (label != null && !label.getString().isEmpty()) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int lw = tr.getWidth(label);
            ctx.fill(x + 6, y, x + 12 + lw, y + 1, BG_4);
            ctx.drawText(tr, label, x + 9, y - 4, TEXT_4, false);
        }
    }

    /**
     * A control's box (buttons, fields, sliders). Hovered or focused: lighter fill and accent border.
     * Inactive buttons in the mod are the selected tab, so they get an accent underline instead.
     */
    public static void box(DrawContext ctx, int x, int y, int w, int h, boolean hot, boolean active) {
        ctx.fill(x, y, x + w, y + h, hot && active ? BG_1 : BG_2);
        outline(ctx, x, y, w, h, hot && active ? ACCENT : BORDER);
        if (!active) {
            ctx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, ACCENT);
        }
    }
}
