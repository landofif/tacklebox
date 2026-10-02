package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.trigger.Trigger;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.trigger.TriggerStore;
import dev.landofif.dofisaddons.util.Mono;
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
public class TriggerListScreen extends Screen {
    private final Screen parent;
    private RowListWidget list;
    private String status = "";
    private long statusUntil;

    public TriggerListScreen(Screen parent) {
        super(Mono.text("Dofi's Addons · Chat Triggers"));
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
        addDrawableChild(ButtonWidget.builder(Mono.text("+ New trigger"), b -> client.setScreen(new PresetPickerScreen(this)))
                .dimensions(startX, buttonY, bw, 20)
                .tooltip(Tooltip.of(Mono.text("Start blank or from a ready-made FishOnMC preset")))
                .build());
        addDrawableChild(ButtonWidget.builder(Mono.text("Import"), b -> onImport())
                .dimensions(startX + (bw + 4), buttonY, bw, 20)
                .tooltip(Tooltip.of(Mono.text("Add a trigger from a share code on your clipboard")))
                .build());
        addDrawableChild(ButtonWidget.builder(Mono.text("Reload file"), b -> {
                    TriggerStore.load();
                    rebuild();
                    setStatus("Reloaded triggers.json", Formatting.GREEN);
                }).dimensions(startX + (bw + 4) * 2, buttonY, bw, 20)
                .tooltip(Tooltip.of(Mono.text("Re-read config/dofisaddons/triggers.json (after editing it by hand)")))
                .build());
        addDrawableChild(ButtonWidget.builder(Mono.text("Done"), b -> close())
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
                    .tooltip(Tooltip.of(Mono.text(t.enabled ? "On: click to switch off" : "Off: click to switch on")))
                    .build();

            ButtonWidget main = ButtonWidget.builder(mainLabel(t, mainWidth), b -> client.setScreen(new TriggerEditScreen(this, t.copyKeepId(), false)))
                    .dimensions(0, 0, mainWidth, 20)
                    .tooltip(Tooltip.of(Mono.text("Click to edit\nWhen chat says: " + t.pattern
                            + "\nFired this session: " + TriggerEngine.count(t))))
                    .build();

            ButtonWidget up = ButtonWidget.builder(Mono.text("▲"), b -> {
                TriggerStore.move(index, -1);
                rebuild();
            }).dimensions(0, 0, 12, 20).tooltip(Tooltip.of(Mono.text("Move up"))).build();
            up.active = index > 0;

            ButtonWidget down = ButtonWidget.builder(Mono.text("▼"), b -> {
                TriggerStore.move(index, 1);
                rebuild();
            }).dimensions(0, 0, 12, 20).tooltip(Tooltip.of(Mono.text("Move down"))).build();
            down.active = index < triggers.size() - 1;

            ButtonWidget share = ButtonWidget.builder(Mono.text("Share"), b -> {
                client.keyboard.setClipboard(TriggerStore.exportCode(t));
                setStatus("Copied \"" + t.name + "\" share code to clipboard", Formatting.GREEN);
            }).dimensions(0, 0, 40, 20).tooltip(Tooltip.of(Mono.text("Copy a code others can Import"))).build();

            ButtonWidget delete = ButtonWidget.builder(Mono.text("✕").formatted(Formatting.RED), b -> confirmDelete(t))
                    .dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Mono.text("Delete"))).build();

            List<ClickableWidget> row = new ArrayList<>(List.of(toggle, main, up, down, share, delete));
            list.addRow(new RowListWidget.Row(row));
        }
    }

    private Text toggleLabel(Trigger t) {
        return Mono.text(t.enabled ? "●" : "○").formatted(t.enabled ? Formatting.GREEN : Formatting.DARK_GRAY);
    }

    private Text mainLabel(Trigger t, int width) {
        String name = t.name == null || t.name.isBlank() ? "(unnamed)" : t.name;
        String preview = RichText.strip(t.title.enabled ? t.title.title : "");
        String label = name + (preview.isBlank() ? "" : "  → " + preview);
        label = this.textRenderer.trimToWidth(Mono.text(label), width - 12).getString();
        return Mono.text(label).formatted(t.enabled ? Formatting.WHITE : Formatting.GRAY);
    }

    private void confirmDelete(Trigger t) {
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                TriggerStore.remove(t);
            }
            client.setScreen(this);
        }, Mono.text("Delete \"" + t.name + "\"?"), Mono.text("This can't be undone. Tip: use Share first to keep a copy.")));
    }

    private void onImport() {
        Trigger imported = TriggerStore.importCode(client.keyboard.getClipboard());
        if (imported == null) {
            setStatus("No share code on the clipboard (it starts with dofi-trigger:)", Formatting.RED);
            return;
        }
        TriggerStore.put(imported);
        rebuild();
        setStatus("Imported \"" + imported.name + "\"", Formatting.GREEN);
    }

    void setStatus(String message, Formatting color) {
        this.status = "§" + color.getCode() + message;
        this.statusUntil = System.currentTimeMillis() + 3500;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 10, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer,
                Mono.text("Show your own titles and sounds when a chat message appears").formatted(Formatting.GRAY),
                this.width / 2, 23, 0xFFFFFFFF);

        if (TriggerStore.all().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer,
                    Mono.text("No triggers yet. Click \"+ New trigger\" to make one.").formatted(Formatting.GRAY),
                    this.width / 2, this.height / 2 - 10, 0xFFFFFFFF);
        }
        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            context.drawCenteredTextWithShadow(textRenderer, Mono.text(status), this.width / 2, this.height - 42, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
