package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;

/**
 * Rod not cast: a rod in hand and no bobber out for {@code rodIdleSeconds} hands the line "TACKLEBOX » Rod not cast for 60s" to
 * the triggers (not to chat), once per wait. The "rod not cast" preset turns it into a title and a sound. It never casts.
 * The clock starts only after the first cast since joining, and putting the rod away resets it.
 */
public final class RodIdle {
    public static final int MIN_SECONDS = 10;
    public static final int MAX_SECONDS = 600;

    private static boolean cast;
    private static long since = -1;
    private static boolean fired;

    private RodIdle() {}

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null || !TriggerEngine.isOnFishOnMc()) {
            reset();
            return;
        }
        boolean rod = p.getMainHandStack().isOf(Items.FISHING_ROD) || p.getOffHandStack().isOf(Items.FISHING_ROD);
        String line = step(rod, p.fishHook != null, System.currentTimeMillis(), TackleboxSettings.get().rodIdleSeconds);
        if (line != null) {
            TriggerEngine.onGameMessage(Text.literal(line), false);
        }
    }

    /** One tick of the clock; the line to hand to the triggers, or null. */
    public static String step(boolean rodInHand, boolean bobberOut, long now, int seconds) {
        if (bobberOut) {
            cast = true;
            since = -1;
            fired = false;
            return null;
        }
        if (!rodInHand || !cast) {
            since = -1;
            fired = false;
            return null;
        }
        if (since < 0) {
            since = now;
        }
        if (fired || now - since < seconds * 1000L) {
            return null;
        }
        fired = true;
        return line(seconds);
    }

    static String line(int seconds) {
        return "TACKLEBOX » Rod not cast for " + seconds + "s";
    }

    public static int clamp(int seconds) {
        return Math.max(MIN_SECONDS, Math.min(MAX_SECONDS, seconds));
    }

    public static void reset() {
        cast = false;
        since = -1;
        fired = false;
    }
}
