package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.util.RichText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Draws trigger titles. One title is on screen at a time; if another fires while one is showing,
 * it waits its turn (up to a few), so two quick triggers don't overwrite each other.
 */
public final class TitleOverlay {
    private static final int MAX_QUEUED = 4;

    private record Shown(Trigger.TitleSettings settings, RichText title, RichText subtitle) {}

    private static final Deque<Shown> QUEUE = new ArrayDeque<>();
    private static Shown current;
    private static long startedAt;

    private TitleOverlay() {}

    public static void show(Trigger.TitleSettings settings, String title, String subtitle) {
        int titleColor = RichText.parseColor(settings.titleColor, 0xFFFFFF);
        int subtitleColor = RichText.parseColor(settings.subtitleColor, 0xBBBBBB);
        Shown shown = new Shown(settings, RichText.parse(title, titleColor), RichText.parse(subtitle, subtitleColor));
        if (current == null) {
            start(shown);
        } else {
            if (QUEUE.size() >= MAX_QUEUED) {
                QUEUE.pollFirst();
            }
            QUEUE.addLast(shown);
        }
    }

    /** Shows a title right away, dropping anything queued (used by the editor preview). */
    public static void preview(Trigger.TitleSettings settings, String title, String subtitle) {
        QUEUE.clear();
        current = null;
        show(settings, title, subtitle);
    }

    public static void clear() {
        QUEUE.clear();
        current = null;
    }

    private static void start(Shown shown) {
        current = shown;
        startedAt = System.currentTimeMillis();
    }

    public static void render(DrawContext context) {
        if (current == null) {
            return;
        }
        Trigger.TitleSettings s = current.settings;
        long fadeIn = (long) (s.fadeInSeconds * 1000);
        long stay = (long) (s.staySeconds * 1000);
        long fadeOut = (long) (s.fadeOutSeconds * 1000);
        long total = fadeIn + stay + fadeOut;
        long age = System.currentTimeMillis() - startedAt;
        if (age >= total) {
            current = null;
            Shown next = QUEUE.pollFirst();
            if (next != null) {
                start(next);
            }
            return;
        }

        float alpha;
        if (age < fadeIn) {
            alpha = fadeIn == 0 ? 1f : age / (float) fadeIn;
        } else if (age < fadeIn + stay) {
            alpha = 1f;
        } else {
            alpha = fadeOut == 0 ? 0f : 1f - (age - fadeIn - stay) / (float) fadeOut;
        }
        // Progress through the entrance (0 -> 1), used by the animations.
        float enter = fadeIn == 0 ? 1f : MathHelper.clamp(age / (float) fadeIn, 0f, 1f);

        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int width = context.getScaledWindowWidth();
        int height = context.getScaledWindowHeight();

        float scale = (float) s.scale;
        float offsetX = 0;
        float offsetY = 0;
        int titleChars = current.title.length();
        int subtitleChars = current.subtitle.length();

        switch (s.animation) {
            case POP -> {
                // Overshoots a little, then settles: reads as a quick "pop".
                float t = enter;
                float overshoot = 1f + 0.35f * (float) Math.sin(t * Math.PI) * (1f - t * 0.5f);
                scale *= t < 1f ? Math.max(0.2f, t) * overshoot : 1f;
            }
            case SLIDE -> offsetY = -24f * (1f - easeOut(enter));
            case TYPEWRITER -> {
                // Types the title at ~30 chars/s once it has started, then the subtitle.
                float typed = age / 33f;
                titleChars = Math.min(titleChars, (int) typed);
                subtitleChars = Math.min(subtitleChars, Math.max(0, (int) typed - current.title.length()));
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
        float centerX = width / 2f + offsetX;
        float titleY = height * (float) (s.heightPercent / 100.0) + offsetY;

        Text title = current.title.toText(titleChars);
        drawCentered(context, tr, title, centerX, titleY, scale, color, s.shadow);

        if (!current.subtitle.isEmpty()) {
            float subScale = Math.max(0.5f, (float) s.scale / 2f);
            float subY = titleY + tr.fontHeight * scale * 0.5f + 4f + tr.fontHeight * subScale * 0.5f;
            Text subtitle = current.subtitle.toText(subtitleChars);
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
