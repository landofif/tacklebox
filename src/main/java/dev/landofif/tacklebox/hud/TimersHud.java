package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.SoundQueue;
import dev.landofif.tacklebox.display.TitleOverlay;
import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

/**
 * The timers hud ({@link Timers} decides what it says) and the titles for a tackle shop restock and a cosmetic store reset.
 * The titles always show, whether the hud is on or not (luna, 2026-11-15).
 */
public final class TimersHud {
    private static int ticks;

    private TimersHud() {}

    /** A restock line gives its title, a vote line counts a vote. {@code s} is the normalized line; true when something is to be saved. */
    public static boolean onChat(String s, long now) {
        if (Timers.restockLine(s)) {
            show(Timers.restocked());
            return false;
        }
        if (Timers.voteLine(s)) {
            Timers.addVote(TackleboxSettings.get().votes, now);
            return true;
        }
        return false;
    }

    /** Twice a second: the title when the cosmetic store resets. */
    public static void tick(MinecraftClient client) {
        if (ticks++ % 10 != 0) {
            return;
        }
        if (client.world == null || !TriggerEngine.isOnFishOnMc()) {
            Timers.reset();
            return;
        }
        Timers.Popup p = Timers.cosmeticReset(System.currentTimeMillis());
        if (p != null) {
            show(p);
        }
    }

    static void show(Timers.Popup p) {
        Trigger.TitleSettings t = new Trigger.TitleSettings();
        t.scale = 2.5;
        t.staySeconds = 4;
        t.animation = Trigger.Animation.SLIDE;
        TitleOverlay.show("timers", t, "&l" + p.title(), p.subtitle());
        Trigger.SoundSettings sound = new Trigger.SoundSettings();
        sound.soundId = "minecraft:block.note_block.chime";
        SoundQueue.play(sound);
    }

    public static void renderHud(DrawContext ctx) {
        if (TackleboxSettings.get().timersHud.on && TriggerEngine.isOnFishOnMc()) {
            draw(ctx);
        }
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}. */
    public static int[] draw(DrawContext ctx) {
        TackleboxSettings s = TackleboxSettings.get();
        return TextHud.draw(ctx, s.timersHud, "timers", Timers.rows(System.currentTimeMillis(), s.votes, s.timersVotes, ServerHud::clock));
    }
}
