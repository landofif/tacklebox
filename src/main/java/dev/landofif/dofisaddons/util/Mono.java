package dev.landofif.dofisaddons.util;

import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.MathHelper;

import java.util.Optional;

/**
 * The mod's monospaced font: DM Mono Light, with DM Mono Medium for bold (the same face and weights
 * as l's system24 Discord theme). Every piece of text the mod draws goes through here so the whole
 * mod shares one look. Characters the font lacks fall back to Minecraft's default font.
 *
 * <p>Minecraft rasterises a TTF once at a fixed {@code oversample} and draws it with nearest-neighbour
 * sampling, so the glyphs only look clean when one atlas pixel lands on one screen pixel. The font is
 * therefore shipped as {@code mono_1 … mono_8} and {@code mono_bold_1 … mono_bold_8} (oversample 1–8,
 * otherwise identical) and text is pointed at the variant that matches the real pixels per GUI unit it
 * is drawn at.
 *
 * <p>Vanilla's shadow and fake bold both offset a copy of the glyph by a whole GUI unit, which smears a
 * thin-stroked font into a blur, so mono text has no shadow (unless asked for, then a soft one), and
 * bold switches to the real Medium face instead of fake bold.
 */
public final class Mono {
    private static final int MAX_OVERSAMPLE = 8;
    /** Font definition name prefixes, in the order of {@link dev.landofif.dofisaddons.DofiSettings.Font}; minecraft has none. */
    private static final String[] PREFIX = {"mono", "jb"};
    private static final StyleSpriteSource[][] SOURCES = new StyleSpriteSource[PREFIX.length][MAX_OVERSAMPLE + 1];
    private static final StyleSpriteSource[][] BOLD_SOURCES = new StyleSpriteSource[PREFIX.length][MAX_OVERSAMPLE + 1];
    /** Shadow for mono text drawn straight over the world, where it needs a little contrast. */
    private static final int SOFT_SHADOW = 0x66000000;

    static {
        for (int f = 0; f < PREFIX.length; f++) {
            for (int i = 1; i <= MAX_OVERSAMPLE; i++) {
                SOURCES[f][i] = new StyleSpriteSource.Font(Identifier.of("dofisaddons", PREFIX[f] + "_" + i));
                BOLD_SOURCES[f][i] = new StyleSpriteSource.Font(Identifier.of("dofisaddons", PREFIX[f] + "_bold_" + i));
            }
        }
    }

    private Mono() {}

    /**
     * l's house style for everything the mod writes: no capital letters, no apostrophes. Applied here so
     * it also holds for values that come from the server or the player (wallet "38.48K", chat captures,
     * typed HUD lines). Formatting codes are case-insensitive, so lowercasing "§A" is harmless.
     */
    public static String house(String s) {
        return s.toLowerCase(java.util.Locale.ROOT).replace("'", "").replace("\u2019", "");
    }

    /** A plain literal in the mono font, sized for drawing at GUI scale (lowercase, see {@link #house}). */
    public static MutableText text(String s) {
        return Text.literal(house(s)).setStyle(style(Style.EMPTY));
    }

    /** A literal in the Medium (bold) face, for headers in screens. Don't add {@code BOLD} on top. */
    public static MutableText bold(String s) {
        if (!custom()) { // the minecraft font: its own bold
            return Text.literal(house(s)).setStyle(Style.EMPTY.withBold(true));
        }
        return Text.literal(house(s)).setStyle(Style.EMPTY.withFont(BOLD_SOURCES[font()][oversampleFor(1f)]).withoutShadow());
    }

    /** Same style, switched to the mods font (keeps colour etc.; drops shadow, except in the minecraft font). */
    public static Style style(Style style) {
        return custom() ? style.withFont(source(1f)).withoutShadow() : style.withFont(StyleSpriteSource.DEFAULT);
    }

    public static boolean isMono(Style style) {
        return style.getFont() instanceof StyleSpriteSource.Font f
                && f.id().getNamespace().equals("dofisaddons") && (f.id().getPath().startsWith("mono") || f.id().getPath().startsWith("jb"));
    }

    /** The font variant for text drawn at {@code drawScale} on top of the GUI scale. */
    public static StyleSpriteSource source(float drawScale) {
        return custom() ? SOURCES[font()][oversampleFor(drawScale)] : StyleSpriteSource.DEFAULT;
    }

