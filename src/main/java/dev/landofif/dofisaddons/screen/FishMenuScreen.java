package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.compat.FoeMainMenu;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.ui.Hypr;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.hud.BaitInfo;
import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.menu.Locations;
import dev.landofif.dofisaddons.ui.Arc;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.util.Glyphs;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.block.Block;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The fishonmc main menu and /travel as a screen of the mods own: big icons you can rearrange, no cursor
 * stack to pick up. It reads the real chest the server opened (its slots keep updating) and a click sends
 * the plain click on that slot, like pressing the button in the chest would.
 *
 * In /travel every location is a circle with its wiki picture, a ring for the progress to the next
 * location level and the level on top, plus a panel with the best place to fish and noted bait strategies.
 */
public class FishMenuScreen extends DofiScreen {
    private static final int PAD = 14;
    private static final int TOP = 20;
    private static final int BAR = 30;

    /** One button of the menu: a filled chest slot, and where it sits in the grid. */
    private record Tile(Slot slot, ItemStack stack, String name, String key, Locations.Location loc, FishMenu.Info info) {}

    private final GenericContainerScreen original;
    private final ScreenHandler handler;
    private boolean editing;
    private Tile dragged;
    private double dragX, dragY;
    private ButtonWidget editButton;

    // geometry of the last frame, for hit tests
    private final List<Tile> tiles = new ArrayList<>();
    private int[][] cells = new int[0][];
    private int gridX, gridY, cols, rows;
    private float cellW, cellH, scale;

    public FishMenuScreen(GenericContainerScreen original) {
        super(Text.literal(travel(original) ? "travel" : "menu"));
        this.original = original;
        this.handler = original.getScreenHandler();
    }

