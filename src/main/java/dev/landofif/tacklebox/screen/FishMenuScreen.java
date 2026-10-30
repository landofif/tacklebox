package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.compat.FoeMainMenu;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.hud.BaitInfo;
import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.menu.MenuRows;
import dev.landofif.tacklebox.menu.QuestRedo;
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
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * The fishonmc main menu, /travel and the button menus (quests, stats, crew) as one dark panel of the mods own:
 * an accent line on top, tabs that run the menu commands, and every server button as a row with its icon, one
 * line about it and a bar when its lore has a count. It reads the real chest the server opened (its slots keep
 * updating) and a click sends the plain click on that slot, like pressing the button in the chest would.
 *
 * The main menu puts the daily missions beside the buttons; places (/travel, compendium) get their level and the
 * best place to fish with its noted bait beside them.
 */
public class FishMenuScreen extends TackleboxScreen {
    private static final int HEADER = 46, FOOTER = 16, ROW = 22, PAD = 10, GAP = 14;
    private static final int PANEL = 0xF0101016, LINE = 0x22FFFFFF, TRACK = 0x33FFFFFF, HOVER = 0x18FFFFFF;
    private static final String TRAVEL_TITLE = "픁", COMPENDIUM_TITLE = "픂";

    /** The tabs: label, the command it runs, and the menu name that makes it the active one. */
    private record Tab(String label, String command, String menu) {}

    private static final List<Tab> TABS = List.of(new Tab("menu", "menu", "main menu"), new Tab("travel", "travel", "travel"),
            new Tab("quests", "quests", "quests"), new Tab("stats", "stats", "stats"), new Tab("crew", "crew", "crew"));

