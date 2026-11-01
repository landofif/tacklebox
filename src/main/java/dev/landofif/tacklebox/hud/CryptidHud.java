package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.SoundQueue;
import dev.landofif.tacklebox.display.TitleOverlay;
import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Pattern;

/**
 * The cryptids hud and its popups ({@link Cryptids} decides what they say): moons, rainbow and storms from the chat lines the
 * server hud reads, the clock windows from the time in the top bar, the next moon from the last rise seen in chat. Each kind
 * has its own switch in the settings ({@code cryptidOff}); the popups are not triggers, so they need nothing in /triggers.
 */
public final class CryptidHud {
    private static final Pattern RISE = Pattern.compile("^[^a-z0-9]*(full|blue|super|blood) moon > an? .* has risen", Pattern.CASE_INSENSITIVE);
    private static int ticks;

    private CryptidHud() {}

    /**
     * A moonrise line resets the 160 minute cycle to it, an own rare catch of a cryptid drop ends its dry streak. {@code s} is the
     * normalized line; true when something changed (to be saved).
     */
    public static boolean onChat(String s, long now) {
        if (RISE.matcher(s).find()) {
            TackleboxSettings.get().moonRiseAt = now;
            return true;
        }
        String key = Cryptids.dropKey(s);
        if (key != null) {
            TackleboxSettings.get().cryptidDry.put(key, 0);
            return true;
        }
        return false;
    }

    /** An own catch: one more for every dry streak of an event on at that location; a fabled fish ends the fabled one. True when something changed. */
    public static boolean onCatch(String location, String variant, long now) {
        var dry = TackleboxSettings.get().cryptidDry;
        var keys = Cryptids.counting(state(now), location);
        for (String key : keys) {
            dry.merge(key, 1, Integer::sum);
        }
        if (keys.contains("fabled") && "fabled".equalsIgnoreCase(variant)) {
            dry.put("fabled", 0);
        }
        return !keys.isEmpty();
    }

    public static Cryptids.State state(long now) {
        TackleboxSettings s = TackleboxSettings.get();
        String storm = ServerHud.event("Storm");
        return new Cryptids.State(now, ServerHud.moon(), s.moonRiseAt, storm == null ? "" : storm, ServerHud.event("Rainbow") != null,
                ServerHud.fabledPlace(), Cryptids.clockMinutes(ServerInfo.time, ServerInfo.ampm), new HashSet<>(s.cryptidOff), s.cryptidDry);
    }

    /** Twice a second: a popup for every event that started, if popups are on. */
    public static void tick(MinecraftClient client) {
        if (ticks++ % 10 != 0) {
            return;
        }
        if (client.world == null || !TriggerEngine.isOnFishOnMc()) {
            Cryptids.reset();
            return;
        }
        for (Cryptids.Popup p : Cryptids.started(state(System.currentTimeMillis()))) {
            if (TackleboxSettings.get().cryptidPopups) {
                show(p);
            }
        }
    }

    static void show(Cryptids.Popup p) {
        Trigger.TitleSettings t = new Trigger.TitleSettings();
        t.titleColor = String.format("#%06x", p.color() & 0xFFFFFF);
        t.scale = 2.5;
        t.staySeconds = 4;
        t.animation = Trigger.Animation.SLIDE;
        TitleOverlay.show("cryptids", t, "&l" + p.title(), p.subtitle());
        Trigger.SoundSettings sound = new Trigger.SoundSettings();
        sound.soundId = "minecraft:block.note_block.chime";
        SoundQueue.play(sound);
    }

    public static void renderHud(DrawContext ctx) {
        if (TackleboxSettings.get().cryptidHud && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}; with preview an empty hud still shows a line so it can be moved. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        TackleboxSettings s = TackleboxSettings.get();
        return s.cryptidPos.draw(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), (x, y) -> {
            s.cryptidX = x;
            s.cryptidY = y;
            return Foe.scaled(ctx, x, y, s.cryptidScale, c -> drawAt100(c, preview));
        });
    }

    private static int[] drawAt100(DrawContext ctx, boolean preview) {
        TackleboxSettings s = TackleboxSettings.get();
        List<Cryptids.Row> rows = new ArrayList<>(Cryptids.rows(state(System.currentTimeMillis()), ServerHud::clock));
        if (rows.isEmpty()) {
            if (!preview) {
                return null;
            }
            rows.add(new Cryptids.Row("nothing on", "", 0, true));
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = 0;
        for (Cryptids.Row r : rows) {
            w = Math.max(w, indent(r) + tr.getWidth(Foe.text(r.label())) + (r.value().isEmpty() ? 0 : 12 + tr.getWidth(Foe.text(r.value()))));
        }
        w += 16;
        int h = 25 + rows.size() * 11;
        Foe.title(ctx, s.cryptidX, s.cryptidY, w, s.cryptidAlign, Foe.text("cryptids"));
        for (int i = 0; i < rows.size(); i++) {
            Cryptids.Row r = rows.get(i);
            int y = s.cryptidY + 20 + i * 11;
            Text l = Foe.text(r.label()), v = Foe.text(r.value());
            int lx = s.cryptidX + 8 + indent(r), vx = s.cryptidX + w - 8 - tr.getWidth(v); // left keeps the values in a column on the right
            if (s.cryptidAlign != TackleboxSettings.HudAlign.LEFT) {
                lx = s.cryptidAlign.x(s.cryptidX + 8, w - 16, indent(r) + tr.getWidth(l) + (r.value().isEmpty() ? 0 : 12 + tr.getWidth(v))) + indent(r);
                vx = lx + tr.getWidth(l) + 12;
            }
            ctx.drawTextWithShadow(tr, l, lx, y, r.color() != 0 ? r.color() : r.head() ? Foe.DIM : Foe.TEXT);
            ctx.drawTextWithShadow(tr, v, vx, y, r.head() ? Foe.TEXT : Foe.DIM);
        }
        return new int[]{s.cryptidX, s.cryptidY, w, h};
    }

    /** The spots of an event sit a little in from its name. */
    private static int indent(Cryptids.Row r) {
        return r.head() || r.color() != 0 ? 0 : 6;
    }
}
