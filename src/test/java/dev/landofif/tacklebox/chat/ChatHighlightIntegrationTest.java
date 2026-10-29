package dev.landofif.tacklebox.chat;

import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** A player line as the server sends it, through the time stamp rewrite and the highlight, the way ChatHudMixin runs them. */
class ChatHighlightIntegrationTest {
    @Test
    void serverLineGetsStampAndHighlight() {
        Text line = Text.literal("! ").append(Text.literal("[113] ").formatted(Formatting.AQUA))
                .append(Text.literal("bob » anyone seen luna at kenai?").formatted(Formatting.WHITE));
        Text out = ChatHighlight.apply(ChatTimes.rewrite(line, 2, 0), "luna", true, List.of(ChatHighlightTest.entry("kenai", 0x55FFFF)));
        String s = out.getString();
        assertTrue(s.endsWith("[113] bob » anyone seen luna at kenai?"), s);
        assertEquals(ChatHighlight.GOLD, ChatHighlightTest.colorAt(out, s.indexOf("luna")).getRgb());
        assertEquals(0x55FFFF, ChatHighlightTest.colorAt(out, s.indexOf("kenai")).getRgb());
        assertEquals(Formatting.WHITE.getColorValue(), ChatHighlightTest.colorAt(out, s.indexOf("anyone")).getRgb());
        // the line text is unchanged, so hide rules, boxes and stacking still compare the same words
        assertEquals(ChatTimes.rewrite(line, 2, 0).getString(), s);
    }
}
