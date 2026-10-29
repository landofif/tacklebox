package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.compat.FoeFriends;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * "Hide nearby players": while your line is out, other players within the range of you or your bobber are not drawn,
 * and neither are the server's nametags over them. Crewmates and friends (foecollab's lists) stay. Only players in the
 * tab list count, so the server's npcs (also player entities) always show. Worked out once per tick.
 */
public final class NearbyPlayers {
    public static final int MIN_RANGE = 2, MAX_RANGE = 12;
    /** The tag window over a player, the same as the nametag hiding in EntityRenderManagerMixin. */
    private static final double TAG_RADIUS_SQ = 0.7 * 0.7, TAG_MIN_ABOVE = 1.75, TAG_MAX_ABOVE = 4.0;

    private static final List<PlayerEntity> hidden = new ArrayList<>();
    private static ClientWorld world;
    private static long time = Long.MIN_VALUE;

    private NearbyPlayers() {}

    public static int clampRange(int range) {
        return Math.max(MIN_RANGE, Math.min(MAX_RANGE, range));
    }

    /** True for a hidden player, or a nametag (text display or armour stand) riding or floating over one. */
    public static boolean hidden(Entity entity) {
        refresh();
        if (hidden.isEmpty()) {
            return false;
        }
        if (entity instanceof PlayerEntity) {
            return hidden.contains(entity);
        }
        if (!(entity instanceof DisplayEntity.TextDisplayEntity) && !(entity instanceof ArmorStandEntity)) {
            return false;
        }
        Entity root = entity.getRootVehicle();
        for (PlayerEntity p : hidden) {
            if (root == p || over(entity.getX() - p.getX(), entity.getZ() - p.getZ(), entity.getY() - p.getY())) {
                return true;
            }
        }
        return false;
    }

    private static void refresh() {
        MinecraftClient client = MinecraftClient.getInstance();
        ClientWorld w = client.world;
        if (w == world && (w == null || w.getTime() == time)) {
            return;
        }
        world = w;
        time = w == null ? Long.MIN_VALUE : w.getTime();
        hidden.clear();
        ClientPlayerEntity self = client.player;
        TackleboxSettings s = TackleboxSettings.get();
        if (w == null || self == null || !s.hideNearPlayers) {
            return;
        }
        FishingBobberEntity hook = self.fishHook;
        if (hook == null) {
            return;
        }
        Set<UUID> kept = FoeFriends.kept();
        for (PlayerEntity p : w.getPlayers()) {
            if (p != self && hides(listed(self.networkHandler, p.getUuid()), kept.contains(p.getUuid()),
                    p.squaredDistanceTo(self), p.squaredDistanceTo(hook), s.nearPlayersRange)) {
                hidden.add(p);
            }
        }
    }

    private static boolean listed(ClientPlayNetworkHandler handler, UUID uuid) {
        PlayerListEntry entry = handler.getPlayerListEntry(uuid);
        return entry != null && handler.getListedPlayerListEntries().contains(entry);
    }

    /** A real player who is not kept and is within the range of you or your bobber. */
    static boolean hides(boolean inTabList, boolean kept, double toSelfSq, double toBobberSq, int range) {
        double r = (double) range * range;
        return inTabList && !kept && (toSelfSq <= r || toBobberSq <= r);
    }

    /** A tag at this offset from a player's feet floats over that player. */
    static boolean over(double dx, double dz, double dy) {
        return dx * dx + dz * dz <= TAG_RADIUS_SQ && dy >= TAG_MIN_ABOVE && dy <= TAG_MAX_ABOVE;
    }
}
