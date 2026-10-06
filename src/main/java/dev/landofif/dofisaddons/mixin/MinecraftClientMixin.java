package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.compat.FoeConfig;
import dev.landofif.dofisaddons.compat.FoeScreens;
import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.screen.FishMenuScreen;
import dev.landofif.dofisaddons.screen.SettingsScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import dev.landofif.dofisaddons.screen.DofiTitleScreen;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.TitleScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * The title screen you picked in settings → look: the mods own, foecollab's, or minecraft's. foecollab
 * swaps the minecraft one for its own while it starts up; for "minecraft" that swap is skipped. Also
 * sends foecollab's cloth config screen to the mods settings → foecollab tab, so both share one screen.
 */
@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
    @Shadow
    public @Nullable Screen currentScreen;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$titleScreen(@Nullable Screen screen, CallbackInfo ci) {
        if (FoeScreens.isClothConfig(screen) && FoeConfig.available()) {
            // one settings screen for both mods: foecollab's options live in settings → foecollab
            ci.cancel();
            setScreen(SettingsScreen.foecollab(currentScreen));
            return;
        }
        if (FishMenu.wants(screen)) {
            ci.cancel();
            setScreen(new FishMenuScreen((GenericContainerScreen) screen));
            return;
        }
        DofiSettings settings = StatsTracker.settings();
        if (settings == null || settings.titleChoice == null) {
            return;
        }
        if (screen instanceof TitleScreen) {
            Screen replacement = switch (settings.titleChoice) {
                case DOFIS -> new DofiTitleScreen();
                case FOECOLLAB -> FoeScreens.title();
                case MINECRAFT -> null;
            };
            if (replacement != null) {
                ci.cancel();
                setScreen(replacement);
            }
        } else if (settings.titleChoice == DofiSettings.TitleChoice.MINECRAFT && FoeScreens.isTitle(screen)
                && currentScreen instanceof TitleScreen) {
            ci.cancel();
        }
    }
}
