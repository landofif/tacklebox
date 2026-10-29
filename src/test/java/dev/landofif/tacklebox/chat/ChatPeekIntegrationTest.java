package dev.landofif.tacklebox.chat;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Hold the peek key, scroll up, a line arrives, let go: back at the newest line. */
class ChatPeekIntegrationTest {
    /**
     * Vanilla chat's scroll rules as javap shows them: scroll(int) clamps to the lines there are,
     * addVisibleMessage scrolls one more while chat is open and scrolled up, resetScroll goes to 0.
     * "Open" is asked through ChatPeek.focused, the way ChatHudMixin wraps isChatFocused.
     */
    private static final class Chat {
        int lines = 100;
        final int shown = 20;
        int scrolled;

        void scroll(int n) {
            scrolled = Math.max(0, Math.min(scrolled + n, lines - shown));
        }

        void add() {
            lines++;
            if (ChatPeek.focused(false) && scrolled > 0) {
                scroll(1);
            }
        }
    }

    @Test
    void peekScrollNewLineRelease() {
        Chat chat = new Chat();
        Runnable toBottom = () -> chat.scrolled = 0;
        ChatPeek.update(false, false, false, toBottom);

        ChatPeek.update(true, false, false, toBottom); // press
        assertTrue(ChatPeek.active());
        chat.scroll(ChatPeek.lines(1, false)); // one notch up
        chat.scroll(ChatPeek.lines(1, true)); // one with shift
        assertEquals(8, chat.scrolled);

        chat.add(); // the lines you are reading stay put
        assertEquals(9, chat.scrolled);

        ChatPeek.update(false, false, false, toBottom); // release
        assertFalse(ChatPeek.active());
        assertEquals(0, chat.scrolled);

        chat.scroll(7); // after letting go a new line does not keep a place (vanilla: chat closed)
        chat.add();
        assertEquals(7, chat.scrolled);
    }
}
