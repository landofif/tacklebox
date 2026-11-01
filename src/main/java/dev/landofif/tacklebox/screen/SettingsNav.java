package dev.landofif.tacklebox.screen;

/**
 * Where {@link SettingsScreen} is: a page, and on it either the grid of tiles or one tile opened. Esc and back close the
 * tile first, a page change always lands on the grid.
 */
final class SettingsNav {
    int page, open = -1;

    boolean opened() {
        return open >= 0;
    }

    void open(int tile) {
        open = tile;
    }

    /** Back to the grid; false when it was there already (esc then closes the screen). */
    boolean back() {
        if (open < 0) {
            return false;
        }
        open = -1;
        return true;
    }

    void page(int to) {
        page = to;
        open = -1;
    }

    static int cols(int width) {
        return width >= 760 ? 4 : width >= 540 ? 3 : width >= 340 ? 2 : 1;
    }

    /** Lays {@code n} tiles of height {@code h} out in rows across {@code width}; returns the height they take. */
    static int grid(int n, int width, int gap, int h, int[] tx, int[] ty, int[] tw) {
        int cols = cols(width);
        int colW = (width - gap * (cols - 1)) / cols;
        for (int i = 0; i < n; i++) {
            tx[i] = (i % cols) * (colW + gap);
            ty[i] = (i / cols) * (h + gap);
            tw[i] = colW;
        }
        int rows = (n + cols - 1) / cols;
        return Math.max(0, rows * (h + gap) - gap);
    }
}
