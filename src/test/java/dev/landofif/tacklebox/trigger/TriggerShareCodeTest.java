package dev.landofif.tacklebox.trigger;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.*;

class TriggerShareCodeTest {
    private static Trigger sample() {
        Trigger t = new Trigger();
        t.name = "quest done";
        t.pattern = "quest complete";
        return t;
    }

    @Test
    void newCodesStartWithTheNewNameAndComeBack() {
        String code = TriggerStore.exportCode(sample());
        assertTrue(code.startsWith("tacklebox-trigger:"));
        Trigger back = TriggerStore.importCode("look at this " + code + " nice");
        assertNotNull(back);
        assertEquals("quest done", back.name);
        assertEquals("quest complete", back.pattern);
    }

    @Test
    void codesMadeBeforeTheRenameStillImport() {
        String json = new com.google.gson.Gson().toJson(sample());
        String old = "dofi-trigger:" + Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8));
        Trigger back = TriggerStore.importCode(old);
        assertNotNull(back);
        assertEquals("quest done", back.name);
    }

    @Test
    void textWithoutACodeGivesNull() {
        assertNull(TriggerStore.importCode("tacklebox is a mod"));
        assertNull(TriggerStore.importCode(null));
    }
}
