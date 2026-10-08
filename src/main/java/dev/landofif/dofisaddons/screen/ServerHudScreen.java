package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.hud.ServerHud;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Which lines the server hud shows and in what order: one chip per line (green is on, click to switch, drag to move it to
 * another place), the picture's opacity, and the hud itself on the right as it looks with the choices, so there is nothing to guess.
 */
public class ServerHudScreen extends DofiScreen {
    private static final int LEFT = 14, TOP = 34, CHIP_W = 112, CHIP_H = 16, GAP = 4, COLUMNS = 3;

    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();
    private List<String> order = ServerHud.order();
    /** The chip being held (index into {@link #order}, -1 for none), where the cursor is, and whether it moved far enough to be a drag. */
    private int held = -1;
    private double heldX, heldY, pressX, pressY;
    private boolean dragging;
    private ButtonWidget shortCatches;

    public ServerHudScreen(Screen parent) {
        super(Foe.text("dofis addons · server hud lines"));
        this.parent = parent;
    }

    private int chipX(int i) {
        return LEFT + i % COLUMNS * (CHIP_W + GAP);
    }

    private int chipY(int i) {
        return TOP + i / COLUMNS * (CHIP_H + GAP);
    }

    private int chipAt(double x, double y) {
        for (int i = 0; i < order.size(); i++) {
            if (x >= chipX(i) && x < chipX(i) + CHIP_W && y >= chipY(i) && y < chipY(i) + CHIP_H) {
                return i;
            }
        }
        return -1;
    }

    @Override
    protected void init() {
        int y = TOP + (order.size() + COLUMNS - 1) / COLUMNS * (CHIP_H + GAP) + 6;
        int full = COLUMNS * CHIP_W + (COLUMNS - 1) * GAP, half = (full - GAP) / 2;
        addDrawableChild(new ValueSlider(LEFT, y, full, 20, "picture opacity", 0, 100, 5, "%", s.serverHudImage, v -> s.serverHudImage = (int) v));
        shortCatches = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            s.serverHudShortCatches = !s.serverHudShortCatches;
            shortLabel();
        }).dimensions(LEFT, y + 24, half, 20).build());
        shortLabel();
        addDrawableChild(ButtonWidget.builder(Foe.text("reset order"), b -> {
            s.serverHudOrder = new ArrayList<>();
            order = ServerHud.order();
        }).dimensions(LEFT + half + GAP, y + 24, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("open picture folder"), b -> {
            try {
                java.nio.file.Files.createDirectories(ServerHud.IMAGE.getParent());
            } catch (java.io.IOException ignored) {
            }
            net.minecraft.util.Util.getOperatingSystem().open(ServerHud.IMAGE.getParent());
        }).dimensions(LEFT, y + 48, full, 20).build());
    }

    private void shortLabel() {
        shortCatches.setMessage(Text.literal(Foe.smallCaps("short catches 4.3k")).formatted(s.serverHudShortCatches ? Formatting.GREEN : Formatting.DARK_GRAY));
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        int i = click.button() == 0 ? chipAt(click.x(), click.y()) : -1;
        if (i < 0) {
            return false;
        }
        held = i;
        pressX = heldX = click.x();
        pressY = heldY = click.y();
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (held >= 0) {
            heldX = click.x();
            heldY = click.y();
            dragging |= Math.abs(heldX - pressX) + Math.abs(heldY - pressY) > 4;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (held < 0) {
            return super.mouseReleased(click);
        }
        int from = held, to = chipAt(click.x(), click.y());
        held = -1;
        if (!dragging) { // a click: on or off
            String key = order.get(from);
            if (!s.serverHudHide.remove(key)) {
                s.serverHudHide.add(key);
            }
        } else if (to >= 0 && to != from) { // the chip takes the place it was dropped on
            order.add(to, order.remove(from));
            s.serverHudOrder = new ArrayList<>(order);
        }
        dragging = false;
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int target = dragging ? chipAt(mouseX, mouseY) : -1;
        for (int i = 0; i < order.size(); i++) {
            chip(ctx, order.get(i), chipX(i), chipY(i), i == held && dragging ? 0.35f : 1f, i != held && chipAt(mouseX, mouseY) == i);
            if (i == target && i != held) {
                ctx.drawStrokedRectangle(chipX(i), chipY(i), CHIP_W, CHIP_H, Foe.ACCENT);
            }
        }
        if (dragging) {
            chip(ctx, order.get(held), (int) heldX - CHIP_W / 2, (int) heldY - CHIP_H / 2, 1f, true);
        }
        int x = LEFT + COLUMNS * (CHIP_W + GAP) + 24;
        ctx.drawTextWithShadow(textRenderer, Foe.text("preview"), x, TOP - 12, Foe.DIM);
        ServerHud.draw(ctx, x, TOP);
        List<OrderedText> hint = textRenderer.wrapLines(Foe.sentence("Click a chip to switch its line on or off, drag it to move the line. The two prospect chips make one line, at the place of the first. Luck, scale, speeds and prospect fill in when you open /menu."), width - 2 * LEFT);
        for (int i = 0; i < hint.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, hint.get(i), LEFT, height - 6 - (hint.size() - i) * 10, Foe.FAINT);
        }
    }

    private void chip(DrawContext ctx, String key, int x, int y, float alpha, boolean hot) {
        Foe.button(ctx, x, y, CHIP_W, CHIP_H, hot);
        boolean on = !s.serverHudHide.contains(key);
        int color = (on ? 0x55FF55 : 0x555555) | (int) (alpha * 255) << 24;
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.text(ServerHud.label(key)), x + CHIP_W / 2, y + (CHIP_H - 8) / 2, color);
    }

    @Override
    public void close() {
        DofiSettings.save();
        client.setScreen(parent);
    }
}
