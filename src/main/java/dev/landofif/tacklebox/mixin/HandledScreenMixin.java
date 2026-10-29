package dev.landofif.tacklebox.mixin;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.landofif.tacklebox.display.PetSale;
import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.menu.QuestRedo;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.component.type.NbtComponent;
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
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/**
 * Chests, the inventory and fishonmc menus with no box at all: the menu name where a box would put it, a
 * label above your own inventory, and no slot boxes. fishonmc draws its menu art with glyphs in the title, so
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
    private boolean tacklebox$restyled() {
        Object self = this;
        // the auction house pages (title art U+D541-D546) keep the server's chest art
        return TackleboxSettings.get().flatMenus && !title.getString().chars().anyMatch(c -> c >= 0xD541 && c <= 0xD546) && (self instanceof GenericContainerScreen || self instanceof ShulkerBoxScreen || self instanceof HopperScreen
                || self instanceof Generic3x3ContainerScreen || self instanceof InventoryScreen);
    }

    /** Top of your own inventory grid (relative to the panel), or -1 if the screen has none. */
    @Unique
    private int tacklebox$playerGridTop() {
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
    private void tacklebox$panel(HandledScreen<?> self, DrawContext ctx, float delta, int mouseX, int mouseY, Operation<Void> original) {
        if (!tacklebox$restyled()) {
            original.call(self, ctx, delta, mouseX, mouseY);
            return;
        }
        dev.landofif.tacklebox.display.MenuBackground.draw(ctx, x, y, backgroundWidth, backgroundHeight);
        boolean inventory = self instanceof InventoryScreen;
        if (!inventory) { // the armour slots start at the top of the inventory, so it has no title
            Foe.title(ctx, x, y, Foe.text(Glyphs.menuName(title)));
        }
        int grid = tacklebox$playerGridTop();
        if (grid > 0 && !inventory) {
            ctx.drawTextWithShadow(textRenderer, Foe.text("inventory"), x + 8, y + grid - 11, Foe.TITLE);
        }
        if (inventory && client != null && client.player != null) {
            // the player preview and the crafting arrow were part of the texture
            InventoryScreen.drawEntity(ctx, x + 26, y + 8, x + 75, y + 78, 30, 0.0625F, mouseX, mouseY, client.player);
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("→"), x + 143, y + 32, Foe.DIM);
        }
    }

    /** A pet the store would pay more for than the auction asks: a see-through green square behind its item (setting petProfitMarks). */
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void tacklebox$profitTint(DrawContext ctx, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (TackleboxSettings.get().petProfitMarks && PetSale.of(slot.getStack()).filter(PetSale.Sale::profitable).isPresent()) {
            ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x6655FF55);
        }
    }

    /** In the /travel chest a place in a frenzy gets a see-through green square behind its item, the fabled event's place a red one. */
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void tacklebox$travelMark(DrawContext ctx, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (slot.inventory instanceof PlayerInventory || slot.getStack().isEmpty()) {
            return;
        }
        int mark = Locations.mark(slot.getStack().getName().getString(), ServerHud.fabledPlace());
        NbtComponent data = mark != 0 ? slot.getStack().get(DataComponentTypes.CUSTOM_DATA) : null;
        if (data != null && Locations.isTravelItem(data.copyNbt())) {
            ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x88000000 | (mark & 0xFFFFFF));
        }
    }

    /** In /quests the quest items are remembered, and the difficulty and tier the clicked quest had get a see-through green square. */
    @Inject(method = "drawSlot", at = @At("HEAD"))
    private void tacklebox$questRedo(DrawContext ctx, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
        if (slot.inventory instanceof PlayerInventory || slot.getStack().isEmpty() || !"quests".equals(Glyphs.menuName(getTitle()))) {
            return;
        }
        String name = slot.getStack().getName().getString();
        List<Text> lore = slot.getStack().getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines();
        QuestRedo.see(QuestRedo.number(name, slot.id), lore);
        if (QuestRedo.marks(name, lore)) {
            ctx.fill(slot.x, slot.y, slot.x + 16, slot.y + 16, 0x8855FF55);
        }
    }

    @Inject(method = "onMouseClick(Lnet/minecraft/screen/slot/Slot;IILnet/minecraft/screen/slot/SlotActionType;)V", at = @At("HEAD"))
    private void tacklebox$questClick(Slot slot, int slotId, int button, SlotActionType action, CallbackInfo ci) {
        if (slot == null || slot.inventory instanceof PlayerInventory || slot.getStack().isEmpty() || !"quests".equals(Glyphs.menuName(getTitle()))) {
            return;
        }
        QuestRedo.click(QuestRedo.number(slot.getStack().getName().getString(), slot.id),
                slot.getStack().getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines());
    }

    /** The title and the "inventory" label are drawn in the box now. */
    @WrapOperation(method = "renderMain", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/client/gui/screen/ingame/HandledScreen;drawForeground(Lnet/minecraft/client/gui/DrawContext;II)V"))
    private void tacklebox$noForeground(HandledScreen<?> self, DrawContext ctx, int mouseX, int mouseY, Operation<Void> original) {
        if (!tacklebox$restyled()) {
            original.call(self, ctx, mouseX, mouseY);
        }
    }
}
