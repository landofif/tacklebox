package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.TackleboxSettings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * The up arrow history of what you sent, messages and commands, kept in a file so it is still
 * there after a restart or a reconnect. Vanilla keeps only commands (command_history.txt) and
 * reloads only those when it clears chat on disconnect. Hooked in ChatHudMixin.
 */
public final class SentHistory {
    private static final Logger LOG = LoggerFactory.getLogger("tacklebox");
    /** Vanilla's up arrow history holds 100. */
    static final int KEEP = 100;

    private SentHistory() {}

    private static Path file() {
        return TackleboxSettings.dir().resolve("sent-history.txt");
    }

    /** Puts the saved history in place of vanilla's; leaves vanilla's when nothing is saved yet. */
    public static void load(Collection<String> history) {
        load(history, file());
    }

    static void load(Collection<String> history, Path path) {
        List<String> saved = read(path);
        if (!saved.isEmpty()) {
            history.clear();
            history.addAll(saved);
        }
    }

    public static void save(Collection<String> history) {
        write(file(), history);
    }

    static List<String> read(Path path) {
        try {
            if (!Files.exists(path)) {
                return List.of();
            }
            List<String> lines = new ArrayList<>(Files.readAllLines(path, StandardCharsets.UTF_8));
            lines.removeIf(String::isBlank);
            return lines.size() > KEEP ? lines.subList(lines.size() - KEEP, lines.size()) : lines;
        } catch (IOException e) {
            LOG.warn("could not read {}", path, e);
            return List.of();
        }
    }

    static void write(Path path, Collection<String> history) {
        List<String> lines = new ArrayList<>(history);
        lines.removeIf(s -> s == null || s.isBlank() || s.contains("\n"));
        if (lines.size() > KEEP) {
            lines = lines.subList(lines.size() - KEEP, lines.size());
        }
        try {
            Files.createDirectories(path.getParent());
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOG.warn("could not write {}", path, e);
        }
    }
}
