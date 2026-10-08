package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.menu.Vaults;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.util.Glyphs;
import dev.landofif.dofisaddons.compat.FoeRarity;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /pv as one screen: all seven personal vaults side by side, with one search over all of them. The vault
 * that is open is read live, the others show what they held when you last opened them (menu/Vaults). The
 * open vault and your inventory are live: clicks go to the server as slot clicks, the same as in the chest.
 * Clicking another vault types /pv n (the player's own click, the only command this screen sends).
 */
public class VaultScreen extends DofiScreen {
    private static final int PAD = 14;
    private static final int GAP = 8;
    private static final int LABEL = 11;

    private final GenericContainerScreen original;
    private final int current;
    private TextFieldWidget search;
    /** Whether a stack matches the search, by identity; cleared when the query or the vaults change. */
    private final Map<ItemStack, Boolean> hits = new IdentityHashMap<>();
    /** What the last frame drew: slots you can click (handler slot id) and the vaults, for hit tests. */
    private record Spot(int x, int y, int w, int h, int id) {
        boolean at(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }
    private final List<Spot> spots = new ArrayList<>();
    private final List<Spot> cells = new ArrayList<>();
    private String lastQuery = "";
    private int lastVersion = -1;

    public VaultScreen(GenericContainerScreen original, int current) {
        super(Text.literal("personal vaults"));
        this.original = original;
        this.current = current;
    }

    @Override
    protected void init() {
        search = new TextFieldWidget(textRenderer, vx() + PAD, vy() + 20, 220, 18, Foe.text("search"));
        search.setPlaceholder(Foe.text("search all vaults...").withColor(Foe.FAINT & 0xFFFFFF));
        search.setMaxLength(64);
        addDrawableChild(search);
        setInitialFocus(search);
        int y = vy() + vh() - 24, right = vx() + vw() - PAD;
        // buttons sit beside the inventory, bottom right
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

    public GenericContainerScreen original() {
        return original;
    }

    private static String haystack(ItemStack st) {
        StringBuilder sb = new StringBuilder(Registries.ITEM.getId(st.getItem()).getPath().replace('_', ' ')).append('\n').append(Glyphs.plain(st.getName()));
        var lore = st.get(DataComponentTypes.LORE);
        if (lore != null) {
            lore.lines().forEach(l -> sb.append('\n').append(Glyphs.plain(l)));
        }
        return sb.toString();
    }

    private boolean matches(ItemStack st, String query) {
        return hits.computeIfAbsent(st, x -> haystack(x).contains(query));
    }

    private List<ItemStack> live() {
        List<ItemStack> out = new ArrayList<>(Vaults.SLOTS);
        for (int i = 0; i < Vaults.SLOTS; i++) {
            out.add(original.getScreenHandler().slots.get(i).getStack());
        }
        return out;
    }

    /** One item scaled to a slot of the given size, with its rarity circle behind and its count on top. */
    private void item(DrawContext ctx, ItemStack st, int x, int y, int size) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(size / 16f, size / 16f);
        FoeRarity.circle(ctx, 0, 0, st);
        ctx.drawItem(st, 0, 0);
        ctx.drawStackOverlay(textRenderer, st, 0, 0);
        ctx.getMatrices().popMatrix();
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        Vaults.snapshot(current, original.getScreenHandler());
        String query = Glyphs.plain(Text.literal(search.getText())).strip();
        if (!query.equals(lastQuery) || lastVersion != Vaults.version) {
            hits.clear();
            lastQuery = query;
            lastVersion = Vaults.version;
        }
        spots.clear();
        cells.clear();
        ScreenHandler handler = original.getScreenHandler();
        // your inventory at the bottom, vault slots above it in whatever layout gives the biggest slots
        int inv = 18, invH = 3 * inv + 4 + inv;
        int top = vy() + 44, bottom = vy() + vh() - invH - 12;
        int cols = 4, slot = 0;
        for (int c : new int[]{7, 6, 5, 4, 3, 2, 1}) {
            int r = (Vaults.COUNT + c - 1) / c;
            int cw = (vw() - 2 * PAD - (c - 1) * GAP) / c;
            int sz = Math.min(28, Math.min(cw / 9, (bottom - top - (r - 1) * GAP - r * LABEL) / (r * 5)));
            if (sz > slot) {
                slot = sz;
                cols = c;
            }
        }
        slot = Math.max(6, slot);
        int cellW = (vw() - 2 * PAD - (cols - 1) * GAP) / cols;
        int gridW = 9 * slot, cellH = LABEL + 5 * slot;
        ItemStack tip = ItemStack.EMPTY;
        int hoverX = -1, hoverY = -1;
        for (int n = 1; n <= Vaults.COUNT; n++) {
            int x = vx() + PAD + ((n - 1) % cols) * (cellW + GAP) + (cellW - gridW) / 2;
            int y = top + ((n - 1) / cols) * (cellH + GAP);
            List<ItemStack> items = n == current ? live() : Vaults.get(n);
            int found = items == null ? 0 : (int) items.stream().filter(st -> !st.isEmpty() && !query.isEmpty() && matches(st, query)).count();
            boolean hot = n != current && mouseX >= x && mouseX < x + gridW && mouseY >= y && mouseY < y + cellH;
            if (n != current) {
                cells.add(new Spot(x, y, gridW, cellH, n));
            }
            String label = "vault " + n + (n == current ? " (open)" : "") + (!query.isEmpty() && items != null ? "  " + found + " found" : "");
            ctx.drawText(textRenderer, Foe.text(label), x, y, n == current || hot || found > 0 ? Foe.ACCENT : Foe.DIM, false);
            if (items == null) {
                ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("click to load (/pv " + n + ")").withColor((hot ? Foe.ACCENT : Foe.DIM) & 0xFFFFFF), x + gridW / 2, y + LABEL + 2 * slot, 0xFFFFFFFF);
                continue;
            }
            for (int i = 0; i < Vaults.SLOTS; i++) {
                int sx = x + (i % 9) * slot, sy = y + LABEL + (i / 9) * slot;
                ItemStack st = items.get(i);
                ctx.fill(sx, sy, sx + slot - 1, sy + slot - 1, 0x30000000);
                if (n == current) {
                    spots.add(new Spot(sx, sy, slot - 1, slot - 1, i));
                }
                if (!st.isEmpty()) {
                    item(ctx, st, sx, sy, slot - 1);
                    if (!query.isEmpty()) {
                        if (matches(st, query)) {
                            ctx.drawStrokedRectangle(sx, sy, slot - 1, slot - 1, Foe.ACCENT);
                        } else {
                            ctx.fill(sx, sy, sx + slot - 1, sy + slot - 1, 0xB0000000);
                        }
                    }
                }
                if (n == current && mouseX >= sx && mouseX < sx + slot - 1 && mouseY >= sy && mouseY < sy + slot - 1) {
                    ctx.fill(sx, sy, sx + slot - 1, sy + slot - 1, 0x60FFFFFF);
                    tip = st;
                }
            }
        }
        // the inventory: three rows, then the hotbar
        int ix = vx() + (vw() - 9 * inv) / 2, iy = vy() + vh() - invH - 6;
        ctx.drawText(textRenderer, Foe.text("inventory"), ix, iy - 10, Foe.DIM, false);
        for (Slot sl : handler.slots) {
            if (!(sl.inventory instanceof PlayerInventory)) {
                continue;
            }
            int k = sl.getIndex();
            if (k > 35) {
                continue;
            }
            int sx = ix + (k % 9) * inv, sy = k < 9 ? iy + 3 * inv + 4 : iy + (k / 9 - 1) * inv;
            ctx.fill(sx, sy, sx + inv - 1, sy + inv - 1, 0x30000000);
            spots.add(new Spot(sx, sy, inv - 1, inv - 1, sl.id));
            ItemStack st = sl.getStack();
            if (!st.isEmpty()) {
                item(ctx, st, sx, sy, inv - 1);
                if (!query.isEmpty() && !matches(st, query)) {
                    ctx.fill(sx, sy, sx + inv - 1, sy + inv - 1, 0xB0000000);
                }
            }
            if (mouseX >= sx && mouseX < sx + inv - 1 && mouseY >= sy && mouseY < sy + inv - 1) {
                ctx.fill(sx, sy, sx + inv - 1, sy + inv - 1, 0x60FFFFFF);
                tip = st;
            }
        }
        if (vw() > 520) {
            ctx.drawText(textRenderer, Foe.sentence("Click another vault to open it with /pv."), vx() + PAD + 230, vy() + 25, Foe.DIM, false);
        }
        ItemStack held = handler.getCursorStack();
        if (!held.isEmpty()) {
            ctx.drawItem(held, mouseX - 8, mouseY - 8);
            ctx.drawStackOverlay(textRenderer, held, mouseX - 8, mouseY - 8);
        } else if (!tip.isEmpty()) {
            ctx.drawItemTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        for (Spot sp : spots) {
            if (sp.at(click.x(), click.y())) {
                client.interactionManager.clickSlot(original.getScreenHandler().syncId, sp.id, click.button(),
                        click.hasShift() ? SlotActionType.QUICK_MOVE : SlotActionType.PICKUP, client.player);
                return true;
            }
        }
        for (Spot c : cells) {
            if (c.at(click.x(), click.y())) {
                client.player.networkHandler.sendChatCommand("pv " + c.id);
                return true;
            }
        }
        return false;
    }
}
