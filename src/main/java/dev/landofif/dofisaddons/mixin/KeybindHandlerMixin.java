package dev.landofif.dofisaddons.mixin;

import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.lang.reflect.Field;

/**
 * Foecollabs bait sorting key becomes a toggle: its tick sets {@code visualizeBaitSorting} to whether the key is held,
 * and at the end of the tick each press flips our own state, which is written back instead. By name, reflection,
 * {@code require = 0}, see {@link BoxRendererMixin}.
 */
@Mixin(targets = "io.github.foecollab.handler.KeybindHandler")
abstract class KeybindHandlerMixin {
    private static boolean dofisaddons$wasHeld, dofisaddons$on;

    @Inject(method = "tick(Lnet/minecraft/class_310;)V", at = @At("TAIL"), remap = false, require = 0)
    private void dofisaddons$toggle(MinecraftClient client, CallbackInfo ci) {
        if (client.currentScreen == null) { // foecollab only reads the key with a screen open
            return;
        }
        try {
            Field f = getClass().getField("visualizeBaitSorting");
            boolean held = f.getBoolean(this);
            if (held && !dofisaddons$wasHeld) {
                dofisaddons$on = !dofisaddons$on;
            }
            dofisaddons$wasHeld = held;
            f.setBoolean(this, dofisaddons$on);
        } catch (ReflectiveOperationException ignored) {
            // foecollab changed; keep its hold behaviour
        }
    }
}
