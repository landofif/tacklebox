package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.compat.FoeDailyMissions;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Teaches foecollab's daily mission tracker the layout of 2026-10-11, see {@link FoeDailyMissions}. By name, {@code require = 0}, see {@link BoxRendererMixin}. */
@Mixin(targets = "io.github.foecollab.handler.DailyQuestHandler")
abstract class DailyQuestHandlerMixin {
    @Shadow(remap = false) public List<Object> quests;
    @Shadow(remap = false) public boolean questMenuState;

    @Inject(method = "tick", at = @At("TAIL"), remap = false, require = 0)
    private void dofisaddons$newLayout(CallbackInfo ci) {
        if (questMenuState) {
            FoeDailyMissions.fill(quests, MinecraftClient.getInstance());
        }
    }
}
