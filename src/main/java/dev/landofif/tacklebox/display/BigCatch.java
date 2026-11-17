package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.Trigger;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;

import java.util.List;

/**
 * The big catch moment (2026-11-17, luna: "do number 1 first"; docs/display/catch-effects.md): when you catch a fabled,
 * trophy, mythical or gigantic fish, a soft glow in its colour fades in and out at the screen edges, one light band sweeps
 * across the catch title's name ({@link CatchTitle}) and a sound plays. Nothing moves, shakes or flashes. The catch comes from
 * {@link CatchWatch}: the fish item, or the server's subtitle when the inventory is full.
 */
public final class BigCatch {
    /** The kinds that count, in their order when a fish fits several, with the server's colour of each (research section 2). */
    public enum Kind {
        FABLED(0x82171E), TROPHY(0xD5BF3B), MYTHICAL(0xC93832), GIGANTIC(0xAF3333);

        public final int rgb;

        Kind(int rgb) {
            this.rgb = rgb;
        }
    }

    /** One sound of a kind: a vanilla sound id, its volume and pitch. Notes of one kind play together. */
    public record Note(String id, float volume, float pitch) {}

    static final long GLOW_IN = 150, GLOW_HOLD = 150, GLOW_OUT = 300;
    static final long SHINE_MS = 700;
    /** Half the light band's width, in widths of the name line, and how strong it is in its middle. */
    static final float SHINE_HALF = 0.2f, SHINE_PEAK = 0.9f;
    /** A white frame that is clear in the middle, tinted with the kind's colour and stretched over the screen. */
    static final Identifier EDGE_GLOW = Identifier.of("tacklebox", "textures/gui/edge_glow.png");
    static final int GLOW_TEX = 256;

    private static Kind kind;
    private static long at;

    private BigCatch() {}

    // ---------------------------------------------------------------- which fish

    /** The first kind this fish fits that is switched on, or null (a normal fish, or its kinds are off). */
    public static Kind kind(String rarity, String variant, String size, TackleboxSettings s) {
        for (Kind k : Kind.values()) {
            boolean fits = switch (k) {
                case FABLED -> "fabled".equals(variant);
                case TROPHY -> "trophy".equals(variant);
                case MYTHICAL -> "mythical".equals(rarity);
                case GIGANTIC -> "gigantic".equals(size);
            };
            if (fits && on(k, s)) {
                return k;
            }
        }
        return null;
    }

    static boolean on(Kind k, TackleboxSettings s) {
        return switch (k) {
            case FABLED -> s.bigFabled;
            case TROPHY -> s.bigTrophy;
            case MYTHICAL -> s.bigMythical;
            case GIGANTIC -> s.bigGigantic;
        };
    }

    /** The sound of a kind: a deep one for fabled, a bell chord for trophy, level up for mythical, an amethyst chime for gigantic. */
    public static List<Note> sound(Kind k) {
        return switch (k) {
            case FABLED -> List.of(new Note("minecraft:block.conduit.activate", 0.9f, 0.9f));
            case TROPHY -> List.of(new Note("minecraft:block.note_block.bell", 0.7f, 1.0f),
                    new Note("minecraft:block.note_block.bell", 0.7f, 1.26f),
                    new Note("minecraft:block.note_block.bell", 0.7f, 1.5f));
            case MYTHICAL -> List.of(new Note("minecraft:entity.player.levelup", 0.8f, 1.0f));
            case GIGANTIC -> List.of(new Note("minecraft:block.amethyst_block.chime", 1.0f, 0.8f));
        };
    }

    // ---------------------------------------------------------------- when

    /** A catch of yours: starts the moment when it is a kind that counts and plays its sound. Returns the kind, or null. */
    public static Kind onCatch(String rarity, String variant, String size, long now) {
        TackleboxSettings s = TackleboxSettings.get();
        Kind k = s.bigCatch ? kind(rarity, variant, size, s) : null;
        if (k == null) {
            return null;
        }
        kind = k;
        at = now;
        if (s.bigSound) {
            for (Note n : sound(k)) {
                Trigger.SoundSettings play = new Trigger.SoundSettings();
                play.soundId = n.id();
                play.volume = n.volume();
                play.pitch = n.pitch();
                SoundQueue.play(play);
            }
        }
        return k;
    }

    /** The kind of the moment that started last, null when there is none. */
    public static Kind current() {
        return kind;
    }

    /** When the title shine starts on a catch title shown at {@code titleAt}, -1 when it has no shine (not big, or switched off). */
    public static long shineStart(long titleAt) {
        TackleboxSettings s = TackleboxSettings.get();
        return kind != null && s.bigCatch && s.bigShine && at >= titleAt ? at : -1;
    }

    /** The light band's colour: the kind's, halfway to white, so it shows on white text too. */
    public static int shineColor() {
        return kind == null ? 0xFFFFFF : lighten(kind.rgb, 0.55f);
    }

    static int lighten(int rgb, float k) {
        int r = rgb >> 16 & 0xFF, g = rgb >> 8 & 0xFF, b = rgb & 0xFF;
        return Math.round(r + (255 - r) * k) << 16 | Math.round(g + (255 - g) * k) << 8 | Math.round(b + (255 - b) * k);
    }

    /** A new catch title (and tests): the last catch's moment is over. */
    public static void clear() {
        kind = null;
        at = 0;
    }

    // ---------------------------------------------------------------- how strong

    /** The edge glow {@code t} ms into the moment, {@code peak} at most: fades in, holds, fades out, each step eased. */
    public static float glow(long t, float peak) {
        if (t < 0 || t >= GLOW_IN + GLOW_HOLD + GLOW_OUT) {
            return 0;
        }
        if (t < GLOW_IN) {
            return peak * ease(t / (float) GLOW_IN);
        }
        if (t < GLOW_IN + GLOW_HOLD) {
            return peak;
        }
        return peak * ease(1 - (t - GLOW_IN - GLOW_HOLD) / (float) GLOW_OUT);
    }

    /**
     * How much of the light band lies on a letter at {@code x} (0 the left end of the name line, 1 its right end), {@code t}
     * ms after the lines came: the band crosses from just left of the line to just right of it in {@link #SHINE_MS}.
     */
    public static float shine(float x, long t) {
        if (t < 0 || t > SHINE_MS) {
            return 0;
        }
        float centre = -SHINE_HALF + (1 + 2 * SHINE_HALF) * t / SHINE_MS;
        float d = Math.abs(x - centre) / SHINE_HALF;
        return d >= 1 ? 0 : SHINE_PEAK * ease(1 - d);
    }

    private static float ease(float x) {
        return x * x * (3 - 2 * x);
    }

    // ---------------------------------------------------------------- drawing

    /** How strong the edge glow is at {@code now}: 0 with no moment, or with the glow switched off. */
    public static float glowAlpha(long now) {
        TackleboxSettings s = TackleboxSettings.get();
        return kind == null || !s.bigCatch || !s.bigGlow ? 0 : glow(now - at, s.bigGlowOpacity / 100f);
    }

    /** The edge glow over the whole screen while the moment lasts. */
    public static void renderHud(DrawContext ctx) {
        int alpha = Math.round(glowAlpha(System.currentTimeMillis()) * 255);
        if (alpha <= 0) {
            return;
        }
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, EDGE_GLOW, 0, 0, 0f, 0f, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(),
                GLOW_TEX, GLOW_TEX, GLOW_TEX, GLOW_TEX, alpha << 24 | kind.rgb);
    }
}
