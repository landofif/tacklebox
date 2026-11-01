package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.compat.FoeArmor;
import dev.landofif.tacklebox.hud.EventPage;
import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.menu.MenuRows;
import dev.landofif.tacklebox.menu.MenuTiles;
import dev.landofif.tacklebox.menu.QuestRedo;
import dev.landofif.tacklebox.menu.StatsPage;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import dev.landofif.tacklebox.util.Glyphs;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.block.Block;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.registry.Registries;
import net.minecraft.util.DyeColor;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * The fishonmc main menu, /travel and the button menus (quests, stats, crew) as one dark panel of the mods own:
 * an accent line on top, tabs that run the menu commands, and every server button as a row with its icon, one
 * line about it and a bar when its lore has a count. It reads the real chest the server opened (its slots keep
 * updating) and a click sends the plain click on that slot, like pressing the button in the chest would.
 *
 * The collections overview, places (/travel, compendium) and level pages are tiles instead of rows (luna, 2026-11-01):
 * a grid with the level big on each tile and the details in a hover box.
 */
public class FishMenuScreen extends TackleboxScreen {
    private static final int HEADER = 46, FOOTER = 16, ROW = 22, PAD = 10, GAP = 14;
    private static final int PANEL = 0xFF101016, LINE = 0x22FFFFFF, TRACK = 0x33FFFFFF, HOVER = 0x18FFFFFF, TILE = 0x0CFFFFFF;
    private static final String TRAVEL_TITLE = "픁", COMPENDIUM_TITLE = "픂";

    /** The tabs: label, the command it runs, and the menu name that makes it the active one. */
    private record Tab(String label, String command, String menu) {}

    private static final List<Tab> TABS = List.of(new Tab("menu", "menu", "main menu"), new Tab("travel", "travel", "travel"),
            new Tab("quests", "quests", "quests"), new Tab("stats", "stats", "stats"), new Tab("crew", "crew", "crew"),
            new Tab("event", "event", "event"));

    /** One button of the menu: a filled chest slot and what its row shows. */
    private record Tile(Slot slot, ItemStack stack, MenuRows.Row row, Locations.Location loc, FishMenu.Info info) {}

    private enum Kind { COLLECTIONS, PLACES, LEVELS }

    /** A tile page: the tiles in the order they show, the other buttons as rows under them, and what the header says. */
    private record Grid(Kind kind, List<Tile> tiles, List<Tile> rest, int cols, String title, String summary) {}

    /** Something clickable from the last frame. */
    private record Hit(int x, int y, int w, int h, Runnable action, Tile tile) {
        boolean contains(double mx, double my) {
            return mx >= x && mx < x + w && my >= y && my < y + h;
        }
    }

    /** The profile head's level and progress, kept so every tab can show it in the header. */
    private static MenuRows.Row profile;

    private final GenericContainerScreen original;
    private final ScreenHandler handler;
    private final List<Tile> tiles = new ArrayList<>();
    private final List<Hit> hits = new ArrayList<>();
    private final float[] scroll = new float[2];
    private int splitX;
    /** The /stats page when this menu is one, read again every frame. */
    private StatsPage.Stats stats;
    /** This frame's tile page, null for rows; whether its grid scrolls; the hover box of a collection or place tile. */
    private Grid grid;
    private boolean gridScrolls;
    private List<String> tip;
    /** The model wearing the set on the /stats page, and whose profile it was made for. */
    private net.minecraft.client.network.OtherClientPlayerEntity model;
    private String modelOwner;
    /** foecollabs "hide armor" would hide the set on the model, so it is off while a stats page shows. */
    private final FoeArmor armor = new FoeArmor();

    public FishMenuScreen(GenericContainerScreen original) {
        super(Text.empty()); // the panel draws its own title
        this.original = original;
        this.handler = original.getScreenHandler();
    }

    /** The chest screen this one replaces (its title is what tells which menu it is). */
    public GenericContainerScreen original() {
        return original;
    }

