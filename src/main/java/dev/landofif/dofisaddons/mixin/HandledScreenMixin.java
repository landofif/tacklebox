package dev.landofif.dofisaddons.mixin;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.ui.Foe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.util.Glyphs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * Chests, the inventory and fishonmc menus as one foecollab box: the menu name in its top edge, a label
 * above your own inventory, and no slot boxes. fishonmc draws its menu art with glyphs in the title, so
 * dropping the title glyphs drops the art too (see util/Glyphs). Only screens that are a plain grid of
 * slots are restyled; furnaces, anvils and the like keep their textures because their backgrounds carry
 * progress arrows and buttons.
 */
@Mixin(HandledScreen.class)
abstract class HandledScreenMixin<T extends ScreenHandler> extends Screen {
    @Shadow
    protected int x;
    @Shadow
    protected int y;
    @Shadow
    protected int backgroundWidth;
    @Shadow
    protected int backgroundHeight;
    @Shadow
    @Final
    protected T handler;
    @Shadow
    protected @Nullable Slot focusedSlot;

    private HandledScreenMixin(Text title) {
        super(title);
    }

    @Unique
    private boolean dofisaddons$restyled() {
        Object self = this;
        return DofiSettings.get().flatMenus && (self instanceof GenericContainerScreen || self instanceof ShulkerBoxScreen || self instanceof HopperScreen
                || self instanceof Generic3x3ContainerScreen || self instanceof InventoryScreen);
    }

    /** Top of your own inventory grid (relative to the panel), or -1 if the screen has none. */
    @Unique
    private int dofisaddons$playerGridTop() {
        int top = Integer.MAX_VALUE;
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof PlayerInventory && slot.getIndex() >= 9 && slot.getIndex() < 36) {
                top = Math.min(top, slot.y);
            }
        }
        return top == Integer.MAX_VALUE ? -1 : top;
    }

    @WrapOperation(method = "renderBackground", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawBackground(Lnet/minecraft/client/gui/DrawContext;FII)V"))
    private void dofisaddons$panel(HandledScreen<?> self, DrawContext ctx, float delta, int mouseX, int mouseY, Operation<Void> original) {
        if (!dofisaddons$restyled()) {
            original.call(self, ctx, delta, mouseX, mouseY);
            return;
        }
        boolean inventory = self instanceof InventoryScreen;
        Foe.panel(ctx, x, y, backgroundWidth, backgroundHeight, inventory ? null : Foe.text(Glyphs.menuName(title)), 100); // the armour slots start at the top of the inventory
        int grid = dofisaddons$playerGridTop();
        if (grid > 0 && !inventory) {
            ctx.drawTextWithShadow(textRenderer, Foe.text("inventory"), x + 8, y + grid - 11, Foe.TITLE);
        }
        if (inventory && client != null && client.player != null) {
            // the player preview and the crafting arrow were part of the texture
            Foe.box(ctx, x + 26, y + 8, 49, 70, 100);
            InventoryScreen.drawEntity(ctx, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F, mouseX, mouseY, client.player);
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("→"), x + 143, y + 32, Foe.DIM);
        }
    }

    /** The title and the "inventory" label are drawn in the box now. */
    @WrapOperation(method = "renderMain", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V"))
    private void dofisaddons$noForeground(HandledScreen<?> self, DrawContext ctx, int mouseX, int mouseY, Operation<Void> original) {
        if (!dofisaddons$restyled()) {
            original.call(self, ctx, mouseX, mouseY);
        }
    }
}
