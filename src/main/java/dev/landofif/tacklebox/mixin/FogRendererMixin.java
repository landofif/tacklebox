package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.block.enums.CameraSubmersionType;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.fog.FogRenderer;
import org.joml.Vector4f;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** The black sky setting: the fog in the air is black too, or the horizon would stay bright. Water, lava and snow fog keep their colour. */
@Mixin(FogRenderer.class)
abstract class FogRendererMixin {
    @ModifyReturnValue(method = "getFogColor", at = @At("RETURN"))
    private Vector4f tacklebox$blackFog(Vector4f color, Camera camera) {
        if (!TackleboxSettings.get().blackSky || camera.getSubmersionType() != CameraSubmersionType.ATMOSPHERIC) return color;
        return new Vector4f(0f, 0f, 0f, color.w);
    }
}
