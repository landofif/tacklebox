package dev.landofif.tacklebox.chat;

import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneId;

import static org.junit.jupiter.api.Assertions.*;

class ChatTimesTest {
    @Test
    void formatsAs24HourLocalTime() {
        long t = LocalDateTime.of(2026, 10, 10, 21, 5, 9).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals("21:05:09", ChatTimes.format(t));
        long morning = LocalDateTime.of(2026, 10, 10, 1, 4, 55).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals("01:04:55", ChatTimes.format(morning));
    }

    @Test
    void remembersByMessageObjectNotByText() {
        Text a = Text.literal("same");
        Text b = Text.literal("same");
        long before = System.currentTimeMillis();
        ChatTimes.record(a);
        assertTrue(ChatTimes.at(a) >= before);
        assertEquals(-1, ChatTimes.at(b));
    }

    @Test
    void forgetsOldestPastTheLimit() {
        Text first = Text.literal("first");
        ChatTimes.record(first);
        for (int i = 0; i < 2200; i++) {
            ChatTimes.record(Text.literal("x" + i));
        }
        assertEquals(-1, ChatTimes.at(first));
    }

    @Test
    void removesTheBangBeforeAPlayerLine() {
        Text m = Text.literal("\uE001").append(Text.literal("! [113] name \u00bb hi"));
        assertEquals("\uE001[113] name \u00bb hi", ChatTimes.rewrite(m, 0, 0).getString());
        Text plain = Text.literal("Great Lakes! teleported");
        assertSame(plain, ChatTimes.rewrite(plain, 0, 0));
    }

    @Test
    void stampGoesInFrontWhenOn() {
        long t = LocalDateTime.of(2026, 10, 10, 21, 5, 9).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
        assertEquals("[21:05:09] x", ChatTimes.rewrite(Text.literal("x"), 1, t).getString());
        assertEquals("[21:05] [1] a", ChatTimes.rewrite(Text.literal("! [1] a"), 2, t).getString());
        assertEquals("[1] a", ChatTimes.bare("[21:05] [1] a"));
        assertEquals("[1] a", ChatTimes.bare("[21:05:09] [1] a"));
        assertEquals("[1] a", ChatTimes.bare("[1] a"));
    }
}
