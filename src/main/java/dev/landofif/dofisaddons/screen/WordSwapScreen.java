package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.display.WordSwap;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Formatting;

import java.util.List;

/** Word swaps: a phrase and the word shown instead of it, everywhere text is drawn. */
public class WordSwapScreen extends DofiScreen {
    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();
    private RowListWidget list;

    public WordSwapScreen(Screen parent) {
        super(Foe.text("dofis addons · word swaps"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(460, width - 20);
        int left = (width - w) / 2;
        list = new RowListWidget(client, left, 40, w, height - 40 - 36, 24);
        addDrawableChild(list);
        rebuild();
        int bw = (w - 8) / 3;
        int y = height - 28;
        addDrawableChild(ButtonWidget.builder(Foe.text("+ word"), b -> {
            s.words.add(new WordSwap.Entry());
            rebuild();
        }).dimensions(left, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(onOff(s.wordSwap), b -> {
            s.wordSwap = !s.wordSwap;
            b.setMessage(onOff(s.wordSwap));
        }).dimensions(left + bw + 4, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(left + 2 * (bw + 4), y, bw, 20).build());
    }

    private static net.minecraft.text.Text onOff(boolean on) {
        return Foe.text(on ? "swaps: on" : "swaps: off");
    }

    private void rebuild() {
        double scroll = list.getScrollY();
        list.clearRows();
        int fieldW = Math.max(50, (list.getRowWidth() - 5 * 20 - 6 * 4) / 2);
        for (WordSwap.Entry e : s.words) {
            TextFieldWidget from = field(fieldW, "replace this", e.from, v -> e.from = v);
            TextFieldWidget to = field(fieldW, "with this", e.to, v -> e.to = v);
            ButtonWidget on = ButtonWidget.builder(Foe.text(e.enabled ? "on" : "off"), b -> {
                e.enabled = !e.enabled;
                b.setMessage(Foe.text(e.enabled ? "on" : "off"));
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.sentence("turn this swap on or off"))).build();
            ButtonWidget aa = ButtonWidget.builder(Foe.text(e.caseSensitive ? "Aa" : "aa"), b -> {
                e.caseSensitive = !e.caseSensitive;
                b.setMessage(Foe.text(e.caseSensitive ? "Aa" : "aa"));
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.sentence("Aa: the capitals must match. aa: any capitals match."))).build();
            SwatchButton swatch = new SwatchButton(0, 0, 20, 20, () -> e.color < 0 ? 0x404040 : e.color,
                    () -> client.setScreen(new ColorPickerScreen(this, "word colour", e.color < 0 ? 0xFFFFFF : e.color, c -> e.color = c & 0xFFFFFF)));
            ButtonWidget noColor = ButtonWidget.builder(Foe.text("∅"), b -> {
                e.color = -1;
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.sentence("use the colour of the text it replaces"))).build();
            ButtonWidget delete = ButtonWidget.builder(Foe.text("✕").formatted(Formatting.RED), b -> {
                s.words.remove(e);
                rebuild();
            }).dimensions(0, 0, 20, 20).build();
            list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(from, to, swatch, noColor, on, aa, delete)));
        }
        list.setScrollY(scroll);
    }

    private TextFieldWidget field(int w, String hint, String value, java.util.function.Consumer<String> set) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, 0, 0, w, 20, Foe.text(hint));
        f.setMaxLength(128);
        f.setText(value);
        f.setPlaceholder(Foe.text(hint).formatted(Formatting.DARK_GRAY));
        f.setChangedListener(set);
        return f;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("the first word is shown as the second, in chat, huds, menus and tooltips")
                .withColor(Foe.DIM & 0xFFFFFF), width / 2, 23, 0xFFFFFFFF);
        if (s.words.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("no words yet. click + word to add one.")
                    .withColor(Foe.DIM & 0xFFFFFF), width / 2, 70, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        DofiSettings.save();
        client.setScreen(parent);
    }
}
