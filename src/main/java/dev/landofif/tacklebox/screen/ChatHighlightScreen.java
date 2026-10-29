package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.chat.ChatHighlight;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Words shown in a colour inside chat lines, and whether your own name is one of them. */
public class ChatHighlightScreen extends TackleboxScreen {
    private final Screen parent;
    private final TackleboxSettings s = TackleboxSettings.get();
    private RowListWidget list;

    public ChatHighlightScreen(Screen parent) {
        super(Foe.text("tacklebox · chat highlights"));
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
            s.chatHighlights.add(new ChatHighlight.Entry());
            rebuild();
        }).dimensions(left, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(nameLabel(), b -> {
            s.chatHighlightName = !s.chatHighlightName;
            b.setMessage(nameLabel());
        }).dimensions(left + bw + 4, y, bw, 20).tooltip(Tooltip.of(Foe.sentence("your own name in gold, except in lines you sent"))).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(left + 2 * (bw + 4), y, bw, 20).build());
    }

    private Text nameLabel() {
        return Foe.text("your name: ").append(Foe.text(s.chatHighlightName ? "on" : "off").formatted(s.chatHighlightName ? Formatting.GREEN : Formatting.RED));
    }

    private void rebuild() {
        double scroll = list.getScrollY();
        list.clearRows();
        int fieldW = Math.max(50, list.getRowWidth() - 3 * 20 - 4 * 4);
        for (ChatHighlight.Entry e : s.chatHighlights) {
            TextFieldWidget word = new TextFieldWidget(textRenderer, 0, 0, fieldW, 20, Foe.text("word"));
            word.setMaxLength(64);
            word.setText(e.word);
            word.setPlaceholder(Foe.text("word to highlight").formatted(Formatting.DARK_GRAY));
            word.setChangedListener(v -> e.word = v);
            SwatchButton swatch = new SwatchButton(0, 0, 20, 20, () -> e.color,
                    () -> client.setScreen(new ColorPickerScreen(this, "highlight colour", e.color, c -> e.color = c & 0xFFFFFF)));
            ButtonWidget on = ButtonWidget.builder(Foe.text(e.enabled ? "on" : "off"), b -> {
                e.enabled = !e.enabled;
                b.setMessage(Foe.text(e.enabled ? "on" : "off"));
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.sentence("turn this word on or off"))).build();
            ButtonWidget delete = ButtonWidget.builder(Foe.text("✕").formatted(Formatting.RED), b -> {
                s.chatHighlights.remove(e);
                rebuild();
            }).dimensions(0, 0, 20, 20).build();
            list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(word, swatch, on, delete)));
        }
        list.setScrollY(scroll);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("these words get their colour in chat and the chat boxes, whole words, any capitals")
                .withColor(Foe.DIM & 0xFFFFFF), width / 2, 23, 0xFFFFFFFF);
        if (s.chatHighlights.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("no words yet. click + word to add one.")
                    .withColor(Foe.DIM & 0xFFFFFF), width / 2, 70, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        TackleboxSettings.save();
        client.setScreen(parent);
    }
}
