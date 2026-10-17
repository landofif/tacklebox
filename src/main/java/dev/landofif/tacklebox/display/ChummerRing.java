package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * The chummer range ring (setting chummerRing, docs/huds/chummer-range.md): finds every placed chummer among the
 * loaded text displays and draws a one block high wall on the ground at its range. Display only, no mixins. Every chummer
 * the client tracks gets a ring, not only the one you stand in; display entities are tracked far beyond render distance.
 */
public final class ChummerRing {
    private static final int SCAN_TICKS = 10;
    private static final int GROUND_SEARCH = 6;

    /** A placed chummer: the hologram's x and z, and the ground under it (the hologram itself bobs up and down). */
    private record Placed(double x, double groundY, double z) {}

    private static List<Placed> placed = List.of();
    private static int countdown;

    private ChummerRing() {}

    public static void register() {
        WorldRenderEvents.AFTER_ENTITIES.register(ChummerRing::render);
    }

    /** Looks through the loaded entities every {@link #SCAN_TICKS} ticks. */
    public static void tick(MinecraftClient client) {
        if (client.world == null || !TackleboxSettings.get().chummerRing) {
            placed = List.of();
            countdown = 0; // scan at once when it is switched on or a world loads
            return;
        }
        if (--countdown > 0) {
            return;
        }
        countdown = SCAN_TICKS;
        List<Placed> found = new ArrayList<>();
        for (Entity entity : client.world.getEntities()) {
            if (entity instanceof DisplayEntity.TextDisplayEntity text && !text.isRemoved()) {
                if (ChummerText.parse(text.getText().getString()).isPresent()) {
                    found.add(new Placed(text.getX(), groundY(client.world, text), text.getZ()));
                }
            }
        }
        placed = found;
    }

    /** The top of the first solid block under the hologram, looking down a few blocks; the hologram's own y bobs. */
    private static double groundY(World world, Entity text) {
        BlockPos.Mutable pos = new BlockPos.Mutable(text.getBlockX(), 0, text.getBlockZ());
        for (int dy = 0; dy <= GROUND_SEARCH; dy++) {
            pos.setY(text.getBlockY() - dy);
            VoxelShape shape = world.getBlockState(pos).getCollisionShape(world, pos);
            if (!shape.isEmpty()) {
                return pos.getY() + shape.getMax(Direction.Axis.Y) + ChummerRingGeometry.LIFT;
            }
        }
        return ChummerRingGeometry.fallbackY(text.getY());
    }

    public static void reset() {
        placed = List.of();
        countdown = 0;
    }

    /** Vertices are relative to the camera, like every other quad the world renderer draws (see WorldRenderContext#consumers). */
    private static void render(WorldRenderContext context) {
        List<Placed> rings = placed;
        TackleboxSettings settings = TackleboxSettings.get();
        if (rings.isEmpty() || !settings.chummerRing) {
            return;
        }
        Vec3d camera = context.worldState().cameraRenderState.pos;
        int color = ChummerRingGeometry.argb(settings.chummerRingColor);
        VertexConsumer buffer = context.consumers().getBuffer(RenderLayers.debugQuads());
        for (Placed ring : rings) {
            ChummerRingGeometry.emit((float) (ring.x - camera.x), (float) (ring.groundY - camera.y),
                    (float) (ring.z - camera.z), (x, y, z) -> buffer.vertex(x, y, z).color(color));
        }
    }
}
