package dev.landofif.tacklebox.chat;

import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.*;

/** ctrl + f: search a long chat, pick a result, leave the search and land on that message. */
class ChatSearchJumpIntegrationTest {
    @Test
    void pickedResultIsOnScreenAfterTheJump() {
        List<ChatHudLine> messages = new ArrayList<>(); // newest first
        for (int i = 0; i < 300; i++) {
            messages.add(ChatCopyTest.message(i % 50 == 7 ? "Steelhead caught " + i + "|second line" : "chatter " + i));
        }
        Predicate<Text> hidden = ChatCopyTest.search("Steelhead");
        List<ChatHudLine.Visible> searched = ChatCopyTest.visible(messages, hidden);
        // the third result from the bottom, its second line
        ChatHudLine picked = ChatCopy.messageOf(searched, messages, searched.get(5).content(), hidden);
        assertNotNull(picked);
        assertEquals("Steelhead caught 107|second line", picked.content().getString());

        List<ChatHudLine.Visible> all = ChatCopyTest.visible(messages, t -> false);
        int shown = 20;
        int scroll = ChatCopy.jumpScroll(all, messages.indexOf(picked), shown);
        // the lines on screen after the jump contain both lines of the picked message
        List<String> onScreen = all.subList(scroll, scroll + shown).stream()
                .map(v -> { StringBuilder b = new StringBuilder(); v.content().accept((i, s, c) -> { b.appendCodePoint(c); return true; }); return b.toString(); })
                .toList();
        assertTrue(onScreen.contains("Steelhead caught 107"), onScreen.toString());
        assertTrue(onScreen.contains("second line"));
    }
}
