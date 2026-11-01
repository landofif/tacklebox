package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.compat.FoeArmor;
import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import dev.landofif.tacklebox.util.Glyphs;
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
 * presets equip button, and so does number key n for the nth card on the page. Reads the real chest slots, so paging and
 * the armor/parts switch keep working.
 */
public class PresetsScreen extends TackleboxScreen {
    private final java.util.Map<Object, Float> focus = new java.util.HashMap<>();
    private long lastFrame;
    private float dt;
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
        if (st.isEmpty()) {
            return "empty";
        }
        var dye = st.get(DataComponentTypes.DYED_COLOR);
        var cmd = st.get(DataComponentTypes.CUSTOM_MODEL_DATA);
        var model = st.get(DataComponentTypes.ITEM_MODEL);
        return Registries.ITEM.getId(st.getItem()) + "|" + (dye == null ? "" : dye.rgb()) + "|"
                + (cmd == null ? "" : cmd.floats().stream().map(String::valueOf).collect(java.util.stream.Collectors.joining(","))) + "|"
                + (cmd == null ? "" : String.join(",", cmd.strings())) + "|" + (model == null ? "" : model);
    }

    private static ItemStack unpack(String text) {
        if (text.equals("empty")) {
            return ItemStack.EMPTY;
        }
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

    /** foecollabs "hide armor" would hide the armor on the models, so it is off while this screen is open. */
    private final FoeArmor armor = new FoeArmor();

    @Override
    public void removed() {
        armor.show(false);
        super.removed();
    }

    @Override
    protected void init() {
        armor.show(true);
        int y = vy() + vh() - BAR + 4;
        int left = vx() + PAD, right = vx() + vw() - PAD;
        addDrawableChild(ButtonWidget.builder(Foe.text("previous"), b -> nav("back")).dimensions(left, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("next"), b -> nav("next")).dimensions(left + 74, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("switch armor / parts"), b -> nav("filter")).dimensions(vx() + vw() / 2 - 70, y, 140, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("chest view"), b -> {
            FishMenu.bypass = true;
            client.setScreen(original);
        }).dimensions(right - 144, y, 70, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("close"), b -> close()).dimensions(right - 70, y, 70, 20).build());
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
                if (st.isOf(Items.GUNPOWDER) && List.of("reel", "pole", "line").contains(data(st, "type"))) {
                    pieces.add(ItemStack.EMPTY); // an empty part slot in a parts preset
                } else if (!st.isEmpty() && !st.isOf(Items.BREAD)) {
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
        var cache = TackleboxSettings.get().presetCache;
        boolean dirty = false;
        for (Card c : cards) { // remember what the page showed, so the other page can be drawn without opening it
            List<String> now = c.pieces.stream().map(PresetsScreen::pack).toList();
            if (!now.equals(cache.get(c.filter + ":" + c.preset))) {
                cache.put(c.filter + ":" + c.preset, new ArrayList<>(now));
                dirty = true;
            }
            if (c.filter.equals("armor")) { // luck and scale for the bait picker
                var stats = dev.landofif.tacklebox.menu.BaitPicker.armorStats(c.pieces);
                if (!stats.equals(TackleboxSettings.get().presetStats.get("armor:" + c.preset))) {
                    TackleboxSettings.get().presetStats.put("armor:" + c.preset, stats);
                    dirty = true;
                }
            }
        }
        if (dirty) {
            TackleboxSettings.save();
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
        otherNav = !others.isEmpty() && others.get(0).preset < first ? "back" : "next";
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
        long now = net.minecraft.util.Util.getMeasuringTimeMs();
        dt = Math.min(0.1f, (now - lastFrame) / 1000f);
        lastFrame = now;
        collect();
        if (cards.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("waiting for the menu...").withColor(Foe.DIM & 0xFFFFFF), vx() + vw() / 2, vy() + vh() / 2, 0xFFFFFFFF);
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
            drawCard(ctx, others.get(i), x, otherY, cardW, otherH, false, -1, -1, TackleboxSettings.get().presetsOtherBg);
            Hypr.rrect(ctx, x, otherY, cardW, otherH, 5, 0x80000000);
        }
        if (!tip.isEmpty()) {
            ctx.drawItemTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }

    /** A copy of the piece that renders on a player even where the server gives the armor no model of its own. */
    static ItemStack wearable(ItemStack stack, EquipmentSlot slot) {
        ItemStack out = stack.copy();
        out.set(DataComponentTypes.EQUIPPABLE, net.minecraft.component.type.EquippableComponent.builder(slot)
                .model(net.minecraft.item.equipment.EquipmentAssetKeys.LEATHER).build());
        return out;
    }

    /** Whether you are wearing this armor preset: every piece in it matches (by its custom data) what sits in that slot. */
    private boolean worn(Card c) {
        int pieces = 0;
        for (ItemStack st : c.pieces) {
            net.minecraft.entity.EquipmentSlot slot = armorSlot(st);
            if (slot == null) {
                continue;
            }
            var mine = client.player.getEquippedStack(slot).get(DataComponentTypes.CUSTOM_DATA);
            var card = st.get(DataComponentTypes.CUSTOM_DATA);
            if (card == null || mine == null || !card.copyNbt().equals(mine.copyNbt())) {
                return false;
            }
            pieces++;
        }
        return pieces > 0;
    }

    /** Draws one card and returns the item under the mouse, if any. */
    private ItemStack drawCard(DrawContext ctx, Card c, int x, int y, int w, int h, boolean hover, int mouseX, int mouseY, boolean bg) {
        float f = 0;
        if (bg) { // a tile: the card under the mouse (or the preset you have on) gets the turning gradient border
            float fo = Hypr.approach(focus.getOrDefault(c.preset, 0f), hover ? 1 : 0, dt, 14);
            focus.put(c.preset, fo);
            f = fo;
            float lit = Math.max(fo, worn(c) ? 1 : 0);
            Hypr.rrect(ctx, x, y, w, h, 5, Hypr.BASE);
            if (lit > 0.02f) {
                double angle = Math.toRadians(45) + (net.minecraft.util.Util.getMeasuringTimeMs() % 10000) / 10000.0 * Math.PI * 2;
                Hypr.Paint grad = Hypr.angled(Hypr.acc1(), Hypr.acc2(), x, y, w, h, angle, 1);
                Hypr.ring(ctx, x, y, w, h, 5, 1, (px, py) -> Hypr.mix(Hypr.INACTIVE, grad.at(px, py), lit), 12);
            } else {
                Hypr.ring(ctx, x, y, w, h, 5, 1, Hypr.INACTIVE);
            }
        }
        Text label = Foe.text(c.label());
        ctx.drawTextWithShadow(textRenderer, label, x + (w - textRenderer.getWidth(label)) / 2, y + 6, Hypr.mix(Hypr.SUB, Hypr.acc2(), f));

        boolean armor = c.pieces.stream().anyMatch(s -> armorSlot(s) != null);
        int iconSize = armor ? 16 : Math.max(16, Math.min(Math.min(h - 40, 56), (w - 16 - 4 * c.pieces.size()) / Math.max(1, c.pieces.size())));
        int rowW = c.pieces.size() * iconSize + (c.pieces.size() - 1) * 4;
        int iy = armor ? y + h - iconSize - 8 : y + (h - iconSize) / 2 + 6; // parts sit in the middle, armor under the model
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
            int boxH = bottom - top, boxW = w - 8;
            int mx0 = x + (w - boxW) / 2;
            InventoryScreen.drawEntity(ctx, mx0, top, mx0 + boxW, bottom, (int) (boxH * 0.47), 0.0625f, mouseX, mouseY, m);
        }
        ItemStack tip = ItemStack.EMPTY;
        int ix = x + (w - rowW) / 2;
        for (ItemStack p : c.pieces) {
            if (p.isEmpty()) { // empty part slot: just a faint box
                Hypr.ring(ctx, ix, iy, iconSize, iconSize, 3, 1, Hypr.OVERLAY);
                ix += iconSize + 4;
                continue;
            }
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
        for (int k = 0; k < client.options.hotbarKeys.length; k++) {
            if (client.options.hotbarKeys[k].matchesKey(input)) {
                int i = keyCard(k, cards.size());
                if (i >= 0) {
                    sendClick(cards.get(i).equip.id, 0);
                }
                return true;
            }
        }
        return super.keyPressed(input);
    }

    /** The card a hotbar key (0 for key 1) equips: the nth card of the shown page, or -1 if the page has fewer cards. */
    static int keyCard(int hotbarKey, int cardCount) {
        return hotbarKey < cardCount ? hotbarKey : -1;
    }

    @Override
    protected int sizePct() {
        return TackleboxSettings.get().presetsSize;
    }
}
