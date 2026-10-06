package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.trigger.Trigger;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.trigger.TriggerStore;
import dev.landofif.dofisaddons.util.RichText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

/**
 * Home screen: every trigger in a list. Click a trigger to edit it, the dot to switch it on/off,
 * arrows to reorder, "Share" to copy a code, and the bin to delete it.
 */
public class TriggerListScreen extends DofiScreen {
    private final Screen parent;
    private RowListWidget list;
    private String status = "";
    private long statusUntil;

    public TriggerListScreen(Screen parent) {
        super(Foe.text("dofis addons · chat triggers"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listWidth = Math.min(420, this.width - 20);
        int listX = (this.width - listWidth) / 2;
        int top = 40;
        int bottom = this.height - 34;
        list = new RowListWidget(this.client, listX, top, listWidth, bottom - top, 24);
        addDrawableChild(list);

        int buttonY = this.height - 28;
        int bw = Math.min(100, (listWidth - 12) / 4);
        int startX = (this.width - (bw * 4 + 12)) / 2;
        addDrawableChild(ButtonWidget.builder(Foe.text("+ new trigger"), b -> client.setScreen(new PresetPickerScreen(this)))
                .dimensions(startX, buttonY, bw, 20)
                .tooltip(Tooltip.of(Foe.text("start blank or from a ready-made fishonmc preset")))
                .build());
        addDrawableChild(ButtonWidget.builder(Foe.text("import"), b -> onImport())
                .dimensions(startX + (bw + 4), buttonY, bw, 20)
                .tooltip(Tooltip.of(Foe.text("add a trigger from a share code on your clipboard")))
                .build());
        addDrawableChild(ButtonWidget.builder(Foe.text("reload file"), b -> {
                    TriggerStore.load();
                    rebuild();
                    setStatus("reloaded triggers.json", Formatting.GREEN);
                }).dimensions(startX + (bw + 4) * 2, buttonY, bw, 20)
                .tooltip(Tooltip.of(Foe.text("re-read config/dofisaddons/triggers.json (after editing it by hand)")))
                .build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close())
                .dimensions(startX + (bw + 4) * 3, buttonY, bw, 20).build());

        rebuild();
    }

    private void rebuild() {
        list.clearRows();
        List<Trigger> triggers = TriggerStore.all();
        int rowWidth = list.getRowWidth();
        int fixed = 20 + 12 + 12 + 40 + 20 + 2 * 5;
        int mainWidth = Math.max(60, rowWidth - fixed);

        for (int i = 0; i < triggers.size(); i++) {
            final int index = i;
            Trigger t = triggers.get(i);

            ButtonWidget toggle = ButtonWidget.builder(toggleLabel(t), b -> {
                        t.enabled = !t.enabled;
                        TriggerStore.save();
                        b.setMessage(toggleLabel(t));
                        rebuild();
                    }).dimensions(0, 0, 20, 20)
                    .tooltip(Tooltip.of(Foe.text(t.enabled ? "on: click to switch off" : "off: click to switch on")))
                    .build();

            ButtonWidget main = ButtonWidget.builder(mainLabel(t, mainWidth), b -> client.setScreen(new TriggerEditScreen(this, t.copyKeepId(), false)))
                    .dimensions(0, 0, mainWidth, 20)
                    .tooltip(Tooltip.of(Foe.text("click to edit\nwhen chat says: " + t.pattern
                            + "\nfired this session: " + TriggerEngine.count(t))))
                    .build();

            ButtonWidget up = ButtonWidget.builder(Foe.text("▲"), b -> {
                TriggerStore.move(index, -1);
                rebuild();
            }).dimensions(0, 0, 12, 20).tooltip(Tooltip.of(Foe.text("move up"))).build();
            up.active = index > 0;

            ButtonWidget down = ButtonWidget.builder(Foe.text("▼"), b -> {
                TriggerStore.move(index, 1);
                rebuild();
            }).dimensions(0, 0, 12, 20).tooltip(Tooltip.of(Foe.text("move down"))).build();
            down.active = index < triggers.size() - 1;

            ButtonWidget share = ButtonWidget.builder(Foe.text("share"), b -> {
                client.keyboard.setClipboard(TriggerStore.exportCode(t));
                setStatus("copied \"" + t.name + "\" share code to clipboard", Formatting.GREEN);
            }).dimensions(0, 0, 40, 20).tooltip(Tooltip.of(Foe.text("copy a code others can import"))).build();

            ButtonWidget delete = ButtonWidget.builder(Foe.text("✕").formatted(Formatting.RED), b -> confirmDelete(t))
                    .dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.text("delete"))).build();

            List<ClickableWidget> row = new ArrayList<>(List.of(toggle, main, up, down, share, delete));
            list.addRow(new RowListWidget.Row(row));
        }
    }

    private Text toggleLabel(Trigger t) {
        return Foe.text(t.enabled ? "●" : "○").formatted(t.enabled ? Formatting.GREEN : Formatting.DARK_GRAY);
    }

    private Text mainLabel(Trigger t, int width) {
        String name = t.name == null || t.name.isBlank() ? "(unnamed)" : t.name;
        String preview = RichText.strip(t.title.enabled ? t.title.title : "");
        String label = name + (preview.isBlank() ? "" : "  → " + preview);
        label = this.textRenderer.trimToWidth(Foe.text(label), width - 12).getString();
        return Foe.text(label).formatted(t.enabled ? Formatting.WHITE : Formatting.GRAY);
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
            setStatus("no share code on the clipboard (it starts with dofi-trigger:)", Formatting.RED);
            return;
        }
        TriggerStore.put(imported);
        rebuild();
        setStatus("imported \"" + imported.name + "\"", Formatting.GREEN);
    }

    void setStatus(String message, Formatting color) {
        this.status = "§" + color.getCode() + message;
        this.statusUntil = System.currentTimeMillis() + 3500;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
                Foe.text("show your own titles and sounds when a chat message appears").formatted(Formatting.GRAY),
                this.width / 2, 23, 0xFFFFFFFF);

        if (TriggerStore.all().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Foe.text("no triggers yet. click \"+ new trigger\" to make one.").formatted(Formatting.GRAY),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
        }
        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            context.drawCenteredTextWithShadow(textRenderer, Foe.text(status), this.width / 2, this.height - 42, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
