package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.screen.SettingsPages.Page;
import dev.landofif.tacklebox.screen.SettingsPages.Row;
import dev.landofif.tacklebox.screen.SettingsPages.Tile;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import dev.landofif.tacklebox.ui.Hypr.Bezier;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /tacklebox: the mods own settings, dressed like a hyprland desktop. A bar on top with a pill per page (a workspace), the
 * sections of the page as rounded tiles with gaps; the tile under the mouse is the focused one and gets the animated
 * gradient border. A page shows its tiles small, in a grid, with their name and on/off switch; clicking one opens it alone with
 * all its rows, and esc or its back label returns to the grid. Search shows the matching rows open. Everything moves on hyprland's curves: tiles pop in one after another, a page change slides and
 * fades, the pill glides, toggles and sliders ease, and closing pops out again.
 */
public class SettingsScreen extends Screen {
    private static final int OUT = 10, GAP = 6, BAR_Y = 6, BAR_H = 20, PAD = 8, TITLE_H = 20, ROW_H = 22, TILE_R = 4, GRID_H = 40, OPEN_W = 420;
    private static final int POP_MS = 420, STAGGER_MS = 45, SWITCH_MS = 450, CLOSE_MS = 220, SLIDE = 36;

    /** The tiles being shown (a page's grid, one opened tile, or search results) with their layout and scroll. */
    private static final class View {
        final List<Tile> tiles;
        final boolean search, grid;
        final int dir;
        final long born;
        final int[] tx, ty, tw, th;
        final float[] focus;
        int focusIdx, contentH, ox, oy;
        float scroll, target;

        View(List<Tile> tiles, boolean search, boolean grid, int dir, long born) {
            this.tiles = tiles;
            this.search = search;
            this.grid = grid;
            this.dir = dir;
            this.born = born;
            int n = tiles.size();
            tx = new int[n];
            ty = new int[n];
            tw = new int[n];
            th = new int[n];
            focus = new float[n];
        }
    }

    private final Screen parent;
    private final List<Page> pages = SettingsPages.build();
    private final long openedAt = Util.getMeasuringTimeMs();
    private long last = openedAt, closeAt = -1, tipSince;
    private float dt;
    private final SettingsNav nav = new SettingsNav();
    private View cur, prev;
    private long switchedAt;
    private float pillX = -1, pillW;
    private String query = "";
    private TextFieldWidget search;
    private Row dragging, tipRow;
    private int[] pillBox = new int[0];
    private int closeX;

    public SettingsScreen(Screen parent) {
        super(Foe.text("tacklebox"));
        this.parent = parent;
        cur = pageView(0, openedAt);
    }

    private int contentW() {
        return Math.min(width - 2 * OUT, 900);
    }

    private int contentTop() {
        return BAR_Y + BAR_H + OUT;
    }

    private int contentBottom() {
        return height - OUT;
    }

