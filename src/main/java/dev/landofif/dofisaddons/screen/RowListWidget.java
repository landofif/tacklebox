package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.ElementListWidget;

import java.util.List;

/** Scrollable list whose rows are a horizontal strip of widgets with fixed widths. */
public class RowListWidget extends ElementListWidget<RowListWidget.Row> {
    private final int rowWidth;

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

    // the screen's box is the background: no textured strip or separators, just a slim scrollbar
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
        context.fill(x, getY(), x + 2, getBottom(), Foe.FAINT);
        context.fill(x, top, x + 2, top + thumb, Foe.DIM);
    }

    public void addRow(Row row) {
        this.addEntry(row);
    }

    public void clearRows() {
        this.clearEntries();
    }

    public static class Row extends ElementListWidget.Entry<Row> {
        private static final int GAP = 2;
        private final List<ClickableWidget> widgets;

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
            int x = getX();
            for (ClickableWidget w : widgets) {
                w.setX(x);
                w.setY(getY());
                w.render(context, mouseX, mouseY, delta);
                x += w.getWidth() + GAP;
            }
        }
    }
}