    /** Places mode is decided by what is in the chest, because the items arrive just after the screen opens. */
    private boolean isPlaces() {
        int hits = 0;
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) && !slot.getStack().isEmpty()
                    && Locations.byName(Glyphs.plain(slot.getStack().getName())) != null) {
                hits++;
            }
        }
        return hits >= 4;
    }

    private String menuName() {
        if (isPlaces()) {
            return original.getTitle().getString().contains(COMPENDIUM_TITLE) ? "compendium" : "travel";
        }
        String name = Glyphs.menuName(original.getTitle());
        return name.equals("crew") ? FishMenu.crewTitleMenu(names()) : name;
    }

    /** The plain names of the chest's items. */
    private List<String> names() {
        List<String> out = new ArrayList<>();
        for (Slot slot : handler.slots) {
            if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) && !slot.getStack().isEmpty()) {
                out.add(Glyphs.plain(slot.getStack().getName()).trim());
            }
        }
        return out;
    }

    @Override
    public void close() {
        client.player.closeHandledScreen();
    }

    @Override
    public void removed() {
        armor.show(false);
        super.removed();
    }

    // ---- model ----

    /** The level from the tooltip, which the compendium has and /travel does not: remembered, and used when it is missing. */
    private static FishMenu.Info levelOf(Locations.Location loc, ItemStack stack) {
        FishMenu.Info info = FishMenu.info(stack);
        var saved = TackleboxSettings.get().locationLevels;
        if (info.level() >= 0) {
            String now = info.level() + "|" + info.progress();
            if (!now.equals(saved.get(loc.id()))) {
                saved.put(loc.id(), now);
                TackleboxSettings.save();
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

    private static List<String> lore(ItemStack stack) {
        return stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines().stream().map(Text::getString).toList();
    }

    private void collect(boolean places) {
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
            if (name.equals("close") || name.equals("return")) {
                continue; // escape goes back or closes, so the buttons are only in the way
            }
            Locations.Location loc = Locations.byName(name);
            if (places && loc == null) {
                continue; // only the locations; other buttons stay in the server menu
            }
            tiles.add(new Tile(slot, stack, MenuRows.of(stack.getName().getString(), lore(stack)), loc, loc != null ? levelOf(loc, stack) : null));
        }
    }

    // ---- input ----

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        for (Hit h : hits) {
            if (h.contains(click.x(), click.y())) {
                if (h.tile != null) {
                    if ("quests".equals(menuName())) {
                        QuestRedo.click(QuestRedo.number(h.tile.stack.getName().getString(), h.tile.slot.id),
                                h.tile.stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines());
                    }
                    sendClick(h.tile.slot.id, click.button());
                } else {
                    h.action.run();
                }
                return true;
            }
        }
        return false;
    }

    /** The click as the server expects it, without the client picking the item up first. */
    private void sendClick(int slot, int button) {
        client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), (short) slot,
                (byte) button, SlotActionType.PICKUP, Int2ObjectMaps.emptyMap(), net.minecraft.screen.sync.ItemStackHash.EMPTY));
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        int col = mx < splitX ? 0 : 1;
        scroll[col] = Math.max(0, scroll[col] - (float) vertical * ROW); // drawing clamps the bottom
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (input.isEscape()) { // a menu with a return button goes back instead of closing
            for (Slot slot : handler.slots) {
                if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) && Glyphs.plain(slot.getStack().getName()).trim().equals("return")) {
                    sendClick(slot.id, 0);
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
        return TackleboxSettings.get().menuSize;
    }

    // ---- drawing ----

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        boolean places = isPlaces();
        String menu = menuName();
        collect(places);
        hits.clear();
        if (FishMenu.isInstanceList(tiles.stream().map(t -> Glyphs.plain(t.stack.getName()).trim()).toList())) {
            FishMenu.bypass = true; // it shares the crew title, so it is only known once its items are in
            client.setScreen(original);
            return;
        }
        for (Tile t : tiles) {
            if (Glyphs.plain(t.stack.getName()).equals("fishing profile") && !FishMenu.othersProfile(original)) {
                profile = t.row;
            }
        }

        stats = places ? null : StatsPage.read(pieces());
        armor.show(stats != null);
        grid = stats == null && !menu.equals("event") && !tiles.isEmpty() ? grid(menu, places) : null;
        tip = null;

        int w = Math.min(vw() - 16, 600), h = vh() - 16;
        int x = vx() + (vw() - w) / 2, y = vy() + 8;
        ctx.fill(x, y, x + w, y + h, PANEL);
        Hypr.rrect(ctx, x, y, w, 2, 0, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x, w, 1), 4);
        drawHeader(ctx, x, y, w, menu, mouseX, mouseY);

        int bx = x + PAD, by = y + HEADER, bw = w - 2 * PAD, bh = h - HEADER - FOOTER;
        Tile hover = null;
        if (tiles.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("waiting for the menu..."), x + w / 2, by + bh / 2, Foe.DIM);
        } else if (stats != null) {
            hover = drawStats(ctx, bx, by, bw, bh, mouseX, mouseY);
        } else if (menu.equals("event")) {
            hover = drawEvent(ctx, bx, by, bw, bh, mouseX, mouseY);
        } else if (grid != null) {
            hover = drawGrid(ctx, bx, by, bw, bh, mouseX, mouseY, menu);
        } else {
            boolean two = tiles.size() * ROW > bh && bw >= 360;
            int half = two ? (bw - GAP) / 2 : bw;
            int split = two ? (tiles.size() + 1) / 2 : tiles.size();
            hover = column(ctx, 0, tiles.subList(0, split), bx, by, half, bh, mouseX, mouseY, menu);
            splitX = two ? bx + half + GAP / 2 : Integer.MAX_VALUE;
            if (two) {
                Tile right = column(ctx, 1, tiles.subList(split, tiles.size()), bx + half + GAP, by, half, bh, mouseX, mouseY, menu);
                hover = hover != null ? hover : right;
            }
        }

        boolean wheel = stats == null && (grid == null || gridScrolls);
        Text hint = Foe.text(wheel ? "esc = back · wheel = scroll · hover for details" : "esc = back · hover for details");
        ctx.drawText(textRenderer, hint, x + w - PAD - textRenderer.getWidth(hint), y + h - 12, Foe.FAINT, false);
        if (tip != null) {
            drawTip(ctx, tip, mouseX, mouseY);
        } else if (hover != null) {
            ctx.drawItemTooltip(textRenderer, hover.stack, mouseX, mouseY);
        }
    }

    /** A short hover box: the first line is the title in the accent, the rest in sentence case, wrapped. */
    private void drawTip(DrawContext ctx, List<String> lines, int mouseX, int mouseY) {
        List<OrderedText> out = new ArrayList<>();
        for (int i = 0; i < lines.size(); i++) {
            String s = lines.get(i);
            Text line = i == 0 ? Foe.text(s).withColor(Hypr.acc2() & 0xFFFFFF) : Foe.sentence(s);
            out.addAll(s.isEmpty() ? List.of(OrderedText.EMPTY) : textRenderer.wrapLines(line, 220));
        }
        ctx.drawOrderedTooltip(textRenderer, out, mouseX, mouseY);
    }

    private void drawHeader(DrawContext ctx, int x, int y, int w, String menu, int mouseX, int mouseY) {
        // on a /stats page the header is that profile's: its name and level
        boolean page = stats != null;
        var event = menu.equals("event") ? EventPage.state() : null;
        String title = page && !stats.owner().isEmpty() ? stats.owner() : event != null && !event.title.isEmpty() ? event.title
                : grid != null && !grid.title().isEmpty() ? grid.title() : menu;
        ctx.drawText(textRenderer, Foe.bold(title), x + PAD, y + 9, Hypr.acc2(), false);
        int lvl = page ? stats.level() : profile != null ? profile.level() : -1;
        int rx = x + w - PAD;
        if (lvl >= 0) { // the fishing level, like the event gui's placement
            String have = page ? stats.amount() : profile.amount();
            float progress = page ? stats.progress() : profile.progress();
            if (!have.isEmpty()) {
                Text amount = Text.literal(have.toLowerCase(java.util.Locale.ROOT));
                rx -= textRenderer.getWidth(amount);
                ctx.drawText(textRenderer, amount, rx, y + 9, Foe.DIM, false);
                rx -= 66;
                bar(ctx, rx, y + 12, 60, progress, false);
            }
            Text level = Foe.text("lvl " + lvl);
            rx -= textRenderer.getWidth(level) + 6;
            ctx.drawText(textRenderer, level, rx, y + 9, Foe.TEXT, false);
            String placed = event != null && event.points >= 0 ? (event.placement.isEmpty() ? "" : event.placement + " · ") + Math.round(event.points) + " pts" : "";
            if (!placed.isEmpty()) {
                Text place = Foe.text(placed);
                rx -= textRenderer.getWidth(place) + 14;
                ctx.drawText(textRenderer, place, rx, y + 9, Foe.DIM, false);
            }
        }
        if (grid != null && !grid.summary().isEmpty()) { // "1 max · 7 almost there"
            Text summary = Foe.text(grid.summary());
            rx -= textRenderer.getWidth(summary) + (lvl >= 0 ? 14 : 0);
            ctx.drawText(textRenderer, summary, rx, y + 9, Foe.DIM, false);
        }

        int tx = x + PAD, ty = y + 27;
        for (Tab tab : TABS) {
            Text label = Foe.text(tab.label);
            int tw = textRenderer.getWidth(label);
            boolean active = tab.menu.equals(menu), hot = in(mouseX, mouseY, tx - 3, ty - 4, tw + 6, 16);
            ctx.drawText(textRenderer, label, tx, ty, active || hot ? Foe.TEXT : Foe.DIM, false);
            if (active) {
                Hypr.rrect(ctx, tx, ty + 11, tw, 2, 0, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), tx, tw, 1), 4);
            }
            hits.add(new Hit(tx - 3, ty - 4, tw + 6, 16, () -> client.player.networkHandler.sendChatCommand(tab.command), null));
            tx += tw + 16;
        }
        rx = x + w - PAD;
        Text close = Text.literal("×");
        rx -= textRenderer.getWidth(close);
        textButton(ctx, close, rx, ty, mouseX, mouseY, this::close);
        Text server = Foe.text("server menu");
        rx -= textRenderer.getWidth(server) + 12;
        textButton(ctx, server, rx, ty, mouseX, mouseY, () -> {
            FishMenu.bypass = true;
            client.setScreen(original);
        });
        ctx.fill(x + PAD, y + HEADER - 6, x + w - PAD, y + HEADER - 5, LINE);
    }

    private void textButton(DrawContext ctx, Text text, int x, int y, int mouseX, int mouseY, Runnable action) {
        int tw = textRenderer.getWidth(text);
        boolean hot = in(mouseX, mouseY, x - 3, y - 4, tw + 6, 16);
        if (hot) {
            ctx.fill(x - 3, y - 4, x + tw + 3, y + 12, HOVER);
        }
        ctx.drawText(textRenderer, text, x, y, hot ? Foe.TEXT : Foe.DIM, false);
        hits.add(new Hit(x - 3, y - 4, tw + 6, 16, action, null));
    }

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    /** A scrolling list of rows; returns the tile under the mouse. */
    private Tile column(DrawContext ctx, int col, List<Tile> rows, int x, int y, int w, int h, int mouseX, int mouseY, String menu) {
        scroll[col] = Math.min(scroll[col], Math.max(0, rows.size() * ROW - h));
        int top = y - Math.round(scroll[col]);
        Tile hover = null;
        ctx.enableScissor(x, y, x + w, y + h);
        for (int i = 0; i < rows.size(); i++) {
            int ry = top + i * ROW;
            if (ry + ROW <= y || ry >= y + h) {
                continue;
            }
            Tile t = rows.get(i);
            boolean hot = in(mouseX, mouseY, x, Math.max(y, ry), w, Math.min(ry + ROW, y + h) - Math.max(y, ry));
            drawRow(ctx, t, x, ry, w, hot, menu);
            if (hot) {
                hover = t;
            }
            int vy = Math.max(y, ry);
            hits.add(new Hit(x, vy, w, Math.min(ry + ROW, y + h) - vy, null, t));
        }
        ctx.disableScissor();
        return hover;
    }

    private void drawRow(DrawContext ctx, Tile t, int x, int y, int w, boolean hot, String menu) {
        // the full name: Glyphs.plain could drop "*FRENZY!*" if the server put it in a pack font
        int mark = t.loc != null ? Locations.mark(t.stack.getName().getString(), ServerHud.fabledPlace()) : 0;
        if (menu.equals("quests")) {
            List<Text> lore = t.stack.getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines();
            QuestRedo.see(QuestRedo.number(t.stack.getName().getString(), t.slot.id), lore);
            if (QuestRedo.marks(t.stack.getName().getString(), lore)) {
                mark = 0xFF55FF55;
            }
        }
        if (mark != 0) {
            ctx.fill(x, y + 1, x + w, y + ROW - 1, (hot ? 0x60000000 : 0x40000000) | (mark & 0xFFFFFF));
        } else if (hot) {
            ctx.fill(x, y + 1, x + w, y + ROW - 1, HOVER);
        }
        int square = squareColor(t.stack);
        if (square != 0) { // the quest menus coloured blocks as flat swatches, like in foecollab
            ctx.fill(x + 5, y + 5, x + 17, y + 17, square);
        } else {
            ctx.drawItem(t.stack, x + 3, y + 3);
        }

        FishMenu.Info info = t.info;
        int level = info != null ? info.level() : t.row.level();
        float progress = info != null ? info.progress() : t.row.progress();
        String right = info != null ? (level >= 0 ? "lvl " + level : "") : t.row.amount().toLowerCase(java.util.Locale.ROOT);
        int rw = right.isEmpty() ? 0 : textRenderer.getWidth(Foe.text(right));
        int barW = progress >= 0 ? Math.min(70, w / 4) : 0;
        // the amount sits on the name line, the bar on the line under it
        int tx = x + 22, room = w - 22 - 4 - (rw > 0 ? rw + 8 : 0), subRoom = w - 22 - 4 - (barW > 0 ? barW + 8 : 0);

        String name = t.loc != null ? t.loc.name() : t.row.name();
        ctx.drawText(textRenderer, fit(Foe.text(name), room), tx, y + 2, hot ? Hypr.acc2() : Foe.TEXT, false);
        if (!t.row.sub().isEmpty()) {
            ctx.drawText(textRenderer, fit(Foe.sentence(t.row.sub()), subRoom), tx, y + 12, Foe.DIM, false);
        }
        if (rw > 0) {
            ctx.drawText(textRenderer, Foe.text(right), x + w - 4 - rw, y + 3, progress >= 1 ? Foe.TITLE : Foe.TEXT, false);
        }
        if (barW > 0) {
            bar(ctx, x + w - 4 - barW, y + 15, barW, progress, progress >= 1);
        }
    }

    /** A thin bar: a faint track, the accent gradient up to {@code progress}, green when full. */
    private static void bar(DrawContext ctx, int x, int y, int w, float progress, boolean done) {
        ctx.fill(x, y, x + w, y + 2, TRACK);
        int fill = Math.round(w * Math.max(0, Math.min(1, progress)));
        if (done) {
            ctx.fill(x, y, x + fill, y + 2, Foe.TITLE);
        } else if (fill > 0) {
            Hypr.rrect(ctx, x, y, fill, 2, 0, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x, w, 1), 4);
        }
    }

    /** Cut to {@code w} pixels with ".." at the end. */
    private Text fit(Text text, int w) {
        if (textRenderer.getWidth(text) <= w) {
            return text;
        }
        String s = text.getString();
        while (s.length() > 1 && textRenderer.getWidth(s + "..") > w) {
            s = s.substring(0, s.length() - 1);
        }
        return Text.literal(s + "..").setStyle(text.getStyle());
    }

    /** A section title in grey with something in the accent on its right, and a line under it. */
    private int section(DrawContext ctx, String title, String right, int x, int y, int w) {
        ctx.drawText(textRenderer, Foe.text(title), x, y, Foe.DIM, false);
        if (!right.isEmpty()) {
            Text r = Foe.text(right);
            ctx.drawText(textRenderer, r, x + w - textRenderer.getWidth(r), y, Hypr.acc2(), false);
        }
        ctx.fill(x, y + 11, x + w, y + 12, LINE);
        return y + 16;
    }

    // ---- event page ----

    /** /event: the reward thresholds with their bars on the left, the missions from the book on the right, the other items below the thresholds. */
    private Tile drawEvent(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        var state = EventPage.state();
        int left = (w - GAP) / 2, rx = x + left + GAP, rw = w - left - GAP;
        List<Tile> steps = new ArrayList<>(), others = new ArrayList<>();
        for (Tile t : tiles) {
            String name = Glyphs.plain(t.stack.getName()).trim();
            var step = name.startsWith("threshold ") ? state.thresholds.stream()
                    .filter(th -> name.matches("threshold " + th.n() + "\\b.*")).findFirst().orElse(null) : null;
            if (step != null) {
                steps.add(new Tile(t.slot, t.stack, EventPage.threshold(step), null, null));
            } else {
                others.add(t);
            }
        }
        Tile hover = null;
        int ty = section(ctx, "thresholds", steps.stream().filter(t -> t.row.done()).count() + " / " + steps.size() + " reached", x, y, left);
        for (Tile t : steps) {
            hover = gearRow(ctx, t, x, ty, left, mouseX, mouseY, hover);
            ty += ROW;
        }
        ty += 6;
        for (Tile t : others) {
            if (ty + ROW > y + h) {
                break;
            }
            hover = gearRow(ctx, t, x, ty, left, mouseX, mouseY, hover);
            ty += ROW;
        }

        var missions = EventPage.missions();
        int done = (int) missions.stream().filter(m -> m.done()).count();
        int my = section(ctx, "missions", done + " / " + missions.size() + " done", rx, y, rw);
        scroll[1] = Math.min(scroll[1], Math.max(0, missions.size() * ROW - (y + h - my)));
        splitX = rx - GAP / 2;
        ctx.enableScissor(rx, my, rx + rw, y + h);
        int top = my - Math.round(scroll[1]);
        for (int i = 0; i < missions.size(); i++) {
            int ry = top + i * ROW;
            if (ry + ROW > my && ry < y + h) {
                drawRow(ctx, new Tile(null, ItemStack.EMPTY, EventPage.mission(missions.get(i)), null, null), rx, ry, rw, false, "event");
            }
        }
        ctx.disableScissor();
        if (missions.isEmpty()) {
            wrap(ctx, "the missions show once the event missions book is in.", rx, my, rw, Foe.DIM);
        }
        return hover;
    }

    // ---- stats page ----

    /** The chest items as the stats page reads them: slot id, name, lore and custom_data. */
    private List<StatsPage.Piece> pieces() {
        List<StatsPage.Piece> out = new ArrayList<>();
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) && !stack.isEmpty()) {
                out.add(new StatsPage.Piece(slot.id, stack.getName().getString(), lore(stack), data(stack)));
            }
        }
        return out;
    }

    private static net.minecraft.nbt.NbtCompound data(ItemStack stack) {
        var data = stack.get(DataComponentTypes.CUSTOM_DATA);
        return data != null ? data.copyNbt() : new net.minecraft.nbt.NbtCompound();
    }

    private Slot slot(int id) {
        return id >= 0 && id < handler.slots.size() ? handler.getSlot(id) : null;
    }

    /** A screen tile for a chest slot with its own row text, or null when the slot is empty. */
    private Tile gear(int id, String sub, float progress, String amount) {
        Slot slot = slot(id);
        if (slot == null || slot.getStack().isEmpty()) {
            return null;
        }
        return new Tile(slot, slot.getStack(), new MenuRows.Row(MenuRows.clean(slot.getStack().getName().getString()), sub, -1, progress, amount), null, null);
    }

    /** Rarity, then armour quality and tier, from the item's custom_data. */
    private static String about(ItemStack stack) {
        var d = data(stack);
        List<String> parts = new ArrayList<>();
        String rarity = d.getString("rarity", "").toLowerCase(java.util.Locale.ROOT);
        if (!rarity.isEmpty()) {
            parts.add(rarity);
        }
        if (d.getString("type", "").equals("hat")) {
            parts.add("hat");
        }
        d.getInt("quality").ifPresent(q -> parts.add("quality " + q + "%"));
        d.getInt("tier").ifPresent(t -> parts.add("tier " + t));
        return String.join(" · ", parts);
    }

    /** The profile layout: the model and showcase on the left, gear in the middle, number tiles and the pet on the right, buttons below. */
    private Tile drawStats(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        Tile hover = null;
        int buttonsH = 18, body = h - buttonsH - 6;
        int left = Math.max(96, Math.min(130, w / 4));
        int right = (w - left - 2 * GAP) * 9 / 20, mid = w - left - right - 2 * GAP;
        int mx = x + left + GAP, rx = mx + mid + GAP;

        // left: the player wearing the set, the five showcase slots under it
        int show = 18, showY = y + drawModel(ctx, x, y, left, body - show - 6, mouseX, mouseY) + 6;
        int sx = x + (left - 5 * show) / 2;
        for (int id : stats.displays()) {
            Slot slot = slot(id);
            boolean empty = slot.getStack().isOf(net.minecraft.item.Items.BREAD); // "Empty Profile Display"
            boolean hot = in(mouseX, mouseY, sx, showY, show, show);
            if (empty) {
                Hypr.ring(ctx, sx + 1, showY + 1, show - 2, show - 2, 3, 1, hot ? Hypr.OVERLAY : LINE);
            } else {
                ctx.fill(sx, showY, sx + show, showY + show, hot ? HOVER : 0x0CFFFFFF);
                ctx.drawItem(slot.getStack(), sx + 1, showY + 1);
            }
            Tile t = new Tile(slot, slot.getStack(), MenuRows.of(slot.getStack().getName().getString(), lore(slot.getStack())), null, null);
            hits.add(new Hit(sx, showY, show, show, null, t));
            hover = hot ? t : hover;
            sx += show;
        }

        // middle: armour, then the rod parts with the line's durability
        ctx.enableScissor(mx, y, mx + mid, y + body);
        int ty = section(ctx, "armour", "", mx, y, mid);
        for (int id : new int[]{stats.hat(), stats.helmet(), stats.chest(), stats.legs(), stats.boots()}) {
            Tile t = gear(id, id >= 0 ? about(slot(id).getStack()) : "", -1, "");
            if (t != null) {
                hover = gearRow(ctx, t, mx, ty, mid, mouseX, mouseY, hover);
                ty += ROW;
            }
        }
        ty = section(ctx, "rod", "", mx, ty + 6, mid);
        for (int id : new int[]{stats.reel(), stats.pole(), stats.line()}) {
            boolean line = id == stats.line() && stats.lineMax() > 0;
            Tile t = gear(id, id >= 0 ? about(slot(id).getStack()) : "", line ? stats.lineHave() / (float) stats.lineMax() : -1,
                    line ? stats.lineHave() + "/" + stats.lineMax() : "");
            if (t != null) {
                hover = gearRow(ctx, t, mx, ty, mid, mouseX, mouseY, hover);
                ty += ROW;
            }
        }
        ctx.disableScissor();

        // right: up to four number tiles in order, the pet card under them
        int tw = (right - 6) / 2, th = 38, i = 0;
        for (StatsPage.Tile n : new StatsPage.Tile[]{stats.power(), stats.runes(), stats.catches(), stats.bait()}) {
            if (n == null) {
                continue;
            }
            int tx = rx + (i % 2) * (tw + 6), tyy = y + (i / 2) * (th + 6);
            i++;
            Tile t = gear(n.slot(), "", -1, "");
            boolean hot = in(mouseX, mouseY, tx, tyy, tw, th);
            ctx.fill(tx, tyy, tx + tw, tyy + th, hot ? 0x1CFFFFFF : 0x0CFFFFFF);
            // a number big; a word (a lure's name) at the normal size when it does not fit big
            Text value = Foe.text(n.value().isEmpty() ? "-" : n.value().toLowerCase(java.util.Locale.ROOT));
            int big = textRenderer.getWidth(value) * 2 <= tw - 12 ? 2 : 1;
            ctx.getMatrices().pushMatrix();
            ctx.getMatrices().translate(tx + 6, tyy + 6);
            ctx.getMatrices().scale(big, big);
            ctx.drawText(textRenderer, fit(value, (tw - 12) / big), 0, 0, Hypr.acc2(), false);
            ctx.getMatrices().popMatrix();
            ctx.drawText(textRenderer, fit(Foe.text(n.sub()), tw - 12), tx + 6, tyy + th - 12, Foe.DIM, false);
            if (t != null) {
                hits.add(new Hit(tx, tyy, tw, th, null, t));
                hover = hot ? t : hover;
            }
        }
        StatsPage.Pet pet = stats.pet();
        Tile petTile = pet == null ? null : gear(pet.slot(), "", -1, "");
        if (petTile != null) {
            int py = section(ctx, "pet", pet.level() >= 0 ? "lvl " + pet.level() : "", rx, y + (i + 1) / 2 * (th + 6) + 4, right);
            int ph = 34;
            boolean hot = in(mouseX, mouseY, rx, py, right, ph);
            if (hot) {
                ctx.fill(rx, py, rx + right, py + ph, HOVER);
            }
            ctx.drawItem(petTile.stack, rx + 3, py + 3);
            ctx.drawText(textRenderer, fit(Foe.text(pet.name()), right - 26), rx + 22, py + 3, hot ? Hypr.acc2() : Foe.TEXT, false);
            if (!pet.item().isEmpty()) {
                ctx.drawText(textRenderer, fit(Foe.sentence(pet.item()), right - 26), rx + 22, py + 13, Foe.DIM, false);
            }
            if (pet.max()) {
                Text max = Foe.text("max");
                ctx.drawText(textRenderer, max, rx + right - textRenderer.getWidth(max) - 3, py + ph - 9, Foe.TITLE, false);
            } else if (pet.progress() >= 0) {
                Text amount = Text.literal(pet.amount().toLowerCase(java.util.Locale.ROOT));
                int aw = textRenderer.getWidth(amount);
                bar(ctx, rx + 3, py + ph - 5, right - aw - 12, pet.progress(), pet.progress() >= 1);
                ctx.drawText(textRenderer, amount, rx + right - aw - 3, py + ph - 9, Foe.DIM, false);
            }
            hits.add(new Hit(rx, py, right, ph, null, petTile));
            hover = hot ? petTile : hover;
        }

        // bottom: the other server buttons as small text buttons
        int bxx = x, byy = y + h - buttonsH + 4;
        for (int id : stats.buttons()) {
            Tile t = gear(id, "", -1, "");
            if (t == null) {
                continue;
            }
            Text label = Foe.text(t.row.name().replace(" (Click)", ""));
            int lw = textRenderer.getWidth(label);
            if (bxx + lw > x + w) {
                break;
            }
            boolean hot = in(mouseX, mouseY, bxx - 3, byy - 4, lw + 6, 16);
            if (hot) {
                ctx.fill(bxx - 3, byy - 4, bxx + lw + 3, byy + 12, HOVER);
            }
            ctx.drawText(textRenderer, label, bxx, byy, hot ? Foe.TEXT : Foe.DIM, false);
            hits.add(new Hit(bxx - 3, byy - 4, lw + 6, 16, null, t));
            hover = hot ? t : hover;
            bxx += lw + 14;
        }
        return hover;
    }

    private Tile gearRow(DrawContext ctx, Tile t, int x, int y, int w, int mouseX, int mouseY, Tile hover) {
        boolean hot = in(mouseX, mouseY, x, y, w, ROW);
        drawRow(ctx, t, x, y, w, hot, "stats");
        hits.add(new Hit(x, y, w, ROW, null, t));
        return hot ? t : hover;
    }

    /**
     * The profile's player wearing its chest, legs and boots (and a helmet if there is one); the hat is a pack model
     * on a leather boot, so it stays in the list. Another player's skin comes from the tab list when they are online.
     * Scaled to fit both sides of the room it has (the arms clipped when it was scaled by the height only); returns
     * the height of its box, which is about the model's.
     */
    private int drawModel(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY) {
        String owner = stats.owner().isEmpty() ? client.player.getName().getString() : stats.owner();
        if (model == null || !owner.equalsIgnoreCase(modelOwner)) {
            var entry = client.getNetworkHandler().getPlayerListEntry(owner);
            boolean mine = owner.equalsIgnoreCase(client.player.getName().getString());
            com.mojang.authlib.GameProfile gp = mine ? client.player.getGameProfile() : entry != null ? entry.getProfile()
                    : new com.mojang.authlib.GameProfile(net.minecraft.util.Uuids.getOfflinePlayerUuid(owner), owner);
            model = new net.minecraft.client.network.OtherClientPlayerEntity(client.world, gp);
            model.getDataTracker().set(net.minecraft.entity.PlayerLikeEntity.PLAYER_MODE_CUSTOMIZATION_ID, (byte) 0x7F); // all skin layers
            modelOwner = owner;
        }
        int[] ids = {stats.helmet(), stats.chest(), stats.legs(), stats.boots()};
        net.minecraft.entity.EquipmentSlot[] slots = {net.minecraft.entity.EquipmentSlot.HEAD, net.minecraft.entity.EquipmentSlot.CHEST,
                net.minecraft.entity.EquipmentSlot.LEGS, net.minecraft.entity.EquipmentSlot.FEET};
        for (int i = 0; i < ids.length; i++) {
            Slot slot = slot(ids[i]);
            model.equipStack(slots[i], slot == null || slot.getStack().isEmpty() ? ItemStack.EMPTY : PresetsScreen.wearable(slot.getStack(), slots[i]));
        }
        // a player is about 1.9 blocks tall and, with the arms, armour and the turn towards the mouse, 1.1 wide
        int size = (int) Math.min(h / 2.1f, w / 1.2f), box = Math.min(h, Math.round(size * 2.1f));
        ctx.fill(x, y, x + w, y + box, 0x0CFFFFFF);
        net.minecraft.client.gui.screen.ingame.InventoryScreen.drawEntity(ctx, x, y, x + w, y + box, size, 0.0625f, mouseX, mouseY, model);
        return box;
    }

    // ---- best location ----

    // ---- tile pages ----

    /** The tile page of this menu, or null when it is rows. Places keep the server's order (luna, 2026-11-01). */
    private Grid grid(String menu, boolean places) {
        if (places) {
            int max = 0;
            List<String> marks = new ArrayList<>();
            for (Tile t : tiles) {
                max += t.info.level() >= MenuTiles.PLACE_TOP ? 1 : 0;
                String word = MenuTiles.markWord(mark(t));
                if (!word.isEmpty()) {
                    marks.add(word + " at " + t.loc.name());
                }
            }
            return new Grid(Kind.PLACES, tiles, List.of(), 4, "", MenuTiles.line(max > 0 ? max + " max" : "", String.join(" · ", marks)));
        }
        List<Tile> picked = new ArrayList<>(), rest = new ArrayList<>();
        if (menu.equals("collections")) {
            for (Tile t : tiles) {
                (Glyphs.plain(t.stack.getName()).trim().endsWith(" collection") ? picked : rest).add(t);
            }
            Predicate<Tile> done = t -> t.row.level() >= MenuTiles.COLLECTION_TOP;
            int max = (int) picked.stream().filter(done).count();
            int almost = (int) picked.stream().filter(t -> !done.test(t) && t.row.progress() >= MenuTiles.ALMOST).count();
            return new Grid(Kind.COLLECTIONS, MenuTiles.byNextLevel(picked, t -> t.row.progress(), done), rest, 5, "", MenuTiles.summary(max, almost));
        }
        if (!MenuTiles.isLevelPage(tiles.stream().map(t -> Glyphs.plain(t.stack.getName()).trim()).toList())) {
            return null;
        }
        for (Tile t : tiles) {
            (MenuTiles.level(Glyphs.plain(t.stack.getName()).trim()) > 0 ? picked : rest).add(t);
        }
        long done = picked.stream().filter(t -> state(t) == MenuTiles.State.DONE).count();
        return new Grid(Kind.LEVELS, picked, rest, 5, MenuTiles.levelPageTitle(lore(picked.get(0).stack)), done + "/" + picked.size() + " done");
    }

    private static int mark(Tile t) {
        // the full name: Glyphs.plain could drop "*FRENZY!*" if the server put it in a pack font
        return Locations.mark(t.stack.getName().getString(), ServerHud.fabledPlace());
    }

    private static MenuTiles.State state(Tile t) {
        return MenuTiles.state(Registries.ITEM.getId(t.stack.getItem()).getPath());
    }

    /** The grid, as tall as fits without scrolling down to {@code MenuTiles.MIN_H}; the other buttons as rows under it. */
    private Tile drawGrid(DrawContext ctx, int x, int y, int w, int h, int mouseX, int mouseY, String menu) {
        int restRows = (grid.rest().size() + 1) / 2, room = h - (restRows == 0 ? 0 : restRows * ROW + MenuTiles.GAP);
        MenuTiles.Layout l = MenuTiles.layout(grid.tiles().size(), grid.cols(), w, room);
        gridScrolls = l.scrolls();
        splitX = Integer.MAX_VALUE; // the wheel scrolls the grid
        scroll[0] = Math.min(scroll[0], Math.max(0, l.height() - room));
        int top = y - Math.round(scroll[0]);
        Tile hover = null;
        ctx.enableScissor(x, y, x + w, y + room);
        for (int i = 0; i < grid.tiles().size(); i++) {
            int tx = x + i % l.cols() * (l.tw() + MenuTiles.GAP), ty = top + i / l.cols() * (l.th() + MenuTiles.GAP);
            if (ty + l.th() <= y || ty >= y + room) {
                continue;
            }
            Tile t = grid.tiles().get(i);
            int vy = Math.max(y, ty), vh = Math.min(ty + l.th(), y + room) - vy;
            boolean hot = in(mouseX, mouseY, tx, vy, l.tw(), vh);
            drawTile(ctx, t, tx, ty, l.tw(), l.th(), hot);
            hits.add(new Hit(tx, vy, l.tw(), vh, null, t));
            if (hot) {
                hover = t;
                tip = switch (grid.kind()) {
                    case COLLECTIONS -> MenuTiles.collectionTip(t.row, lore(t.stack));
                    case PLACES -> MenuTiles.placeTip(t.loc, MenuTiles.markWord(mark(t)), t.info.level(), t.info.progress(), t.row.sub());
                    case LEVELS -> null; // the server's tooltip has the rewards
                };
            }
        }
        ctx.disableScissor();
        int half = (w - GAP) / 2, ry = y + Math.min(l.height(), room) + MenuTiles.GAP;
        for (int i = 0; i < grid.rest().size(); i++) {
            Tile t = grid.rest().get(i);
            int rx = x + i % 2 * (half + GAP), yy = ry + i / 2 * ROW;
            boolean hot = in(mouseX, mouseY, rx, yy, half, ROW);
            drawRow(ctx, t, rx, yy, half, hot, menu);
            hits.add(new Hit(rx, yy, half, ROW, null, t));
            hover = hot ? t : hover;
        }
        return hover;
    }

    /** A collection (icon, short name, level big, bar) or a place (name, frenzy or fabled, level big, bar); level pages have their own. */
    private void drawTile(DrawContext ctx, Tile t, int x, int y, int w, int h, boolean hot) {
        int mark = grid.kind() == Kind.PLACES ? mark(t) : 0;
        ctx.fill(x, y, x + w, y + h, mark != 0 ? 0x40000000 | (mark & 0xFFFFFF) : TILE);
        if (hot) { // luna's choice: the hovered tile outlined in the accent
            outline(ctx, x, y, w, h, Hypr.acc2());
        }
        if (grid.kind() == Kind.LEVELS) {
            drawLevel(ctx, t, x, y, w, h);
            return;
        }
        boolean collection = grid.kind() == Kind.COLLECTIONS;
        int level = collection ? t.row.level() : t.info.level();
        float progress = collection ? t.row.progress() : t.info.progress();
        boolean done = level >= (collection ? MenuTiles.COLLECTION_TOP : MenuTiles.PLACE_TOP);
        int bw = 0;
        if (done) { // a green "max" at the normal size
            Text max = Foe.text("max");
            bw = textRenderer.getWidth(max);
            ctx.drawText(textRenderer, max, x + w - 6 - bw, y + 6, Foe.TITLE, false);
        } else if (level >= 0) {
            Text big = Text.literal(String.valueOf(level));
            bw = textRenderer.getWidth(big) * 2;
            big(ctx, big, x + w - 6 - bw, y + 5, Foe.TEXT);
        }
        int nx = x + 6;
        if (collection) {
            ctx.drawItem(t.stack, x + 5, y + 5);
            nx = x + 25;
        }
        int room = x + w - 6 - bw - 4 - nx, ny = y + 6;
        Text name = Foe.text(collection ? MenuTiles.shortName(t.row.name()) : t.loc.name());
        List<OrderedText> lines = textRenderer.wrapLines(name, room);
        for (OrderedText line : lines.size() <= 2 ? lines : List.of(fit(name, room).asOrderedText())) { // two lines, else cut
            ctx.drawText(textRenderer, line, nx, ny, Foe.TEXT, false);
            ny += 10;
        }
        String word = MenuTiles.markWord(mark);
        if (!word.isEmpty()) {
            ctx.drawText(textRenderer, Foe.text(word), nx, ny, Hypr.mix(mark, 0xFFFFFFFF, .5f), false);
        }
        if (done || progress >= 0) {
            tileBar(ctx, x + 6, y + h - 7, w - 12, done ? 1 : progress, done ? Foe.TITLE : Hypr.acc2());
        }
    }

    /** A level of a level page: its number big (green done, accent the one being worked on, faint later), the count, the bar. */
    private void drawLevel(DrawContext ctx, Tile t, int x, int y, int w, int h) {
        MenuTiles.State state = state(t);
        int color = state == MenuTiles.State.DONE ? Foe.TITLE : state == MenuTiles.State.NOW ? Hypr.acc2() : Foe.FAINT;
        big(ctx, Text.literal(String.valueOf(MenuTiles.level(Glyphs.plain(t.stack.getName()).trim()))), x + 6, y + 5, color);
        if (state != MenuTiles.State.DONE && !t.row.amount().isEmpty()) {
            Text count = Text.literal(MenuTiles.count(t.row.amount()));
            ctx.drawText(textRenderer, count, x + w - 6 - textRenderer.getWidth(count), y + 6, state == MenuTiles.State.NOW ? Foe.TEXT : Foe.DIM, false);
        }
        if (state == MenuTiles.State.DONE || t.row.progress() >= 0) {
            tileBar(ctx, x + 6, y + h - 7, w - 12, state == MenuTiles.State.DONE ? 1 : t.row.progress(), color);
        }
    }

    /** Text at twice the size. */
    private void big(DrawContext ctx, Text text, int x, int y, int color) {
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(2, 2);
        ctx.drawText(textRenderer, text, 0, 0, color, false);
        ctx.getMatrices().popMatrix();
    }

    /** A tile's bar: a faint track and one solid fill (a gradient is a fill per 4 px, on every tile). Rounded down, so 99.7% is not full. */
    private static void tileBar(DrawContext ctx, int x, int y, int w, float progress, int color) {
        ctx.fill(x, y, x + w, y + 2, TRACK);
        int fill = (int) (w * Math.max(0, Math.min(1, progress)));
        if (fill > 0) {
            ctx.fill(x, y, x + fill, y + 2, color);
        }
    }

    private static void outline(DrawContext ctx, int x, int y, int w, int h, int color) {
        ctx.fill(x, y, x + w, y + 1, color);
        ctx.fill(x, y + h - 1, x + w, y + h, color);
        ctx.fill(x, y + 1, x + 1, y + h - 1, color);
        ctx.fill(x + w - 1, y + 1, x + w, y + h - 1, color);
    }

    /** Draws wrapped text, returns the y below it. */
    private int wrap(DrawContext ctx, String s, int x, int y, int w, int color) {
        for (var line : textRenderer.wrapLines(Foe.text(s), w)) {
            ctx.drawText(textRenderer, line, x, y, color, false);
            y += 10;
        }
        return y;
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
}
