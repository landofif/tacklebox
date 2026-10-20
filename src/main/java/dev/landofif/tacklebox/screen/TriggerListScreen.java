package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.trigger.TriggerStore;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import dev.landofif.tacklebox.ui.Hypr.Bezier;
import dev.landofif.tacklebox.util.RichText;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;

import java.util.ArrayList;
import java.util.List;

/**
 * Every trigger as a tile (the settings screen look, now only the mod screens carry it). Click the dot to switch one on or off, the name to edit
 * it, the arrows to reorder, share to copy a code, the red dot to delete. New, import, reload and done are in the bar below.
 */
public class TriggerListScreen extends Screen {
    private static final int ROW_H = 28, PITCH = 32, TOP = 44, PAD = 6, R = 4;
    private static final String[] BAR = {"+ new trigger", "import", "reload file", "done"};

    private final Screen parent;
    private final long born = Util.getMeasuringTimeMs();
    private long last = born, statusUntil;
    private float dt, scroll, target;
    private float[] focus = new float[0];
    private String status = "";
    private int statusColor;
    private int[] barBox = new int[BAR.length * 2];

    public TriggerListScreen(Screen parent) {
        super(Foe.text("tacklebox · chat triggers"));
        this.parent = parent;
    }

    private int listW() {
        return Math.min(520, width - 20);
    }

    private int listX() {
        return (width - listW()) / 2;
    }

    private int bottom() {
        return height - 40;
    }

    /** x of the zones of a row: dot, name end, up, down, share (and its width), delete. */
    private int[] zones(int x, int w) {
        int del = x + w - PAD - 14;
        int shareW = textRenderer.getWidth(Foe.smallCaps("share")) + 14;
        int share = del - 4 - shareW;
        int down = share - 4 - 14;
        int up = down - 2 - 14;
        return new int[]{x + PAD, up - 6, up, down, share, shareW, del};
    }

    private void setBy(Formatting f) {
        statusColor = f == Formatting.RED ? Hypr.RED : Hypr.acc2();
    }

    void setStatus(String message, Formatting color) {
        status = message;
        setBy(color);
        statusUntil = System.currentTimeMillis() + 3500;
    }

