package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.hud.CustomHud;
import dev.landofif.dofisaddons.hud.HudPresets;
import dev.landofif.dofisaddons.hud.HudStore;
import dev.landofif.dofisaddons.util.Mono;
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

/** Every custom HUD in a list: switch on/off, edit, reorder, share, delete, and move them all. */
public class HudListScreen extends Screen {
    private final Screen parent;
    private RowListWidget list;
    private String status = "";
    private long statusUntil;

    public HudListScreen(Screen parent) {
        super(Mono.text("Dofi's Addons · Custom HUDs"));
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

        int y = this.height - 28;
        int bw = Math.min(100, (listWidth - 12) / 4);
        int startX = (this.width - (bw * 4 + 12)) / 2;
        addDrawableChild(ButtonWidget.builder(Mono.text("+ New HUD"), b -> client.setScreen(new HudPresetScreen(this)))
                .dimensions(startX, y, bw, 20)
                .tooltip(Tooltip.of(Mono.text("start blank or from a ready-made hud"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("Import"), b -> onImport())
                .dimensions(startX + (bw + 4), y, bw, 20)
                .tooltip(Tooltip.of(Mono.text("add a hud from a code on your clipboard (dofi or foecollab codes)"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("Move HUDs"), b -> client.setScreen(new HudMoveScreen(this)))
                .dimensions(startX + (bw + 4) * 2, y, bw, 20)
                .tooltip(Tooltip.of(Mono.text("drag every hud into place, scroll to resize"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("Done"), b -> close())
                .dimensions(startX + (bw + 4) * 3, y, bw, 20).build());
        rebuild();
    }

    private void rebuild() {
        list.clearRows();
        List<CustomHud> huds = HudStore.all();
        int mainWidth = Math.max(60, list.getRowWidth() - (20 + 12 + 12 + 40 + 20 + 10));
        for (int i = 0; i < huds.size(); i++) {
            final int index = i;
            CustomHud h = huds.get(i);
            ButtonWidget toggle = ButtonWidget.builder(dot(h.enabled), b -> {
                        h.enabled = !h.enabled;
                        HudStore.save();
                        rebuild();
                    }).dimensions(0, 0, 20, 20)
                    .tooltip(Tooltip.of(Mono.text(h.enabled ? "shown: click to hide" : "hidden: click to show"))).build();
            String name = h.name == null || h.name.isBlank() ? "(unnamed)" : h.name;
            String label = textRenderer.trimToWidth(Mono.text(name + "  · " + h.lines.size() + " lines"), mainWidth - 12).getString();
            ButtonWidget main = ButtonWidget.builder(Mono.text(label).formatted(h.enabled ? Formatting.WHITE : Formatting.GRAY),
                            b -> client.setScreen(new HudEditScreen(this, h.copy(), index)))
                    .dimensions(0, 0, mainWidth, 20)
                    .tooltip(Tooltip.of(Mono.text("click to edit"))).build();
            ButtonWidget up = ButtonWidget.builder(Mono.text("▲"), b -> {
                HudStore.move(index, -1);
                rebuild();
            }).dimensions(0, 0, 12, 20).build();
            up.active = index > 0;
            ButtonWidget down = ButtonWidget.builder(Mono.text("▼"), b -> {
                HudStore.move(index, 1);
                rebuild();
            }).dimensions(0, 0, 12, 20).build();
            down.active = index < huds.size() - 1;
            ButtonWidget share = ButtonWidget.builder(Mono.text("Share"), b -> {
                client.keyboard.setClipboard(HudStore.exportCode(h));
                setStatus("copied \"" + name + "\" share code to clipboard", Formatting.GREEN);
            }).dimensions(0, 0, 40, 20).tooltip(Tooltip.of(Mono.text("copy a code others can import"))).build();
            ButtonWidget delete = ButtonWidget.builder(Mono.text("✕").formatted(Formatting.RED), b -> confirmDelete(h))
                    .dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Mono.text("delete"))).build();
            list.addRow(new RowListWidget.Row(new ArrayList<ClickableWidget>(List.of(toggle, main, up, down, share, delete))));
        }
    }

    private static Text dot(boolean on) {
        return Mono.text(on ? "●" : "○").formatted(on ? Formatting.GREEN : Formatting.DARK_GRAY);
    }

    private void confirmDelete(CustomHud h) {
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                HudStore.all().remove(h);
                HudStore.save();
            }
            client.setScreen(this);
        }, Mono.text("delete \"" + h.name + "\"?"), Mono.text("this cant be undone. tip: use share first to keep a copy.")));
    }

    private void onImport() {
        CustomHud h = HudStore.importCode(client.keyboard.getClipboard());
        if (h == null) {
            setStatus("no hud code on the clipboard", Formatting.RED);
            return;
        }
        HudStore.all().add(h);
        HudStore.save();
        rebuild();
        setStatus("imported \"" + h.name + "\"", Formatting.GREEN);
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
                Mono.text("your own info boxes on screen, built from stats and server info").formatted(Formatting.GRAY),
                this.width / 2, 23, 0xFFFFFFFF);
        if (HudStore.all().isEmpty()) {
            context.drawCenteredTextWithShadow(textRenderer, Mono.text("no huds yet. click \"+ new hud\".").formatted(Formatting.GRAY),
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

    /** Pick a starting point for a new HUD. */
    static class HudPresetScreen extends Screen {
        private final HudListScreen parent;

        HudPresetScreen(HudListScreen parent) {
            super(Mono.text("New HUD · pick a starting point"));
            this.parent = parent;
        }

        @Override
        protected void init() {
            int w = Math.min(320, this.width - 20);
            int x = (this.width - w) / 2;
            RowListWidget list = new RowListWidget(this.client, x, 32, w, this.height - 66, 24);
            addDrawableChild(list);
            for (HudPresets.Preset p : HudPresets.all()) {
                ButtonWidget b = ButtonWidget.builder(Mono.text(p.name()), btn -> {
                            CustomHud hud = p.hud().copy();
                            hud.enabled = true;
                            client.setScreen(new HudEditScreen(parent, hud, -1));
                        }).dimensions(0, 0, list.getRowWidth(), 20)
                        .tooltip(Tooltip.of(Mono.text(p.description()))).build();
                list.addRow(new RowListWidget.Row(new ArrayList<ClickableWidget>(List.of(b))));
            }
            addDrawableChild(ButtonWidget.builder(Mono.text("Back"), b -> close())
                    .dimensions(this.width / 2 - 50, this.height - 28, 100, 20).build());
        }

        @Override
        public void render(DrawContext context, int mouseX, int mouseY, float delta) {
            super.render(context, mouseX, mouseY, delta);
            context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 12, 0xFFFFFFFF);
        }

        @Override
        public void close() {
            client.setScreen(parent);
        }
    }
}
