package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.input.CommandAliases;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Aliases saved in settings.json come back through load and sanitize and expand, even when the file is damaged. */
class CommandAliasIntegrationTest {
    @Test
    void savedAliasesSurviveALoadAndExpand() {
        String json = """
                {"aliases": [null, {"name": "sb", "command": "/sellall bait"}, {"name": null, "command": null}]}
                """;
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        assertEquals(2, s.aliases.size());
        assertEquals("sellall bait 5", CommandAliases.expand("sb 5", s.aliases));
        assertEquals("pv 1", CommandAliases.expand("pv 1", s.aliases));
    }
}
