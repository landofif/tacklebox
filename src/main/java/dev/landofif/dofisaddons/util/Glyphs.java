package dev.landofif.dofisaddons.util;

import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;

import java.util.Map;
import java.util.Optional;

/**
 * Turns fishonmc menu titles into plain names. The server draws its menu art by putting resource pack
 * glyphs in the screen title: private-use characters, hangul syllables mapped to images, and runs in pack
 * fonts. Drop all of that and what is left is the readable part ("Tackle Shop"). Titles that are only art
 * are named from the table in docs/fishonmc-research.md (section 8).
 */
public final class Glyphs {
    /** The art glyph a title-only menu uses, and its name. */
    private static final Map<String, String> ART_ONLY = Map.of(
            "픲", "stats",       // 픲
            "할", "main menu",   // 할
            "픹", "quests",      // 픹
            "핒", "crew");       // 핒
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡʏᴢ";
    private static final String PLAIN = "abcdefghijklmnopqrstuvwyz";

    private Glyphs() {}

    /** The readable name of a menu, lowercase, or "menu" when the title is only art. */
    public static String menuName(Text title) {
        String raw = title.getString();
        String plain = plain(title);
        if (plain.isEmpty()) {
            for (Map.Entry<String, String> e : ART_ONLY.entrySet()) {
                if (raw.contains(e.getKey())) {
                    return e.getValue();
                }
            }
            return "menu";
        }
        return plain;
    }

    /** Text in the default font with glyphs, formatting codes and small caps cleaned out. */
    public static String plain(Text text) {
        StringBuilder in = new StringBuilder();
        text.visit((style, s) -> {
            if (StyleSpriteSource.DEFAULT.equals(style.getFont())) {
                in.append(s);
            }
            return Optional.empty();
        }, Style.EMPTY);
        StringBuilder out = new StringBuilder();
        boolean space = false;
        for (int i = 0; i < in.length(); ) {
            int cp = in.codePointAt(i);
            i += Character.charCount(cp);
            if (cp == '§') {
                i = Math.min(in.length(), i + 1); // formatting code
                continue;
            }
            if (isArt(cp) || Character.isISOControl(cp)) {
                continue;
            }
            int caps = SMALL_CAPS.indexOf(cp);
            if (caps >= 0) {
                cp = PLAIN.charAt(caps);
            }
            if (Character.isWhitespace(cp)) {
                space = out.length() > 0;
                continue;
            }
            if (space) {
                out.append(' ');
                space = false;
            }
            out.appendCodePoint(cp);
        }
        return Mono.house(out.toString());
    }

    /** Private-use characters and hangul syllables: what the fishonmc pack maps its images to. */
    public static boolean isArt(int cp) {
        return (cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0xF0000 || (cp >= 0xAC00 && cp <= 0xD7A3);
    }
}
