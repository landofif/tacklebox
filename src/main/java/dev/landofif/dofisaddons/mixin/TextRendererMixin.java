package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.font.TextHandler;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.text.Style;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * "Mono font everywhere", width side: text is measured with the same font swap the drawer makes (see
 * {@link TextRendererDrawerMixin}), so layout, wrapping and cursors stay right.
 */
@Mixin(TextRenderer.class)
abstract class TextRendererMixin {
    @Shadow
    @Final
    @Mutable
    private TextHandler handler;

    @Inject(method = "<init>", at = @At("RETURN"))
    private void dofisaddons$monoWidths(TextRenderer.GlyphsProvider fonts, CallbackInfo ci) {
        this.handler = new TextHandler((codePoint, style) -> {
            Style s = Mono.everywhere(style, false);
            return fonts.getGlyphs(s.getFont()).get(codePoint).getMetrics().getAdvance(s.isBold());
        });
    }
}
