package dev.landofif.tacklebox.chat;

import net.minecraft.util.collection.ArrayListDeque;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Send a message and a command, restart (vanilla loads only commands), the up arrow has both. */
class SentHistoryIntegrationTest {
    @TempDir
    Path dir;

    @Test
    void messagesSurviveARestart() {
        Path f = dir.resolve("sent-history.txt");
        ArrayListDeque<String> session = new ArrayListDeque<>(100);
        session.addLast("/travel kenai");
        SentHistory.write(f, session);
        session.addLast("gg");
        SentHistory.write(f, session);

        ArrayListDeque<String> restarted = new ArrayListDeque<>(100);
        restarted.addLast("/travel kenai"); // what vanilla reads from command_history.txt
        SentHistory.load(restarted, f);
        assertEquals(List.of("/travel kenai", "gg"), List.copyOf(restarted));
    }
}
