package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.projectile.FishingBobberEntity;

/** The "hide other bobbers" rule, shared by the render cull and the foecollab bait display skip. */
public final class OtherBobbers {
    /** Lag test from {@code /tacklebox debug displays}: not saved, off on every start. */
    public static boolean hideDisplays;

    private OtherBobbers() {}

    /** True for a bobber that is not yours while the setting is on. */
    public static boolean hidden(Entity entity) {
        return entity instanceof FishingBobberEntity bobber
                && hides(TackleboxSettings.Bobber.hideOtherBobbers, bobber.getPlayerOwner(), MinecraftClient.getInstance().player);
    }

    static boolean hides(boolean setting, Object owner, Object self) {
        return setting && self != null && owner != self;
    }
}