    @Override
    protected void init() {
        int sw = Math.max(70, Math.min(120, width / 5));
        closeX = width - OUT - 4 - 14;
        search = new TextFieldWidget(textRenderer, closeX - 8 - sw + 8, BAR_Y + 6, sw - 16, 10, Text.empty());
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

    /** Closes with the pop out animation; the screen goes away in {@link #tick}. */
    @Override
    public void close() {
        if (closeAt < 0) {
            closeAt = Util.getMeasuringTimeMs();
        }
    }

    @Override
    public void tick() {
        if (closeAt >= 0 && Util.getMeasuringTimeMs() - closeAt >= CLOSE_MS) {
            client.setScreen(parent);
        }
    }

    // ---- state changes

    private View pageView(int dir, long now) {
        return new View(pages.get(nav.page).tiles(), false, true, dir, now);
    }

    private void go(int to) {
        if (to < 0 || to >= pages.size() || closeAt >= 0 || (to == nav.page && cur.grid)) {
            return;
        }
        long now = Util.getMeasuringTimeMs();
        int dir = to == nav.page ? -1 : Integer.compare(to, nav.page);
        nav.page(to);
        query = "";
        search.setText("");
        swap(pageView(dir, now), now);
    }

    private void open(int tile) {
        long now = Util.getMeasuringTimeMs();
        nav.open(tile);
        swap(new View(List.of(cur.tiles.get(tile)), false, false, 1, now), now);
    }

    private boolean back() {
        if (closeAt >= 0 || !nav.back()) {
            return false;
        }
        long now = Util.getMeasuringTimeMs();
        swap(pageView(-1, now), now);
        return true;
    }

    private void swap(View next, long now) {
        prev = cur;
        cur = next;
        switchedAt = now;
        dragging = null;
    }

    private void onQuery(String text) {
        String q = text.trim().toLowerCase(Locale.ROOT);
        if (q.equals(query)) {
            return;
        }
        query = q;
        long now = Util.getMeasuringTimeMs();
        nav.back();
        if (q.isEmpty()) {
            swap(pageView(0, now), now);
            return;
        }
        List<Tile> found = new ArrayList<>();
        for (Page p : pages) {
            for (Tile t : p.tiles()) {
                List<Row> rows = t.rows().stream().filter(r -> r.name.contains(q) || r.desc.toLowerCase(Locale.ROOT).contains(q)).toList();
                if (!rows.isEmpty()) {
                    found.add(new Tile(p.name() + " · " + t.title(), Foe.bold(p.name() + " · " + t.title()), rows));
                }
            }
        }
        View next = new View(found, true, false, 0, cur.search ? cur.born : now);
        if (cur.search) {
            cur = next; // typing more letters updates the tiles in place
        } else {
            swap(next, now);
        }
    }

    // ---- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (closeAt >= 0) {
            return true;
        }
        double mx = click.x(), my = click.y();
        if (my >= BAR_Y && my < BAR_Y + BAR_H) {
            if (mx >= closeX && mx < closeX + 14) {
                close();
                return true;
            }
            for (int i = 0; i + 1 < pillBox.length; i += 2) {
                if (mx >= pillBox[i] && mx < pillBox[i + 1]) {
                    go(i / 2);
                    return true;
                }
            }
        } else if (my >= contentTop() && my < contentBottom()) {
            if (cur.grid) {
                for (int i = 0; i < cur.tiles.size(); i++) {
                    int x = cur.ox + cur.tx[i], y = cur.oy + cur.ty[i], w = cur.tw[i];
                    if (mx >= x && mx < x + w && my >= y && my < y + GRID_H) {
                        Row main = cur.tiles.get(i).main();
                        if (main != null && mx >= switchX(main, x, w) - 4) {
                            main.click(mx, click.button());
                        } else {
                            open(i);
                        }
                        return true;
                    }
                }
                return true;
            }
            if (!cur.search && mx >= cur.ox + cur.tx[0] && mx < cur.ox + cur.tx[0] + cur.tw[0] && my >= cur.oy && my < cur.oy + TITLE_H) {
                back(); // the title row of an opened tile is its back label
                return true;
            }
            for (Tile t : cur.tiles) {
                for (Row r : t.rows()) {
                    if (mx >= r.x && mx < r.x + r.w && my >= r.y && my < r.y + ROW_H) {
                        r.click(mx, click.button());
                        if (r.drags()) {
                            dragging = r;
                        }
                        return true;
                    }
                }
            }
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
        if (my >= BAR_Y && my < BAR_Y + BAR_H) {
            go(nav.page - (int) Math.signum(vertical)); // scrolling the bar changes workspace, like waybar
        } else {
            cur.target = Math.max(0, Math.min(Math.max(0, cur.contentH - (contentBottom() - contentTop())), cur.target - (float) vertical * 30));
        }
        return true;
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        int n = input.key() - GLFW.GLFW_KEY_1;
        if ((input.modifiers() & GLFW.GLFW_MOD_ALT) != 0 && n >= 0 && n < pages.size()) {
            go(n); // alt + number, like super + number
            return true;
        }
        if (input.key() == GLFW.GLFW_KEY_ESCAPE && back()) {
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

        float closeP = closeAt < 0 ? 0 : Hypr.clamp01((now - closeAt) / (float) CLOSE_MS);
        float barP = Bezier.OUT_QUINT.at((now - openedAt) / 450f);
        float fade = barP * (1 - closeP);
        drawBar(ctx, mouseX, mouseY, fade, now, barP, closeP);

        int top = contentTop(), bottom = contentBottom();
        ctx.enableScissor(0, top - 2, width, bottom);
        float sw = Hypr.clamp01((now - switchedAt) / (float) SWITCH_MS);
        float slide = Bezier.OUT_QUINT.at(sw);
        if (prev != null) {
            if (sw >= 1) {
                prev = null;
            } else {
                drawView(ctx, prev, Math.round(-cur.dir * slide * SLIDE), 1 - Math.min(1, slide * 1.8f), false, mouseX, mouseY, now, closeP);
            }
        }
        drawView(ctx, cur, Math.round(cur.dir * (1 - slide) * SLIDE), 1, true, mouseX, mouseY, now, closeP);
        ctx.disableScissor();
        drawScrollbar(ctx, fade);
        drawTip(ctx, mouseX, mouseY, now, fade);
    }

    private void drawBar(DrawContext ctx, int mouseX, int mouseY, float fade, long now, float barP, float closeP) {
        int lift = Math.round((1 - barP) * -BAR_H) - Math.round(closeP * BAR_H / 2f);
        int bx = OUT, bw = width - 2 * OUT, by = BAR_Y + lift;
        Hypr.rrect(ctx, bx, by, bw, BAR_H, 5, Hypr.alpha(Hypr.BAR, fade));

        pillBox = new int[pages.size() * 2];
        int x = bx + 5, py = by + 3;
        for (int i = 0; i < pages.size(); i++) {
            String num = String.valueOf(i + 1), name = Foe.smallCaps(pages.get(i).name());
            int w = textRenderer.getWidth(num) + 4 + textRenderer.getWidth(name) + 14;
            pillBox[i * 2] = x;
            pillBox[i * 2 + 1] = x + w;
            if (i == nav.page) {
                pillX = pillX < 0 ? x : Hypr.approach(pillX, x, dt, 16); // the active pill glides to its place
                pillW = pillW <= 0 ? w : Hypr.approach(pillW, w, dt, 16);
            }
            x += w + 2;
        }
        if (pillX >= 0) {
            Hypr.rrect(ctx, Math.round(pillX), py, Math.round(pillW), 14, 7,
                    Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), Math.round(pillX), Math.round(pillW), fade), 8);
        }
        for (int i = 0; i < pages.size(); i++) {
            int x0 = pillBox[i * 2], w = pillBox[i * 2 + 1] - x0;
            boolean hot = mouseX >= x0 && mouseX < x0 + w && mouseY >= BAR_Y && mouseY < BAR_Y + BAR_H;
            boolean on = i == nav.page;
            if (hot && !on) {
                Hypr.rrect(ctx, x0, py, w, 14, 7, Hypr.alpha(Hypr.SURFACE, fade));
            }
            int num = on ? Hypr.CRUST : Hypr.MUTED, label = on ? Hypr.CRUST : hot ? Hypr.TEXT : Hypr.SUB;
            String n = String.valueOf(i + 1);
            ctx.drawText(textRenderer, n, x0 + 7, py + 3, Hypr.alpha(num, fade), false);
            ctx.drawText(textRenderer, Foe.smallCaps(pages.get(i).name()), x0 + 7 + textRenderer.getWidth(n) + 4, py + 3, Hypr.alpha(label, fade), false);
        }

