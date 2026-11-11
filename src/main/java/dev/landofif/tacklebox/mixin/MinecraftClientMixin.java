package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.screen.FishMenuScreen;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.menu.ChestScale;
import net.minecraft.client.util.Window;

/**
 * Swaps the fishonmc menu chests for the mods menu screens (see menu/FishMenu), and puts server chest menus on their
 * own gui scale (menu/ChestScale).
 */
@Mixin(MinecraftClient.class)
abstract class MinecraftClientMixin {
    @Shadow
    public @Nullable Screen currentScreen;

    @Shadow
    public abstract void setScreen(@Nullable Screen screen);

    @Inject(method = "setScreen", at = @At("HEAD"), cancellable = true)
    private void tacklebox$screens(@Nullable Screen screen, CallbackInfo ci) {
        if (FishMenu.wants(screen)) {
            ci.cancel();
            GenericContainerScreen chest = (GenericContainerScreen) screen;
            int pv = dev.landofif.tacklebox.menu.Vaults.number(screen);
            setScreen(pv > 0 ? new dev.landofif.tacklebox.screen.VaultScreen(chest, pv) : FishMenu.isPresets(screen) ? new dev.landofif.tacklebox.screen.PresetsScreen(chest) : new FishMenuScreen(chest));
        }
    }

    /** Before the new screen reads the scaled size for its layout. */
    @Inject(method = "setScreen", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;getScaledWidth()I", ordinal = 0))
    private void tacklebox$chestScale(@Nullable Screen screen, CallbackInfo ci) {
        ChestScale.apply((MinecraftClient) (Object) this, false);
    }

    /** Closing to no screen has no layout step, so the games scale comes back here. */
    @Inject(method = "setScreen", at = @At("TAIL"))
    private void tacklebox$chestScaleBack(@Nullable Screen screen, CallbackInfo ci) {
        ChestScale.apply((MinecraftClient) (Object) this, true);
    }

    /** A window resize or a gui scale change while a chest is open keeps the chest scale. */
    @WrapOperation(method = "onResolutionChanged", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/util/Window;calculateScaleFactor(IZ)I"))
    private int tacklebox$chestScaleResize(Window window, int guiScale, boolean unicode, Operation<Integer> original) {
        return original.call(window, ChestScale.guiScale(ChestScale.isChest(currentScreen), TackleboxSettings.get().chestScale, guiScale), unicode);
    }

    @Inject(method = "doItemUse", at = @At("HEAD"), cancellable = true)
    private void tacklebox$use(CallbackInfo ci) {
        if (dev.landofif.tacklebox.input.FishKey.blockUse()) { // the fish key on right mouse is sneak in the minigame
            ci.cancel();
            return;
        }
        dev.landofif.tacklebox.display.BobberTimer.onUse((MinecraftClient) (Object) this);
    }
}
