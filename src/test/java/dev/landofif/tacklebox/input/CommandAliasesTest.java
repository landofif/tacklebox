package dev.landofif.tacklebox.input;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CommandAliasesTest {
    private static CommandAliases.Entry alias(String name, String command, boolean enabled) {
        CommandAliases.Entry e = new CommandAliases.Entry();
        e.name = name;
        e.command = command;
        e.enabled = enabled;
        return e;
    }

    @Test
    void replacesTheFirstWordAndKeepsTheArguments() {
        List<CommandAliases.Entry> a = List.of(alias("sb", "/sellall bait", true));
        assertEquals("sellall bait", CommandAliases.expand("sb", a));
        assertEquals("sellall bait now", CommandAliases.expand("sb now", a));
    }

    @Test
    void matchesCaseInsensitivelyAndIgnoresSlashesInTheEditor() {
        assertEquals("spawn", CommandAliases.expand("HOME", List.of(alias("/home", "spawn", true))));
    }

    @Test
    void leavesOtherCommandsDisabledAndEmptyAliasesAlone() {
        List<CommandAliases.Entry> a = List.of(alias("sb", "sellall", false), alias("", "x", true), alias("x", " ", true));
        assertEquals("sb", CommandAliases.expand("sb", a));
        assertEquals("sbx", CommandAliases.expand("sbx", List.of(alias("sb", "sellall", true))));
        assertEquals("x", CommandAliases.expand("x", a));
    }

    @Test
    void neverExpandsTwice() {
        List<CommandAliases.Entry> a = List.of(alias("a", "b", true), alias("b", "c", true));
        assertEquals("b", CommandAliases.expand("a", a));
    }
}
