package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * "Mono font everywhere", drawing side: every character the game draws goes through here, and runs in
 * the minecraft font are switched to dm mono (see {@link Mono#everywhere}).
 */
@Mixin(targets = "net.minecraft.client.font.TextRenderer$Drawer")
abstract class TextRendererDrawerMixin {
    @Shadow
    @Final
    private boolean shadow;

    @ModifyVariable(method = "accept(ILnet/minecraft/text/Style;I)Z", at = @At("HEAD"), argsOnly = true)
    private Style dofisaddons$mono(Style style) {
        return Mono.everywhere(style, this.shadow);
    }
}
