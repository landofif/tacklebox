package dev.landofif.tacklebox.util;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ImageClipboardTest {
    @TempDir
    Path dir;

    @AfterEach
    void restore() {
        ImageClipboard.runner = ImageClipboard::exec;
    }

    @Test
    void runsWlCopyAsPngWithTheFileAsInput() {
        List<Object> got = new ArrayList<>();
        ImageClipboard.runner = (command, input) -> {
            got.add(command);
            got.add(input);
            return 0;
        };
        Path png = dir.resolve("shot.png");
        assertTrue(ImageClipboard.copy(png));
        assertEquals(List.of(List.of("wl-copy", "--type", "image/png"), png), got);
    }

    @Test
    void failsWithoutCrashing() {
        ImageClipboard.runner = (command, input) -> 1; // no wayland session
        assertFalse(ImageClipboard.copy(dir.resolve("shot.png")));
        ImageClipboard.runner = (command, input) -> {
            throw new IOException("Cannot run program \"wl-copy\": error=2, No such file or directory");
        };
        assertFalse(ImageClipboard.copy(dir.resolve("shot.png")));
    }

    @Test
    void realProcessGetsTheFileOnItsInput() throws Exception {
        Path in = Files.writeString(dir.resolve("in.txt"), "png bytes");
        assertEquals(0, ImageClipboard.exec(List.of("cmp", "-s", "-", in.toString()), in), "the file is the command's input");
        assertNotEquals(0, ImageClipboard.exec(List.of("false"), in));
        assertThrows(IOException.class, () -> ImageClipboard.exec(List.of("tacklebox-no-such-tool"), in));
    }
}
