package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;

/**
 * Smooth cast (from fishutils). The client simulates your bobber's flight itself, but every tick it also
 * lerps toward the last position the server sent, which lags behind on a slow connection: the bobber
 * jerks back, then flies the whole path again. While your own bobber is in the air the server updates
 * are ignored (see EntityInterpolationMixin); once it touches water or ground they apply again, so where
 * it rests stays exact. Other entities, other players bobbers included, are never touched.
 */
public final class SmoothCast {
    private static int trackedId = -1;
    private static boolean landed;

    private SmoothCast() {}

    public static boolean suppress(Entity entity) {
        if (entity == null || !(entity instanceof FishingBobberEntity) || !StatsTracker.settings().smoothCast) {
            return false;
        }
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        FishingBobberEntity bobber = player == null ? null : player.fishHook;
        if (bobber == null || entity != bobber) {
            return false;
        }
        if (bobber.getId() != trackedId) {
            trackedId = bobber.getId();
            landed = false;
        }
        if (landed) {
            return false;
        }
        if (bobber.isTouchingWater() || bobber.isOnGround()) {
            landed = true; // latched, so a one-tick water flicker cant switch it back on
            return false;
        }
        return true;
    }
}
