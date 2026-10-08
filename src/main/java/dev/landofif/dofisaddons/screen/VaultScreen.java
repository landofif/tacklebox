package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.menu.Vaults;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.util.Glyphs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;

import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /pv as one screen: all seven personal vaults side by side, with one search over all of them. The vault
 * that is open is read live, the others show what they held when you last opened them (menu/Vaults). The
 * mod never types /pv for you, so a vault you have not opened yet stays empty here. The chest view button
 * gives the real chest back for moving items.
 */
public class VaultScreen extends DofiScreen {
    private static final int PAD = 14;
    private static final int GAP = 8;
    private static final int LABEL = 11;

    private final GenericContainerScreen original;
    private final int current;
    private TextFieldWidget search;
    /** Stacks that match the search, by identity; searched again when the query or the vaults change. */
    private final Map<ItemStack, Boolean> hits = new IdentityHashMap<>();
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

    private void research(String query) {
        hits.clear();
        lastQuery = query;
        lastVersion = Vaults.version;
        if (query.isEmpty()) {
            return;
        }
        for (int n = 1; n <= Vaults.COUNT; n++) {
            List<ItemStack> items = Vaults.get(n);
            if (items != null) {
                items.stream().filter(s -> !s.isEmpty() && haystack(s).contains(query)).forEach(s -> hits.put(s, true));
            }
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        Vaults.snapshot(current, original.getScreenHandler());
        String query = Glyphs.plain(Text.literal(search.getText())).strip();
        if (!query.equals(lastQuery) || lastVersion != Vaults.version) {
            research(query);
        }
        int cols = 4, rows = 2;
        int top = vy() + 44, bottom = vy() + vh() - 30;
        int cellW = (vw() - 2 * PAD - (cols - 1) * GAP) / cols;
        int slot = Math.max(6, Math.min(18, Math.min(cellW / 9, (bottom - top - GAP - rows * LABEL) / (rows * 5))));
        int gridW = 9 * slot, cellH = LABEL + 5 * slot;
        ItemStack tip = ItemStack.EMPTY;
        for (int n = 1; n <= Vaults.COUNT; n++) {
            int x = vx() + PAD + ((n - 1) % cols) * (cellW + GAP) + (cellW - gridW) / 2;
            int y = top + ((n - 1) / cols) * (cellH + GAP);
            List<ItemStack> items = Vaults.get(n);
            int found = items == null ? 0 : (int) items.stream().filter(hits::containsKey).count();
            String label = "vault " + n + (n == current ? " (open)" : "") + (!query.isEmpty() && items != null ? "  " + found + " found" : "");
            ctx.drawText(textRenderer, Foe.text(label), x, y, n == current || found > 0 ? Foe.ACCENT : Foe.DIM, false);
            if (items == null) {
                ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("type /pv " + n + " to load").withColor(Foe.FAINT & 0xFFFFFF), x + gridW / 2, y + LABEL + 2 * slot, 0xFFFFFFFF);
                continue;
            }
            for (int i = 0; i < Vaults.SLOTS; i++) {
                int sx = x + (i % 9) * slot, sy = y + LABEL + (i / 9) * slot;
                ItemStack st = items.get(i);
                ctx.fill(sx, sy, sx + slot - 1, sy + slot - 1, 0x30000000);
                if (st.isEmpty()) {
                    continue;
                }
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate(sx, sy);
                ctx.getMatrices().scale((slot - 1) / 16f, (slot - 1) / 16f);
                ctx.drawItem(st, 0, 0);
                ctx.drawStackOverlay(textRenderer, st, 0, 0);
                ctx.getMatrices().popMatrix();
                if (!query.isEmpty()) {
                    if (hits.containsKey(st)) {
                        ctx.drawStrokedRectangle(sx, sy, slot - 1, slot - 1, Foe.ACCENT);
                    } else {
                        ctx.fill(sx, sy, sx + slot - 1, sy + slot - 1, 0xB0000000);
                    }
                }
                if (mouseX >= sx && mouseX < sx + slot - 1 && mouseY >= sy && mouseY < sy + slot - 1) {
                    tip = st;
                }
            }
        }
        ctx.drawText(textRenderer, Foe.sentence("Vaults fill in as you open them with /pv 1-7."), vx() + PAD, vy() + vh() - 18, Foe.DIM, false);
        if (!tip.isEmpty()) {
            ctx.drawItemTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }
}
