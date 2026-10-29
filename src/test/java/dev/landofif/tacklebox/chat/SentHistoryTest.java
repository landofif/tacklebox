package dev.landofif.tacklebox.chat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SentHistoryTest {
    @TempDir
    Path dir;

    @Test
    void roundTripKeepsOrderAndBothKinds() {
        Path f = dir.resolve("tacklebox/sent-history.txt");
        SentHistory.write(f, List.of("hi all", "/travel kenai", "anyone at kenai?"));
        assertEquals(List.of("hi all", "/travel kenai", "anyone at kenai?"), SentHistory.read(f));
    }

    @Test
    void keepsTheNewestHundred() {
        Path f = dir.resolve("h.txt");
        List<String> many = new ArrayList<>();
        for (int i = 0; i < 130; i++) {
            many.add("msg " + i);
        }
        SentHistory.write(f, many);
        List<String> back = SentHistory.read(f);
        assertEquals(100, back.size());
        assertEquals("msg 30", back.getFirst());
        assertEquals("msg 129", back.getLast());
    }

    @Test
    void noFileLeavesVanillaHistory() {
        List<String> history = new ArrayList<>(List.of("/sellall"));
        SentHistory.load(history, dir.resolve("missing.txt"));
        assertEquals(List.of("/sellall"), history);
    }

    @Test
    void blankLinesAreDropped() throws Exception {
        Path f = dir.resolve("h.txt");
        Files.writeString(f, "a\n\n  \nb\n");
        assertEquals(List.of("a", "b"), SentHistory.read(f));
    }
}
