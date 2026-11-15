package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Hide rules on the lines a mod adds to the chat itself (ChatHudMixin asks {@link ChatTweaks#allowAdded}). */
class ChatTweaksTest {
    /** luna's rule and foecollab's line of 2026-11-15, which stayed in chat. */
    private static final String RULE = "» You went * fish dry before catching a *";
    private static final String FOE_LINE = "FOE » You went 629 fish dry before catching a Pet";

    private List<ChatFilter> before;

    @BeforeEach
    void rules() {
        TackleboxSettings s = TackleboxSettings.get();
        before = s.chatFilters;
        s.chatFilters = new ArrayList<>(List.of(new ChatFilter(RULE, Trigger.MatchMode.SIMPLE)));
        TriggerEngine.assumeFishOnMc = true;
    }

    @AfterEach
    void restore() {
        TackleboxSettings.get().chatFilters = before;
        TriggerEngine.assumeFishOnMc = false;
    }

    @Test
    void aModsOwnLineIsHidden() {
        ChatFilter rule = TackleboxSettings.get().chatFilters.get(0);
        assertFalse(ChatTweaks.allowAdded(FOE_LINE));
        assertEquals(1, ChatTweaks.hidden(rule));
    }

    @Test
    void otherLinesStay() {
        assertTrue(ChatTweaks.allowAdded("tacklebox » Lynx Pet after 75 catches"));
        assertTrue(ChatTweaks.allowAdded("PET DROP! You pulled a [COMMON] Lynx Pet"));
    }

    @Test
    void aRuleSwitchedOffHidesNothing() {
        TackleboxSettings.get().chatFilters.get(0).enabled = false;
        assertTrue(ChatTweaks.allowAdded(FOE_LINE));
    }
}
