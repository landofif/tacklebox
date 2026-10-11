package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.duck.BobberStateAccess;
import net.minecraft.client.render.entity.state.FishingBobberEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FishingBobberEntityState.class)
public class FishingBobberStateMixin implements BobberStateAccess {
    @Unique
    private boolean tacklebox$mine;

    @Override
    public boolean tacklebox$isMine() {
        return tacklebox$mine;
    }

    @Override
    public void tacklebox$setMine(boolean mine) {
        this.tacklebox$mine = mine;
    }
}