    /** Index into {@link #PREFIX} of the chosen font (the first one while settings are not loaded). */
    private static int font() {
        dev.landofif.dofisaddons.DofiSettings settings = dev.landofif.dofisaddons.stats.StatsTracker.settings();
        return settings == null || settings.font == null ? 0 : Math.min(settings.font.ordinal(), PREFIX.length);
    }

    /** False when the minecraft font is chosen: the mod then draws its text like vanilla does. */
    private static boolean custom() {
        return font() < PREFIX.length;
    }

    private static int oversampleFor(float drawScale) {
        MinecraftClient client = MinecraftClient.getInstance();
        float guiScale = client == null || client.getWindow() == null ? 2 : client.getWindow().getScaleFactor();
        return MathHelper.clamp(Math.round(guiScale * drawScale), 1, MAX_OVERSAMPLE);
    }

    /**
     * Used by the text renderer for every run of text in the game when "mono font everywhere" is on: a run
     * in the minecraft default font is pointed at the mono variant for the GUI scale, bold becomes the
     * Medium face, and a requested shadow becomes the soft one. Runs in other fonts (server glyph fonts,
     * the mods own mono text) are left alone, and so are the colours the fishonmc pack shaders key on:
     * its text effects and its no-shadow colour (#4e5c24), which only work in the
     * minecraft font. Characters dm mono lacks (pack glyphs, small caps) fall back to the default font.
     */
    public static Style everywhere(Style style, boolean shadow) {
        dev.landofif.dofisaddons.DofiSettings settings = dev.landofif.dofisaddons.stats.StatsTracker.settings();
        if (settings == null || !settings.monoEverywhere || !custom() || !StyleSpriteSource.DEFAULT.equals(style.getFont())) {
            return style;
        }
        net.minecraft.text.TextColor color = style.getColor();
        if (color != null && isPackShaderColor(color.getRgb())) {
            return style;
        }
        int i = oversampleFor(1f);
        // Called once per character; consecutive characters share one Style object, so remember the last.
        Converted last = lastConverted;
        if (last != null && last.in == style && last.shadow == shadow && last.oversample == i) {
            return last.out;
        }
        Style out = style.withFont(style.isBold() ? BOLD_SOURCES[font()][i] : SOURCES[font()][i]).withBold(false);
        if (shadow && style.getShadowColor() == null) {
            out = out.withShadowColor(SOFT_SHADOW);
        }
        lastConverted = new Converted(style, shadow, i, out);
        return out;
    }

    /**
     * Colours the fishonmc pack's text shader treats specially (assets/minecraft/shaders/include/
     * text_effects_config.glsl matches colour / 4): effects at (240, 240, 0..64), (255, 255, 248) and
     * (94, 171, 136), plus the no-shadow colour #4e5c24.
     */
    private static boolean isPackShaderColor(int rgb) {
        int r = (rgb >> 16 & 0xFF) >> 2;
        int g = (rgb >> 8 & 0xFF) >> 2;
        int b = (rgb & 0xFF) >> 2;
        return (r == 60 && g == 60 && b <= 16) || (r == 63 && g == 63 && b == 62) || (r == 23 && g == 42 && b == 34)
                || rgb == 0x4E5C24;
    }

    private record Converted(Style in, boolean shadow, int oversample, Style out) {}

    private static volatile Converted lastConverted;

    /** {@link #prepare(Text, int, boolean)} for text drawn at {@code drawScale} in the GUI. */
    public static Text prepare(Text text, float drawScale, boolean shadow) {
        return prepare(text, oversampleFor(drawScale), shadow);
    }

    /**
     * Re-targets every mono run in {@code text} at the variant for {@code oversample} (the Medium face for
     * bold runs, instead of fake bold), with either no shadow or a soft one. Runs in other fonts (server
     * glyphs) are left alone. Use a fixed oversample for text drawn in the world.
     */
    public static Text prepare(Text text, int oversample, boolean shadow) {
        if (!custom()) {
            return text;
        }
        int i = MathHelper.clamp(oversample, 1, MAX_OVERSAMPLE);
        MutableText out = Text.empty();
        text.visit((style, s) -> {
            if (isMono(style)) {
                style = style.withFont(style.isBold() ? BOLD_SOURCES[font()][i] : SOURCES[font()][i]).withBold(false);
                style = shadow ? style.withShadowColor(SOFT_SHADOW) : style.withoutShadow();
            }
            out.append(Text.literal(isMono(style) ? house(s) : s).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
