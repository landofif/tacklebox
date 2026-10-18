package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.input.CommandAliases;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.Consumer;

/** Command aliases: a short command and the command it is sent as. */
public class AliasScreen extends TackleboxScreen {
    private final Screen parent;
    private final TackleboxSettings s = TackleboxSettings.get();
    private RowListWidget list;

    public AliasScreen(Screen parent) {
        super(Foe.text("tacklebox · command aliases"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(460, width - 20);
        int left = (width - w) / 2;
        list = new RowListWidget(client, left, 40, w, height - 40 - 36, 24);
        addDrawableChild(list);
        rebuild();
        int bw = (w - 4) / 2;
        int y = height - 28;
        addDrawableChild(ButtonWidget.builder(Foe.text("+ alias"), b -> {
            s.aliases.add(new CommandAliases.Entry());
            rebuild();
        }).dimensions(left, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(left + bw + 4, y, bw, 20).build());
    }

    private void rebuild() {
        double scroll = list.getScrollY();
        list.clearRows();
        int fieldW = Math.max(50, (list.getRowWidth() - 2 * 20 - 3 * 4) / 2);
        for (CommandAliases.Entry e : s.aliases) {
            TextFieldWidget name = field(fieldW, "alias", e.name, v -> e.name = v);
            TextFieldWidget command = field(fieldW, "sends this command", e.command, v -> e.command = v);
            ButtonWidget on = ButtonWidget.builder(Foe.text(e.enabled ? "on" : "off"), b -> {
                e.enabled = !e.enabled;
                b.setMessage(Foe.text(e.enabled ? "on" : "off"));
            }).dimensions(0, 0, 20, 20).build();
            ButtonWidget delete = ButtonWidget.builder(Foe.text("✕").formatted(Formatting.RED), b -> {
                s.aliases.remove(e);
                rebuild();
            }).dimensions(0, 0, 20, 20).build();
            list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(name, command, on, delete)));
        }
        list.setScrollY(scroll);
    }

    private TextFieldWidget field(int w, String hint, String value, Consumer<String> set) {
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
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("type the alias with a slash. it is sent as the command, with what you add after it")
                .withColor(Foe.DIM & 0xFFFFFF), width / 2, 23, 0xFFFFFFFF);
        if (s.aliases.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("no aliases yet. click + alias to add one.")
                    .withColor(Foe.DIM & 0xFFFFFF), width / 2, 70, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        TackleboxSettings.save();
        client.setScreen(parent);
    }
}
