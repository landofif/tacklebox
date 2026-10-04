package dev.landofif.dofisaddons.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.util.Glyphs;
import dev.landofif.dofisaddons.util.Mono;
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
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Chests, the inventory and fishonmc menus as system24 panels: the menu name set into the top border,
 * a labelled line above your own inventory, and flat slot boxes instead of the texture. fishonmc draws
 * its menu art with glyphs in the title, so dropping the title glyphs drops the art too (see util/Glyphs).
 * Only screens that are a plain grid of slots are restyled; furnaces, anvils and the like keep their
 * textures because their backgrounds carry progress arrows and buttons.
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
        return S24.menus() && (self instanceof GenericContainerScreen || self instanceof ShulkerBoxScreen || self instanceof HopperScreen
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
        String name = inventory ? "inventory" : Glyphs.menuName(title);
        int top = inventory ? 0 : 4; // the inventory's armour slots start right at the top
        S24.panel(ctx, x, y + top, backgroundWidth, backgroundHeight - top, Mono.text(name));
        int grid = dofisaddons$playerGridTop();
        if (grid > 0) {
            S24.divider(ctx, x, y + grid - 7, backgroundWidth, inventory ? null : Mono.text("inventory"));
        }
        if (inventory && client != null && client.player != null) {
            // the player preview box and the crafting arrow were part of the texture
            S24.rect(ctx, x + 26, y + 8, 49, 70, S24.BG_3, S24.BORDER);
            InventoryScreen.drawEntity(ctx, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F, mouseX, mouseY, client.player);
            ctx.drawCenteredTextWithShadow(textRenderer, Mono.text("→"), x + 143, y + 32, S24.TEXT_4);
        }
    }

    /** The title and "inventory" label are in the borders now. */
    @WrapOperation(method = "renderMain", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V"))
    private void dofisaddons$noForeground(HandledScreen<?> self, DrawContext ctx, int mouseX, int mouseY, Operation<Void> original) {
        if (!dofisaddons$restyled()) {
            original.call(self, ctx, mouseX, mouseY);
        }
    }

    @Inject(method = "drawSlots", at = @At("HEAD"))
    private void dofisaddons$slotBoxes(DrawContext ctx, int mouseX, int mouseY, CallbackInfo ci) {
        if (!dofisaddons$restyled()) {
            return;
        }
        for (Slot slot : handler.slots) {
            if (slot.isEnabled()) {
                S24.fill(ctx, slot.x - 1, slot.y - 1, 17, 17, S24.BG_2);
            }
        }
    }

    @Inject(method = "drawSlotHighlightBack", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$noHighlightBack(DrawContext ctx, CallbackInfo ci) {
        if (dofisaddons$restyled()) {
            ci.cancel();
        }
    }

    @Inject(method = "drawSlotHighlightFront", at = @At("HEAD"), cancellable = true)
    private void dofisaddons$highlight(DrawContext ctx, CallbackInfo ci) {
        if (!dofisaddons$restyled()) {
            return;
        }
        ci.cancel();
        if (focusedSlot != null && focusedSlot.canBeHighlighted()) {
            int sx = focusedSlot.x;
            int sy = focusedSlot.y;
            S24.fill(ctx, sx - 1, sy - 1, 17, 17, S24.alpha(0x30, S24.ACCENT));
            S24.outline(ctx, sx - 2, sy - 2, 19, 19, S24.ACCENT);
        }
    }
}
