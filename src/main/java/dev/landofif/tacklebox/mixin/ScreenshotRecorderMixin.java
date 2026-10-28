package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.display.ScreenshotPreview;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.io.File;
import java.util.function.Consumer;

/** Hands each screenshot to display/ScreenshotPreview right after the game wrote the file (the save lambda, on the io thread). */
@Mixin(ScreenshotRecorder.class)
abstract class ScreenshotRecorderMixin {
    @Inject(method = "method_22691", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/texture/NativeImage;writeTo(Ljava/io/File;)V", shift = At.Shift.AFTER))
    private static void tacklebox$preview(NativeImage image, File file, Consumer<Text> messages, CallbackInfo ci) {
        try {
            ScreenshotPreview.saved(image, file);
        } catch (RuntimeException e) { // the game would print "couldn't save screenshot" for a file it did save
            Tacklebox.LOGGER.warn("screenshot preview failed", e);
        }
    }
}
