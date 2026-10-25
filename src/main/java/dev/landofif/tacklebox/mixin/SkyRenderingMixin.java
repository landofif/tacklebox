package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.SkyRendering;
import net.minecraft.client.render.state.SkyRenderState;
import net.minecraft.client.world.ClientWorld;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The black sky setting: no sun, moon or stars, a black sky colour and no sunset glow. Only the client's picture changes. */
@Mixin(SkyRendering.class)
abstract class SkyRenderingMixin {
    @Inject(method = "updateRenderState", at = @At("TAIL"))
    private void tacklebox$blackSky(ClientWorld world, float tickProgress, Camera camera, SkyRenderState state, CallbackInfo ci) {
        if (!TackleboxSettings.get().blackSky) return;
        state.skyColor = 0xFF000000;
        state.sunriseAndSunsetColor = 0;
        state.starBrightness = 0;
        state.rainGradient = 0;
    }

    @Inject(method = "renderCelestialBodies", at = @At("HEAD"), cancellable = true)
    private void tacklebox$noCelestialBodies(CallbackInfo ci) {
        if (TackleboxSettings.get().blackSky) ci.cancel();
    }
}
