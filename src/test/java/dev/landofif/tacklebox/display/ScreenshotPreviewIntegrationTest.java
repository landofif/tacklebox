package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.util.ImageClipboard;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.*;

/** A saved screenshot goes to the clipboard, its card says so, and a left click on open in a screen opens that file (luna 2026-10-28). */
class ScreenshotPreviewIntegrationTest {
    private static final int SCREEN_W = 480;
    private static final Consumer<Path> OPENER = ScreenshotPreview.opener;
    private static final LongSupplier CLOCK = ScreenshotPreview.clock;
    private static final ImageClipboard.Runner RUNNER = ImageClipboard.runner;

    private long now = 5_000_000;
    private final List<Path> copied = new ArrayList<>(), opened = new ArrayList<>();

    @AfterEach
    void restore() {
        ScreenshotPreview.opener = OPENER;
        ScreenshotPreview.clock = CLOCK;
        ImageClipboard.runner = RUNNER;
    }

    private void fake(int exitCode) {
        ScreenshotPreview.clock = () -> now;
        ScreenshotPreview.opener = opened::add;
        ImageClipboard.runner = (command, input) -> {
            copied.add(input);
            return exitCode;
        };
    }

    @Test
    void savedThenCopiedThenOpenedFromChat() {
        fake(0);
        Path file = Path.of("screenshots", "2026-10-28_14.03.11.png");
        ScreenshotPreview.show(file, 2560, 1440);
        assertEquals(List.of(file), copied);
        ScreenshotCard card = ScreenshotPreview.card();
        assertEquals(file, card.file());
        assertTrue(card.copied());

        now += 1000;
        int[] b = card.button(SCREEN_W, now);
        assertFalse(ScreenshotPreview.click(b[0] + 1, b[1] + 1, 1, SCREEN_W), "right click is left to the screen");
        assertFalse(ScreenshotPreview.click(b[0] - 20, b[1] - 20, 0, SCREEN_W), "a click on the picture is left to the screen");
        assertTrue(opened.isEmpty());
        assertTrue(ScreenshotPreview.click(b[0] + 1, b[1] + 1, 0, SCREEN_W));
        assertEquals(List.of(file), opened);
    }

    @Test
    void failedCopyStillShowsAndANewScreenshotReplacesIt() {
        fake(1);
        ScreenshotPreview.show(Path.of("first.png"), 1920, 1080);
        assertFalse(ScreenshotPreview.card().copied());
        now += 2000;
        ScreenshotPreview.show(Path.of("second.png"), 1920, 1080);
        assertEquals(Path.of("second.png"), ScreenshotPreview.card().file());

        now += ScreenshotCard.SLIDE_MS * 2 + ScreenshotCard.SHOW_MS; // the second card is gone too
        int[] b = ScreenshotPreview.card().button(SCREEN_W, now);
        assertFalse(ScreenshotPreview.click(b[0] + 1, b[1] + 1, 0, SCREEN_W));
        assertTrue(opened.isEmpty());
    }
}
