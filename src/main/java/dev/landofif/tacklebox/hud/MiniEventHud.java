package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;

import java.util.List;

/**
 * The mini event hud ({@link MiniEvent} decides what it says). While it is on, and only on FishOnMC, it sends /minievent
 * quietly every 10 minutes and shortly after a catch and hides the answer: the one command the mod sends on a timer, which
 * luna chose on 2026-11-15 (docs/rules/text-and-automation.md). Off sends nothing.
 */
public final class MiniEventHud {
    private static final List<TextHud.Row> SAMPLE = List.of(new TextHud.Row("ends in", "2d 1h"), new TextHud.Row("daily reroll", "18h 13m"),
            new TextHud.Row("med", "59/100 · ∞ x25"), new TextHud.Row("med rays", "16/18 · daily"));

    private MiniEventHud() {}

    private static boolean active() {
        return TackleboxSettings.get().miniEventHud.on && TriggerEngine.isOnFishOnMc();
    }

    /** A chat message as sent; true when it is the answer to the mod's own /minievent and is to be hidden. */
    public static boolean onChat(String raw, long now) {
        return TackleboxSettings.get().miniEventHud.on && MiniEvent.onChat(raw, now);
    }

    /** The same with the message's colours, which the missions' rows keep. */
    public static boolean onChat(net.minecraft.text.Text message, long now) {
        return TackleboxSettings.get().miniEventHud.on
                && MiniEvent.onChat(message.getString(), dev.landofif.tacklebox.util.ChatColors.each(message), now);
    }

    public static void tick(MinecraftClient client) {
        if (client.player == null || !active()) {
            MiniEvent.reset();
            return;
        }
        if (MiniEvent.due(System.currentTimeMillis(), client.currentScreen != null)) {
            client.player.networkHandler.sendChatCommand("minievent");
        }
    }

    /** An own catch: counted for the missions it fits, and the server is asked again in a few seconds. */
    public static void onCatch(MiniEvent.Fish f, long now) {
        if (TackleboxSettings.get().miniEventHud.on) {
            MiniEvent.onCatch(f, now);
            MiniEvent.askAfterCatch(now);
        }
    }

    public static void renderHud(DrawContext ctx) {
        if (active()) {
            draw(ctx, false);
        }
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}, or null with no event; a preview shows a sample then. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        List<TextHud.Row> rows = MiniEvent.rows(System.currentTimeMillis());
        return TextHud.draw(ctx, TackleboxSettings.get().miniEventHud, MiniEvent.title(), rows.isEmpty() && preview ? SAMPLE : rows);
    }
}
