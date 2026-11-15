package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.screen.SettingsPages.Page;
import dev.landofif.tacklebox.screen.SettingsPages.Row;
import dev.landofif.tacklebox.screen.SettingsPages.Tile;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * /tacklebox: the mods own settings as three columns. The pages at the left (an item and a name each), the groups of the
 * picked page in the middle with their on/off switch, and the settings of the picked group at the right. The tools page
 * lists its links in the middle. Search shows the matching settings of every page at the right. Nothing slides or pops:
 * the screen and the right column fade in over {@link #FADE_MS}.
 */
public class SettingsScreen extends Screen {
    private static final int OUT = 10, GAP = 6, PAD = 8, HEAD_H = 28, ROW_H = 22, LIST_H = 24, R = 4, MAX_H = 420, FADE_MS = 200;

    /** What the right column shows under one heading: a group, or the matches of a group in a search. */
    private record Section(String heading, List<Row> rows) {}

    private final Screen parent;
    private final List<Page> pages = SettingsPages.build();
    private final SettingsNav nav = new SettingsNav(pages.size());
    private final Map<String, ItemStack> icons = new HashMap<>();
    private final long openedAt = Util.getMeasuringTimeMs();
    private long last = openedAt, closeAt = -1, detailAt = openedAt, tipSince;
    private float dt, scroll, scrollTarget;
    private int groupScroll, contentH;
    private String query = "";
    private List<Section> found = List.of();
    private TextFieldWidget search;
    private Row dragging, tipRow;
    /** The columns: left edge, then the widths of pages, groups and settings; and the top and height of all three. */
    private int[] col = new int[4];
    private int top, panelH;

    public SettingsScreen(Screen parent) {
        super(Foe.text("tacklebox"));
        this.parent = parent;
    }

    private boolean narrow() {
        return width < SettingsNav.NARROW;
    }

    private int groupsX() {
        return col[0] + col[1] + GAP;
    }

    private int detailX() {
        return groupsX() + col[2] + GAP;
    }

    private Page page() {
        return pages.get(nav.page);
    }

    @Override
    protected void init() {
        col = SettingsNav.columns(width, GAP);
        panelH = Math.min(height - 2 * OUT, MAX_H);
        top = (height - panelH) / 2;
        // the search sits over the pages; when those are icons only, in the heading of the groups
        int sx = narrow() ? groupsX() + 6 : col[0] + 6, sw = (narrow() ? col[2] : col[1]) - 12;
        search = new TextFieldWidget(textRenderer, sx + 7, top + 9, sw - 14, 10, Text.empty());
        search.setDrawsBackground(false);
        search.setMaxLength(40);
        search.setEditableColor(Hypr.TEXT);
        search.setPlaceholder(Foe.text("search").withColor(Hypr.MUTED & 0xFFFFFF));
        search.setText(query);
        search.setChangedListener(this::onQuery);
        addDrawableChild(search);
        setInitialFocus(search);
    }

    @Override
    public void removed() {
        TackleboxSettings.save();
    }

    /** Closes with a fade; the screen goes away in {@link #tick}. */
    @Override
    public void close() {
        if (closeAt < 0) {
            closeAt = Util.getMeasuringTimeMs();
        }
    }

    @Override
    public void tick() {
        if (closeAt >= 0 && Util.getMeasuringTimeMs() - closeAt >= FADE_MS) {
            client.setScreen(parent);
        }
    }

    // ---- state changes

    /** The right column shows something else: start at its top and fade it in. */
    private void changed() {
        detailAt = Util.getMeasuringTimeMs();
        scroll = scrollTarget = 0;
        dragging = null;
    }

    private void go(int to) {
        if (closeAt < 0 && nav.page(to)) {
            groupScroll = 0;
            clearSearch();
            changed();
        }
    }

    private void pick(int group) {
        boolean wasSearch = !query.isEmpty();
        clearSearch();
        if (wasSearch || group != nav.group()) {
            nav.group(group);
            changed();
        }
    }

    private void clearSearch() {
        if (!query.isEmpty()) {
            query = "";
            found = List.of();
            search.setText("");
        }
    }

    private void onQuery(String text) {
        String q = text.trim().toLowerCase(Locale.ROOT);
        if (q.equals(query)) {
            return;
        }
        boolean wasSearch = !query.isEmpty();
        query = q;
        List<Section> hits = new ArrayList<>();
        if (!q.isEmpty()) {
            for (Page p : pages) {
                for (Tile t : p.tiles()) {
                    List<Row> rows = t.rows().stream().filter(r -> r.name.contains(q) || r.desc.toLowerCase(Locale.ROOT).contains(q)).toList();
                    if (!rows.isEmpty()) {
                        hits.add(new Section(Foe.smallCaps(p.name() + " › " + t.title()), rows));
                    }
                }
            }
        }
        found = hits;
        if (wasSearch && !q.isEmpty()) {
            scroll = scrollTarget = 0; // typing more letters updates the list in place
        } else {
            changed();
        }
    }

    /** The headings and rows of the right column. */
    private List<Section> sections() {
        if (!query.isEmpty()) {
            return found;
        }
        if (page().links()) {
            return List.of();
        }
        return List.of(new Section(null, page().tiles().get(nav.group()).rows()));
    }

    /** The rows of the middle column: the links of the tools page, or one row per group. */
    private int listSize() {
        return page().links() ? page().tiles().get(0).rows().size() : page().tiles().size();
    }

    private int maxGroupScroll() {
        return Math.max(0, listSize() * LIST_H + 4 - (panelH - HEAD_H));
    }

    private int listTop() {
        return top + HEAD_H - groupScroll;
    }

    private int pagesTop() {
        return top + (narrow() ? 6 : HEAD_H);
    }

    // ---- input

    private static boolean in(double mx, double my, int x, int y, int w, int h) {
        return mx >= x && mx < x + w && my >= y && my < y + h;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closeAt >= 0) {
            return true;
        }
        double mx = click.x(), my = click.y();
        if (in(mx, my, detailX() + col[3] - 22, top + 4, 20, 20)) {
            close();
            return true;
        }
        if (in(mx, my, col[0], pagesTop(), col[1], pages.size() * LIST_H)) {
            go((int) (my - pagesTop()) / LIST_H);
            return true;
        }
        if (in(mx, my, groupsX(), top + HEAD_H, col[2], panelH - HEAD_H)) {
            int i = (int) Math.floor((my - listTop()) / LIST_H);
            if (i >= 0 && i < listSize()) {
                if (page().links()) {
                    page().tiles().get(0).rows().get(i).click(mx, click.button());
                } else {
                    Row main = page().tiles().get(i).main();
                    if (main != null && mx >= groupsX() + col[2] - PAD - main.width(textRenderer) - 4) {
                        main.click(mx, click.button());
                    } else {
                        pick(i);
                    }
                }
            }
            return true;
        }
        if (in(mx, my, detailX(), top + HEAD_H, col[3], panelH - HEAD_H)) {
            for (Section s : sections()) {
                for (Row r : s.rows()) {
                    if (in(mx, my, r.x, r.y, r.w, ROW_H)) {
                        r.click(mx, click.button());
                        if (r.drags()) {
                            dragging = r;
                        }
                        return true;
                    }
                }
            }
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging != null) {
            dragging.drag(click.x());
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (mx < groupsX()) {
            go(nav.page - (int) Math.signum(vertical)); // scrolling the pages walks through them
        } else if (mx < detailX()) {
            groupScroll = Math.max(0, Math.min(maxGroupScroll(), groupScroll - (int) Math.signum(vertical) * LIST_H));
        } else {
            scrollTarget -= (float) vertical * 30; // clamped when drawn
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int n = input.key() - GLFW.GLFW_KEY_1;
        if ((input.modifiers() & GLFW.GLFW_MOD_ALT) != 0 && n >= 0 && n < pages.size()) {
            go(n);
            return true;
        }
        return super.keyPressed(input);
    }

    // ---- drawing

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        dt = Math.min(0.1f, (now - last) / 1000f);
        last = now;
        float fade = Hypr.clamp01((now - openedAt) / (float) FADE_MS) * (closeAt < 0 ? 1 : 1 - Hypr.clamp01((now - closeAt) / (float) FADE_MS));
        boolean live = closeAt < 0;
        Row tip = null;

        drawPages(ctx, mouseX, mouseY, fade, live);
        Row t1 = drawGroups(ctx, mouseX, mouseY, fade, live);
        Row t2 = drawDetail(ctx, mouseX, mouseY, fade * Hypr.clamp01((now - detailAt) / (float) FADE_MS), fade, live);
        tip = t1 != null ? t1 : t2;
        if (tip != tipRow) {
            tipRow = tip;
            tipSince = now;
        }
        drawTip(ctx, mouseX, mouseY, now, fade);
    }

    /** A vanilla item as an icon; items cannot fade, so they only show once the screen is mostly there. */
    private void icon(DrawContext ctx, String id, int x, int y, float fade) {
        if (id == null || fade < 0.6f) {
            return;
        }
        ctx.drawItem(icons.computeIfAbsent(id, k -> new ItemStack(Registries.ITEM.get(Identifier.ofVanilla(k)))), x, y);
    }

    private void drawPages(DrawContext ctx, int mouseX, int mouseY, float fade, boolean live) {
        int x = col[0], w = col[1];
        Hypr.rrect(ctx, x, top, w, panelH, 5, Hypr.alpha(Hypr.BAR, fade));
        if (!narrow()) {
            drawSearch(ctx, mouseX, mouseY, fade);
        }
        int y = pagesTop();
        for (int i = 0; i < pages.size(); i++, y += LIST_H) {
            boolean on = i == nav.page, hot = live && in(mouseX, mouseY, x, y, w, LIST_H);
            if (on) {
                Hypr.rrect(ctx, x + 4, y + 1, w - 8, LIST_H - 2, narrow() ? R : (LIST_H - 2) / 2, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x + 4, w - 8, fade), 8);
            } else if (hot) {
                Hypr.rrect(ctx, x + 4, y + 1, w - 8, LIST_H - 2, narrow() ? R : (LIST_H - 2) / 2, Hypr.alpha(Hypr.SURFACE, fade));
            }
            icon(ctx, pages.get(i).icon(), narrow() ? x + (w - 16) / 2 : x + 10, y + 4, fade);
            if (!narrow()) {
                ctx.drawText(textRenderer, Foe.smallCaps(pages.get(i).name()), x + 32, y + 8, Hypr.alpha(on ? Hypr.CRUST : hot ? Hypr.TEXT : Hypr.SUB, fade), !on);
            }
        }
    }

    private void drawSearch(DrawContext ctx, int mouseX, int mouseY, float fade) {
        Hypr.rrect(ctx, search.getX() - 7, top + 6, search.getWidth() + 14, 16, 8, Hypr.alpha(search.isFocused() ? Hypr.OVERLAY : Hypr.SURFACE, fade));
        search.setAlpha(fade);
    }

    /** The groups of the page with their switch, or the links of the tools page. Returns the switch or link under the mouse. */
    private Row drawGroups(DrawContext ctx, int mouseX, int mouseY, float fade, boolean live) {
        int x = groupsX(), w = col[2];
        Hypr.rrect(ctx, x, top, w, panelH, R, Hypr.alpha(Hypr.BASE, fade));
        if (narrow()) {
            drawSearch(ctx, mouseX, mouseY, fade);
        } else {
            ctx.drawTextWithShadow(textRenderer, Foe.bold(page().name()), x + PAD + 2, top + 10, Hypr.alpha(Hypr.TEXT, fade));
        }
        Row tip = null;
        boolean links = page().links();
        List<Tile> tiles = page().tiles();
        groupScroll = Math.min(groupScroll, maxGroupScroll());
        ctx.enableScissor(x, top + HEAD_H, x + w, top + panelH - 2);
        boolean inside = live && in(mouseX, mouseY, x, top + HEAD_H, w, panelH - HEAD_H);
        int y = listTop();
        for (int i = 0; i < listSize(); i++, y += LIST_H) {
            boolean hot = inside && mouseY >= y && mouseY < y + LIST_H;
            boolean on = !links && query.isEmpty() && i == nav.group();
            if (on || hot) {
                Hypr.rrect(ctx, x + 4, y + 1, w - 8, LIST_H - 2, R, Hypr.alpha(Hypr.OVERLAY, (on ? 0.55f : 0.3f) * fade));
            }
            if (on) {
                Hypr.ring(ctx, x + 4, y + 1, w - 8, LIST_H - 2, R, 1, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x + 4, w - 8, fade), 8);
            }
            Row link = links ? tiles.get(0).rows().get(i) : null;
            Row main = links ? null : tiles.get(i).main();
            icon(ctx, links ? link.icon : tiles.get(i).icon(), x + 10, y + 4, fade);
            int room = w - 32 - PAD - (main == null ? 0 : main.width(textRenderer) + 6);
            String name = fit(links ? link.label : Foe.smallCaps(tiles.get(i).title()), room);
            ctx.drawTextWithShadow(textRenderer, name, x + 32, y + 8, Hypr.alpha(on || hot ? Hypr.TEXT : Hypr.SUB, fade));
            if (main != null) {
                int sx = x + w - PAD - main.width(textRenderer);
                boolean hotSwitch = hot && mouseX >= sx - 4;
                main.draw(ctx, textRenderer, sx, y + (LIST_H - Row.H) / 2, fade, hotSwitch, dt);
                if (hotSwitch) {
                    tip = main;
                }
            } else if (hot && links) {
                tip = link;
            }
        }
        ctx.disableScissor();
        return tip;
    }

    private String fit(String s, int room) {
        return textRenderer.getWidth(s) <= room ? s : textRenderer.trimToWidth(s, Math.max(0, room - textRenderer.getWidth("…"))) + "…";
    }

    /** The settings of the picked group, or what the search found. Returns the row under the mouse. */
    private Row drawDetail(DrawContext ctx, int mouseX, int mouseY, float fade, float frame, boolean live) {
        int x = detailX(), w = col[3];
        Hypr.rrect(ctx, x, top, w, panelH, R, Hypr.alpha(Hypr.BASE, frame));
        boolean hotClose = live && in(mouseX, mouseY, x + w - 22, top + 4, 20, 20);
        Hypr.rrect(ctx, x + w - 16, top + 10, 8, 8, 4, Hypr.alpha(hotClose ? Hypr.RED : Hypr.mix(Hypr.RED, Hypr.SURFACE, 0.4f), frame));

        boolean searching = !query.isEmpty();
        Tile tile = page().tiles().get(page().links() ? 0 : nav.group());
        int hx = x + PAD;
        if (!searching) {
            icon(ctx, tile.icon(), hx, top + 6, fade);
            hx += 22;
        }
        ctx.drawTextWithShadow(textRenderer, searching ? Foe.bold("search") : tile.heading(), hx, top + 10, Hypr.alpha(Hypr.TEXT, fade));

        List<Section> sections = sections();
        int view = panelH - HEAD_H - 4;
        scrollTarget = Math.max(0, Math.min(Math.max(0, contentH - view), scrollTarget));
        scroll = Hypr.approach(scroll, scrollTarget, dt, 18);
        String note = searching && sections.isEmpty() ? "nothing found" : !searching && page().links() ? "pick a screen in the list" : null;
        if (note != null) {
            ctx.drawText(textRenderer, Foe.smallCaps(note), x + PAD, top + HEAD_H + 6, Hypr.alpha(Hypr.MUTED, fade), false);
        }
        Row tip = null;
        int clipTop = top + HEAD_H, clipBottom = top + panelH - 4;
        boolean inside = live && dragging == null && in(mouseX, mouseY, x, clipTop, w, clipBottom - clipTop);
        ctx.enableScissor(x, clipTop, x + w, clipBottom);
        int y = clipTop - Math.round(scroll);
        for (Section s : sections) {
            if (s.heading() != null) {
                ctx.drawText(textRenderer, s.heading(), x + PAD, y + 8, Hypr.alpha(Hypr.MUTED, fade), false);
                y += 20;
            }
            for (Row r : s.rows()) {
                if (y + ROW_H < clipTop || y > clipBottom) { // rows scrolled out of view cost nothing and cannot be clicked
                    r.y = -9999;
                    y += ROW_H;
                    continue;
                }
                boolean hover = inside && mouseY >= y && mouseY < y + ROW_H;
                r.x = x;
                r.y = y;
                r.w = w;
                if (hover) {
                    tip = r;
                }
                r.hot = Hypr.approach(r.hot, hover || r == dragging ? 1 : 0, dt, 20);
                if (r.hot > 0.02f) {
                    Hypr.rrect(ctx, x + 3, y + 1, w - 6, ROW_H - 2, 3, Hypr.alpha(Hypr.OVERLAY, 0.4f * r.hot * fade));
                }
                int cw = r.width(textRenderer);
                ctx.drawTextWithShadow(textRenderer, fit(r.label, w - 2 * PAD - cw - 8), x + PAD, y + 7, Hypr.alpha(Hypr.mix(Hypr.SUB, Hypr.TEXT, r.hot), fade));
                r.draw(ctx, textRenderer, x + w - PAD - cw, y + (ROW_H - Row.H) / 2, fade, hover || r == dragging, dt);
                y += ROW_H;
            }
        }
        ctx.disableScissor();
        contentH = y + Math.round(scroll) - clipTop;
        if (contentH > view) {
            int thumb = Math.max(16, view * view / contentH);
            int ty = clipTop + Math.round((view - thumb) * (scroll / (contentH - view)));
            Hypr.rrect(ctx, x + w - 4, ty, 2, thumb, 1, Hypr.alpha(Hypr.acc1(), 0.7f * fade));
        }
        return tip;
    }

    /** What a setting does, as a small popup that fades in after the mouse rests on it. */
    private void drawTip(DrawContext ctx, int mouseX, int mouseY, long now, float fade) {
        if (tipRow == null || tipRow.desc.isEmpty() || dragging != null || closeAt >= 0) {
            return;
        }
        float a = Hypr.clamp01((now - tipSince - 400) / 160f) * fade;
        if (a < 0.03f) {
            return;
        }
        List<OrderedText> lines = textRenderer.wrapLines(Foe.sentence(tipRow.desc), 190);
        int w = 0;
        for (OrderedText l : lines) {
            w = Math.max(w, textRenderer.getWidth(l));
        }
        w += 14;
        int h = lines.size() * 10 + 10;
        int x = Math.min(mouseX + 12, width - w - 4), y = Math.min(mouseY + 12, height - h - 4);
        Hypr.rrect(ctx, x, y, w, h, 4, Hypr.alpha(0xF0181825, a));
        Hypr.ring(ctx, x, y, w, h, 4, 1, Hypr.alpha(Hypr.acc1(), 0.8f * a));
        for (int i = 0; i < lines.size(); i++) {
            ctx.drawText(textRenderer, lines.get(i), x + 7, y + 6 + i * 10, Hypr.alpha(Hypr.TEXT, a), false);
        }
    }
}