    // ---- input

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        double mx = click.x(), my = click.y();
        if (my >= height - 32 && my < height - 12) {
            for (int i = 0; i < BAR.length; i++) {
                if (mx >= barBox[i * 2] && mx < barBox[i * 2 + 1]) {
                    barAction(i);
                    return true;
                }
            }
        }
        if (my >= TOP && my < bottom() && mx >= listX() && mx < listX() + listW()) {
            List<Trigger> all = TriggerStore.all();
            int i = (int) ((my - TOP + scroll) / PITCH);
            if (i >= 0 && i < all.size() && (my - TOP + scroll) - i * PITCH < ROW_H) {
                int[] z = zones(listX(), listW());
                Trigger t = all.get(i);
                if (mx < z[0] + 20) {
                    t.enabled = !t.enabled;
                    TriggerStore.save();
                } else if (mx >= z[6]) {
                    confirmDelete(t);
                } else if (mx >= z[4]) {
                    client.keyboard.setClipboard(TriggerStore.exportCode(t));
                    setStatus("copied \"" + t.name + "\" share code to clipboard", Formatting.GREEN);
                } else if (mx >= z[3] && mx < z[3] + 14) {
                    TriggerStore.move(i, 1);
                } else if (mx >= z[2] && mx < z[2] + 14) {
                    TriggerStore.move(i, -1);
                } else if (mx < z[1]) {
                    client.setScreen(new TriggerEditScreen(this, t.copyKeepId(), false));
                }
                return true;
            }
        }
        return super.mouseClicked(click, doubled);
    }

    private void barAction(int i) {
        switch (i) {
            case 0 -> client.setScreen(new PresetPickerScreen(this));
            case 1 -> onImport();
            case 2 -> {
                TriggerStore.load();
                setStatus("reloaded triggers.json", Formatting.GREEN);
            }
            default -> close();
        }
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        target = Math.max(0, Math.min(Math.max(0, TriggerStore.all().size() * PITCH - PAD - (bottom() - TOP)), target - (float) vertical * 40));
        return true;
    }

    private void confirmDelete(Trigger t) {
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                TriggerStore.remove(t);
            }
            client.setScreen(this);
        }, Foe.text("delete \"" + t.name + "\"?"), Foe.text("this cant be undone. tip: use share first to keep a copy.")));
    }

    private void onImport() {
        Trigger imported = TriggerStore.importCode(client.keyboard.getClipboard());
        if (imported == null) {
            setStatus("no share code on the clipboard (it starts with tacklebox-trigger:)", Formatting.RED);
            return;
        }
        TriggerStore.put(imported);
        setStatus("imported \"" + imported.name + "\"", Formatting.GREEN);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }

    // ---- drawing

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = Util.getMeasuringTimeMs();
        dt = Math.min(0.1f, (now - last) / 1000f);
        last = now;
        List<Trigger> all = TriggerStore.all();
        if (focus.length != all.size()) {
            focus = java.util.Arrays.copyOf(focus, all.size());
        }
        float head = Bezier.OUT_QUINT.at((now - born) / 450f);
        ctx.drawTextWithShadow(textRenderer, Foe.bold("chat triggers"), listX() + 2, 14 - Math.round((1 - head) * 8), Hypr.alpha(Hypr.acc2(), head));
        String sub = Foe.smallCaps("show your own titles and sounds when a chat message appears");
        ctx.drawText(textRenderer, sub, listX() + 2, 26 - Math.round((1 - head) * 8), Hypr.alpha(Hypr.MUTED, head), false);

        int top = TOP, bottom = bottom();
        target = Math.max(0, Math.min(Math.max(0, all.size() * PITCH - PAD - (bottom - top)), target));
        scroll = Hypr.approach(scroll, target, dt, 18);
        List<Text> tip = null;
        int x = listX(), w = listW();
        ctx.enableScissor(0, top - 2, width, bottom);
        for (int i = 0; i < all.size(); i++) {
            int y = top - Math.round(scroll) + i * PITCH;
            if (y + ROW_H < top - 4 || y > bottom) {
                continue;
            }
            Trigger t = all.get(i);
            float p = (now - born - Math.min(i, 12) * 35L) / 400f;
            float a = Bezier.OUT_QUINT.at(p);
            float scale = 0.9f + 0.1f * Bezier.OVERSHOOT.at(p);
            boolean over = mouseY >= Math.max(top, y) && mouseY < Math.min(bottom, y + ROW_H) && mouseX >= x && mouseX < x + w;
            focus[i] = Hypr.approach(focus[i], over ? 1 : 0, dt, 14);
            if (a < 0.02f) {
                continue;
            }
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate(x + w / 2f, y + ROW_H / 2f);
            m.scale(scale, scale);
            m.translate(-x - w / 2f, -y - ROW_H / 2f);
            List<Text> t1 = drawRow(ctx, t, i, all.size(), x, y, w, a, over, mouseX, now);
            m.popMatrix();
            if (t1 != null) {
                tip = t1;
            }
        }
        ctx.disableScissor();
        if (all.isEmpty()) {
            String none = Foe.smallCaps("no triggers yet. click \"+ new trigger\" to make one.");
            ctx.drawText(textRenderer, none, (width - textRenderer.getWidth(none)) / 2, height / 2 - 10, Hypr.alpha(Hypr.MUTED, head), false);
        }
        drawBar(ctx, mouseX, mouseY, head);
        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            String s = Foe.smallCaps(status);
            ctx.drawText(textRenderer, s, (width - textRenderer.getWidth(s)) / 2, height - 46, statusColor, false);
        }
        if (tip != null) {
            ctx.drawTooltip(textRenderer, tip, mouseX, mouseY);
        }
    }

    /** One tile; returns the tooltip of the zone under the mouse, if any. */
    private List<Text> drawRow(DrawContext ctx, Trigger t, int i, int n, int x, int y, int w, float a, boolean over, int mouseX, long now) {
        float f = focus[i];
        Hypr.rrect(ctx, x, y, w, ROW_H, R, Hypr.alpha(Hypr.BASE, a));
        if (f > 0.02f) {
            double angle = Math.toRadians(45) + (now % 10000) / 10000.0 * Math.PI * 2;
            Hypr.Paint grad = Hypr.angled(Hypr.acc1(), Hypr.acc2(), x, y, w, ROW_H, angle, 1);
            Hypr.ring(ctx, x, y, w, ROW_H, R, 1, (px, py) -> Hypr.alpha(Hypr.mix(Hypr.INACTIVE, grad.at(px, py), f), a), 12);
        } else {
            Hypr.ring(ctx, x, y, w, ROW_H, R, 1, Hypr.alpha(Hypr.INACTIVE, a));
        }
        int[] z = zones(x, w);
        int cy = y + (ROW_H - 14) / 2;
        List<Text> tip = null;

        // the on/off dot
        boolean dotHot = over && mouseX < z[0] + 20;
        Hypr.rrect(ctx, z[0] + 2, cy + 2, 10, 10, 5, t.enabled ? Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), z[0] + 2, 10, a) : (px, py) -> Hypr.alpha(dotHot ? Hypr.MUTED : Hypr.OVERLAY, a), 3);
        if (dotHot) {
            tip = List.of(Foe.sentence(t.enabled ? "On: click to switch off" : "Off: click to switch on"));
        }

        // name and the title it shows
        String name = t.name == null || t.name.isBlank() ? "(unnamed)" : t.name;
        String preview = RichText.strip(t.title.enabled ? t.title.title : "");
        int room = z[1] - (z[0] + 20);
        String nm = textRenderer.trimToWidth(Foe.smallCaps(name), room);
        ctx.drawTextWithShadow(textRenderer, nm, z[0] + 20, y + 10, Hypr.alpha(t.enabled ? Hypr.TEXT : Hypr.MUTED, a));
        int used = textRenderer.getWidth(nm);
        if (!preview.isBlank() && room - used > 30) {
            String pv = textRenderer.trimToWidth("→ " + Foe.smallCaps(preview), room - used - 8);
            ctx.drawText(textRenderer, pv, z[0] + 20 + used + 8, y + 10, Hypr.alpha(Hypr.MUTED, a), false);
        }
        if (over && mouseX >= z[0] + 20 && mouseX < z[1]) {
            tip = List.of(Foe.sentence("Click to edit"), Foe.sentence("When chat says: " + t.pattern), Foe.sentence("Fired this session: " + TriggerEngine.count(t)));
        }

        // arrows, share, delete
        arrow(ctx, z[2], cy, "▲", i > 0, over && mouseX >= z[2] && mouseX < z[2] + 14, a);
        arrow(ctx, z[3], cy, "▼", i < n - 1, over && mouseX >= z[3] && mouseX < z[3] + 14, a);
        boolean shareHot = over && mouseX >= z[4] && mouseX < z[4] + z[5];
        if (shareHot) {
            Hypr.rrect(ctx, z[4], cy, z[5], 14, 7, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), z[4], z[5], a), 8);
        } else {
            Hypr.rrect(ctx, z[4], cy, z[5], 14, 7, Hypr.alpha(Hypr.SURFACE, a));
        }
        ctx.drawTextWithShadow(textRenderer, Foe.smallCaps("share"), z[4] + 7, cy + 3, Hypr.alpha(shareHot ? Hypr.CRUST : Hypr.TEXT, a));
        boolean delHot = over && mouseX >= z[6];
        Hypr.rrect(ctx, z[6], cy, 14, 14, 7, Hypr.alpha(delHot ? Hypr.RED : Hypr.mix(Hypr.RED, Hypr.SURFACE, 0.55f), a));
        if (shareHot) {
            tip = List.of(Foe.sentence("Copy a code others can import"));
        } else if (delHot) {
            tip = List.of(Foe.sentence("Delete"));
        }
        return tip;
    }

    private void arrow(DrawContext ctx, int x, int y, String glyph, boolean on, boolean hot, float a) {
        Hypr.rrect(ctx, x, y, 14, 14, 7, Hypr.alpha(hot && on ? Hypr.OVERLAY : Hypr.SURFACE, a));
        ctx.drawText(textRenderer, glyph, x + (14 - textRenderer.getWidth(glyph)) / 2, y + 3, Hypr.alpha(on ? Hypr.TEXT : Hypr.OVERLAY, a), false);
    }

    private void drawBar(DrawContext ctx, int mouseX, int mouseY, float a) {
        int total = 0;
        int[] ws = new int[BAR.length];
        for (int i = 0; i < BAR.length; i++) {
            ws[i] = textRenderer.getWidth(Foe.smallCaps(BAR[i])) + 18;
            total += ws[i] + 2;
        }
        total += 6;
        int bx = (width - total) / 2, by = height - 34 + Math.round((1 - a) * 12);
        Hypr.rrect(ctx, bx, by, total, 24, 5, Hypr.alpha(Hypr.BAR, a));
        int x = bx + 4;
        for (int i = 0; i < BAR.length; i++) {
            barBox[i * 2] = x;
            barBox[i * 2 + 1] = x + ws[i];
            boolean hot = mouseX >= x && mouseX < x + ws[i] && mouseY >= by && mouseY < by + 24;
            if (hot) {
                Hypr.rrect(ctx, x, by + 5, ws[i], 14, 7, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), x, ws[i], a), 8);
            }
            ctx.drawText(textRenderer, Foe.smallCaps(BAR[i]), x + 9, by + 8, Hypr.alpha(hot ? Hypr.CRUST : Hypr.SUB, a), false);
            x += ws[i] + 2;
        }
    }
}
