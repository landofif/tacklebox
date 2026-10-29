package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.util.RichText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.LongSupplier;

/**
 * Draws trigger titles, each from the moment its trigger fires. Titles of the same trigger stack: the newest sits at the
 * trigger's height and older ones move up a place (at most {@link #MAX_STACKED}). A title of another trigger clears the
 * screen and shows alone.
 */
public final class TitleOverlay {
    static final int MAX_STACKED = 3;
    /** How long an older title takes to move up a place. */
    private static final long MOVE_MS = 150;
    /** Space between two stacked titles. */
    private static final float GAP = 4f;
    /** How visible a title is on its first frame: it fades in from here, so it is readable at once. */
    static final float FIRST_ALPHA = 0.4f;
    /** The size a popping title starts at. */
    static final float FIRST_POP = 0.7f;
    static LongSupplier clock = System::currentTimeMillis;

    /** A title on screen; its trigger says which titles stack. */
    public static final class Shown {
        private final String trigger;
        private final Trigger.TitleSettings settings;
        private final long startedAt;
        private RichText title;
        private RichText subtitle;
        /** 0 at the trigger's height, 1 one title higher, ... */
        private int place;
        private float movedFrom;
        private long movedAt;

        private Shown(String trigger, Trigger.TitleSettings settings, RichText title, RichText subtitle, long startedAt) {
            this.trigger = trigger;
            this.settings = settings;
            this.title = title;
            this.subtitle = subtitle;
            this.startedAt = startedAt;
        }

        public String trigger() {
            return trigger;
        }

        public Text title() {
            return title.toText();
        }

        public Text subtitle() {
            return subtitle.toText();
        }

        public long startedAt() {
            return startedAt;
        }

        public int place() {
            return place;
        }
    }

    /** Oldest first. */
    private static final List<Shown> SHOWN = new ArrayList<>();

    private TitleOverlay() {}

    /** Shows a title of {@code trigger} (an id) right away. @return the title, for {@link #update} */
    public static Shown show(String trigger, Trigger.TitleSettings settings, String title, String subtitle) {
        long now = clock.getAsLong();
        SHOWN.removeIf(s -> now - s.startedAt >= total(s.settings));
        if (!SHOWN.isEmpty() && !SHOWN.get(0).trigger.equals(trigger)) {
            SHOWN.clear();
        }
        while (SHOWN.size() >= MAX_STACKED) {
            SHOWN.remove(0);
        }
        for (Shown s : SHOWN) {
            s.movedFrom = place(s, now);
            s.movedAt = now;
            s.place++;
        }
        Shown shown = new Shown(trigger, settings, parseTitle(settings, title), parseSubtitle(settings, subtitle), now);
        SHOWN.add(shown);
        return shown;
    }

    /** New text for a title already on screen; its timer goes on. */
    public static void update(Shown shown, String title, String subtitle) {
        shown.title = parseTitle(shown.settings, title);
        shown.subtitle = parseSubtitle(shown.settings, subtitle);
    }

    /** Shows a title alone right away (the editor's preview). */
    public static void preview(Trigger.TitleSettings settings, String title, String subtitle) {
        SHOWN.clear();
        show("preview", settings, title, subtitle);
    }

    public static void clear() {
        SHOWN.clear();
    }

    /** The titles on screen, oldest first. */
    public static List<Shown> shown() {
        return Collections.unmodifiableList(SHOWN);
    }

    private static RichText parseTitle(Trigger.TitleSettings settings, String title) {
        return RichText.parse(title, RichText.parseColor(settings.titleColor, 0xFFFFFF));
    }

    private static RichText parseSubtitle(Trigger.TitleSettings settings, String subtitle) {
        return RichText.parse(subtitle, RichText.parseColor(settings.subtitleColor, 0xBBBBBB));
    }

    private static long total(Trigger.TitleSettings s) {
        return (long) (s.fadeInSeconds * 1000) + (long) (s.staySeconds * 1000) + (long) (s.fadeOutSeconds * 1000);
    }

    /** Where a title is on its way up: its place, eased over {@link #MOVE_MS}. */
    static float place(Shown s, long now) {
        float t = Math.min(1f, Math.max(0f, (now - s.movedAt) / (float) MOVE_MS));
        return s.movedFrom + (s.place - s.movedFrom) * easeOut(t);
    }

