package dev.landofif.tacklebox.screen;

/**
 * Where {@link SettingsScreen} is: a page and the group picked on it. Every page remembers its group, so coming back to a
 * page shows what was open there; a page seen for the first time shows its first group.
 */
final class SettingsNav {
    /** Screens narrower than this show the pages as icons only. */
    static final int NARROW = 520;
    int page;
    private final int[] group;

    SettingsNav(int pages) {
        group = new int[pages];
    }

    int group() {
        return group[page];
    }

    void group(int g) {
        group[page] = g;
    }

    /** False when {@code to} is not a page or is the page already shown. */
    boolean page(int to) {
        if (to < 0 || to >= group.length || to == page) {
            return false;
        }
        page = to;
        return true;
    }

    /**
     * The three columns for a screen {@code width} wide, centred with {@code gap} between them: the left edge, then the
     * widths of the pages, the groups and the settings.
     */
    static int[] columns(int width, int gap) {
        int total = Math.min(width - 20, 760);
        int side = width >= NARROW ? 120 : 28;
        int groups = total >= 640 ? 190 : total >= 420 ? 150 : 110;
        return new int[]{(width - total) / 2, side, groups, total - side - groups - 2 * gap};
    }
}
