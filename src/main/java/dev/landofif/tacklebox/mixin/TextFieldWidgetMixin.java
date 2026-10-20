package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Hypr;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/** Text fields get the rounded look of the buttons (with the flat buttons setting): a dark fill, the accent outline while focused. */
@Mixin(TextFieldWidget.class)
abstract class TextFieldWidgetMixin extends ClickableWidget {
    private TextFieldWidgetMixin(int x, int y, int width, int height, Text message) {
        super(x, y, width, height, message);
    }

    @WrapOperation(method = "renderWidget", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/DrawContext;drawGuiTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIII)V"), require = 0)
    private void tacklebox$roundedField(DrawContext ctx, RenderPipeline pipeline, Identifier id, int x, int y, int w, int h, Operation<Void> original) {
        if (!TackleboxSettings.Menus.flatButtons) {
            original.call(ctx, pipeline, id, x, y, w, h);
            return;
        }
        int r = Math.min(4, h / 2);
        Hypr.rrect(ctx, x, y, w, h, r, 0xB4181825);
        if (isFocused()) {
            Hypr.ring(ctx, x, y, w, h, r, 1, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x, w, 1), 8);
        } else {
            Hypr.ring(ctx, x, y, w, h, r, 1, Hypr.INACTIVE);
        }
    }
}
