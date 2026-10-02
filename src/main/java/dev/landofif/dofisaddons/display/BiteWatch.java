package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.duck.BobberCatchAccess;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.trigger.Trigger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.projectile.FishingBobberEntity;

import java.util.Locale;

/**
 * Watches your own bobber's synced {@code caughtFish} flag (ported from fishutils). Its rising edge is
 * the bite, read off your own rod, so a neighbour's bite can never set it off. It marks the bobber
 * timer as bitten and can show a short message.
 */
public final class BiteWatch {
    private static boolean wasCaught;
    private static int trackedHookId = -1;

    private BiteWatch() {}

    public static void tick(MinecraftClient client) {
        FishingBobberEntity hook = client.player == null ? null : client.player.fishHook;
        if (hook == null) {
            wasCaught = false;
            trackedHookId = -1;
            return;
        }
        boolean caught = hook instanceof BobberCatchAccess access && access.dofisaddons$caughtFish();
        if (hook.getId() != trackedHookId) {
            trackedHookId = hook.getId();
            wasCaught = caught; // a fresh cast never fires retroactively
        } else if (caught && !wasCaught) {
            onBite();
            wasCaught = true;
        } else if (!caught) {
            wasCaught = false;
        }
    }

    private static void onBite() {
        BobberTimer.markBite();
        DofiSettings s = StatsTracker.settings();
        if (s.biteMessage && s.biteMessageText != null && !s.biteMessageText.isBlank()) {
            Trigger.TitleSettings t = new Trigger.TitleSettings();
            t.titleColor = String.format(Locale.ROOT, "#%06x", s.biteMessageColor);
            t.scale = 2.0;
            t.heightPercent = 40;
            t.fadeInSeconds = 0.1;
            t.staySeconds = 1.6;
            t.fadeOutSeconds = 0.4;
            t.animation = Trigger.Animation.POP;
            TitleOverlay.show(t, s.biteMessageText, "");
        }
    }
}
