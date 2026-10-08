package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.screen.FishMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Swaps the fishonmc menu chests for the mods menu screens (see menu/FishMenu).
 */
@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
    @Shadow
    public @Nullable Screen currentScreen;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$screens(@Nullable Screen screen, CallbackInfo ci) {
        if (FishMenu.wants(screen)) {
            ci.cancel();
            GenericContainerScreen chest = (GenericContainerScreen) screen;
            int pv = dev.landofif.dofisaddons.menu.Vaults.number(screen);
            setScreen(pv > 0 ? new dev.landofif.dofisaddons.screen.VaultScreen(chest, pv) : FishMenu.isPresets(screen) ? new dev.landofif.dofisaddons.screen.PresetsScreen(chest) : new FishMenuScreen(chest));
        }
    }
}
