package dev.landofif.tacklebox.chat;

import com.mojang.serialization.JsonOps;
import net.minecraft.text.Text;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/** A session's chat and boxes are saved on disconnect and read back in the same order on the next join. */
class ChatRestoreIntegrationTest {
    /** Vanilla's text codec needs the game's registries, which do not start in these tests: words only. */
    @BeforeAll
    static void plainCodec() {
        ChatRestore.codec = () -> com.mojang.serialization.Codec.STRING.xmap(Text::literal, Text::getString);
    }

    @TempDir
    Path dir;

    @Test
    void sessionComesBack() {
        List<ChatRestore.Saved> chat = List.of(
                new ChatRestore.Saved(ChatHighlight.apply(Text.literal("[3] bob » hey luna"), "luna", true, List.of()), 1000),
                new ChatRestore.Saved(Text.literal("SHOP > sold 4 fish for $20 (x2)"), 2000));
        Map<String, List<ChatRestore.Saved>> boxes = new LinkedHashMap<>();
        boxes.put("dms", List.of(new ChatRestore.Saved(Text.literal("✉ from amy: gg"), 1500)));
        boxes.put("crew", List.of(new ChatRestore.Saved(Text.literal("CREWS > amy joined"), 1600)));
        Path f = dir.resolve("tacklebox/chat-restore.json");
        ChatRestore.write(f, new ChatRestore.Snapshot(3000, chat, boxes), JsonOps.INSTANCE);

        ChatRestore.Snapshot back = ChatRestore.read(f, JsonOps.INSTANCE);
        assertEquals(List.of("[3] bob » hey luna", "SHOP > sold 4 fish for $20 (x2)"),
                back.chat().stream().map(s -> s.text().getString()).toList());
        assertEquals(List.of("dms", "crew"), List.copyOf(back.boxes().keySet()));
        assertEquals(1500, back.boxes().get("dms").getFirst().at());
    }
}
