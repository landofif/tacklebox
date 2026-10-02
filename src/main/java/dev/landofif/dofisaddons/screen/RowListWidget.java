package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.Anim;
import dev.landofif.dofisaddons.ui.S24;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.List;

/**
 * Scrollable list whose rows are a horizontal strip of widgets with fixed widths. A new list (a screen
 * opening, a tab switch) brings its rows in one after another; rows added later just appear.
 */
public class RowListWidget extends ElementListWidget<RowListWidget.Row> {
    private static final long ROW_MS = 200;
    private static final long STAGGER_MS = 28;
    private static final int MAX_STAGGERED = 12;
    private static final int ROW_SLIDE = 10;

    private final int rowWidth;
    private final long builtAt = Anim.now();

    public RowListWidget(MinecraftClient client, int x, int y, int width, int height, int itemHeight) {
        super(client, width, height, y, itemHeight);
        this.setX(x);
        this.rowWidth = width - 14;
    }

    @Override
    public int getRowWidth() {
        return rowWidth;
    }

    @Override
    protected int getScrollbarX() {
        return this.getX() + this.width - 6;
    }

    // system24 look: no textured strip or separators, just a slim scrollbar (see ui/S24).
    @Override
    protected void drawMenuListBackground(DrawContext context) {
    }

    @Override
    protected void drawHeaderAndFooterSeparators(DrawContext context) {
    }

    @Override
    protected void drawScrollbar(DrawContext context, int mouseX, int mouseY) {
        int max = getMaxScrollY();
        if (max <= 0) {
            return;
        }
        int x = getScrollbarX() + 2;
        int thumb = Math.max(16, height * height / (height + max));
        int top = getY() + (int) ((height - thumb) * (getScrollY() / max));
        context.fill(x, getY(), x + 2, getBottom(), S24.BORDER);
        context.fill(x, top, x + 2, top + thumb, S24.ACCENT_DIM);
    }

    public void addRow(Row row) {
        row.appearAt = builtAt + Math.min(children().size(), MAX_STAGGERED) * STAGGER_MS;
        this.addEntry(row);
    }

    public void clearRows() {
        this.clearEntries();
    }

    public static class Row extends ElementListWidget.Entry<Row> {
        private static final int GAP = 2;
        private final List<ClickableWidget> widgets;
        long appearAt;

        /** Widgets are laid out left to right using their own widths. */
        public Row(List<ClickableWidget> widgets) {
            this.widgets = widgets;
        }

        @Override
        public List<? extends Element> children() {
            return widgets;
        }

        @Override
        public List<? extends Selectable> selectableChildren() {
            return widgets;
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, boolean hovered, float delta) {
            long now = Anim.now();
            float e = now < appearAt ? 0f : Anim.easeOut(Anim.progress(appearAt, ROW_MS));
            if (e < 1f) {
                context.getMatrices().pushMatrix();
                context.getMatrices().translate((1f - e) * ROW_SLIDE, 0);
            }
            int x = getX();
            int y = getY();
            for (ClickableWidget w : widgets) {
                w.setX(x);
                w.setY(y);
                w.render(context, mouseX, mouseY, delta);
                x += w.getWidth() + GAP;
            }
            if (e < 1f) {
                context.getMatrices().popMatrix();
                // fade in: the panel colour over the row, lifting away
                context.fill(getX() - 2, y - 2, getX() + getWidth() + ROW_SLIDE + 2, y + getHeight(), S24.alpha((int) ((1f - e) * 255), S24.BG_4));
            }
        }
    }
}
