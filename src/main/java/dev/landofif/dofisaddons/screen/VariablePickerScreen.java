package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.hud.HudEngine;
import dev.landofif.dofisaddons.hud.HudSources;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/** Browse every value a HUD line can show, with what it is right now. Click one to add it. */
public class VariablePickerScreen extends DofiScreen {
    private final Screen parent;
    private final Consumer<String> onPick;
    private RowListWidget list;
    private String query = "";

    public VariablePickerScreen(Screen parent, Consumer<String> onPick) {
        super(Mono.text("insert a value"));
        this.parent = parent;
        this.onPick = onPick;
    }

    @Override
    protected void init() {
        int w = Math.min(440, this.width - 20);
        int x = (this.width - w) / 2;
        TextFieldWidget search = new TextFieldWidget(textRenderer, x, 22, w, 18, Text.empty());
        search.setPlaceholder(Mono.text("search: fish, mythical, wallet, since...").formatted(Formatting.DARK_GRAY));
        search.setText(query);
        search.setChangedListener(q -> {
            query = q;
            rebuild();
        });
        addDrawableChild(search);
        setInitialFocus(search);

        list = new RowListWidget(this.client, x, 46, w, this.height - 80, 22);
        addDrawableChild(list);
        addDrawableChild(ButtonWidget.builder(Mono.text("back"), b -> close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20).build());
        rebuild();
    }

    private void rebuild() {
        list.clearRows();
        String q = query.trim().toLowerCase(Locale.ROOT);
        int rowW = list.getRowWidth();
        String lastCategory = null;
        for (HudSources.Entry e : HudSources.CATALOG) {
            String hay = (e.category() + " " + e.description() + " " + e.token()).toLowerCase(Locale.ROOT);
            if (!q.isEmpty() && !hay.contains(q)) {
                continue;
            }
            if (!e.category().equals(lastCategory)) {
                lastCategory = e.category();
                ButtonWidget header = ButtonWidget.builder(Mono.text("— " + e.category() + " —").withColor(S24.ACCENT & 0xFFFFFF), b -> {})
                        .dimensions(0, 0, rowW, 20).build();
                header.active = false;
                list.addRow(new RowListWidget.Row(new ArrayList<ClickableWidget>(List.of(header))));
            }
            HudEngine.ParseResult now = HudEngine.parse(e.token());
            String value = now.complete() ? now.text().getString() : "not available now";
            if (value.length() > 18) value = value.substring(0, 17) + "…";
            String label = e.description() + "  = " + value;
            label = textRenderer.trimToWidth(Mono.text(label), rowW - 10).getString();
            ButtonWidget b = ButtonWidget.builder(Mono.text(label), btn -> {
                        onPick.accept(e.token());
                        client.setScreen(parent);
                    }).dimensions(0, 0, rowW, 20)
                    .tooltip(Tooltip.of(Mono.text("adds " + e.token()))).build();
            list.addRow(new RowListWidget.Row(new ArrayList<ClickableWidget>(List.of(b))));
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
