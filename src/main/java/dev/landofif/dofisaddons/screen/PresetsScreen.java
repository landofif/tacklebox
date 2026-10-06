package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.stats.StatsTracker;
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

    /** One preset: its equip button (null for a page seen earlier) and copies of the items on its row. */
    private record Card(Slot equip, int preset, String filter, List<ItemStack> pieces) {
        String label() {
            return "preset " + preset;
        }
    }

    /** A piece reduced to what is needed to draw it: item, dye colour, custom model data and item model. */
    private static String pack(ItemStack st) {
        var dye = st.get(DataComponentTypes.DYED_COLOR);
        var cmd = st.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        var model = st.get(DataComponentTypes.ITEM_MODEL);
        return Registries.ITEM.getId(st.getItem()) + "|" + (dye == null ? "" : dye.rgb()) + "|"
                + (cmd == null ? "" : cmd.floats().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))) + "|"
                + (cmd == null ? "" : String.join(",", cmd.strings())) + "|" + (model == null ? "" : model);
    }

    private static ItemStack unpack(String text) {
        String[] f = (text + " ").split("\\|", -1);
        ItemStack st = new ItemStack(Registries.ITEM.get(net.minecraft.util.Identifier.of(f[0])));
        if (!f[1].isEmpty()) {
            st.set(DataComponentTypes.DYED_COLOR, new net.minecraft.component.type.DyedColorComponent(Integer.parseInt(f[1])));
        }
        if (!f[2].isEmpty() || !f[3].isEmpty()) {
            st.set(DataComponentTypes.CUSTOM_MODEL_DATA, new net.minecraft.component.type.CustomModelDataComponent(
                    java.util.Arrays.stream(f[2].split(",")).filter(x -> !x.isEmpty()).map(Float::valueOf).toList(), List.of(),
                    java.util.Arrays.stream(f[3].split(",")).filter(x -> !x.isEmpty()).toList(), List.of()));
        }
        if (!f[4].isBlank()) {
            st.set(DataComponentTypes.ITEM_MODEL, net.minecraft.util.Identifier.of(f[4].strip()));
        }
        return st;
    }

    private final GenericContainerScreen original;
    private final ScreenHandler handler;
    private final Map<Integer, OtherClientPlayerEntity> models = new HashMap<>();
    private final List<Card> cards = new ArrayList<>();
    private final List<Card> others = new ArrayList<>();
    private int cardX, cardY, cardW, cardH, gap, otherY, otherH;
    private String otherNav = "next";

    public PresetsScreen(GenericContainerScreen original) {
        super(Text.literal("presets"));
        this.original = original;
        this.handler = original.getScreenHandler();
    }

    private boolean armorWasHidden;
    private boolean armorTouched;

    /** foecollabs "hide armor" cancels armor drawing everywhere, the models here included: switch it off while this screen is open. */
    private void showArmor(boolean show) {
        try {
            Object cfg = Class.forName("io.github.foecollab.config.FOEConfig").getMethod("getConfig").invoke(null);
            Object fun = cfg.getClass().getField("fun").get(cfg);
            var f = fun.getClass().getField("hideArmor");
            if (show && !armorTouched) {
                armorWasHidden = f.getBoolean(fun);
                armorTouched = true;
                f.setBoolean(fun, false);
            } else if (!show && armorTouched) {
                f.setBoolean(fun, armorWasHidden);
                armorTouched = false;
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            // no foecollab, nothing hides the armor
        }
    }

    @Override
    public void removed() {
        showArmor(false);
        super.removed();
    }

    @Override
    protected void init() {
        showArmor(true);
        int y = vy() + vh() - BAR + 4;
        int left = vx() + PAD, right = vx() + vw() - PAD;
        addDrawableChild(ButtonWidget.builder(Mono.text("previous"), b -> nav("prev")).dimensions(left, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("next"), b -> nav("next")).dimensions(left + 74, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("switch armor / parts"), b -> nav("filter")).dimensions(vx() + vw() / 2 - 70, y, 140, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("chest view"), b -> {
            FishMenu.bypass = true;
            client.setScreen(original);
        }).dimensions(right - 144, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("close"), b -> close()).dimensions(right - 70, y, 70, 20).build());
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
        others.clear();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory || !data(slot.getStack(), "type").equals("equip")) {
                continue;
            }
            List<ItemStack> pieces = new ArrayList<>();
            for (int i = slot.id + 1; i % 9 != 0 && i < handler.slots.size(); i++) {
                ItemStack st = handler.slots.get(i).getStack();
                if (!st.isEmpty() && !st.isOf(Items.BREAD)) {
                    pieces.add(st.copy());
                }
            }
            var cd = slot.getStack().get(DataComponentTypes.CUSTOM_DATA);
            int n = cd == null ? 0 : cd.copyNbt().getInt("preset", 0);
            Card c = new Card(slot, n, data(slot.getStack(), "filter"), pieces);
            cards.add(c);
        }
        if (cards.isEmpty()) {
            return;
        }
        var cache = StatsTracker.settings().presetCache;
        boolean dirty = false;
        for (Card c : cards) { // remember what the page showed, so the other page can be drawn without opening it
            List<String> now = c.pieces.stream().map(PresetsScreen::pack).toList();
            if (!now.equals(cache.get(c.filter + ":" + c.preset))) {
                cache.put(c.filter + ":" + c.preset, new ArrayList<>(now));
                dirty = true;
            }
        }
        if (dirty) {
            StatsTracker.saveSettings();
        }
        String filter = cards.get(0).filter;
        int first = cards.stream().mapToInt(Card::preset).min().getAsInt();
        cache.forEach((key, packed) -> {
            int colon = key.indexOf(':');
            int n = Integer.parseInt(key.substring(colon + 1));
            if (key.substring(0, colon).equals(filter) && cards.stream().noneMatch(k -> k.preset == n)) {
                others.add(new Card(null, n, filter, packed.stream().map(PresetsScreen::unpack).toList()));
            }
        });
        others.sort(java.util.Comparator.comparingInt(Card::preset));
        otherNav = !others.isEmpty() && others.get(0).preset < first ? "prev" : "next";
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
        if (!others.isEmpty() && click.y() >= otherY && click.y() < otherY + otherH) {
            nav(otherNav);
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
            ctx.drawCenteredTextWithShadow(textRenderer, Mono.text("waiting for the menu...").withColor(S24.TEXT_4 & 0xFFFFFF), vx() + vw() / 2, vy() + vh() / 2, 0xFFFFFFFF);
            return;
        }
        gap = 8;
        int n = cards.size();
        cardW = Math.min(150, (vw() - 2 * PAD - (n - 1) * gap) / n);
        int avail = vh() - TOP - BAR - 6;
        cardH = others.isEmpty() ? avail : (avail - gap) / 2;
        otherH = cardH;
        cardX = vx() + (vw() - (n * cardW + (n - 1) * gap)) / 2;
        // the rows keep their places (lower page on top), the page you are on is the lit one
        boolean liveTop = others.isEmpty() || cards.get(0).preset < others.get(0).preset;
        int topY = vy() + TOP, botY = topY + cardH + gap;
        cardY = liveTop ? topY : botY;
        otherY = liveTop ? botY : topY;
        int hover = cardAt(mouseX, mouseY);
        ItemStack tip = ItemStack.EMPTY;
        for (int i = 0; i < n; i++) {
            ItemStack t = drawCard(ctx, cards.get(i), cardX + i * (cardW + gap), cardY, cardW, cardH, i == hover, mouseX, mouseY, true);
            tip = t.isEmpty() ? tip : t;
        }
        for (int i = 0; i < Math.min(others.size(), n); i++) { // the other page, greyed out, click to go there
            int x = cardX + i * (cardW + gap);
            drawCard(ctx, others.get(i), x, otherY, cardW, otherH, false, -1, -1, StatsTracker.settings().presetsOtherBg);
            ctx.fill(x, otherY, x + cardW, otherY + otherH, 0x80000000);
        }
        if (!tip.isEmpty()) {
            ctx.drawItemTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }

    /** A copy of the piece that renders on a player even where the server gives the armor no model of its own. */
    private static ItemStack wearable(ItemStack stack, EquipmentSlot slot) {
        ItemStack out = stack.copy();
        out.set(DataComponentTypes.EQUIPPABLE, net.minecraft.component.type.EquippableComponent.builder(slot)
                .model(net.minecraft.item.equipment.EquipmentAssetKeys.LEATHER).build());
        return out;
    }

    /** Draws one card and returns the item under the mouse, if any. */
    private ItemStack drawCard(DrawContext ctx, Card c, int x, int y, int w, int h, boolean hover, int mouseX, int mouseY, boolean bg) {
        if (bg) {
            S24.rect(ctx, x, y, w, h, hover ? S24.BG_1 : S24.BG_2, hover ? accent() : S24.BORDER);
        }
        Text label = Mono.text(c.label());
        ctx.drawText(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + 6, hover ? accent() : S24.TEXT_2, false);

        boolean armor = c.pieces.stream().anyMatch(s -> armorSlot(s) != null);
        int iconSize = armor ? 16 : Math.max(16, Math.min(40, (w - 12) / Math.max(1, c.pieces.size())));
        int rowW = c.pieces.size() * iconSize + (c.pieces.size() - 1) * 4;
        int iy = y + h - iconSize - 8;
        if (armor) {
            OtherClientPlayerEntity m = models.computeIfAbsent(c.preset, id -> new OtherClientPlayerEntity(client.world, client.player.getGameProfile()));
            m.getDataTracker().set(net.minecraft.entity.PlayerLikeEntity.PLAYER_MODE_CUSTOMIZATION_ID,
                    client.player.getDataTracker().get(net.minecraft.entity.PlayerLikeEntity.PLAYER_MODE_CUSTOMIZATION_ID)); // second skin layer
            for (EquipmentSlot s : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                m.equipStack(s, ItemStack.EMPTY);
            }
            for (ItemStack p : c.pieces) {
                EquipmentSlot s = armorSlot(p);
                if (s != null) {
                    m.equipStack(s, wearable(p, s));
                }
            }
            int top = y + 20, bottom = iy - 6;
            int boxH = bottom - top, boxW = Math.min(w - 8, boxH * 7 / 10);
            int mx0 = x + (w - boxW) / 2;
            InventoryScreen.drawEntity(ctx, mx0, top, mx0 + boxW, bottom, (int) (boxH * 0.34), 0.0625f, mouseX, mouseY, m);
        }
        ItemStack tip = ItemStack.EMPTY;
        int ix = x + (w - rowW) / 2;
        for (ItemStack p : c.pieces) {
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(ix, iy);
            ctx.getMatrices().scale(iconSize / 16f, iconSize / 16f);
            ctx.drawItem(p, 0, 0);
            ctx.getMatrices().popMatrix();
            if (mouseX >= ix && mouseX < ix + iconSize && mouseY >= iy && mouseY < iy + iconSize) {
                tip = p;
            }
            ix += iconSize + 4;
        }
        return tip;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (client.options.inventoryKey.matchesKey(input)) { // e closes it like the chest
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    protected int sizePct() {
        return StatsTracker.settings().presetsSize;
    }

    private static int accent() {
        DofiSettings s = StatsTracker.settings();
        return s == null || s.presetsColor == DofiSettings.DEFAULT_GUI_COLOR ? S24.ACCENT : 0xFF000000 | s.presetsColor;
    }
}
