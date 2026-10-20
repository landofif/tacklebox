package dev.landofif.tacklebox.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import org.spongepowered.asm.mixin.Mixin;

/** Foecollabs equipment hud is switched off while the mods own row (hud/EquipmentRow) is on. By name, {@code require = 0}. */
@Mixin(targets = "io.github.foecollab.screens.hud.EquipmentHud")
abstract class EquipmentHudMixin {
    @WrapMethod(method = "render", remap = false, require = 0)
    private void tacklebox$off(DrawContext ctx, MinecraftClient client, Operation<Void> original) {
        if (!TackleboxSettings.Huds.equipHud) {
            original.call(ctx, client);
        }
    }
}
