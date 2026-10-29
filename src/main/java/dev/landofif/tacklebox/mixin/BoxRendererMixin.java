package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Foecollab draws the panels and buttons of its inventory with its box textures through one method. Boxes
 * disappear (the inventory is just its contents, like ours) and its buttons become our flat ones. By name:
 * foecollab is never compiled against, and {@code require = 0} so an update that renames it only loses this.
 */
@Mixin(targets = "io.github.foecollab.screens.widget.container.BoxRenderer")
abstract class BoxRendererMixin {
    @Inject(method = "draw(Lnet/minecraft/class_332;Lnet/minecraft/class_2960;IIIII)V", at = @At("HEAD"), cancellable = true, remap = false, require = 0)
    private static void tacklebox$ourLook(DrawContext ctx, Identifier texture, int x, int y, int w, int h, int color, CallbackInfo ci) {
        if (!TackleboxSettings.get().flatMenus) {
            return;
        }
        ci.cancel();
        String path = texture.getPath();
        if (path.contains("solid")) { // the buttons; the alt texture is the hovered one
            Foe.button(ctx, x, y, w, h, path.contains("alt"));
        }
    }
}
