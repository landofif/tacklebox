package dev.landofif.tacklebox.chat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ChatPeekTest {
    private int resets;

    @BeforeEach
    void release() {
        ChatPeek.update(false, false, false, () -> {});
        resets = 0;
    }

    @Test
    void wheelNotchIsSevenLinesOrOneWithShift() {
        assertEquals(7, ChatPeek.lines(1, false));
        assertEquals(-7, ChatPeek.lines(-1, false));
        assertEquals(1, ChatPeek.lines(1, true));
        assertEquals(-1, ChatPeek.lines(-1, true));
        assertEquals(7, ChatPeek.lines(3, false)); // a fast wheel still counts as one notch, like ChatScreen
        assertEquals(2, ChatPeek.lines(0.4, false));
    }

    @Test
    void holdingPeeksOnlyWithNoScreen() {
        ChatPeek.update(true, false, false, () -> resets++);
        assertTrue(ChatPeek.active());
        assertTrue(ChatPeek.focused(false));
        ChatPeek.update(false, false, false, () -> resets++);
        assertFalse(ChatPeek.active());
        assertFalse(ChatPeek.focused(false));
        assertTrue(ChatPeek.focused(true));

        ChatPeek.update(true, true, false, () -> resets++); // inventory open
        assertFalse(ChatPeek.active());
    }

    @Test
    void lettingGoScrollsDownOnce() {
        ChatPeek.update(true, false, false, () -> resets++);
        ChatPeek.update(true, false, false, () -> resets++);
        assertEquals(0, resets);
        ChatPeek.update(false, false, false, () -> resets++);
        ChatPeek.update(false, false, false, () -> resets++);
        assertEquals(1, resets);
    }

    @Test
    void openingAScreenWhilePeekingEndsIt() {
        ChatPeek.update(true, false, false, () -> resets++);
        ChatPeek.update(false, true, false, () -> resets++); // inventory: back down
        assertEquals(1, resets);
    }

    @Test
    void openingChatWhilePeekingKeepsThePlace() {
        ChatPeek.update(true, false, false, () -> resets++);
        ChatPeek.update(false, true, true, () -> resets++);
        assertFalse(ChatPeek.active());
        assertEquals(0, resets);
    }
}
