package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.TackleboxSettings;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * The pet price table (luna, 2026-11-15) from the FishOnMC community's pet price sheet, a Google Sheet: rating bands down
 * the side, rarities across, a "Max Scale/Luck" column and notes. Downloaded as CSV at most once a day and kept on disk
 * ({@code config/tacklebox/petprices.csv}), so a restart does not download it again. The cells are shown exactly as the
 * sheet writes them. The screen, the once a day rule and the sheet come from FOE+ (itshaxr/FOE-Plus, PetPriceSheetHandler),
 * which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow); no code was copied.
 */
public final class PetPrices {
    /** The sheet, for the credit on the screen. */
    public static final String SHEET = "docs.google.com/spreadsheets/d/1mmOt9-hvCfT3flCekELM7F3FcOrbygwKGOrNyLwA2dk";
    private static final String CSV = "https://" + SHEET + "/export?format=csv&gid=0";
    /** A good copy is kept this long; after a failed download the next try waits {@link #RETRY}. */
    public static final long DAY = 24 * 60 * 60_000L, RETRY = 10 * 60_000L;
    private static final int MAX_BYTES = 200_000;

    private static List<List<String>> table;
    private static long failedAt;
    private static boolean busy;

    private PetPrices() {}

    // ---------------------------------------------------------------- pure

    /** A CSV text as rows of cells: quoted cells with commas, doubled quotes and line breaks, CRLF or LF. */
    public static List<List<String>> parse(String csv) {
        List<List<String>> rows = new ArrayList<>();
        List<String> row = new ArrayList<>();
        StringBuilder cell = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < csv.length(); i++) {
            char c = csv.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < csv.length() && csv.charAt(i + 1) == '"') {
                    cell.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    cell.append(c);
                }
            } else if (c == '"') {
                quoted = true;
            } else if (c == ',') {
                row.add(cell.toString());
                cell.setLength(0);
            } else if (c == '\n') {
                row.add(cell.toString());
                cell.setLength(0);
                rows.add(row);
                row = new ArrayList<>();
            } else if (c != '\r') {
                cell.append(c);
            }
        }
        if (cell.length() > 0 || !row.isEmpty()) {
            row.add(cell.toString());
            rows.add(row);
        }
        return rows;
    }

    /**
     * The rows as a table to show: every row as wide as the widest, the spaces around a cell gone, columns that are empty in
     * every row dropped, empty rows kept as gaps between the others (not at the top or the bottom). The text of a cell is
     * not changed.
     */
    public static List<List<String>> table(List<List<String>> rows) {
        int cols = rows.stream().mapToInt(List::size).max().orElse(0);
        List<Integer> keep = new ArrayList<>();
        for (int c = 0; c < cols; c++) {
            int col = c;
            if (rows.stream().anyMatch(r -> col < r.size() && !r.get(col).isBlank())) {
                keep.add(c);
            }
        }
        List<List<String>> out = new ArrayList<>();
        for (List<String> r : rows) {
            List<String> line = new ArrayList<>();
            for (int c : keep) {
                line.add(c < r.size() ? r.get(c).strip() : "");
            }
            boolean empty = line.stream().allMatch(String::isEmpty);
            if (!empty || (!out.isEmpty() && !out.get(out.size() - 1).stream().allMatch(String::isEmpty))) {
                out.add(line);
            }
        }
        while (!out.isEmpty() && out.get(out.size() - 1).stream().allMatch(String::isEmpty)) {
            out.remove(out.size() - 1);
        }
        return out;
    }

    /** Whether a download is the sheet and not an error or sign-in page: no markup, at least two rows of at least two cells. */
    public static boolean looksRight(String body) {
        if (body == null || body.isBlank() || body.stripLeading().startsWith("<")) {
            return false;
        }
        return table(parse(body)).stream().filter(r -> r.stream().filter(c -> !c.isEmpty()).count() >= 2).count() >= 2;
    }

    /** Whether to download now: no good copy of the last day, and no failed try in the last ten minutes. */
    public static boolean due(long goodAt, long failedAt, long now) {
        return now - goodAt >= DAY && (failedAt == 0 || now - failedAt >= RETRY);
    }

    /** How old the table is, for the screen: "updated 3h ago", "updated 2d ago", "updated just now". */
    public static String age(long goodAt, long now) {
        long min = Math.max(0, now - goodAt) / 60_000;
        return "updated " + (min < 1 ? "just now" : min < 60 ? min + "m ago" : min < 48 * 60 ? min / 60 + "h ago" : min / 1440 + "d ago");
    }

    // ---------------------------------------------------------------- disk and download

    static List<List<String>> load(Path file) {
        try {
            return Files.isRegularFile(file) ? table(parse(Files.readString(file, StandardCharsets.UTF_8))) : List.of();
        } catch (IOException e) {
            return List.of();
        }
    }

    static void store(Path file, String body) throws IOException {
        Files.createDirectories(file.getParent());
        Path tmp = file.resolveSibling(file.getFileName() + ".tmp");
        Files.writeString(tmp, body, StandardCharsets.UTF_8);
        Files.move(tmp, file, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
    }

    private static Path file() {
        return TackleboxSettings.dir().resolve("petprices.csv");
    }

    /** The table as last downloaded; empty when there is none yet. */
    public static synchronized List<List<String>> table() {
        if (table == null) {
            table = load(file());
        }
        return table;
    }

    public static synchronized boolean busy() {
        return busy;
    }

    public static synchronized boolean failed() {
        return failedAt != 0;
    }

    /** Called when the screen opens: downloads the sheet in the background when {@link #due} says so. */
    public static synchronized void refresh(long now) {
        TackleboxSettings s = TackleboxSettings.get();
        boolean have = !table().isEmpty();
        if (busy || !due(have ? s.petPricesAt : 0, failedAt, now)) {
            return;
        }
        busy = true;
        HttpClient http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).connectTimeout(Duration.ofSeconds(10)).build();
        HttpRequest request = HttpRequest.newBuilder(URI.create(CSV)).timeout(Duration.ofSeconds(20)).GET().build();
        http.sendAsync(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8)).whenComplete((response, error) -> {
            String body = error == null && response.statusCode() == 200 ? response.body() : null;
            done(body != null && body.length() <= MAX_BYTES && looksRight(body) ? body : null, System.currentTimeMillis());
        });
    }

    private static synchronized void done(String body, long now) {
        busy = false;
        if (body == null) {
            failedAt = now;
            Tacklebox.LOGGER.warn("Could not download the pet price sheet; trying again in ten minutes");
            return;
        }
        try {
            store(file(), body);
        } catch (IOException e) {
            Tacklebox.LOGGER.error("Could not save the pet price sheet", e);
        }
        table = table(parse(body));
        failedAt = 0;
        TackleboxSettings.get().petPricesAt = now;
        TackleboxSettings.save();
    }
}
