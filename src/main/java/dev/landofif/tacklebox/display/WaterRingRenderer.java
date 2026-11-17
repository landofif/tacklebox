package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.fluid.FluidState;
import net.minecraft.particle.DustParticleEffect;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import net.minecraft.world.World;

import java.util.List;

/**
 * Feeds {@link WaterRings} from the world and draws them (docs/display/catch-effects.md). The rings are drawn at
 * {@code WorldRenderEvents.END_MAIN}: its buffers are drawn right after the translucent terrain, so a ring lies on top of the
 * water and blends with it (at AFTER_ENTITIES, where the chummer wall draws, the water would be drawn over it). The drawing
 * only reads the rings the ticks saved.
 */
public final class WaterRingRenderer {
    private static final Random RANDOM = Random.create();

    private WaterRingRenderer() {}

    public static void register() {
        WorldRenderEvents.END_MAIN.register(WaterRingRenderer::render);
    }

    /** Every tick: your bobber's spot on the water, the bite rings while a fish swims in, rings that are over go. */
    public static void tick(MinecraftClient client) {
        long now = System.currentTimeMillis();
        FishingBobberEntity hook = client.player == null ? null : client.player.fishHook;
        if (hook != null && client.world != null && TackleboxSettings.get().waterFx) {
            WaterRings.onBobber(hook.getX(), waterTop(client.world, hook.getX(), hook.getY(), hook.getZ()), hook.getZ(), now);
        }
        WaterRings.tick(now, hook != null && BiteCountdown.active(now), BiteCountdown.seconds(now));
    }

    /**
     * The water's top at a spot: the fluid height of the block the spot is in or of the one under it (the fish particle sits
     * on the water block's top, the bobber floats around the surface), else {@code y} itself.
     */
    public static double waterTop(World world, double x, double y, double z) {
        BlockPos.Mutable pos = new BlockPos.Mutable(MathHelper.floor(x), MathHelper.floor(y), MathHelper.floor(z));
        for (int i = 0; i < 2; i++) {
            pos.setY(MathHelper.floor(y) - i);
            FluidState fluid = world.getFluidState(pos);
            if (!fluid.isEmpty()) {
                return pos.getY() + fluid.getHeight(world, pos);
            }
        }
        return y;
    }

    /** A few dust sparkles in the rarity colour rising slowly over the catch ring. The particle switches can hide them. */
    public static void sparkles(WaterRings.Spot spot, String rarity, int count) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (count <= 0 || client == null || client.world == null) {
            return;
        }
        DustParticleEffect dust = new DustParticleEffect(WaterRings.catchColor(rarity), 1.0f);
        for (int i = 0; i < count; i++) {
            double angle = Math.PI * 2 * i / count + RANDOM.nextDouble() * 0.5, r = 0.3 + RANDOM.nextDouble() * 0.6;
            client.world.addParticleClient(dust, spot.x() + Math.cos(angle) * r, spot.y() + 0.1 + RANDOM.nextDouble() * 0.2,
                    spot.z() + Math.sin(angle) * r, 0, 1, 0);
        }
    }

    /** Vertices are relative to the camera, like every other quad the world renderer draws (see WorldRenderContext#consumers). */
    private static void render(WorldRenderContext context) {
        List<WaterRings.Ring> rings = WaterRings.rings();
        if (rings.isEmpty() || !TackleboxSettings.get().waterFx) {
            return;
        }
        long now = System.currentTimeMillis();
        Vec3d camera = context.worldState().cameraRenderState.pos;
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.debugQuads());
        for (WaterRings.Ring ring : rings) {
            int alpha = Math.round(ring.alpha(now) * 255);
            if (alpha <= 0 || ring.over(now)) {
                continue;
            }
            int color = alpha << 24 | ring.rgb();
            WaterRings.emit((float) (ring.x() - camera.x), (float) (ring.y() + WaterRings.LIFT - camera.y), (float) (ring.z() - camera.z),
                    ring.radius(now), (x, y, z) -> buffer.vertex(x, y, z).color(color));
        }
    }
}
