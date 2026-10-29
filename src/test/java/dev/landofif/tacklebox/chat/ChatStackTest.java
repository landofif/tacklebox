package dev.landofif.tacklebox.chat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

class ChatStackTest {
    @Test
    void numbersAndCountsAreIgnored() {
        assertEquals("sold # fish for $#", ChatStack.key("sold 12 fish for $1,234.50"));
        assertEquals(ChatStack.key("sold 3 fish for $9"), ChatStack.key("sold 12 fish for $1,234.50 (x4)"));
    }

    @Test
    void countFromTheEnd() {
        assertEquals(1, ChatStack.count("hello"));
        assertEquals(7, ChatStack.count("hello (x7)"));
        assertEquals(1, ChatStack.count("(x7) hello"));
    }

    @Test
    void findsARepeatWithLinesBetween() {
        List<String> chat = List.of("bob » hi", "TACKLE SHOP > restocked", "sold 4 fish for $20 (x2)");
        assertEquals(2, ChatStack.find(chat, Function.identity(), "sold 9 fish for $45"));
        assertEquals(-1, ChatStack.find(chat, Function.identity(), "bob » hello"));
        assertEquals(-1, ChatStack.find(chat, Function.identity(), " "));
    }

    @Test
    void onlyWithinTheWindow() {
        List<String> chat = new ArrayList<>();
        for (int i = 0; i < ChatStack.WINDOW; i++) {
            chat.add("line " + (char) ('a' + i));
        }
        chat.add("old repeat");
        assertEquals(-1, ChatStack.find(chat, Function.identity(), "old repeat"));
        assertEquals(0, ChatStack.find(chat, Function.identity(), "line a"));
    }
}
