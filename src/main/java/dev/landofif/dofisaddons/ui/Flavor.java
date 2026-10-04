package dev.landofif.dofisaddons.ui;

/**
 * The system24 flavors from refact0r/system24 (MIT, theme/flavors/*.theme.css), converted to sRGB from
 * their oklch / hsl / hex values by the same rules as {@link S24}: text-1..5, bg-1..4, the border colour
 * (--border laid over bg-4), accent-2 for hover borders and accent-4 for dim accents. The light and auto
 * flavors are left out: minecraft draws white text in places the mod doesnt control, which would
 * vanish on a light background. hudPanel and hudBorder are the custom hud box colours (their alpha comes
 * from each huds background setting).
 */
public enum Flavor {
    SYSTEM24("system24",
            0xFFEEEEEE, 0xFFCECECE, 0xFFAEAEAE, 0xFF808080, 0xFF484848,
            0xFF303030, 0xFF262626, 0xFF1D1D1D, 0xFF141414, 0xFF2A2A2A, 0xFFB589D6, 0xFF966AB5, 0x0E0F12, 0x4E525C),
    /** caelestia shells material 3 dark scheme (the m3 baseline tones it ships with), not system24. */
    CAELESTIA("caelestia",
            0xFFE6E0E9, 0xFFCAC4D0, 0xFFAEA9B4, 0xFF938F99, 0xFF49454F,
            0xFF36343B, 0xFF2B2930, 0xFF211F26, 0xFF141218, 0xFF2B2930, 0xFFD0BCFF, 0xFF9A82DB, 0x1D1B20, 0x49454F),
    CATPPUCCIN_MOCHA("catppuccin mocha",
            0xFFEAEEFA, 0xFFCDD6F4, 0xFFBAC2DE, 0xFF7F849C, 0xFF585B70,
            0xFF45475A, 0xFF313244, 0xFF181825, 0xFF1E1E2E, 0xFF2F3043, 0xFFCAA6F7, 0xFFBD8EF5, 0x181825, 0x585B70),
    CATPPUCCIN_MACCHIATO("catppuccin macchiato",
            0xFFEAEDFB, 0xFFCAD3F5, 0xFFB8C0E0, 0xFF8087A2, 0xFF5B6078,
            0xFF494D64, 0xFF363A4F, 0xFF1E2030, 0xFF24273A, 0xFF34384D, 0xFFC8A2F6, 0xFFBA8AF4, 0x1E2030, 0x5B6078),
    NORD("nord",
            0xFFECEFF4, 0xFFE5E9F0, 0xFFD8DEE9, 0xFF9AA5BC, 0xFF4C566A,
            0xFF434C5E, 0xFF3B4252, 0xFF272B35, 0xFF2E3440, 0xFF343B49, 0xFF88C0D0, 0xFF74B6C8, 0x272B35, 0x4C566A),
    TOKYO_NIGHT("tokyo night",
            0xFFE9ECFC, 0xFFC0CAF5, 0xFFA9B1D6, 0xFF717AA2, 0xFF414868,
            0xFF32364E, 0xFF262A3B, 0xFF16161D, 0xFF1A1B26, 0xFF292D3E, 0xFF78A0F7, 0xFF537FDF, 0x16161D, 0x414868),
    ROSE_PINE("rose pine",
            0xFFEDECF9, 0xFFDEDDF4, 0xFFC0BDDB, 0xFF918DAA, 0xFF6E6986,
            0xFF322E4C, 0xFF252239, 0xFF201D2F, 0xFF1A1825, 0xFF262336, 0xFFEBBCBA, 0xFFE6AAA8, 0x201D2F, 0x6E6986),
    ROSE_PINE_MOON("rose pine moon",
            0xFFEDECF9, 0xFFDEDDF4, 0xFFC0BDDB, 0xFF918DAA, 0xFF6E6986,
            0xFF454063, 0xFF383450, 0xFF2A273F, 0xFF232136, 0xFF302D47, 0xFFE99895, 0xFFE58380, 0x2A273F, 0x6E6986),
    EVERFOREST("everforest",
            0xFFEADDC0, 0xFFD3C6AA, 0xFFD3C6AA, 0xFFAA9D82, 0xFF435148,
            0xFF3C474C, 0xFF343F44, 0xFF232A2E, 0xFF2C343A, 0xFF3C474C, 0xFFA6BF7F, 0xFF738A4C, 0x232A2E, 0x435148),
    VENCORD("vencord",
            0xFFF1E9DA, 0xFFE4D2B4, 0xFFD4BE98, 0xFFA79A83, 0xFF6D6455,
            0xFF433F3D, 0xFF3C3836, 0xFF31302F, 0xFF282828, 0xFF353434, 0xFFD3869B, 0xFFCD748C, 0x31302F, 0x6D6455);

    public final String label;
    public final int text1, text2, text3, text4, text5;
    public final int bg1, bg2, bg3, bg4, border, accent, accentDim;
    public final int hudPanel, hudBorder;

    Flavor(String label, int text1, int text2, int text3, int text4, int text5,
           int bg1, int bg2, int bg3, int bg4, int border, int accent, int accentDim, int hudPanel, int hudBorder) {
        this.label = label;
        this.text1 = text1;
        this.text2 = text2;
        this.text3 = text3;
        this.text4 = text4;
        this.text5 = text5;
        this.bg1 = bg1;
        this.bg2 = bg2;
        this.bg3 = bg3;
        this.bg4 = bg4;
        this.border = border;
        this.accent = accent;
        this.accentDim = accentDim;
        this.hudPanel = hudPanel;
        this.hudBorder = hudBorder;
    }
}
