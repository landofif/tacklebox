package dev.landofif.tacklebox.chat;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/** Which message a drawn chat line belongs to, and where a jump scrolls to. */
class ChatCopyTest {
    static ChatHudLine message(String text) {
        return new ChatHudLine(0, Text.literal(text), null, null);
    }

    /** What vanilla's addVisibleMessage builds: newest first, one line per part, endOfEntry on a message's last part. */
    static List<ChatHudLine.Visible> visible(List<ChatHudLine> messages, Predicate<Text> hidden) {
        List<ChatHudLine.Visible> out = new ArrayList<>();
        for (int i = messages.size() - 1; i >= 0; i--) { // oldest first, each added at the front
            ChatHudLine m = messages.get(i);
            if (hidden.test(m.content())) {
                continue;
            }
            String[] parts = m.content().getString().split("\\|");
            for (int j = 0; j < parts.length; j++) {
                out.addFirst(new ChatHudLine.Visible(0, Text.literal(parts[j]).asOrderedText(), null, j == parts.length - 1));
            }
        }
        return out;
    }

    static Predicate<Text> search(String query) {
        return t -> !t.getString().contains(query);
    }

    @Test
    void searchedLineCopiesItsOwnMessageNotANewerOne() {
        // newest first, like ChatHud.messages; luna 2026-10-28: right click in ctrl + f copied a recent message
        List<ChatHudLine> messages = List.of(message("hello"), message("fish sold"), message("hi"), message("bye"), message("fish caught"));
        Predicate<Text> hidden = search("fish");
        List<ChatHudLine.Visible> visible = visible(messages, hidden);
        assertEquals(2, visible.size());
        assertSame(messages.get(1), ChatCopy.messageOf(visible, messages, visible.get(0).content(), hidden));
        assertSame(messages.get(4), ChatCopy.messageOf(visible, messages, visible.get(1).content(), hidden));
    }

    @Test
    void everyLineOfAWrappedMessageFindsIt() {
        List<ChatHudLine> messages = List.of(message("a"), message("b1|b2|b3"), message("c"));
        List<ChatHudLine.Visible> visible = visible(messages, t -> false);
        for (int i = 1; i <= 3; i++) {
            assertSame(messages.get(1), ChatCopy.messageOf(visible, messages, visible.get(i).content(), t -> false));
        }
        assertSame(messages.get(2), ChatCopy.messageOf(visible, messages, visible.get(4).content(), t -> false));
    }

    @Test
    void lineThatIsNotInTheChatFindsNothing() {
        List<ChatHudLine> messages = List.of(message("a"));
        assertNull(ChatCopy.messageOf(visible(messages, t -> false), messages, Text.literal("x").asOrderedText(), t -> false));
    }

    @Test
    void scrollPutsTheMessageInTheMiddle() {
        assertEquals(40, ChatSearch.scrollToShow(50, 1, 20, 100));
        assertEquals(41, ChatSearch.scrollToShow(50, 3, 20, 100));
    }

    @Test
    void scrollStaysInsideTheChat() {
        assertEquals(0, ChatSearch.scrollToShow(3, 1, 20, 100)); // near the newest: no scroll
        assertEquals(80, ChatSearch.scrollToShow(98, 1, 20, 100)); // near the oldest: top of the chat
        assertEquals(0, ChatSearch.scrollToShow(5, 1, 20, 10)); // fewer lines than fit
    }

    @Test
    void jumpScrollFindsTheMessagesLines() {
        List<ChatHudLine> messages = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            messages.add(message(i == 30 ? "x|y|z" : "m" + i));
        }
        List<ChatHudLine.Visible> visible = visible(messages, t -> false);
        // message 30 starts 30 lines up and has 3 lines: 30 + 1 - 10
        assertEquals(21, ChatCopy.jumpScroll(visible, 30, 20));
        assertEquals(0, ChatCopy.jumpScroll(visible, 999, 20));
    }
}
