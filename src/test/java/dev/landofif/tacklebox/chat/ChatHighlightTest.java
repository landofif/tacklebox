package dev.landofif.tacklebox.chat;

import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ChatHighlightTest {
    /** The colour of the character at i, or null. */
    static TextColor colorAt(Text t, int i) {
        List<TextColor> colors = new ArrayList<>();
        t.visit((style, s) -> {
            for (int k = 0; k < s.length(); k++) {
                colors.add(style.getColor());
            }
            return java.util.Optional.empty();
        }, Style.EMPTY);
        return colors.get(i);
    }

    static ChatHighlight.Entry entry(String word, int color) {
        ChatHighlight.Entry e = new ChatHighlight.Entry();
        e.word = word;
        e.color = color;
        return e;
    }

    @Test
    void yourNameInGold() {
        Text out = ChatHighlight.apply(Text.literal("[12] bob » hey Luna, coming?"), "luna", true, List.of());
        assertEquals("[12] bob » hey Luna, coming?", out.getString());
        assertEquals(ChatHighlight.GOLD, colorAt(out, 15).getRgb());
        assertNull(colorAt(out, 14));
        assertNull(colorAt(out, 19));
    }

    @Test
    void wholeWordsOnly() {
        Text in = Text.literal("lunar eclipse, lunaa");
        assertSame(in, ChatHighlight.apply(in, "luna", true, List.of()));
    }

    @Test
    void linesYouSentStayPlain() {
        Text in = Text.literal("[12] luna » hi bob");
        assertSame(in, ChatHighlight.apply(in, "luna", true, List.of(entry("bob", 0x55FF55))));
    }

    @Test
    void chosenWordsInTheirColour() {
        Text out = ChatHighlight.apply(Text.literal("a Mythic marlin!"), "luna", true, List.of(entry("mythic", 0xFF55FF)));
        assertEquals(0xFF55FF, colorAt(out, 2).getRgb());
        assertNull(colorAt(out, 9));
    }

    @Test
    void offAndEmptyChangeNothing() {
        Text in = Text.literal("hey luna");
        assertSame(in, ChatHighlight.apply(in, "luna", false, List.of()));
        ChatHighlight.Entry off = entry("hey", 0xFF0000);
        off.enabled = false;
        assertSame(in, ChatHighlight.apply(in, "luna", false, List.of(off, entry(" ", 0xFF0000))));
    }

    @Test
    void hoverTextStays() {
        Style hover = Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal("profile")));
        Text in = Text.literal("hi ").append(Text.literal("luna").setStyle(hover));
        Text out = ChatHighlight.apply(in, "luna", true, List.of());
        List<Style> styles = new ArrayList<>();
        out.visit((style, s) -> { if (s.equals("luna")) styles.add(style); return java.util.Optional.empty(); }, Style.EMPTY);
        assertEquals(1, styles.size());
        assertNotNull(styles.getFirst().getHoverEvent());
        assertEquals(ChatHighlight.GOLD, styles.getFirst().getColor().getRgb());
    }
}