    /** Opacity at {@code age} ms: from {@link #FIRST_ALPHA} to full over the fade-in, full for the stay, then down to nothing. */
    static float alpha(long age, long fadeIn, long stay, long fadeOut) {
        if (age < fadeIn) {
            return FIRST_ALPHA + (1f - FIRST_ALPHA) * age / (float) fadeIn;
        }
        if (age < fadeIn + stay) {
            return 1f;
        }
        return fadeOut == 0 ? 0f : 1f - (age - fadeIn - stay) / (float) fadeOut;
    }

    /** Size of a popping title through its entrance (0 to 1): from {@link #FIRST_POP}, a little past full size, then settled. */
    static float popScale(float enter) {
        if (enter >= 1f) {
            return 1f;
        }
        float t = enter - 1f; // ease-out-back
        float back = 1f + 2.70158f * t * t * t + 1.70158f * t * t;
        return FIRST_POP + (1f - FIRST_POP) * back;
    }

    public static void render(DrawContext context) {
        long now = clock.getAsLong();
        SHOWN.removeIf(s -> now - s.startedAt >= total(s.settings));
        if (SHOWN.isEmpty()) {
            return;
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        for (Shown shown : SHOWN) {
            draw(context, tr, shown, now);
        }
    }

    private static void draw(DrawContext context, TextRenderer tr, Shown shown, long now) {
        Trigger.TitleSettings s = shown.settings;
        long fadeIn = (long) (s.fadeInSeconds * 1000);
        long stay = (long) (s.staySeconds * 1000);
        long fadeOut = (long) (s.fadeOutSeconds * 1000);
        long age = now - shown.startedAt;
        float alpha = alpha(age, fadeIn, stay, fadeOut);
        // Progress through the entrance (0 -> 1), used by the animations.
        float enter = fadeIn == 0 ? 1f : MathHelper.clamp(age / (float) fadeIn, 0f, 1f);

        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        float scale = (float) s.scale;
        float subScale = Math.max(0.5f, (float) s.scale / 2f);
        float offsetX = 0;
        float offsetY = 0;
        int titleChars = shown.title.length();
        int subtitleChars = shown.subtitle.length();

        switch (s.animation) {
            case POP -> scale *= popScale(enter);
            case SLIDE -> offsetY = -24f * (1f - easeOut(enter));
            case TYPEWRITER -> {
                // Types the title at ~30 chars/s once it has started, then the subtitle.
                float typed = age / 33f;
                titleChars = Math.min(titleChars, (int) typed);
                subtitleChars = Math.min(subtitleChars, Math.max(0, (int) typed - shown.title.length()));
                alpha = age < fadeIn + stay ? 1f : alpha;
            }
            case SHAKE -> {
                float strength = 3f * (1f - MathHelper.clamp(age / 900f, 0f, 1f));
                offsetX = (float) (Math.sin(age / 18.0) * strength);
                offsetY = (float) (Math.cos(age / 23.0) * strength * 0.5f);
            }
            case FADE -> {
            }
        }

        int a = MathHelper.clamp((int) (alpha * 255f), 0, 255);
        if (a <= 4) {
            return; // vanilla text treats near-zero alpha as opaque
        }
        int color = (a << 24) | 0xFFFFFF;
        // One stacked place: the title, the subtitle under it and a gap, at full size.
        float block = tr.fontHeight * (float) s.scale + (shown.subtitle.isEmpty() ? 0f : 4f + tr.fontHeight * subScale) + GAP;
        float centerX = width / 2f + offsetX;
        float titleY = height * (float) (s.heightPercent / 100.0) + offsetY - place(shown, now) * block;

        Text title = shown.title.toText(titleChars);
        drawCentered(context, tr, title, centerX, titleY, scale, color, s.shadow);

        if (!shown.subtitle.isEmpty()) {
            float subY = titleY + tr.fontHeight * scale * 0.5f + 4f + tr.fontHeight * subScale * 0.5f;
            Text subtitle = shown.subtitle.toText(subtitleChars);
            drawCentered(context, tr, subtitle, centerX, subY, subScale, color, s.shadow);
        }
    }

    /** Draws {@code text} centred on (x, y) at the given scale. */
    private static void drawCentered(DrawContext context, TextRenderer tr, Text text, float x, float y,
                                     float scale, int color, boolean shadow) {
        context.getMatrices().pushMatrix();
        try {
            context.getMatrices().translate(x, y);
            context.getMatrices().scale(scale, scale);
            int w = tr.getWidth(text);
            context.drawText(tr, text, -w / 2, -tr.fontHeight / 2, color, shadow);
        } finally {
            context.getMatrices().popMatrix();
        }
    }

    private static float easeOut(float t) {
        float inv = 1f - t;
        return 1f - inv * inv * inv;
    }
}
