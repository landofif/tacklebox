package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.DisplayCull;
import dev.landofif.tacklebox.display.Holograms;
import dev.landofif.tacklebox.display.NearbyPlayers;
import dev.landofif.tacklebox.display.OtherBobbers;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Frustum;
import net.minecraft.client.render.entity.EntityRenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.FishingBobberEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Hides entities on your screen only (ported from fishutils): other players bobbers (their lines go
 * with them), the servers over-head nametag stacks (yours, or everyones), and "+66xp" hologram
 * popups. Also players near you while fishing (display/NearbyPlayers). Every world entity passes
 * through shouldRender before it is drawn.
 */
@Mixin(EntityRenderManager.class)
abstract class EntityRenderManagerMixin {
    /** A tag counts as "over" a player within 0.7 blocks sideways and this height window above the feet. */
    private static final double HORIZONTAL_RADIUS_SQ = 0.7 * 0.7;
    private static final double MIN_HEIGHT_ABOVE = 1.75;
    private static final double MAX_HEIGHT_ABOVE = 4.0;
    /** An unrecognised tag over you only counts as your nametag after 5 seconds, so short popups still show. */
    private static final int MIN_NAMETAG_AGE_TICKS = 100;

    @Inject(method = "shouldRender", at = @At("HEAD"), cancellable = true)
    private void tacklebox$cull(Entity entity, Frustum frustum, double x, double y, double z, CallbackInfoReturnable<Boolean> cir) {
        TackleboxSettings s = TackleboxSettings.get();
        ClientPlayerEntity self = MinecraftClient.getInstance().player;

        if (entity instanceof FishingBobberEntity) {
            if (OtherBobbers.hidden(entity)) {
                cir.setReturnValue(false);
            }
            return;
        }

        if (NearbyPlayers.hidden(entity)) {
            cir.setReturnValue(false);
            return;
        }

        if (entity instanceof net.minecraft.entity.decoration.DisplayEntity) {
            if (OtherBobbers.hideDisplays
                    || DisplayCull.hidden(s.hideFarDisplays, entity.squaredDistanceTo(x, y, z), s.farDisplayRange)) {
                cir.setReturnValue(false);
                return;
            }
        }

        boolean nametags = s.hideOwnNametag || s.hideAllNametags;
        if (!nametags && !s.hideXpPopups) {
            return;
        }
        Holograms.Cached c = Holograms.classify(entity);
        if (c == null) {
            return;
        }
        if (c.xpPopup) {
            if (s.hideXpPopups) {
                cir.setReturnValue(false);
            }
            return;
        }
        if (!nametags || c.xpSegment || self == null) {
            return; // a catch summary that mentions xp stays
        }
        if (s.hideAllNametags && isAboveAnyPlayer(entity, self)) {
            cir.setReturnValue(false);
            return;
        }
        if (!s.hideOwnNametag) {
            return;
        }
        if (Holograms.isKnownOwnNametag(entity)) {
            cir.setReturnValue(false);
            return;
        }
        if (!isAttachedTo(entity, self)) {
            return;
        }
        boolean carriesOwnName = c.folded.contains(Holograms.foldedSelfName(self.getName().getString()));
        if (carriesOwnName || entity.age >= MIN_NAMETAG_AGE_TICKS) {
            Holograms.markOwnNametag(entity);
            cir.setReturnValue(false);
        }
    }

    /** Riding you, or floating just over your head with no other player closer to it. */
    private static boolean isAttachedTo(Entity entity, ClientPlayerEntity self) {
        if (entity.getRootVehicle() == self) {
            return true;
        }
        double dx = self.getX() - entity.getX();
        double dz = self.getZ() - entity.getZ();
        double selfDistSq = dx * dx + dz * dz;
        if (selfDistSq > HORIZONTAL_RADIUS_SQ) {
            return false;
        }
        double dy = entity.getY() - self.getY();
        if (dy < MIN_HEIGHT_ABOVE || dy > MAX_HEIGHT_ABOVE) {
            return false;
        }
        for (PlayerEntity other : self.getEntityWorld().getPlayers()) {
            if (other == self) {
                continue;
            }
            double ox = other.getX() - entity.getX();
            double oz = other.getZ() - entity.getZ();
            if (ox * ox + oz * oz < selfDistSq) {
                return false;
            }
        }
        return true;
    }

    /** Riding a player, or floating in the tag window over any player. */
    private static boolean isAboveAnyPlayer(Entity entity, ClientPlayerEntity self) {
        if (entity.getRootVehicle() instanceof PlayerEntity) {
            return true;
        }
        for (PlayerEntity player : self.getEntityWorld().getPlayers()) {
            double dx = player.getX() - entity.getX();
            double dz = player.getZ() - entity.getZ();
            if (dx * dx + dz * dz > HORIZONTAL_RADIUS_SQ) {
                continue;
            }
            double dy = entity.getY() - player.getY();
            if (dy >= MIN_HEIGHT_ABOVE && dy <= MAX_HEIGHT_ABOVE) {
                return true;
            }
        }
        return false;
    }
}
