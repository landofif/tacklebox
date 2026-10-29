package dev.landofif.tacklebox.util;

import dev.landofif.tacklebox.Tacklebox;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Puts a png on the system clipboard with wl-copy (luna 2026-10-28: linux with wayland only is fine). The game can't do it
 * itself: glfw only copies text, and java's own clipboard is off in minecraft (it runs headless).
 */
public final class ImageClipboard {
    public static final List<String> COMMAND = List.of("wl-copy", "--type", "image/png");

    /** Runs a command with a file as its input and returns the exit code; swapped in tests. */
    public interface Runner {
        int run(List<String> command, Path input) throws IOException, InterruptedException;
    }

    public static Runner runner = ImageClipboard::exec;

    private ImageClipboard() {}

    /** False when wl-copy is missing, fails or hangs; the log says which. */
    public static boolean copy(Path png) {
        try {
            int code = runner.run(COMMAND, png);
            if (code != 0) {
                Tacklebox.LOGGER.info("screenshot: wl-copy exited with {}", code);
            }
            return code == 0;
        } catch (IOException e) {
            Tacklebox.LOGGER.info("screenshot: could not run wl-copy ({})", e.toString());
            return false;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /** wl-copy forks into the background to serve the clipboard, so the command itself returns at once. */
    static int exec(List<String> command, Path input) throws IOException, InterruptedException {
        Process p = new ProcessBuilder(command).redirectInput(input.toFile())
                .redirectOutput(ProcessBuilder.Redirect.DISCARD).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!p.waitFor(3, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            return -1;
        }
        return p.exitValue();
    }
}
