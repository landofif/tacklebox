package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.util.Glyphs;
import dev.landofif.dofisaddons.util.Mono;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.InventoryScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.registry.Registries;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The fishonmc /presets menu like a skyblock wardrobe: every preset is a card, armor presets show a player
 * model wearing the set, other presets (rod parts) show their items big. Clicking a card clicks that
 * presets equip button. Reads the real chest slots, so paging and the armor/parts switch keep working.
 */
public class PresetsScreen extends DofiScreen {
    private static final int PAD = 14;
    private static final int TOP = 20;
    private static final int BAR = 30;

    /** One preset: its equip button and the items on its row. */
    private record Card(Slot equip, String label, List<Slot> pieces) {}

    private final GenericContainerScreen original;
    private final ScreenHandler handler;
    private final Map<Integer, OtherClientPlayerEntity> models = new HashMap<>();
    private final List<Card> cards = new ArrayList<>();
    private int cardX, cardY, cardW, cardH, gap;

    public PresetsScreen(GenericContainerScreen original) {
        super(Text.literal("presets"));
        this.original = original;
        this.handler = original.getScreenHandler();
    }

    @Override
    protected void init() {
        int y = height - BAR + 4;
        addDrawableChild(ButtonWidget.builder(Mono.text("previous"), b -> nav("prev")).dimensions(PAD, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("next"), b -> nav("next")).dimensions(PAD + 74, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("switch armor / parts"), b -> nav("filter")).dimensions(width / 2 - 70, y, 140, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("chest view"), b -> {
            FishMenu.bypass = true;
            client.setScreen(original);
        }).dimensions(width - PAD - 144, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("close"), b -> close()).dimensions(width - PAD - 70, y, 70, 20).build());
    }

    @Override
    public void close() {
        client.player.closeHandledScreen();
    }

    /** The original chest screen, for the title check when it is swapped. */
    public GenericContainerScreen original() {
        return original;
    }

    private static String data(ItemStack stack, String key) {
        var cd = stack.get(DataComponentTypes.CUSTOM_DATA);
        return cd == null ? "" : cd.copyNbt().getString(key, "");
    }

    private void nav(String kind) {
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) && data(slot.getStack(), "navigation").equals(kind)) {
                sendClick(slot.id, 0);
                return;
            }
        }
    }

    private void sendClick(int slot, int button) {
        client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), (short) slot,
                (byte) button, SlotActionType.PICKUP, Int2ObjectMaps.emptyMap(), net.minecraft.screen.sync.ItemStackHash.EMPTY));
    }

    private void collect() {
        cards.clear();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory || !data(slot.getStack(), "type").equals("equip")) {
                continue;
            }
            List<Slot> pieces = new ArrayList<>();
            for (int i = slot.id + 1; i % 9 != 0 && i < handler.slots.size(); i++) {
                ItemStack s = handler.slots.get(i).getStack();
                if (!s.isEmpty() && !s.isOf(Items.BREAD)) {
                    pieces.add(handler.slots.get(i));
                }
            }
            var cd = slot.getStack().get(DataComponentTypes.CUSTOM_DATA);
            cards.add(new Card(slot, "preset " + (cd == null ? "" : cd.copyNbt().getInt("preset", 0)), pieces));
        }
    }

    private static EquipmentSlot armorSlot(ItemStack stack) {
        var eq = stack.get(DataComponentTypes.EQUIPPABLE);
        if (eq != null) {
            return eq.slot();
        }
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        return path.endsWith("_helmet") ? EquipmentSlot.HEAD : path.endsWith("_chestplate") ? EquipmentSlot.CHEST
                : path.endsWith("_leggings") ? EquipmentSlot.LEGS : path.endsWith("_boots") ? EquipmentSlot.FEET : null;
    }

    private OtherClientPlayerEntity model(Card card) {
        return models.computeIfAbsent(card.equip.id, id -> new OtherClientPlayerEntity(client.world, client.player.getGameProfile()));
    }

    private int cardAt(double mx, double my) {
        for (int i = 0; i < cards.size(); i++) {
            int x = cardX + i * (cardW + gap);
            if (mx >= x && mx < x + cardW && my >= cardY && my < cardY + cardH) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        int i = cardAt(click.x(), click.y());
        if (i < 0) {
            return false;
        }
        sendClick(cards.get(i).equip.id, click.button());
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        collect();
        if (cards.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Mono.text("waiting for the menu...").withColor(S24.TEXT_4 & 0xFFFFFF), width / 2, height / 2, 0xFFFFFFFF);
            return;
        }
        gap = 8;
        int n = cards.size();
        cardW = Math.min(170, (width - 2 * PAD - (n - 1) * gap) / n);
        cardH = height - TOP - BAR - 6;
        cardX = (width - (n * cardW + (n - 1) * gap)) / 2;
        cardY = TOP;
        int hover = cardAt(mouseX, mouseY);
        ItemStack tip = ItemStack.EMPTY;
        for (int i = 0; i < n; i++) {
            Card c = cards.get(i);
            int x = cardX + i * (cardW + gap);
            S24.rect(ctx, x, cardY, cardW, cardH, i == hover ? S24.BG_1 : S24.BG_2, i == hover ? S24.ACCENT : S24.BORDER);
            Text label = Mono.text(c.label);
            ctx.drawText(textRenderer, label, x + (cardW - textRenderer.getWidth(label)) / 2, cardY + 6, i == hover ? S24.ACCENT : S24.TEXT_2, false);

            boolean armor = c.pieces.stream().anyMatch(s -> armorSlot(s.getStack()) != null);
            int iconSize = armor ? 16 : Math.max(16, Math.min(48, (cardW - 12) / Math.max(1, c.pieces.size())));
            int rowW = c.pieces.size() * iconSize + (c.pieces.size() - 1) * 4;
            int iy = cardY + cardH - iconSize - 8;
            if (armor) {
                OtherClientPlayerEntity m = model(c);
                for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                    m.equipStack(s, ItemStack.EMPTY);
                }
                c.pieces.forEach(p -> {
                    EquipmentSlot s = armorSlot(p.getStack());
                    if (s != null) {
                        m.equipStack(s, p.getStack());
                    }
                });
                int top = cardY + 20, bottom = iy - 6;
                int boxH = bottom - top, boxW = Math.min(cardW - 8, boxH * 7 / 10);
                int mx0 = x + (cardW - boxW) / 2;
                InventoryScreen.drawEntity(ctx, mx0, top, mx0 + boxW, bottom, (int) (boxH * 0.43), 0.0625f, mouseX, mouseY, m);
            }
            int ix = x + (cardW - rowW) / 2;
            for (Slot p : c.pieces) {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(ix, iy);
                ctx.getMatrices().scale(iconSize / 16f, iconSize / 16f);
                ctx.drawItem(p.getStack(), 0, 0);
                ctx.getMatrices().popMatrix();
                if (mouseX >= ix && mouseX < ix + iconSize && mouseY >= iy && mouseY < iy + iconSize) {
                    tip = p.getStack();
                }
                ix += iconSize + 4;
            }
        }
        if (!tip.isEmpty()) {
            ctx.drawItemTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }
}
