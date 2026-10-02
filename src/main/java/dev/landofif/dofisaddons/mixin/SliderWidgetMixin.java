package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.SliderWidget;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Sliders in the mod's screens: a flat box, the filled part shaded, an accent line for the handle. */
@Mixin(SliderWidget.class)
abstract class SliderWidgetMixin {
    @Shadow
    protected double value;

    @Inject(method = "renderWidget", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$flatSlider(DrawContext ctx, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        if (!S24.active()) {
            return;
        }
        SliderWidget self = (SliderWidget) (Object) this;
        int x = self.getX();
        int y = self.getY();
        int w = self.getWidth();
        int h = self.getHeight();
        boolean hot = self.isSelected();
        S24.box(ctx, x, y, w, h, hot, true);
        int fill = x + 1 + (int) Math.round(value * (w - 2));
        ctx.fill(x + 1, y + 1, fill, y + h - 1, 0x22000000 | (S24.ACCENT & 0xFFFFFF));
        ctx.fill(Math.max(x + 1, fill - 1), y + 1, Math.min(x + w - 1, fill + 1), y + h - 1, hot ? S24.ACCENT : S24.ACCENT_DIM);
        var tr = MinecraftClient.getInstance().textRenderer;
        ctx.drawCenteredTextWithShadow(tr, self.getMessage(), x + w / 2, y + (h - 8) / 2, S24.TEXT_1);
        ci.cancel();
    }
}
