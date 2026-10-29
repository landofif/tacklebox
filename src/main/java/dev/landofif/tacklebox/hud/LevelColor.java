package dev.landofif.tacklebox.hud;

/** The colour of a fishing level number: the level bands foecollab's top bar shows, grey at level 0 and white from 300 on. */
public final class LevelColor {
    private static final int[] FROM = {0, 5, 10, 15, 20, 35, 50, 65, 80, 95, 110, 125, 140, 155, 170, 185, 200, 210, 220, 230, 240, 250, 260, 270, 280, 290, 300};
    private static final int[] RGB = {0x8A897E, 0x868A6E, 0x868A6E, 0x868A6E, 0xBBCF53, 0xE8D41E, 0xE8A81E, 0x7FD223, 0x23F133, 0x1EE8B2, 0x1EB6E8, 0x1958E9, 0x411DE8, 0x9B1EE8, 0xBE12D2, 0xE81ED4, 0xE81E61, 0xAC1648, 0x83232B, 0x71316B, 0xA00950, 0xD03EC2, 0xE675DB, 0xDE9DD8, 0xE4C1E1, 0xE1C9DF, 0xFFFEFA};

    private LevelColor() {}

    /** Opaque ARGB colour for the level. */
    public static int of(int level) {
        int i = FROM.length - 1;
        while (i > 0 && level < FROM[i]) {
            i--;
        }
        return 0xFF000000 | RGB[i];
    }
}
