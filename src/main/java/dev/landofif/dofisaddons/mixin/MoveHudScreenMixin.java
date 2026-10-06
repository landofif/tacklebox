package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.compat.FoeMover;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Puts the mods huds into foecollabs hud mover (see compat/FoeMover). Targets its own method, which
 * keeps its name in the released jar, and is optional so a foecollab update that moves it only
 * loses the boxes instead of stopping the game.
 */
@Mixin(targets = "io.github.foecollab.screens.movehud.MoveHudScreen")
abstract class MoveHudScreenMixin extends Screen {
    private MoveHudScreenMixin(Text title) {
        super(title);
    }

    @Inject(method = "renderWidgets", at = @At("RETURN"), remap = false, require = 0)
    private void dofisaddons$ourHuds(CallbackInfo ci) {
        for (ClickableWidget box : FoeMover.boxes()) {
            addDrawableChild(box);
        }
    }
}
