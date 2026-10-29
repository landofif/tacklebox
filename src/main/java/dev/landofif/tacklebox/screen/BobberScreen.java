package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.compat.FoeScreens;
import dev.landofif.tacklebox.display.BobberTimer;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import dev.landofif.tacklebox.ui.Foe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/** The bobber timer and bite notification settings. The world stays clear: both show live while this is open. */
public class BobberScreen extends TackleboxScreen {
    private final Screen parent;
    private final TackleboxSettings s = TackleboxSettings.get();

    public BobberScreen(Screen parent) {
        super(Foe.text("tacklebox · bobber timer"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        BobberTimer.preview = true;
        int w = Math.min(220, width / 2 - 10);
        RowListWidget list = new RowListWidget(client, 10, 23, w, height - 23 - 60, 24);
        addDrawableChild(list);
        int rw = list.getRowWidth();

        header(list, rw, "timer");
        TackleboxSettings.TimerMode[] modes = TackleboxSettings.TimerMode.values();
        cycle(list, rw, "show", modes, m -> m.label, () -> s.timerMode, v -> s.timerMode = v);
        cycle(list, rw, "style", TackleboxSettings.TimerStyle.values(), m -> m.label, () -> s.timerStyle, v -> s.timerStyle = v);
        islider(list, rw, "hud size", 50, 500, 5, "%", () -> s.timerSize, v -> s.timerSize = v);
        islider(list, rw, "bobber size", 25, 400, 5, "%", () -> s.timerWorldSize, v -> s.timerWorldSize = v);
        slider(list, rw, "blocks above bobber", 0, 3, 0.05, "", () -> s.timerHeight, v -> s.timerHeight = v);
        islider(list, rw, "decimals", 0, 2, 1, "", () -> s.timerDecimals, v -> s.timerDecimals = v);
        toggle(list, rw, "unit letter", () -> s.timerSuffix, v -> s.timerSuffix = v);
        toggle(list, rw, "text shadow", () -> s.timerShadow, v -> s.timerShadow = v);
        color(list, rw, "colour", "timer colour", () -> s.timerColor, v -> s.timerColor = v);

        header(list, rw, "bite notification");
        toggle(list, rw, "bite notification", () -> s.biteOn, v -> s.biteOn = v);
        TextFieldWidget text = new TextFieldWidget(textRenderer, 0, 0, rw, 20, Foe.text("text"));
        text.setMaxLength(32);
        text.setText(s.biteText);
        text.setChangedListener(v -> s.biteText = v);
        list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(text)));
        TackleboxSettings.BiteAnim[] anims = TackleboxSettings.BiteAnim.values();
        cycle(list, rw, "animation", anims, a -> a.label, () -> s.biteAnim, v -> s.biteAnim = v);
        slider(list, rw, "stays", 0.5, 5, 0.1, "s", () -> s.biteSeconds, v -> s.biteSeconds = v);
        islider(list, rw, "size", 50, 600, 10, "%", () -> s.biteSize, v -> s.biteSize = v);
        toggle(list, rw, "text shadow", () -> s.biteShadow, v -> s.biteShadow = v);
        color(list, rw, "colour", "bite colour", () -> s.biteColor, v -> s.biteColor = v);
        toggle(list, rw, "hide server title", () -> s.biteHideTitle, v -> s.biteHideTitle = v);
        toggle(list, rw, "focus window on bite", () -> s.biteFocus, v -> s.biteFocus = v);

        int half = (w - 4) / 2;
        addDrawableChild(ButtonWidget.builder(Foe.text("test bite"), b -> BobberTimer.testBite()).dimensions(10, height - 52, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("move huds"), b -> FoeScreens.openMover(this)).dimensions(14 + half, height - 52, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(10, height - 28, w, 20).build());
    }

    private void header(RowListWidget list, int rw, String name) {
        ButtonWidget b = ButtonWidget.builder(Foe.text(name).formatted(Formatting.BOLD), x -> {}).dimensions(0, 0, rw, 20).build();
        b.active = false;
        list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(b)));
    }

    private static Text onOff(String name, boolean on) {
        return Foe.text(name + ": ").append(Foe.text(on ? "on" : "off").formatted(on ? Formatting.GREEN : Formatting.RED));
    }

    private void toggle(RowListWidget list, int rw, String name, BooleanSupplier get, Consumer<Boolean> set) {
        list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(ButtonWidget.builder(onOff(name, get.getAsBoolean()), b -> {
            set.accept(!get.getAsBoolean());
            b.setMessage(onOff(name, get.getAsBoolean()));
        }).dimensions(0, 0, rw, 20).build())));
    }

    private <T> void cycle(RowListWidget list, int rw, String name, T[] values, java.util.function.Function<T, String> label,
                           Supplier<T> get, Consumer<T> set) {
        list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(ButtonWidget.builder(Foe.text(name + ": " + label.apply(get.get())), b -> {
            set.accept(values[(java.util.Arrays.asList(values).indexOf(get.get()) + 1) % values.length]);
            b.setMessage(Foe.text(name + ": " + label.apply(get.get())));
        }).dimensions(0, 0, rw, 20).build())));
    }

    private void slider(RowListWidget list, int rw, String name, double min, double max, double step, String unit,
                        DoubleSupplier get, DoubleConsumer set) {
        list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(new ValueSlider(0, 0, rw, 20, name, min, max, step, unit, get.getAsDouble(), set))));
    }

    private void islider(RowListWidget list, int rw, String name, double min, double max, double step, String unit,
                         IntSupplier get, IntConsumer set) {
        slider(list, rw, name, min, max, step, unit, () -> get.getAsInt(), v -> set.accept((int) Math.round(v)));
    }

    private void color(RowListWidget list, int rw, String name, String pickerTitle, IntSupplier get, IntConsumer set) {
        ButtonWidget open = ButtonWidget.builder(Foe.text(name), b -> openPicker(pickerTitle, get, set)).dimensions(0, 0, rw - 24, 20).build();
        SwatchButton swatch = new SwatchButton(0, 0, 20, 20, get, () -> openPicker(pickerTitle, get, set));
        list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(open, swatch)));
    }

    private void openPicker(String title, IntSupplier get, IntConsumer set) {
        client.setScreen(new ColorPickerScreen(this, title, get.getAsInt(), c -> set.accept(c & 0xFFFFFF)));
    }

    /** No dimming: the timer and the bite have to stay visible while you change them. */
    @Override
    public void renderBackground(DrawContext ctx, int mouseX, int mouseY, float delta) {
        Foe.title(ctx, 4, 4, title);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    public void removed() {
        BobberTimer.preview = false;
    }

    @Override
    public void close() {
        TackleboxSettings.save();
        client.setScreen(parent);
    }
}