        // the focused tile's name in the middle, like the window title in waybar, if there is room for it
        String title = cur.tiles.isEmpty() ? "" : Foe.smallCaps(cur.tiles.get(Math.min(cur.focusIdx, cur.tiles.size() - 1)).title());
        if (!title.isEmpty() && !cur.grid && !cur.search) {
            title = Foe.smallCaps(pages.get(nav.page).name()) + " › " + title;
        }
        int free0 = x + 8, free1 = search.getX() - 12;
        int tw = textRenderer.getWidth(title);
        if (!title.isEmpty() && tw < free1 - free0) {
            ctx.drawText(textRenderer, title, (free0 + free1 - tw) / 2, by + 6, Hypr.alpha(Hypr.MUTED, fade), false);
        }

        // the search pill and the red close dot
        int sx = search.getX() - 8, sw = search.getWidth() + 16;
        Hypr.rrect(ctx, sx, py, sw, 14, 7, Hypr.alpha(search.isFocused() ? Hypr.OVERLAY : Hypr.SURFACE, fade));
        search.setY(by + 5);
        search.setAlpha(fade);
        search.render(ctx, mouseX, mouseY, 0);
        boolean hotClose = mouseX >= closeX && mouseX < closeX + 14 && mouseY >= BAR_Y && mouseY < BAR_Y + BAR_H;
        Hypr.rrect(ctx, closeX, py, 14, 14, 7, Hypr.alpha(hotClose ? Hypr.RED : Hypr.mix(Hypr.RED, Hypr.SURFACE, 0.55f), fade));
    }

    /** Lays the tiles out in columns, each into the shortest one (a masonry grid, like a tiling layout). */
    private void layout(View v) {
        int cw = contentW();
        if (v.grid) {
            v.contentH = SettingsNav.grid(v.tiles.size(), cw, GAP, GRID_H, v.tx, v.ty, v.tw);
            java.util.Arrays.fill(v.th, GRID_H);
            return;
        }
        if (!v.search) { // one opened tile, in the middle
            v.tw[0] = Math.min(cw, OPEN_W);
            v.tx[0] = (cw - v.tw[0]) / 2;
            v.ty[0] = 0;
            v.th[0] = TITLE_H + v.tiles.get(0).rows().size() * ROW_H + 4;
            v.contentH = v.th[0];
            return;
        }
        int cols = cw >= 780 ? 3 : cw >= 500 ? 2 : 1;
        int colW = (cw - GAP * (cols - 1)) / cols;
        int[] colH = new int[cols];
        for (int i = 0; i < v.tiles.size(); i++) {
            int c = 0;
            for (int k = 1; k < cols; k++) {
                if (colH[k] < colH[c]) {
                    c = k;
                }
            }
            v.tx[i] = c * (colW + GAP);
            v.ty[i] = colH[c];
            v.tw[i] = colW;
            v.th[i] = TITLE_H + v.tiles.get(i).rows().size() * ROW_H + 4;
            colH[c] += v.th[i] + GAP;
        }
        int max = 0;
        for (int h : colH) {
            max = Math.max(max, h);
        }
        v.contentH = Math.max(0, max - GAP);
    }

    private void drawView(DrawContext ctx, View v, int xOff, float aMul, boolean live, int mouseX, int mouseY, long now, float closeP) {
        layout(v);
        int top = contentTop(), bottom = contentBottom();
        v.target = Math.max(0, Math.min(Math.max(0, v.contentH - (bottom - top)), v.target));
        v.scroll = Hypr.approach(v.scroll, v.target, dt, 18);
        int ox = (width - contentW()) / 2 + xOff, oy = top - Math.round(v.scroll);
        v.ox = ox;
        v.oy = oy;
        boolean inClip = mouseY >= top && mouseY < bottom;
        if (live && v.tiles.isEmpty()) {
            String none = Foe.smallCaps("nothing found");
            ctx.drawText(textRenderer, none, (width - textRenderer.getWidth(none)) / 2, top + 20, Hypr.alpha(Hypr.MUTED, aMul * (1 - closeP)), false);
        }
        for (int i = 0; i < v.tiles.size(); i++) {
            float p = (now - v.born - (long) i * STAGGER_MS) / (float) POP_MS;
            float scale = (0.88f + 0.12f * Bezier.OVERSHOOT.at(p)) * (1 - 0.13f * closeP);
            float fade = Bezier.OUT_QUINT.at(p) * aMul * (1 - closeP);
            int x = ox + v.tx[i], y = oy + v.ty[i], w = v.tw[i], h = v.th[i];
            if (fade < 0.02f || y + h < top - 20 || y > bottom + 20) {
                continue;
            }
            if (live && inClip && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + h) {
                v.focusIdx = i; // focus follows the mouse and stays on the last tile when it leaves
            }
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate(x + w / 2f, y + h / 2f);
            m.scale(scale, scale);
            m.translate(-x - w / 2f, -y - h / 2f);
            drawTile(ctx, v, i, x, y, w, h, fade, live, mouseX, mouseY, inClip, now);
            m.popMatrix();
        }
    }

    private void drawTile(DrawContext ctx, View v, int i, int x, int y, int w, int h, float fade, boolean live, int mouseX, int mouseY, boolean inClip, long now) {
        Tile tile = v.tiles.get(i);
        v.focus[i] = Hypr.approach(v.focus[i], live && v.focusIdx == i ? 1 : 0, dt, 14);
        float f = v.focus[i];
        if (f > 0.03f) { // a soft shadow under the focused tile
            Hypr.rrect(ctx, x - 2, y - 1, w + 4, h + 5, TILE_R + 2, Hypr.alpha(0x000000, 0.16f * f * fade));
            Hypr.rrect(ctx, x - 1, y, w + 2, h + 2, TILE_R + 1, Hypr.alpha(0x000000, 0.16f * f * fade));
        }
        Hypr.rrect(ctx, x, y, w, h, TILE_R, Hypr.alpha(Hypr.BASE, fade));
        if (f > 0.02f) { // the border turns slowly, like hyprland's borderangle loop
            double angle = Math.toRadians(45) + (now % 10000) / 10000.0 * Math.PI * 2;
            Hypr.Paint grad = Hypr.angled(Hypr.acc1(), Hypr.acc2(), x, y, w, h, angle, 1);
            Hypr.ring(ctx, x, y, w, h, TILE_R, 1, (px, py) -> Hypr.alpha(Hypr.mix(Hypr.INACTIVE, grad.at(px, py), f), fade), 12);
        } else {
            Hypr.ring(ctx, x, y, w, h, TILE_R, 1, Hypr.alpha(Hypr.INACTIVE, fade));
        }
        int hx = x + PAD;
        if (!v.grid && !v.search) {
            boolean hotBack = live && inClip && mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + TITLE_H;
            String back = "‹ " + Foe.smallCaps(pages.get(nav.page).name());
            ctx.drawTextWithShadow(textRenderer, back, hx, y + 7, Hypr.alpha(hotBack ? Hypr.acc1() : Hypr.MUTED, fade));
            hx += textRenderer.getWidth(back) + 8;
        }
        ctx.drawTextWithShadow(textRenderer, tile.heading(), hx, y + 7, Hypr.alpha(Hypr.mix(Hypr.SUB, Hypr.acc2(), f), fade));
        if (v.grid) {
            drawGridTile(ctx, tile, x, y, w, h, fade, live && inClip, mouseX, mouseY, now);
            return;
        }

        int ry = y + TITLE_H;
        int clipTop = contentTop() - ROW_H, clipBottom = contentBottom();
        for (Row r : tile.rows()) {
            if (ry < clipTop || ry > clipBottom) { // rows scrolled out of view cost nothing
                if (live) {
                    r.y = -9999;
                }
                ry += ROW_H;
                continue;
            }
            boolean hover = live && inClip && dragging == null && mouseX >= x && mouseX < x + w && mouseY >= ry && mouseY < ry + ROW_H;
            if (live) {
                r.x = x;
                r.y = ry;
                r.w = w;
                if (hover && r != tipRow) {
                    tipRow = r;
                    tipSince = now;
                } else if (!hover && r == tipRow) {
                    tipRow = null;
                }
            }
            r.hot = Hypr.approach(r.hot, hover || r == dragging ? 1 : 0, dt, 20);
            if (r.hot > 0.02f) {
                Hypr.rrect(ctx, x + 3, ry + 1, w - 6, ROW_H - 2, 3, Hypr.alpha(Hypr.OVERLAY, 0.4f * r.hot * fade));
            }
            int cw = r.width(textRenderer);
            int room = w - 2 * PAD - cw - 8;
            String label = r.label;
            if (textRenderer.getWidth(label) > room) {
                label = textRenderer.trimToWidth(label, room - textRenderer.getWidth("…")) + "…";
            }
            ctx.drawTextWithShadow(textRenderer, label, x + PAD, ry + 7, Hypr.alpha(Hypr.mix(Hypr.SUB, Hypr.TEXT, r.hot), fade));
            r.draw(ctx, textRenderer, x + w - PAD - cw, ry + (ROW_H - Row.H) / 2, fade, hover || r == dragging, dt);
            ry += ROW_H;
        }
    }

    private int switchX(Row main, int x, int w) {
        return x + w - PAD - main.width(textRenderer);
    }

    /** A tile in a page's grid: its name, how many settings it holds and its on/off switch, if it has one. */
    private void drawGridTile(DrawContext ctx, Tile tile, int x, int y, int w, int h, float fade, boolean live, int mouseX, int mouseY, long now) {
        int n = tile.rows().size();
        ctx.drawText(textRenderer, Foe.smallCaps(n == 1 ? "1 setting" : n + " settings"), x + PAD, y + 23, Hypr.alpha(Hypr.MUTED, fade), false);
        Row main = tile.main();
        if (main == null) {
            return;
        }
        int sx = switchX(main, x, w);
        boolean hover = live && mouseX >= sx - 4 && mouseX < x + w && mouseY >= y && mouseY < y + h;
        if (hover && main != tipRow) {
            tipRow = main;
            tipSince = now;
        } else if (!hover && main == tipRow) {
            tipRow = null;
        }
        main.draw(ctx, textRenderer, sx, y + (h - Row.H) / 2, fade, hover, dt);
    }

    private void drawScrollbar(DrawContext ctx, float fade) {
        int view = contentBottom() - contentTop();
        if (cur.contentH <= view) {
            return;
        }
        int thumb = Math.max(16, view * view / cur.contentH);
        int y = contentTop() + Math.round((view - thumb) * (cur.scroll / (cur.contentH - view)));
        Hypr.rrect(ctx, width - 5, y, 2, thumb, 1, Hypr.alpha(Hypr.acc1(), 0.7f * fade));
    }

    /** What a setting does, as a small popup that fades in after the mouse rests on a row (like a notification). */
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
