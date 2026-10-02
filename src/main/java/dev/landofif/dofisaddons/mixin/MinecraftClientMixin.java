package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.screen.DofiTitleScreen;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Swaps the minecraft title screen for the mods own whenever the game opens it. */
@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
    @ModifyVariable(method = "setScreen", at = @At("HEAD"), argsOnly = true)
    private Screen dofisaddons$titleScreen(Screen screen) {
        DofiSettings settings = StatsTracker.settings();
        if (screen instanceof TitleScreen && settings != null && settings.titleScreen) {
            return new DofiTitleScreen();
        }
        return screen;
    }
}
