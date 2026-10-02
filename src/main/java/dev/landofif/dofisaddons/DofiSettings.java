package dev.landofif.dofisaddons;

/** Small global settings. Saved to {@code config/dofisaddons/settings.json}. */
public class DofiSettings {
    /** Stop the "time fishing" clock after this many seconds without a catch. */
    public int pauseAfterSeconds = 120;

    /** Hide the vanilla XP bar and level number on FishOnMC (the level HUD replaces them). */
    public boolean hideVanillaXp = true;

    public BobberMode bobberTimer = BobberMode.ABOVE_BOBBER;
    /** Turn the timer text red after this many seconds (0 = never). */
    public double bobberSlowSeconds = 20;

    // ---- bobber looks (only you see these) ----
    public BobberSkin bobberSkin = BobberSkin.DEFAULT;
    /** Also show your skin on other players' bobbers. */
    public boolean skinEveryone = false;
    public boolean customLineColor = false;
    /** RGB of your own fishing line. */
    public int lineColor = 0x3FA9F5;

    // ---- viewmodel (held item) ----
    public boolean viewmodel = false;
    public double vmScale = 1.0;
    public double vmX = 0;
    public double vmY = 0;
    public double vmZ = 0;
    public double vmRotX = 0;
    public double vmRotY = 0;
    public double vmRotZ = 0;
    /** Skip the dip when you switch items. */
    public boolean vmNoEquipDip = false;
    public SwingStyle swingStyle = SwingStyle.NORMAL;
    /** 1 = normal, 2 = twice as slow, 0.5 = twice as fast. Only the animation changes. */
    public double swingSlowness = 1.0;

    public enum BobberSkin {
        DEFAULT("default", null), OCEAN("ocean", "ocean"), GOLD("gold", "gold"), EMERALD("emerald", "emerald"),
        MIDNIGHT("midnight", "midnight"), CANDY("candy", "candy"), DUCK("duck", "duck"), EMBER("ember", "ember"),
        PEARL("pearl", "pearl"), STAR("star", "star"), RAINBOW("rainbow", "rainbow");

        public final String label;
        public final String texture;

        BobberSkin(String label, String texture) {
            this.label = label;
            this.texture = texture;
        }
    }

    public enum SwingStyle {
        NORMAL("normal"), SMOOTH("smooth"), SNAPPY("snappy"), NONE("no swing");

        public final String label;

        SwingStyle(String label) {
            this.label = label;
        }
    }

    public enum BobberMode {
        ABOVE_BOBBER("above bobber"), UNDER_CROSSHAIR("under crosshair"), OFF("off");

        public final String label;

        BobberMode(String label) {
            this.label = label;
        }

        public BobberMode next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public void sanitize() {
        if (bobberTimer == null) bobberTimer = BobberMode.ABOVE_BOBBER;
        if (bobberSkin == null) bobberSkin = BobberSkin.DEFAULT;
        if (swingStyle == null) swingStyle = SwingStyle.NORMAL;
        lineColor &= 0xFFFFFF;
        vmScale = Math.max(0.1, Math.min(3, vmScale));
        swingSlowness = Math.max(0.25, Math.min(4, swingSlowness));
        pauseAfterSeconds = Math.max(10, Math.min(1800, pauseAfterSeconds));
        bobberSlowSeconds = Math.max(0, Math.min(120, bobberSlowSeconds));
    }
}
