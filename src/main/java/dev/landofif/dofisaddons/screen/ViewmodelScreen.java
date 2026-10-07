package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;

/** The held item settings, applied the moment you change them. The world stays clear so you can see the item. */
public class ViewmodelScreen extends DofiScreen {
    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();

    public ViewmodelScreen(Screen parent) {
        super(Foe.text("dofis addons · viewmodel"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(200, width / 2 - 10);
        RowListWidget list = new RowListWidget(client, 10, 23, w, height - 23 - 36, 24);
        addDrawableChild(list);
        row(list, onOff("custom viewmodel", s.viewmodel), b -> {
            s.viewmodel = !s.viewmodel;
            b.setMessage(onOff("custom viewmodel", s.viewmodel));
        });
        slider(list, w, "size", 0.1, 3, 0.01, "x", () -> s.vmScale, v -> s.vmScale = v);
        slider(list, w, "left / right", -1, 1, 0.01, "", () -> s.vmX, v -> s.vmX = v);
        slider(list, w, "up / down", -1, 1, 0.01, "", () -> s.vmY, v -> s.vmY = v);
        slider(list, w, "forward / back", -1, 1, 0.01, "", () -> s.vmZ, v -> s.vmZ = v);
        slider(list, w, "tilt", -180, 180, 1, "°", () -> s.vmRotX, v -> s.vmRotX = v);
        slider(list, w, "turn", -180, 180, 1, "°", () -> s.vmRotY, v -> s.vmRotY = v);
        slider(list, w, "roll", -180, 180, 1, "°", () -> s.vmRotZ, v -> s.vmRotZ = v);
        row(list, onOff("no switch dip", s.vmNoEquipDip), b -> {
            s.vmNoEquipDip = !s.vmNoEquipDip;
            b.setMessage(onOff("no switch dip", s.vmNoEquipDip));
        });
        row(list, Foe.text("swing style: " + s.swingStyle.label), b -> {
            s.swingStyle = DofiSettings.SwingStyle.values()[(s.swingStyle.ordinal() + 1) % DofiSettings.SwingStyle.values().length];
            b.setMessage(Foe.text("swing style: " + s.swingStyle.label));
        });
        slider(list, w, "swing length", 0.25, 4, 0.01, "x", () -> s.swingSlowness, v -> s.swingSlowness = v);
        slider(list, w, "swing turn", 0, 100, 1, "%", () -> s.swingTurn, v -> s.swingTurn = v);
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(10, height - 28, w, 20).build());
    }

    private static Text onOff(String name, boolean on) {
        return Foe.text(name + ": ").append(Foe.text(on ? "on" : "off").formatted(on ? Formatting.GREEN : Formatting.RED));
    }

    private void row(RowListWidget list, Text label, ButtonWidget.PressAction action) {
        list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(
                ButtonWidget.builder(label, action).dimensions(0, 0, list.getRowWidth(), 20).build())));
    }

    private void slider(RowListWidget list, int w, String name, double min, double max, double step, String unit,
                        DoubleSupplier get, DoubleConsumer set) {
        list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(
                new ValueSlider(0, 0, list.getRowWidth(), 20, name, min, max, step, unit, get.getAsDouble(), set))));
    }

    /** No dimming or blur: the item has to stay visible while you move it. */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Foe.title(ctx, 4, 4, title);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void close() {
        DofiSettings.save();
        client.setScreen(parent);
    }
}
