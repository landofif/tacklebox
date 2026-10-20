package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.tacklebox.hud.FoeCover;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Coerce;

import java.util.List;

/** Foecollab's custom huds are drawn one by one in one renderer; each steps aside on its own (see {@link FoeCover}). */
@Mixin(targets = "io.github.foecollab.screens.hud.CustomHudRenderer")
abstract class FoeCustomHudsMixin {
    @WrapOperation(method = "render", remap = false, require = 0,
            at = @At(value = "INVOKE", target = "Lio/github/foecollab/screens/hud/CustomHudRenderer;renderHud", remap = false))
    private void tacklebox$stepAside(@Coerce Object self, DrawContext ctx, MinecraftClient client,
                                     @Coerce Object hud, List<Text> lines, Operation<Void> original) {
        FoeCover.run(hud, ctx, () -> original.call(self, ctx, client, hud, lines));
    }
}
