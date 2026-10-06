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
 * the top border, square corners. Colours are the theme's oklch values converted to sRGB, and switch
 * with the chosen flavor (see {@link Flavor}). The caelestia style ({@link #round()}, after caelestia-dots/shell)
 * keeps the colours but draws rounded, borderless tonal boxes and pill controls instead.
 */
public final class S24 {
    // text-1 .. text-5
    public static int TEXT_1 = 0xFFEEEEEE;
    public static int TEXT_2 = 0xFFCECECE;
    public static int TEXT_3 = 0xFFAEAEAE;
    public static int TEXT_4 = 0xFF808080;
    public static int TEXT_5 = 0xFF484848;
    // bg-1 (pressed) .. bg-4 (main background)
    public static int BG_1 = 0xFF303030;
    public static int BG_2 = 0xFF262626;
    public static int BG_3 = 0xFF1D1D1D;
    public static int BG_4 = 0xFF141414;
    /** --border (54% grey at 20%) as it lands on bg-4. */
    public static int BORDER = 0xFF2A2A2A;
    /** accent-2 (purple-2): hovered borders and small accent elements. */
    public static int ACCENT = 0xFFB589D6;
    /** accent-4 (purple-4). */
    public static int ACCENT_DIM = 0xFF966AB5;

    /** Custom hud box fill and border (rgb; each hud sets its own alpha). */
    public static int HUD_PANEL = 0x0E0F12;
    public static int HUD_BORDER = 0x4E525C;
    /** The flavor the colours above come from (settings → look). */
    private static Flavor flavor = Flavor.SYSTEM24;

    /** How far a panel's label rises above its top border. */
    public static final int LABEL_RISE = 5;

    private S24() {}

    /** Switches every colour above to a system24 flavor. Takes effect on the next frame. */
    public static void apply(Flavor f) {
        if (f == null) {
            f = Flavor.SYSTEM24;
        }
        flavor = f;
        TEXT_1 = f.text1;
        TEXT_2 = f.text2;
        TEXT_3 = f.text3;
        TEXT_4 = f.text4;
        TEXT_5 = f.text5;
        BG_1 = f.bg1;
        BG_2 = f.bg2;
        BG_3 = f.bg3;
        BG_4 = f.bg4;
        BORDER = f.border;
        ACCENT = f.accent;
        ACCENT_DIM = f.accentDim;
        HUD_PANEL = f.hudPanel;
        HUD_BORDER = f.hudBorder;
    }

    public static Flavor flavor() {
        return flavor;
    }

    /** True while one of the mod's own screens is open (the widget mixins only restyle those). */
    public static boolean active() {
        return MinecraftClient.getInstance().currentScreen instanceof DofiScreen;
    }

    private static void sprite(DrawContext ctx, String id, int x, int y, int w, int h) {
        ctx.drawGuiTexture(net.minecraft.client.gl.RenderPipelines.GUI_TEXTURED, net.minecraft.util.Identifier.ofVanilla(id), x, y, w, h);
    }

    /** True when the plain minecraft style is picked: vanilla widgets, backgrounds, menus and hotbar everywhere. */
    public static boolean vanilla() {
        DofiSettings s = StatsTracker.settings();
        return s != null && s.style == DofiSettings.Style.MINECRAFT;
    }

    /** True when vanilla widgets, lists, backgrounds and tooltips should be drawn in this style right now. */
    public static boolean everywhere() {
        DofiSettings s = StatsTracker.settings();
        return !vanilla() && (active() || (s != null && s.s24Everywhere));
    }

    /** True when chests, the inventory and server menus get the flat panel look. */
    public static boolean menus() {
        DofiSettings s = StatsTracker.settings();
        return s != null && s.s24Menus && !vanilla();
    }

    /** True when the hotbar gets flat boxes. */
    public static boolean hotbar() {
        DofiSettings s = StatsTracker.settings();
        return s != null && s.s24Hotbar && !vanilla();
    }

    /** {@code rgb} at the given alpha (0-255). */
    public static int alpha(int alpha, int rgb) {
        return (alpha << 24) | (rgb & 0xFFFFFF);
    }

    /** True when the caelestia shell style is on: rounded, borderless tonal surfaces instead of square panels. */
    public static boolean round() {
        DofiSettings s = StatsTracker.settings();
        return s != null && s.style == DofiSettings.Style.CAELESTIA;
    }

    /** Corner radius for a box this size in the caelestia style. */
    private static int radius(int w, int h) {
        return Math.min(Math.min(w, h) / 2, 6);
    }

    /** A filled box, rounded in the caelestia style. */
    public static void fill(DrawContext ctx, int x, int y, int w, int h, int color) {
        int r = round() ? radius(w, h) : 0;
        if (r == 0) {
            ctx.fill(x, y, x + w, y + h, color);
            return;
        }
        Round.draw(ctx, x, y, w, h, r, 0, color);
    }

    /**
     * A filled box with a border. In the caelestia style it is rounded and the resting {@link #BORDER} is
     * left out, so only hover, focus and accent rings show.
     */
    public static void rect(DrawContext ctx, int x, int y, int w, int h, int fill, int border) {
        if (vanilla()) { // minecraft style: the vanilla sprites, so resource packs can retexture them
            sprite(ctx, border == ACCENT ? "widget/button_highlighted" : "widget/button", x, y, w, h);
            return;
        }
        fill(ctx, x, y, w, h, fill);
        if (!round() || border != BORDER) {
            outline(ctx, x, y, w, h, border);
        }
    }

    public static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        int r = round() ? radius(w, h) : 0;
        if (r > 0) {
            Round.draw(ctx, x, y, w, h, r, 1, color);
            return;
        }
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** How opaque panels are drawn right now, in percent (huds and screens set it around their panel). */
    public static int panelOpacity = 100;

    /** {@code alpha} (0-255) scaled by {@code pct} percent. */
    public static int scale(int alpha, int pct) {
        return alpha * pct / 100;
    }

    /** A panel: bg-4 fill, thin border, and (if given) a muted label set into the top border. */
    public static void panel(DrawContext ctx, int x, int y, int w, int h, Text label) {
        rect(ctx, x, y, w, h, alpha(scale(0xFF, panelOpacity), BG_4), BORDER);
        if (label != null && !label.getString().isEmpty()) {
            TextRenderer tr = MinecraftClient.getInstance().textRenderer;
            int lw = tr.getWidth(label);
            if (round()) { // caelestia: the label is an accent pill sitting on the top edge
                fill(ctx, x + 6, y - 6, lw + 10, 12, BG_2);
                ctx.drawText(tr, label, x + 11, y - 4, ACCENT, false);
                return;
            }
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
            rect(ctx, x, y, w, h, BG_3, BORDER);
        }
    }

    /** A horizontal border line with a label set into it, like a panel title (splits one panel in two). */
    public static void divider(DrawContext ctx, int x, int y, int w, Text label) {
        ctx.fill(x, y, x + w, y + 1, round() ? BG_2 : BORDER);
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
        if (vanilla()) {
            sprite(ctx, hot && active ? "widget/button_highlighted" : active ? "widget/button" : "widget/button_disabled", x, y, w, h);
            return;
        }
        if (!active && round()) { // caelestia: the selected tab is a filled accent pill
            rect(ctx, x, y, w, h, Anim.mix(BG_2, ACCENT, 0.35f), BORDER);
            return;
        }
        rect(ctx, x, y, w, h, hot && active ? BG_1 : BG_2, hot && active ? ACCENT : BORDER);
        if (!active) {
            ctx.fill(x + 1, y + h - 2, x + w - 1, y + h - 1, ACCENT);
        }
    }
}
