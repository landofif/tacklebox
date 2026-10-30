package dev.landofif.tacklebox.foemixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.tacklebox.hud.FoeCover;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

/**
 * Wraps the draw call of each foecollab hud that can sit under the chat (see {@link FoeCover}). By name and
 * without descriptors, like {@code MoveHudScreenMixin}; optional, so a foecollab update that renames them only
 * loses the option for those huds.
 */
@Mixin(targets = "io.github.foecollab.screens.hud.MainHudRenderer")
abstract class FoeHudsMixin {
    private static final String P = "Lio/github/foecollab/screens/hud/";

    @WrapOperation(method = "render", remap = false, require = 0, at = {
            @At(value = "INVOKE", target = P + "FishTrackerHud;render", remap = false),
            @At(value = "INVOKE", target = P + "PetEquipHud;render", remap = false),
            @At(value = "INVOKE", target = P + "ContestHud;render", remap = false),
            @At(value = "INVOKE", target = P + "BaitHud;render", remap = false),
            @At(value = "INVOKE", target = P + "ChummerHud;render", remap = false),
            @At(value = "INVOKE", target = P + "EquipmentHud;render", remap = false),
            @At(value = "INVOKE", target = P + "CrewHud;render", remap = false),
            @At(value = "INVOKE", target = P + "QuestHud;render", remap = false),
            @At(value = "INVOKE", target = P + "DailyQuestHud;render", remap = false),
            @At(value = "INVOKE", target = P + "LevelHud;render", remap = false)})
    private void tacklebox$stepAside(@Coerce Object hud, DrawContext ctx, MinecraftClient client, Operation<Void> original) {
        FoeCover.run(hud, ctx, () -> original.call(hud, ctx, client));
    }
}
