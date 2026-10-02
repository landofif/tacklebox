package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.StatsScreen;
import net.minecraft.client.gui.screen.world.CreateWorldScreen;
import net.minecraft.client.gui.widget.TabNavigationWidget;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

/**
 * The textured 2px header and footer lines on tabbed screens (create world, statistics) become 1px
 * border lines, and the create world header strip becomes flat.
 */
@Mixin({TabNavigationWidget.class, CreateWorldScreen.class, StatsScreen.class})
abstract class SeparatorMixin {
    @WrapOperation(method = {"render", "renderDarkening(Lnet/minecraft/client/gui/DrawContext;)V"}, require = 0, at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/DrawContext;drawTexture(Lcom/mojang/blaze3d/pipeline/RenderPipeline;Lnet/minecraft/util/Identifier;IIFFIIII)V"))
    private void dofisaddons$flatSeparator(DrawContext ctx, RenderPipeline pipeline, Identifier texture, int x, int y, float u, float v,
                                           int w, int h, int texW, int texH, Operation<Void> original) {
        if (!S24.everywhere()) {
            original.call(ctx, pipeline, texture, x, y, u, v, w, h, texW, texH);
        } else if (texture.equals(Screen.HEADER_SEPARATOR_TEXTURE) || texture.equals(Screen.INWORLD_HEADER_SEPARATOR_TEXTURE)) {
            ctx.fill(x, y + h - 1, x + w, y + h, S24.BORDER);
        } else if (texture.equals(Screen.FOOTER_SEPARATOR_TEXTURE) || texture.equals(Screen.INWORLD_FOOTER_SEPARATOR_TEXTURE)) {
            ctx.fill(x, y, x + w, y + 1, S24.BORDER);
        } else if (texture.getPath().contains("tab_header_background")) {
            ctx.fill(x, y, x + w, y + h, S24.BG_4);
        } else {
            original.call(ctx, pipeline, texture, x, y, u, v, w, h, texW, texH);
        }
    }
}