    /** One button of the menu: a filled chest slot and what its row shows. */
    private record Tile(Slot slot, ItemStack stack, MenuRows.Row row, Locations.Location loc, FishMenu.Info info) {}

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
        return Glyphs.menuName(original.getTitle());
    }

    @Override
    protected void init() {
        if (FoeMainMenu.TITLE.equals(original.getTitle().getString())) {
            FoeMainMenu.open(true); // foecollab's daily mission tracker reads the menu while this is set
        }
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

        int w = Math.min(vw() - 16, 600), h = vh() - 16;
        int x = vx() + (vw() - w) / 2, y = vy() + 8;
        ctx.fill(x, y, x + w, y + h, PANEL);
        Hypr.rrect(ctx, x, y, w, 2, 0, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x, w, 1), 4);
        drawHeader(ctx, x, y, w, menu, mouseX, mouseY);

        int bx = x + PAD, by = y + HEADER, bw = w - 2 * PAD, bh = h - HEADER - FOOTER;
        Tile hover = null;
        if (tiles.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("waiting for the menu..."), x + w / 2, by + bh / 2, Foe.DIM);
        } else if (menu.equals("main menu")) {
            Tile missions = null;
            for (Tile t : tiles) {
                String plain = Glyphs.plain(t.stack.getName());
                if (plain.equals("daily missions")) {
                    missions = t;
                } else if (plain.equals("fishing profile")) {
                    profile = t.row;
                }
            }
            int left = (bw - GAP) * 11 / 20;
            hover = column(ctx, 0, tiles, bx, by, left, bh, mouseX, mouseY, menu);
            splitX = bx + left + GAP / 2;
            drawMissions(ctx, missions, bx + left + GAP, by, bw - left - GAP, bh);
        } else if (places) {
            int left = (bw - GAP) * 3 / 5;
            hover = column(ctx, 0, tiles, bx, by, left, bh, mouseX, mouseY, menu);
            splitX = bx + left + GAP / 2;
            drawBest(ctx, bx + left + GAP, by, bw - left - GAP, bh, hover);
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

        Text hint = Foe.text("esc = back · wheel = scroll · hover for details");
        ctx.drawText(textRenderer, hint, x + w - PAD - textRenderer.getWidth(hint), y + h - 12, Foe.FAINT, false);
        if (hover != null) {
            ctx.drawItemTooltip(textRenderer, hover.stack, mouseX, mouseY);
        }
    }

    private void drawHeader(DrawContext ctx, int x, int y, int w, String menu, int mouseX, int mouseY) {
        ctx.drawText(textRenderer, Foe.bold(menu), x + PAD, y + 9, Hypr.acc2(), false);
        if (profile != null && profile.level() >= 0) { // the fishing level, like the event gui's placement
            int rx = x + w - PAD;
            if (!profile.amount().isEmpty()) {
                Text amount = Text.literal(profile.amount().toLowerCase(java.util.Locale.ROOT));
                rx -= textRenderer.getWidth(amount);
                ctx.drawText(textRenderer, amount, rx, y + 9, Foe.DIM, false);
                rx -= 66;
                bar(ctx, rx, y + 12, 60, profile.progress(), false);
            }
            Text level = Foe.text("lvl " + profile.level());
            rx -= textRenderer.getWidth(level) + 6;
            ctx.drawText(textRenderer, level, rx, y + 9, Foe.TEXT, false);
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
        int rx = x + w - PAD;
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
        int tx = x + 22, room = w - 22 - 4 - Math.max(rw, barW) - (rw > 0 || barW > 0 ? 8 : 0);

        String name = t.loc != null ? t.loc.name() : t.row.name();
        ctx.drawText(textRenderer, fit(Foe.text(name), room), tx, y + 2, hot ? Hypr.acc2() : Foe.TEXT, false);
        if (!t.row.sub().isEmpty()) {
            ctx.drawText(textRenderer, fit(Foe.sentence(t.row.sub()), room), tx, y + 12, Foe.DIM, false);
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

    private void drawMissions(DrawContext ctx, Tile item, int x, int y, int w, int h) {
        if (item == null) {
            return;
        }
        List<String> lore = lore(item.stack);
        List<MenuRows.Mission> missions = MenuRows.missions(lore);
        long done = missions.stream().filter(MenuRows.Mission::done).count();
        int ty = section(ctx, "daily missions", done + " / " + missions.size() + " done", x, y, w);
        String group = null;
        for (MenuRows.Mission m : missions) {
            if (ty > y + h - 30) {
                break;
            }
            if (!m.group().equals(group)) {
                group = m.group();
                ctx.drawText(textRenderer, Foe.text(group), x, ty + 2, Hypr.acc1(), false);
                ty += 13;
            }
            Text amount = Text.literal(m.have() + "/" + m.need());
            int aw = textRenderer.getWidth(amount);
            Text name = fit(Foe.text((m.done() ? "✔ " : "") + m.name()), w - aw - 8);
            ctx.drawText(textRenderer, name, x, ty, m.done() ? Foe.TITLE : Foe.TEXT, false);
            ctx.drawText(textRenderer, amount, x + w - aw, ty, Foe.DIM, false);
            bar(ctx, x, ty + 10, w, m.need() > 0 ? m.have() / (float) m.need() : 0, m.done());
            ty += 17;
        }
        String rerolls = MenuRows.rerolls(lore);
        if (!rerolls.isEmpty()) {
            ctx.drawText(textRenderer, Foe.text(rerolls), x, Math.min(ty + 4, y + h - 10), Foe.FAINT, false);
        }
    }

    // ---- best location ----

    private void drawBest(DrawContext ctx, int x, int y, int w, int h, Tile focus) {
        List<Tile> ranked = new ArrayList<>(tiles.stream().filter(t -> t.info.level() >= 0).toList());
        ranked.sort(Comparator.comparingInt((Tile t) -> t.info.level()).thenComparing(t -> t.loc.name()));
        int ty = section(ctx, "best place to fish", "", x, y, w);
        if (ranked.isEmpty()) {
            ty = wrap(ctx, "no levels found in the tooltips yet, so no ranking. the lowest level is usually the best place.", x, ty, w, Foe.DIM);
        }
        for (int i = 0; i < Math.min(3, ranked.size()); i++) {
            Tile t = ranked.get(i);
            ctx.drawText(textRenderer, Foe.text((i + 1) + ". " + t.loc.name()), x, ty, i == 0 ? Hypr.acc2() : Foe.TEXT, false);
            Text lvl = Foe.text("lvl " + t.info.level());
            ctx.drawText(textRenderer, lvl, x + w - textRenderer.getWidth(lvl), ty, Foe.DIM, false);
            ty += 12;
        }
        Tile show = focus != null ? focus : ranked.isEmpty() ? null : ranked.get(0);
        if (show == null) {
            return;
        }
        ty = section(ctx, "bait at " + show.loc.name(), "", x, ty + 8, w);
        var strategies = Locations.strategies(show.loc);
        if (strategies.isEmpty()) {
            wrap(ctx, "nothing noted for this one yet.", x, ty, w, Foe.DIM);
        }
        for (Locations.Strategy s : strategies) {
            if (ty > y + h - 22) {
                break;
            }
            ctx.drawText(textRenderer, Foe.text(s.bait() + (s.rarity().isEmpty() ? "" : " (" + s.rarity() + ")")), x, ty,
                    s.rarity().isEmpty() ? Foe.TEXT : BaitInfo.rarityColor(s.rarity()) | 0xFF000000, false);
            ty += 10;
            String detail = String.join(", ", java.util.stream.Stream.of(s.quest().isEmpty() ? "" : s.quest() + " quest",
                    s.targets(), s.extra()).filter(p -> !p.isEmpty()).toList());
            ty = wrap(ctx, detail.isEmpty() ? "no notes" : detail, x + 6, ty, w - 6, Foe.DIM) + 2;
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
