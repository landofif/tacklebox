package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import dev.landofif.tacklebox.ui.Foe;
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
 * The server hud's lines: one chip per line (green is on) to switch them, the hud itself on the right as it looks with the
 * choices, where a line is dragged up or down to move it (luna found moving chips unclear), and the picture's opacity.
 */
public class ServerHudScreen extends TackleboxScreen {
    private static final int LEFT = 14, TOP = 34, CHIP_W = 112, CHIP_H = 16, GAP = 4, COLUMNS = 3;

    private final Screen parent;
    private final TackleboxSettings s = TackleboxSettings.get();
    private ButtonWidget shortCatches, roundProspect;
    /** Where the preview was drawn last frame, the row being dragged (-1 for none) and whether the mouse moved far enough for a drag. */
    private int[] preview;
    private int held = -1;
    private double pressX, pressY;
    private boolean dragging;

    public ServerHudScreen(Screen parent) {
        super(Foe.text("tacklebox · server hud lines"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int i = 0;
        for (String key : ServerHud.keys()) {
            ButtonWidget chip = ButtonWidget.builder(Text.empty(), b -> {
                if (!s.serverHudHide.remove(key)) {
                    s.serverHudHide.add(key);
                }
                label(b, key);
            }).dimensions(LEFT + i % COLUMNS * (CHIP_W + GAP), TOP + i / COLUMNS * (CHIP_H + GAP), CHIP_W, CHIP_H).build();
            label(chip, key);
            addDrawableChild(chip);
            i++;
        }
        int y = TOP + (i + COLUMNS - 1) / COLUMNS * (CHIP_H + GAP) + 6;
        int full = COLUMNS * CHIP_W + (COLUMNS - 1) * GAP, half = (full - GAP) / 2;
        addDrawableChild(new ValueSlider(LEFT, y, full, 20, "picture opacity", 0, 100, 5, "%", s.serverHudImage, v -> s.serverHudImage = (int) v));
        shortCatches = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            s.serverHudShortCatches = !s.serverHudShortCatches;
            labels();
        }).dimensions(LEFT, y + 24, half, 20).build());
        roundProspect = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            s.serverHudRoundProspect = !s.serverHudRoundProspect;
            labels();
        }).dimensions(LEFT + half + GAP, y + 24, half, 20).build());
        labels();
        addDrawableChild(ButtonWidget.builder(Foe.text("reset order"), b -> s.serverHudOrder = new ArrayList<>())
                .dimensions(LEFT, y + 48, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("open picture folder"), b -> {
            try {
                java.nio.file.Files.createDirectories(ServerHud.IMAGE.getParent());
            } catch (java.io.IOException ignored) {
            }
            net.minecraft.util.Util.getOperatingSystem().open(ServerHud.IMAGE.getParent());
        }).dimensions(LEFT + half + GAP, y + 48, half, 20).build());
    }

    private void label(ButtonWidget b, String key) {
        boolean on = !s.serverHudHide.contains(key);
        b.setMessage(Text.literal(Foe.smallCaps(ServerHud.label(key))).formatted(on ? Formatting.GREEN : Formatting.DARK_GRAY));
    }

    private void labels() {
        shortCatches.setMessage(Text.literal(Foe.smallCaps("short catches 4.3k")).formatted(s.serverHudShortCatches ? Formatting.GREEN : Formatting.DARK_GRAY));
        roundProspect.setMessage(Text.literal(Foe.smallCaps("round prospect 1/35")).formatted(s.serverHudRoundProspect ? Formatting.GREEN : Formatting.DARK_GRAY));
    }

    /** The row of the preview under the point, or -1. */
    private int rowAt(double x, double y) {
        if (preview == null) {
            return -1;
        }
        for (int i = 0; i < ServerHud.rowKeys().size(); i++) {
            int[] b = ServerHud.rowBounds(preview, i);
            if (x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3]) {
                return i;
            }
        }
        return -1;
    }

    /** The gap a dragged line would land in: the number of rows whose middle is above the point. */
    private int gapAt(double y) {
        int gap = 0;
        for (int i = 0; i < ServerHud.rowKeys().size(); i++) {
            int[] b = ServerHud.rowBounds(preview, i);
            if (b[1] + b[3] / 2 < y) {
                gap = i + 1;
            }
        }
        return gap;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        int row = click.button() == 0 ? rowAt(click.x(), click.y()) : -1;
        if (row < 0) {
            return false;
        }
        held = row;
        pressX = click.x();
        pressY = click.y();
        dragging = false;
        return true;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (held >= 0) {
            dragging |= Math.abs(click.x() - pressX) + Math.abs(click.y() - pressY) > 4;
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        if (held < 0) {
            return super.mouseReleased(click);
        }
        if (dragging && held < ServerHud.rowKeys().size()) {
            ServerHud.move(held, gapAt(click.y()));
        }
        held = -1;
        dragging = false;
        return true;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = LEFT + COLUMNS * (CHIP_W + GAP) + 24;
        ctx.drawTextWithShadow(textRenderer, Foe.text("preview"), x, TOP - 12, Foe.DIM);
        preview = ServerHud.draw(ctx, x, TOP);
        if (preview != null) {
            List<String> keys = ServerHud.rowKeys();
            int hover = held < 0 ? rowAt(mouseX, mouseY) : -1;
            for (int i = 0; i < keys.size(); i++) {
                int[] b = ServerHud.rowBounds(preview, i);
                if (dragging && held < keys.size() && keys.get(i).equals(keys.get(held))) { // the line being moved, all its rows
                    ctx.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x55FFAA00);
                } else if (i == hover) {
                    ctx.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], 0x30FFFFFF);
                }
            }
            if (dragging && !keys.isEmpty()) {
                int gap = gapAt(mouseY);
                int[] b = ServerHud.rowBounds(preview, Math.min(gap, keys.size() - 1));
                int y = b[1] + (gap < keys.size() ? 0 : b[3]);
                ctx.fill(preview[0], y - 1, preview[0] + preview[2], y + 1, Foe.ACCENT);
            }
        }
        List<OrderedText> hint = textRenderer.wrapLines(Foe.sentence("Drag a line in the preview up or down to move it. Click a chip to switch a line on or off. The two prospect chips make one line. Luck, scale, speeds and prospect fill in when you open /menu."), width - 2 * LEFT);
        for (int i = 0; i < hint.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, hint.get(i), LEFT, height - 6 - (hint.size() - i) * 10, Foe.FAINT);
        }
    }

    @Override
    public void close() {
        TackleboxSettings.save();
        client.setScreen(parent);
    }
}
