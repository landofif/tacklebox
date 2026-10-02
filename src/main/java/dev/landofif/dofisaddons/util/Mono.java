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
    private static final StyleSpriteSource[] SOURCES = new StyleSpriteSource[MAX_OVERSAMPLE + 1];
    private static final StyleSpriteSource[] BOLD_SOURCES = new StyleSpriteSource[MAX_OVERSAMPLE + 1];
    /** Shadow for mono text drawn straight over the world, where it needs a little contrast. */
    private static final int SOFT_SHADOW = 0x66000000;

    static {
        for (int i = 1; i <= MAX_OVERSAMPLE; i++) {
            SOURCES[i] = new StyleSpriteSource.Font(Identifier.of("dofisaddons", "mono_" + i));
            BOLD_SOURCES[i] = new StyleSpriteSource.Font(Identifier.of("dofisaddons", "mono_bold_" + i));
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
        return Text.literal(house(s)).setStyle(Style.EMPTY.withFont(BOLD_SOURCES[oversampleFor(1f)]).withoutShadow());
    }

    /** Same style, switched to the mono font (keeps colour etc.; drops shadow). */
    public static Style style(Style style) {
        return style.withFont(source(1f)).withoutShadow();
    }

    public static boolean isMono(Style style) {
        return style.getFont() instanceof StyleSpriteSource.Font f
                && f.id().getNamespace().equals("dofisaddons") && f.id().getPath().startsWith("mono");
    }

    /** The font variant for text drawn at {@code drawScale} on top of the GUI scale. */
    public static StyleSpriteSource source(float drawScale) {
        return SOURCES[oversampleFor(drawScale)];
    }

    private static int oversampleFor(float drawScale) {
        MinecraftClient client = MinecraftClient.getInstance();
        float guiScale = client == null || client.getWindow() == null ? 2 : client.getWindow().getScaleFactor();
        return MathHelper.clamp(Math.round(guiScale * drawScale), 1, MAX_OVERSAMPLE);
    }

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
        int i = MathHelper.clamp(oversample, 1, MAX_OVERSAMPLE);
        MutableText out = Text.empty();
        text.visit((style, s) -> {
            if (isMono(style)) {
                style = style.withFont(style.isBold() ? BOLD_SOURCES[i] : SOURCES[i]).withBold(false);
                style = shadow ? style.withShadowColor(SOFT_SHADOW) : style.withoutShadow();
            }
            out.append(Text.literal(isMono(style) ? house(s) : s).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
