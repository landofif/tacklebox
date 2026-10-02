package dev.landofif.dofisaddons.util;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * The mod's typewriter-style monospaced font (Courier Prime, bundled as
 * {@code assets/dofisaddons/font/mono.json}). Every piece of text the mod draws goes through here
 * so the whole mod shares one look. Characters the font lacks fall back to Minecraft's default font.
 */
public final class Mono {
    public static final Identifier FONT_ID = Identifier.of("dofisaddons", "mono");
    private static final StyleSpriteSource SOURCE = new StyleSpriteSource.Font(FONT_ID);

    private Mono() {}

    /** A plain literal in the mono font. */
    public static MutableText text(String s) {
        return Text.literal(s).setStyle(Style.EMPTY.withFont(SOURCE));
    }

    /** Same style, switched to the mono font (keeps colour, bold, etc.). */
    public static Style style(Style style) {
        return style.withFont(SOURCE);
    }
}
