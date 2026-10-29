package dev.landofif.tacklebox.chat;

import com.mojang.serialization.JsonOps;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ChatRestoreTest {
    /** Vanilla's text codec needs the game's registries, which do not start in these tests: words only. */
    @BeforeAll
    static void plainCodec() {
        ChatRestore.codec = () -> com.mojang.serialization.Codec.STRING.xmap(Text::literal, Text::getString);
    }

    @TempDir
    Path dir;

    @Test
    void linesKeepWordsAndTimes() {
        Text line = Text.literal("bob » ").formatted(Formatting.AQUA)
                .append(Text.literal("hi").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Text.literal("sent 12:00")))));
        Path f = dir.resolve("chat-restore.json");
        ChatRestore.write(f, new ChatRestore.Snapshot(5000, List.of(new ChatRestore.Saved(line, 1234)), Map.of()), JsonOps.INSTANCE);
        ChatRestore.Snapshot back = ChatRestore.read(f, JsonOps.INSTANCE);
        assertEquals(5000, back.savedAt());
        assertEquals(1, back.chat().size());
        assertEquals(line.getString(), back.chat().getFirst().text().getString());
        assertEquals(1234, back.chat().getFirst().at());
    }

    @Test
    void noFileOrABrokenOneIsNothing() throws Exception {
        assertNull(ChatRestore.read(dir.resolve("missing.json"), JsonOps.INSTANCE));
        Path f = dir.resolve("broken.json");
        Files.writeString(f, "{not json");
        assertNull(ChatRestore.read(f, JsonOps.INSTANCE));
    }

    @Test
    void dividerShowsTheDate() {
        long t = java.time.LocalDateTime.of(2026, 10, 29, 14, 32).toInstant(ZoneOffset.UTC).toEpochMilli();
        assertEquals("── 29 oct 14:32 ──", ChatRestore.divider(t, ZoneOffset.UTC).getString()
                .replace("ᴏ", "o").replace("ᴄ", "c").replace("ᴛ", "t")); // Foe.text writes small capitals
    }
}
