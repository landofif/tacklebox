package dev.landofif.tacklebox.chat;

import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Your name and chosen words in a colour inside chat lines (ChatHudMixin, before the boxes take a
 * line, so vanilla chat and every box get it). Only the colour changes; hover and click stay.
 */
public final class ChatHighlight {
    /** Gold, the default for your name and new words. */
    public static final int GOLD = 0xFFAA00;

    /** One highlighted word, saved in settings.json. */
    public static class Entry {
        public String word = "";
        public int color = GOLD;
        public boolean enabled = true;
    }

    private record Word(String lower, int color) {}

    private ChatHighlight() {}

    /**
     * The line with every whole word match coloured, or the same object when nothing matched. Lines
     * you sent ("name »") are left alone. A match has to sit inside one styled piece of the line.
     */
    public static Text apply(Text message, String name, boolean withName, List<Entry> entries) {
        List<Word> words = new ArrayList<>();
        boolean hasName = withName && name != null && !name.isBlank();
        if (hasName) {
            words.add(new Word(name.toLowerCase(Locale.ROOT), GOLD));
        }
        if (entries != null) {
            for (Entry e : entries) {
                if (e != null && e.enabled && e.word != null && !e.word.isBlank()) {
                    words.add(new Word(e.word.trim().toLowerCase(Locale.ROOT), e.color & 0xFFFFFF));
                }
            }
        }
        if (words.isEmpty()) {
            return message;
        }
        String plain = message.getString();
        if (name != null && !name.isBlank() && plain.matches("(?s).*\\b" + java.util.regex.Pattern.quote(name) + " ?».*")) {
            return message;
        }
        MutableText out = Text.empty();
        boolean[] any = {false};
        message.visit((style, s) -> {
            String lower = s.toLowerCase(Locale.ROOT);
            int from = 0;
            for (int i = 0; i < s.length(); ) {
                Word hit = at(lower, i, words);
                if (hit == null) {
                    i++;
                    continue;
                }
                if (i > from) {
                    out.append(Text.literal(s.substring(from, i)).setStyle(style));
                }
                out.append(Text.literal(s.substring(i, i + hit.lower.length())).setStyle(style.withColor(TextColor.fromRgb(hit.color))));
                any[0] = true;
                i += hit.lower.length();
                from = i;
            }
            if (from < s.length()) {
                out.append(Text.literal(s.substring(from)).setStyle(style));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return any[0] ? out : message;
    }

    /** The longest word that starts at i as a whole word, or null. */
    private static Word at(String lower, int i, List<Word> words) {
        if (i > 0 && Character.isLetterOrDigit(lower.charAt(i - 1))) {
            return null;
        }
        Word best = null;
        for (Word w : words) {
            int end = i + w.lower.length();
            if (lower.startsWith(w.lower, i) && (end == lower.length() || !Character.isLetterOrDigit(lower.charAt(end)))
                    && (best == null || w.lower.length() > best.lower.length())) {
                best = w;
            }
        }
        return best;
    }
}
