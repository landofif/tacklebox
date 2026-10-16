package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

/**
 * The chummer range ring (setting chummerRing, docs/huds/chummer-range.md): finds every placed chummer among the
 * loaded text displays and draws a thin flat ring on the ground at its range. Display only, no mixins. Every chummer
 * the client tracks gets a ring, not only the one you stand in; display entities are tracked far beyond render distance.
 */
public final class ChummerRing {
    private static final int SCAN_TICKS = 10;

    /** The position of a placed chummer's hologram, which the ring is built from. */
    private record Placed(double x, double y, double z) {}

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
                    found.add(new Placed(text.getX(), text.getY(), text.getZ()));
                }
            }
        }
        placed = found;
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
            ChummerRingGeometry.emit((float) (ring.x - camera.x), (float) (ChummerRingGeometry.ringY(ring.y) - camera.y),
                    (float) (ring.z - camera.z), (x, y, z) -> buffer.vertex(x, y, z).color(color));
        }
    }
}
