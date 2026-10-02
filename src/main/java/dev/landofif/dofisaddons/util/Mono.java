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
 * The mod's typewriter-style monospaced font (Courier Prime). Every piece of text the mod draws goes
 * through here so the whole mod shares one look. Characters the font lacks fall back to Minecraft's
 * default font.
 *
 * <p>Minecraft rasterises a TTF once at a fixed {@code oversample} and draws it with nearest-neighbour
 * sampling, so the glyphs only look clean when one atlas pixel lands on one screen pixel. The font is
 * therefore shipped as {@code mono_1 … mono_8} (oversample 1–8, otherwise identical) and text is pointed
 * at the variant that matches the real pixels per GUI unit it is drawn at.
 *
 * <p>Vanilla's shadow and fake bold both offset a copy of the glyph by a whole GUI unit, which smears a
 * thin-stroked font into a blur, so mono text has no shadow (unless asked for, then a soft one) and no
 * fake bold.
 */
public final class Mono {
    private static final int MAX_OVERSAMPLE = 8;
    private static final StyleSpriteSource[] SOURCES = new StyleSpriteSource[MAX_OVERSAMPLE + 1];
    /** Shadow for mono text drawn straight over the world, where it needs a little contrast. */
    private static final int SOFT_SHADOW = 0x66000000;

    static {
        for (int i = 1; i <= MAX_OVERSAMPLE; i++) {
            SOURCES[i] = new StyleSpriteSource.Font(Identifier.of("dofisaddons", "mono_" + i));
        }
    }

    private Mono() {}

    /** A plain literal in the mono font, sized for drawing at GUI scale. */
    public static MutableText text(String s) {
        return Text.literal(s).setStyle(style(Style.EMPTY));
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
        MinecraftClient client = MinecraftClient.getInstance();
        float guiScale = client == null || client.getWindow() == null ? 2 : client.getWindow().getScaleFactor();
        return SOURCES[MathHelper.clamp(Math.round(guiScale * drawScale), 1, MAX_OVERSAMPLE)];
    }

    /** {@link #prepare(Text, StyleSpriteSource, boolean)} for text drawn at {@code drawScale} in the GUI. */
    public static Text prepare(Text text, float drawScale, boolean shadow) {
        return prepare(text, source(drawScale), shadow);
    }

    /** {@link #prepare(Text, StyleSpriteSource, boolean)} with a fixed oversample (text drawn in the world). */
    public static Text prepare(Text text, int oversample, boolean shadow) {
        return prepare(text, SOURCES[MathHelper.clamp(oversample, 1, MAX_OVERSAMPLE)], shadow);
    }

    /**
     * Re-targets every mono run in {@code text} at {@code source}, without fake bold, and with either no
     * shadow or a soft one. Runs in other fonts (server glyphs) are left alone.
     */
    private static Text prepare(Text text, StyleSpriteSource source, boolean shadow) {
        MutableText out = Text.empty();
        text.visit((style, s) -> {
            if (isMono(style)) {
                style = style.withFont(source).withBold(false);
                style = shadow ? style.withShadowColor(SOFT_SHADOW) : style.withoutShadow();
            }
            out.append(Text.literal(s).setStyle(style));
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }
}
