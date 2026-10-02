package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Screen backgrounds in every screen: flat bg-4 instead of the panorama, blur and dirt texture when not
 * in a world, and a flat dark wash instead of the gradient over the game. The mods own screens draw
 * their background themselves (see ui/DofiScreen), and get an opening animation here: their contents
 * are drawn slightly lower and covered, and both ease away.
 */
@Mixin(Screen.class)
abstract class ScreenMixin {
    @Shadow
    protected MinecraftClient client;
    @Shadow
    public int width;
    @Shadow
    public int height;

    @Inject(method = "renderPanoramaBackground", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatPanorama(DrawContext ctx, float delta, CallbackInfo ci) {
        if (S24.everywhere()) {
            ctx.fill(0, 0, width, height, S24.BG_4);
            ci.cancel();
        }
    }

    @Inject(method = "applyBlur", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$noBlur(DrawContext ctx, CallbackInfo ci) {
        if (S24.everywhere()) {
            ci.cancel();
        }
    }

    @Inject(method = "renderDarkening(Lnet/minecraft/client/gui/DrawContext;IIII)V", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatDarkening(DrawContext ctx, int x, int y, int w, int h, CallbackInfo ci) {
        if (S24.everywhere()) {
            ctx.fill(x, y, x + w, y + h, client.world == null ? S24.BG_4 : S24.alpha(0xB4, S24.BG_4));
            ci.cancel();
        }
    }

    @Inject(method = "renderInGameBackground", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatInGame(DrawContext ctx, CallbackInfo ci) {
        if (S24.everywhere()) {
            ctx.fill(0, 0, width, height, S24.alpha(0x96, S24.BG_4));
            ci.cancel();
        }
    }

    @WrapOperation(method = "renderWithTooltip", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/Screen;render(Lnet/minecraft/client/gui/DrawContext;IIF)V"))
    private void dofisaddons$intro(Screen self, DrawContext ctx, int mouseX, int mouseY, float delta, Operation<Void> original) {
        if (!(self instanceof DofiScreen dofi)) {
            original.call(self, ctx, mouseX, mouseY, delta);
            return;
        }
        float offset = dofi.introOffset();
        if (offset <= 0.01f) {
            original.call(self, ctx, mouseX, mouseY, delta);
            return;
        }
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(0, offset);
        original.call(self, ctx, mouseX, mouseY, delta);
        ctx.getMatrices().popMatrix();
        dofi.drawIntroCover(ctx);
    }
}
