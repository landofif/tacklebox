package dev.landofif.tacklebox.chat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;

/** A run of sell lines with other chat in between ends as one line at the bottom with the newest numbers. */
class ChatStackIntegrationTest {
    /** The same steps ChatHudMixin takes on vanilla's messages list (newest first). */
    private static void add(List<String> chat, String line) {
        int i = ChatStack.find(chat, Function.identity(), line);
        if (i >= 0) {
            int count = ChatStack.count(chat.remove(i)) + 1;
            line = line + " (x" + count + ")";
        }
        chat.addFirst(line);
    }

    @Test
    void sellLinesStack() {
        List<String> chat = new ArrayList<>();
        add(chat, "SHOP > sold 4 fish for $20");
        add(chat, "bob » anyone at kenai?");
        add(chat, "SHOP > sold 9 fish for $45");
        add(chat, "amy » yes");
        add(chat, "SHOP > sold 1 fish for $5");
        assertEquals(List.of("SHOP > sold 1 fish for $5 (x3)", "amy » yes", "bob » anyone at kenai?"), chat);
    }
}
