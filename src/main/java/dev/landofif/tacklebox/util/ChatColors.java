package dev.landofif.tacklebox.util;

import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** The colours the server gave the words of a chat line, so a hud can show those words the way chat does. */
public final class ChatColors {
    /** No colour found: the hud keeps its own. */
    public static final int NONE = -1;

    private ChatColors() {}

    /**
     * The colour of the first letter of {@code words} in the line, else of their first word: the words come from the normalized
     * line, which drops server glyphs and turns » into >, so the whole of them is not always in the line as sent.
     */
    public static int of(Text line, String words) {
        if (words == null || words.isBlank()) {
            return NONE;
        }
        StringBuilder plain = new StringBuilder();
        List<TextColor> colors = new ArrayList<>();
        line.visit((style, s) -> {
            plain.append(s);
            for (int i = 0; i < s.length(); i++) {
                colors.add(style.getColor());
            }
            return Optional.empty();
        }, Style.EMPTY);
        String w = words.trim();
        int at = plain.indexOf(w);
        if (at < 0) {
            at = plain.indexOf(w.split(" ", 2)[0]);
        }
        TextColor c = at < 0 ? null : colors.get(at);
        return c == null ? NONE : c.getRgb();
    }
}
