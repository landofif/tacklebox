package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.chat.ChatKeys;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** A boss bar line and a tab list footer as the server sends them, then "here" and Tab in chat. */
class ChatHereIntegrationTest {
    @Test
    void bossBarAndFooterToChat() {
        assertTrue(ServerInfo.readBar("☀ 6:40 AM 21° Lofoten Islands"));
        String instance = ChatKeys.instanceOf("ɪɴꜱᴛᴀɴᴄᴇ 7 (lofoten)");
        ChatKeys.Edit e = ChatKeys.here("come to here", 12, ServerInfo.LOCATIONS.get(ServerInfo.locationId), instance);
        assertEquals("come to Lofoten Islands (instance 7)", e.text());
    }
}