    private static boolean travel(GenericContainerScreen screen) {
        int hits = 0;
        for (Slot slot : screen.getScreenHandler().slots) {
            if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) && !slot.getStack().isEmpty()
                    && Locations.byName(Glyphs.plain(slot.getStack().getName())) != null) {
                hits++;
            }
        }
        return hits >= 4;
    }

    /** Travel mode is decided by what is in the chest, because the items arrive just after the screen opens. */
    /** The chest screen this one replaces (its title is what tells which menu it is). */
    public GenericContainerScreen original() {
        return original;
    }

    private boolean isTravel() {
        return travel(original);
    }

    @Override
    protected void init() {
        if (FoeMainMenu.TITLE.equals(original.getTitle().getString())) {
            FoeMainMenu.open(true); // foecollab's daily mission tracker reads the menu while this is set
        }
        int y = vy() + vh() - BAR + 4;
        int left = vx() + PAD;
        editButton = addDrawableChild(ButtonWidget.builder(Foe.text("rearrange"), b -> toggleEdit()).dimensions(left, y, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("reset layout"), b -> {
            DofiSettings s = DofiSettings.get();
            s.menuCells.keySet().removeIf(k -> k.startsWith(prefix()));
            DofiSettings.save();
        }).dimensions(left + 96, y, 90, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("chest view"), b -> {
            FishMenu.bypass = true;
            client.setScreen(original);
        }).dimensions(vx() + vw() - PAD - 90, y, 90, 20).build());
    }

    private void toggleEdit() {
        editing = !editing;
        editButton.setMessage(Foe.text(editing ? "done" : "rearrange"));
    }

    private String prefix() {
        return isTravel() ? "travel:" : "main:";
    }

    @Override
    public void close() {
        client.player.closeHandledScreen();
    }

    @Override
    public void removed() {
        FoeMainMenu.open(false);
        super.removed();
    }

    // ---- model ----

    /** The level from the tooltip, which the compendium has and /travel does not: remembered, and used when it is missing. */
    private static FishMenu.Info levelOf(Locations.Location loc, ItemStack stack) {
        FishMenu.Info info = FishMenu.info(stack);
        var saved = DofiSettings.get().locationLevels;
        if (info.level() >= 0) {
            String now = info.level() + "|" + info.progress();
            if (!now.equals(saved.get(loc.id()))) {
                saved.put(loc.id(), now);
                DofiSettings.save();
            }
            return info;
        }
        String old = saved.get(loc.id());
        if (old == null) {
            return info;
        }
        String[] f = old.split("\\|");
        return new FishMenu.Info(Integer.parseInt(f[0]), Float.parseFloat(f[1]));
    }

    private void collect() {
        tiles.clear();
        for (Slot slot : handler.slots) {
            if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) {
                continue;
            }
            ItemStack stack = slot.getStack();
            String name = Glyphs.plain(stack.getName()).trim();
            if (stack.isEmpty() || stack.isOf(net.minecraft.item.Items.BREAD) || (name.isEmpty() && stack.getItem().toString().contains("glass_pane"))) {
                continue; // empty and filler slots
            }
            if (name.equalsIgnoreCase("close")) {
                continue; // escape closes it, so the button is only in the way
            }
            Locations.Location loc = Locations.byName(name);
            tiles.add(new Tile(slot, stack, name, loc != null ? "travel:" + loc.id() : "main:" + name,
                    loc, loc != null ? levelOf(loc, stack) : null));
        }
        if (isTravel()) {
            tiles.removeIf(t -> t.loc == null); // only the sixteen locations; other buttons stay in the chest view
        }
    }

    /**
     * Puts every tile in a column and row: where the player left it, otherwise where the server had it in the
     * chest (main menu, so it matches the wiki picture) or the first free spot.
     */
    private void arrange(int columns, boolean serverLayout) {
        DofiSettings settings = DofiSettings.get();
        // the server leaves gaps (filler bread is hidden): squeeze out empty columns and rows
        List<Integer> usedCols = tiles.stream().map(t -> t.slot.id % 9).distinct().sorted().toList();
        List<Integer> usedRows = tiles.stream().map(t -> t.slot.id / 9).distinct().sorted().toList();
        if (serverLayout && !tiles.isEmpty()) {
            cols = usedCols.size();
            rows = usedRows.size();
        } else {
            cols = columns;
            rows = Math.max(1, (tiles.size() + cols - 1) / cols);
        }
        for (Tile t : tiles) {
            int[] saved = settings.menuCells.get(t.key);
            if (saved != null && saved.length == 2) {
                rows = Math.max(rows, saved[1] + 1);
            }
        }
        cells = new int[tiles.size()][];
        Set<Integer> used = new HashSet<>();
        for (int i = 0; i < tiles.size(); i++) {
            int[] saved = settings.menuCells.get(tiles.get(i).key);
            if (saved != null && saved.length == 2 && saved[0] >= 0 && saved[0] < cols && used.add(saved[1] * cols + saved[0])) {
                cells[i] = saved;
            }
        }
        int next = 0;
        for (int i = 0; i < tiles.size(); i++) {
            if (cells[i] != null) {
                continue;
            }
            int id = tiles.get(i).slot.id;
            int home = serverLayout ? usedRows.indexOf(id / 9) * cols + usedCols.indexOf(id % 9) : -1;
            if (home >= 0 && used.add(home)) {
                cells[i] = new int[]{home % cols, home / cols};
                continue;
            }
            while (used.contains(next)) {
                next++;
            }
            used.add(next);
            cells[i] = new int[]{next % cols, next / cols};
            rows = Math.max(rows, cells[i][1] + 1);
        }
    }

    private void saveCells() {
        DofiSettings settings = DofiSettings.get();
        for (int i = 0; i < tiles.size(); i++) {
            settings.menuCells.put(tiles.get(i).key, cells[i].clone());
        }
        DofiSettings.save();
    }

    // ---- layout ----

    private void fit(int x, int y, int w, int h, float baseW, float baseH, float maxScale) {
        scale = Math.min(maxScale, Math.min(w / (cols * baseW), h / (rows * baseH)));
        cellW = baseW * scale;
        cellH = baseH * scale;
        gridX = x + Math.round((w - cols * cellW) / 2f);
        gridY = y;
    }

    private float cellX(int col) {
        return gridX + col * cellW;
    }

    private float cellY(int row) {
        return gridY + row * cellH;
    }

    private int tileAt(double mx, double my) {
        for (int i = 0; i < tiles.size(); i++) {
            float x = cellX(cells[i][0]), y = cellY(cells[i][1]);
            if (mx >= x && mx < x + cellW && my >= y && my < y + cellH) {
                return i;
            }
        }
        return -1;
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        int i = tileAt(click.x(), click.y());
        if (i < 0) {
            return false;
        }
        if (editing) {
            dragged = tiles.get(i);
            dragX = click.x();
            dragY = click.y();
        } else {
            sendClick(tiles.get(i).slot.id, click.button());
        }
        return true;
    }

    /** The click as the server expects it, without the client picking the item up first. */
    private void sendClick(int slot, int button) {
        client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), (short) slot,
                (byte) button, SlotActionType.PICKUP, Int2ObjectMaps.emptyMap(), net.minecraft.screen.sync.ItemStackHash.EMPTY));
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragged != null) {
            dragX = click.x();
            dragY = click.y();
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (dragged != null) {
            int from = tiles.indexOf(dragged);
            int col = (int) Math.floor((click.x() - gridX) / cellW);
            int row = (int) Math.floor((click.y() - gridY) / cellH);
            dragged = null;
            if (from >= 0 && col >= 0 && col < cols && row >= 0 && row < rows) {
                for (int i = 0; i < tiles.size(); i++) {
                    if (i != from && cells[i][0] == col && cells[i][1] == row) {
                        cells[i] = cells[from]; // swap places with whatever was there
                    }
                }
                cells[from] = new int[]{col, row};
                saveCells();
            }
            return true;
        }
        return super.mouseReleased(click);
    }

    // ---- drawing ----

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        collect();
        boolean travel = isTravel();
        int right = vx() + vw() - PAD;
        int left = vx() + PAD, top = vy() + TOP, areaH = vh() - TOP - BAR - 6;
        int panelW = travel ? Math.min(200, vw() / 3) : 0;
        int areaW = right - left - panelW - (travel ? 10 : 0);
        boolean oneRow = !travel && !tiles.isEmpty() && tiles.size() <= 4; // a few buttons go in a single line
        if (oneRow) {
            tiles.sort(java.util.Comparator.comparing(t -> !t.name.equalsIgnoreCase("return"))); // return first, the rest as before
        }
        arrange(oneRow ? tiles.size() : 4, !travel && !oneRow);
        fit(left, top, areaW, areaH, travel ? 84 : 66, travel ? 96 : 76, travel ? 1.4f : 2f);

        int hover = dragged != null ? -1 : tileAt(mouseX, mouseY);
        for (int i = 0; i < tiles.size(); i++) {
            if (tiles.get(i) != dragged) {
                drawTile(ctx, tiles.get(i), cellX(cells[i][0]), cellY(cells[i][1]), i == hover);
            }
        }
        if (dragged != null) {
            drawTile(ctx, dragged, (float) dragX - cellW / 2, (float) dragY - cellH / 2, true);
        }
        if (tiles.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("waiting for the menu..."), vx() + vw() / 2, vy() + vh() / 2, Foe.DIM);
        }
        if (travel) {
            drawBest(ctx, right - panelW, top, panelW, areaH, hover >= 0 ? tiles.get(hover) : null);
        }
        if (hover >= 0 && !editing) {
            ctx.drawItemTooltip(textRenderer, tiles.get(hover).stack, mouseX, mouseY);
        }
    }

    private void drawTile(DrawContext ctx, Tile t, float x, float y, boolean hot) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        float w = cellW / scale, h = cellH / scale;
        DofiSettings cfg = DofiSettings.get();
        if (cfg.menuAccent) {
            int iw = (int) w, ih = (int) h;
            int gx = Math.round((gridX - x) / scale), gw = Math.round(cols * cellW / scale); // one gradient across the whole grid
            Hypr.rrect(ctx, 0, 0, iw, ih, 0, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), gx, gw, cfg.menuAccentOpacity / 100f), 8);
            if (hot) {
                Hypr.ring(ctx, 0, 0, iw, ih, 0, 1, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), gx, gw, 1), 8);
            }
        } else {
            ctx.fill(0, 0, (int) w, (int) h, hot ? 0xAA313244 : 0x88181825); // square, so neighbours leave no holes at the corners
        }
        if (t.loc != null) {
            drawLocation(ctx, t, w, h);
        } else {
            int square = squareColor(t.stack);
            if (square != 0) { // the quest menus coloured blocks are flat squares, like in foecollab
                ctx.fill((int) (w - 48) / 2, 8, (int) (w - 48) / 2 + 48, 56, square);
            } else {
                ctx.getMatrices().pushMatrix();
                ctx.getMatrices().translate((w - 48) / 2f, 8);
                ctx.getMatrices().scale(3f, 3f);
                ctx.drawItem(t.stack, 0, 0);
                ctx.getMatrices().popMatrix();
            }
            label(ctx, t.name, w / 2f, 62, w - 8, Foe.TEXT);
        }
        if (editing) {
            ctx.drawText(textRenderer, Foe.text("↔"), 6, 5, Foe.ACCENT, false);
        }
        ctx.getMatrices().popMatrix();
    }

    /** The colour of a shulker box, concrete or terracotta button (the quest, difficulty and tier pages), or 0 for any other item. */
    private static int squareColor(ItemStack stack) {
        String path = Registries.ITEM.getId(stack.getItem()).getPath();
        if (path.equals("shulker_box")) {
            return 0xFF976B97; // undyed has no dye colour
        }
        if (path.equals("terracotta") || path.endsWith("_terracotta")) {
            // map colour keeps terracottas muted tone, so blue terracotta differs from blue concrete
            return 0xFF000000 | Block.getBlockFromItem(stack.getItem()).getDefaultMapColor().color;
        }
        for (String suffix : new String[]{"_shulker_box", "_concrete"}) {
            if (path.endsWith(suffix)) {
                DyeColor dye = DyeColor.byId(path.substring(0, path.length() - suffix.length()), null);
                return dye != null ? 0xFF000000 | dye.getEntityColor() : 0;
            }
        }
        return 0;
    }

    private void drawLocation(DrawContext ctx, Tile t, float w, float h) {
        int d = 56;
        float cx = w / 2f, cy = 8 + d / 2f + 3;
        ctx.getMatrices().pushMatrix(); // the menu's own item, which is the picture the game ships for this place
        ctx.getMatrices().translate(cx - 24, cy - 24);
        ctx.getMatrices().scale(3f, 3f);
        ctx.drawItem(t.stack, 0, 0);
        ctx.getMatrices().popMatrix();
        float r = d / 2f + 4;
        Arc.draw(ctx, cx, cy, r, 3, 0, 1, Foe.FAINT);
        if (t.info.progress() >= 0) {
            Arc.draw(ctx, cx, cy, r, 3, 0, t.info.progress(), Foe.ACCENT);
        }
        String level = t.info.level() >= 0 ? Integer.toString(t.info.level()) : "?";
        Text text = Foe.bold(level);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, cy - 11);
        ctx.getMatrices().scale(2.5f, 2.5f);
        ctx.drawTextWithShadow(textRenderer, text, -textRenderer.getWidth(text) / 2, 0, 0xFFFFFFFF);
        ctx.getMatrices().popMatrix();
        label(ctx, t.loc.name(), cx, 8 + d + 10, w - 8, Foe.TEXT);
        if (t.info.progress() >= 0) {
            label(ctx, Math.round(t.info.progress() * 100) + "%", cx, 8 + d + 20, w - 8, Foe.DIM);
        }
    }

    private void label(DrawContext ctx, String s, float cx, float y, float maxW, int color) {
        Text text = Foe.text(s);
        while (textRenderer.getWidth(text) > maxW && s.length() > 1) {
            s = s.substring(0, s.length() - 1);
            text = Foe.text(s + "..");
        }
        ctx.drawText(textRenderer, text, (int) (cx - textRenderer.getWidth(text) / 2f), (int) y, color, false);
    }

    // ---- best location ----

    private void drawBest(DrawContext ctx, int x, int y, int w, int h, Tile focus) {
        Foe.panel(ctx, x, y, w, h, Foe.text("best place to fish"), 100);
        List<Tile> ranked = new ArrayList<>(tiles.stream().filter(t -> t.info.level() >= 0).toList());
        ranked.sort(Comparator.comparingInt((Tile t) -> t.info.level()).thenComparing(t -> t.loc.name()));
        int ty = y + 21;
        if (ranked.isEmpty()) {
            ty = wrap(ctx, "no levels found in the tooltips yet, so no ranking. the lowest level is usually the best place.", x + 8, ty, w - 16, Foe.DIM);
        }
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            Tile t = ranked.get(i);
            ctx.drawText(textRenderer, Foe.text((i + 1) + ". " + t.loc.name()), x + 8, ty, i == 0 ? Foe.ACCENT : Foe.TEXT, false);
            Text lvl = Foe.text("lvl " + t.info.level());
            ctx.drawText(textRenderer, lvl, x + w - 8 - textRenderer.getWidth(lvl), ty, Foe.DIM, false);
            ty += 12;
        }
        Tile show = focus != null ? focus : ranked.isEmpty() ? null : ranked.get(0);
        if (show == null) {
            return;
        }
        ctx.drawTextWithShadow(textRenderer, Foe.text("bait at " + show.loc.name()), x + 8, ty + 4, Foe.TITLE);
        ty += 12;
        var strategies = Locations.strategies(show.loc);
        if (strategies.isEmpty()) {
            wrap(ctx, "nothing noted for this one yet.", x + 8, ty, w - 16, Foe.DIM);
        }
        for (Locations.Strategy s : strategies) {
            if (ty > y + h - 22) {
                break;
            }
            ctx.drawText(textRenderer, Foe.text(s.bait() + (s.rarity().isEmpty() ? "" : " (" + s.rarity() + ")")), x + 8, ty,
                    s.rarity().isEmpty() ? Foe.TEXT : BaitInfo.rarityColor(s.rarity()) | 0xFF000000, false);
            ty += 10;
            String detail = String.join(", ", java.util.stream.Stream.of(s.quest().isEmpty() ? "" : s.quest() + " quest",
                    s.targets(), s.extra()).filter(p -> !p.isEmpty()).toList());
            ty = wrap(ctx, detail.isEmpty() ? "no notes" : detail, x + 14, ty, w - 22, Foe.DIM) + 2;
        }
    }

    /** Draws wrapped text, returns the y below it. */
    private int wrap(DrawContext ctx, String s, int x, int y, int w, int color) {
        for (var line : textRenderer.wrapLines(Foe.text(s), w)) {
            ctx.drawText(textRenderer, line, x, y, color, false);
            y += 10;
        }
        return y;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.isEscape()) { // a menu with a return button goes back instead of closing
            for (Tile t : tiles) {
                if (t.name.equalsIgnoreCase("return")) {
                    sendClick(t.slot.id, 0);
                    return true;
                }
            }
        }
        if (client.options.inventoryKey.matchesKey(input)) { // e closes it like the chest
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    protected int sizePct() {
        return DofiSettings.get().menuSize;
    }
}
