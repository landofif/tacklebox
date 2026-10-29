package dev.landofif.tacklebox.chat;

import net.minecraft.client.input.KeyInput;
import org.junit.jupiter.api.Test;
import org.lwjgl.glfw.GLFW;

import static org.junit.jupiter.api.Assertions.*;

class ChatKeysTest {
    @Test
    void escapeSkipsTheSuggestions() {
        assertFalse(ChatKeys.toSuggestions(new KeyInput(GLFW.GLFW_KEY_ESCAPE, 0, 0)));
        assertTrue(ChatKeys.toSuggestions(new KeyInput(GLFW.GLFW_KEY_TAB, 0, 0)));
        assertTrue(ChatKeys.toSuggestions(new KeyInput(GLFW.GLFW_KEY_UP, 0, 0)));
    }

    @Test
    void hereBecomesLocationAndInstance() {
        ChatKeys.Edit e = ChatKeys.here("meet me here", 12, "Kenai River", "3");
        assertEquals("meet me Kenai River (instance 3)", e.text());
        assertEquals(e.text().length(), e.cursor());
    }

    @Test
    void outsideAnInstanceOnlyTheLocation() {
        assertEquals("Kenai River", ChatKeys.here("here", 4, "Kenai River", "").text());
    }

    @Test
    void onlyAWholeWordBeforeTheCursor() {
        assertNull(ChatKeys.here("there", 5, "Kenai River", "3"));
        assertNull(ChatKeys.here("here we go", 10, "Kenai River", "3"));
        assertNull(ChatKeys.here("hello", 5, "Kenai River", "3"));
        assertEquals("/cc Cairns ok", ChatKeys.here("/cc HERE ok", 8, "Cairns", "").text()); // cursor in the middle
        assertEquals(10, ChatKeys.here("/cc HERE ok", 8, "Cairns", "").cursor());
    }

    @Test
    void nothingWithoutALocation() {
        assertNull(ChatKeys.here("here", 4, null, "3"));
        assertNull(ChatKeys.here("here", 4, "", "3"));
    }

    @Test
    void instanceFromTheFooter() {
        assertEquals("3", ChatKeys.instanceOf("play.fishonmc.net\nɪɴꜱᴛᴀɴᴄᴇ #3 (12 players)"));
        assertEquals("", ChatKeys.instanceOf("play.fishonmc.net"));
        assertEquals("", ChatKeys.instanceOf(null));
    }
}
