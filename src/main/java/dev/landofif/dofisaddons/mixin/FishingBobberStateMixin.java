package dev.landofif.dofisaddons.mixin;

import net.minecraft.client.render.entity.state.FishingBobberEntityState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(FishingBobberEntityState.class)
public class FishingBobberStateMixin implements BobberStateAccess {
    @Unique
    private boolean dofisaddons$mine;

    @Override
    public boolean dofisaddons$isMine() {
        return dofisaddons$mine;
    }

    @Override
    public void dofisaddons$setMine(boolean mine) {
        this.dofisaddons$mine = mine;
    }
}
