package dev.landofif.dofisaddons.trigger;

import dev.landofif.dofisaddons.util.RichText;

import java.util.UUID;

/**
 * One chat trigger: "when a chat line like X appears, show this title and play this sound".
 * Plain fields so Gson can save it to {@code config/dofisaddons/triggers.json} as-is.
 */
public class Trigger {
    public String id = UUID.randomUUID().toString();
    public String name = "new trigger";
    public boolean enabled = true;

    // ---- When ----
    public MatchMode mode = MatchMode.SIMPLE;
    public String pattern = "";
    public boolean ignoreCase = true;
    /** Only fire while connected to a FishOnMC server. */
    public boolean fishOnMcOnly = true;
    /** Also hide the original chat line. */
    public boolean hideMessage = false;
    /** Minimum seconds between two firings (servers sometimes repeat a line). */
    public double cooldownSeconds = 1.0;

    // ---- Title ----
    public TitleSettings title = new TitleSettings();

    // ---- Sound ----
    public SoundSettings sound = new SoundSettings();

    public enum MatchMode {
        /** Line contains the pattern; {@code *} = anything, {@code {name}} = capture. */
        SIMPLE,
        /** Whole line must equal the pattern (server icons and extra spaces ignored). */
        EXACT,
        /** Java regular expression, searched anywhere in the line. */
        REGEX
    }

    public enum Animation {
        FADE, POP, SLIDE, TYPEWRITER, SHAKE
    }

    public static class TitleSettings {
        public boolean enabled = true;
        public String title = "&lsomething happened!";
        public String subtitle = "";
        public String titleColor = "#ffffff";
        public String subtitleColor = "#bbbbbb";
        /** Title size multiplier; the subtitle is drawn at half of it. */
        public double scale = 3.0;
        /** Vertical position of the title, % of screen height (0 = top, 100 = bottom). */
        public double heightPercent = 35.0;
        public double fadeInSeconds = 0.3;
        public double staySeconds = 2.5;
        public double fadeOutSeconds = 0.6;
        public Animation animation = Animation.POP;
        public boolean shadow = true;
    }

    public static class SoundSettings {
        public boolean enabled = true;
        public String soundId = "minecraft:ui.toast.challenge_complete";
        public double volume = 1.0;
        public double pitch = 1.0;
        /** Total number of plays (1 = play once). */
        public int plays = 1;
        public double secondsBetween = 0.25;
    }

    /** Deep copy with a new id (duplicating / importing). */
    public Trigger copy() {
        Trigger t = copyKeepId();
        t.id = UUID.randomUUID().toString();
        return t;
    }

    /** Deep copy with the same id, so the editor can work on a draft and Cancel throws it away. */
    public Trigger copyKeepId() {
        return TriggerStore.GSON.fromJson(TriggerStore.GSON.toJson(this), Trigger.class);
    }

    /** Fills in anything missing after loading an older or hand-edited file. */
    public void sanitize() {
        if (id == null || id.isBlank()) id = UUID.randomUUID().toString();
        if (name == null) name = "";
        if (mode == null) mode = MatchMode.SIMPLE;
        if (pattern == null) pattern = "";
        if (title == null) title = new TitleSettings();
        if (sound == null) sound = new SoundSettings();
        if (title.title == null) title.title = "";
        if (title.subtitle == null) title.subtitle = "";
        if (title.titleColor == null) title.titleColor = "#ffffff";
        if (title.subtitleColor == null) title.subtitleColor = "#bbbbbb";
        if (title.animation == null) title.animation = Animation.POP;
        // A colour code at the start of the text always won over the colour box, which hid colours picked
        // in the box. Move the code into the box, or drop it if a colour was already picked there.
        RichText.Lead lead = RichText.liftLeadColor(title.title);
        if (lead != null) {
            title.title = lead.rest();
            if (RichText.parseColor(title.titleColor, 0xFFFFFF) == 0xFFFFFF) {
                title.titleColor = String.format(java.util.Locale.ROOT, "#%06x", lead.rgb());
            }
        }
        lead = RichText.liftLeadColor(title.subtitle);
        if (lead != null) {
            title.subtitle = lead.rest();
            if (RichText.parseColor(title.subtitleColor, 0xBBBBBB) == 0xBBBBBB) {
                title.subtitleColor = String.format(java.util.Locale.ROOT, "#%06x", lead.rgb());
            }
        }
        if (sound.soundId == null) sound.soundId = "";
        cooldownSeconds = clamp(cooldownSeconds, 0, 3600);
        title.scale = clamp(title.scale, 0.5, 8);
        title.heightPercent = clamp(title.heightPercent, 0, 100);
        title.fadeInSeconds = clamp(title.fadeInSeconds, 0, 10);
        title.staySeconds = clamp(title.staySeconds, 0, 60);
        title.fadeOutSeconds = clamp(title.fadeOutSeconds, 0, 10);
        sound.volume = clamp(sound.volume, 0, 1);
        sound.pitch = clamp(sound.pitch, 0.5, 2);
        sound.plays = (int) clamp(sound.plays, 1, 20);
        sound.secondsBetween = clamp(sound.secondsBetween, 0.05, 10);
    }

    static double clamp(double v, double min, double max) {
        if (Double.isNaN(v)) return min;
        return Math.max(min, Math.min(max, v));
    }
}
