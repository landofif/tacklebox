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

    public enum BobberMode {
        ABOVE_BOBBER("Above bobber"), UNDER_CROSSHAIR("Under crosshair"), OFF("Off");

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
        pauseAfterSeconds = Math.max(10, Math.min(1800, pauseAfterSeconds));
        bobberSlowSeconds = Math.max(0, Math.min(120, bobberSlowSeconds));
    }
}
