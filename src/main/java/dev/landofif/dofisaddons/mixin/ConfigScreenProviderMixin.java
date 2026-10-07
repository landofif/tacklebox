package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.compat.ClothTab;
import me.shedaniel.autoconfig.gui.ConfigScreenProvider;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import net.minecraft.text.TranslatableTextContent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.util.function.Function;

/** Adds the "dofis addons" tab to foecollab's config screen, just before cloth builds it. */
@Mixin(value = ConfigScreenProvider.class, remap = false)
abstract class ConfigScreenProviderMixin {
    @WrapOperation(method = "get()Lnet/minecraft/client/gui/screen/Screen;", remap = true, at = @At(value = "INVOKE", target = "Ljava/util/function/Function;apply(Ljava/lang/Object;)Ljava/lang/Object;", remap = false), require = 0)
    private Object dofisaddons$tab(Function<Object, Object> build, Object arg, Operation<Object> original) {
        if (arg instanceof ConfigBuilder builder && builder.getTitle().getContent() instanceof TranslatableTextContent t
                && t.getKey().equals("text.autoconfig.foecollab.title")) {
            ClothTab.add(builder);
        }
        return original.call(build, arg);
    }
}
