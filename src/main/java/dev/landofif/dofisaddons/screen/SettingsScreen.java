package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * General settings in three tabs. Every change applies right away (so viewmodel tweaks preview live) and
 * is saved when the screen closes. Each row has a ↺ button that puts that one setting back to default.
 */
public class SettingsScreen extends Screen {
    private enum Tab {
        GENERAL("General"), BOBBER("Bobber"), VIEWMODEL("Viewmodel");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private static final int ROW_H = 24;
    private static final int RESET_W = 20;

    private final Screen parent;
    private final DofiSettings s = StatsTracker.settings();
    private final DofiSettings defaults = new DofiSettings();
    private Tab tab = Tab.GENERAL;
    private RowListWidget list;
    private int left;
    private int contentW;

    public SettingsScreen(Screen parent) {
        super(Mono.text("Dofi's Addons · Settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        contentW = Math.min(360, width - 20);
        left = (width - contentW) / 2;

        int tabW = (contentW - 8) / 3;
        int i = 0;
        for (Tab t : Tab.values()) {
            ButtonWidget b = ButtonWidget.builder(Mono.text(t.label), btn -> {
                tab = t;
                clearAndInit();
            }).dimensions(left + i * (tabW + 4), 22, tabW, 18).build();
            b.active = t != tab;
            addDrawableChild(b);
            i++;
        }

        list = new RowListWidget(client, left, 46, contentW, height - 46 - 34, ROW_H);
        addDrawableChild(list);
        fillRows();

        addDrawableChild(ButtonWidget.builder(Mono.text("Done"), b -> close())
                .dimensions(width / 2 - 75, height - 26, 150, 20).build());
    }

    /** Rebuilds the rows (after a reset) without losing the scroll position. */
    private void refresh() {
        double scroll = list.getScrollY();
        list.clearRows();
        fillRows();
        list.setScrollY(scroll);
    }

    private int rowW() {
        return list.getRowWidth();
    }

    private void fillRows() {
        switch (tab) {
            case GENERAL -> {
                header("Level");
                toggle("Hide vanilla XP bar",
                        "on fishonmc the xp bar shows your fishing level. this hides it and its number so the level hud can take its place. add the level hud from custom huds.",
                        () -> s.hideVanillaXp, v -> s.hideVanillaXp = v, defaults.hideVanillaXp);
                header("Bobber timer");
                cycle("Bobber timer",
                        "shows how long your line has been in the water. turns yellow, then red, as it gets long, and says bite! when something bites. display only.",
                        DofiSettings.BobberMode.values(), m -> m.label, () -> s.bobberTimer, v -> s.bobberTimer = v, defaults.bobberTimer);
                slider("Turn red after", "the timer turns red after this many seconds. 0 = always white.",
                        0, 60, 1, "s", v -> v == 0 ? "never" : String.format(Locale.ROOT, "%.0fs", v),
                        () -> s.bobberSlowSeconds, v -> s.bobberSlowSeconds = v, defaults.bobberSlowSeconds);
                header("Stats");
                slider("Pause fishing clock after",
                        "the time fishing clock (and fish per hour) stops when you havent caught anything for this long, so afk time doesnt count.",
                        10, 600, 10, "s", null,
                        () -> s.pauseAfterSeconds, v -> s.pauseAfterSeconds = (int) v, defaults.pauseAfterSeconds);
            }
            case BOBBER -> {
                header("Skin");
                cycle("Bobber skin", "swaps the look of your bobber. only you see it, other players still see the normal one.",
                        DofiSettings.BobberSkin.values(), k -> k.label, () -> s.bobberSkin, v -> s.bobberSkin = v, defaults.bobberSkin);
                toggle("Skin every bobber", "also uses your skin for other players bobbers, on your screen only.",
                        () -> s.skinEveryone, v -> s.skinEveryone = v, defaults.skinEveryone);
                header("Line");
                toggle("Custom line colour", "colours your own fishing line. other lines stay normal.",
                        () -> s.customLineColor, v -> s.customLineColor = v, defaults.customLineColor);
                color("Line colour", "the colour of your fishing line. click to pick it.",
                        () -> s.lineColor, v -> s.lineColor = v, defaults.lineColor);
            }
            case VIEWMODEL -> {
                header("Held item");
                toggle("Custom viewmodel", "changes how big your held item is and where it sits on screen. turn off to go back to normal.",
                        () -> s.viewmodel, v -> s.viewmodel = v, defaults.viewmodel);
                slider("Size", "how big the held item is. 1 = normal.", 0.1, 3, 0.05, "x", null,
                        () -> s.vmScale, v -> s.vmScale = v, defaults.vmScale);
                slider("Left / right", "moves the item sideways. mirrored for your off hand.", -1, 1, 0.05, "", null,
                        () -> s.vmX, v -> s.vmX = v, defaults.vmX);
                slider("Up / down", "moves the item up or down.", -1, 1, 0.05, "", null,
                        () -> s.vmY, v -> s.vmY = v, defaults.vmY);
                slider("Forward / back", "moves the item closer or further away. negative is further.", -1, 1, 0.05, "", null,
                        () -> s.vmZ, v -> s.vmZ = v, defaults.vmZ);
                slider("Tilt", "tilts the item forward or back, in degrees.", -180, 180, 1, "°", null,
                        () -> s.vmRotX, v -> s.vmRotX = v, defaults.vmRotX);
                slider("Turn", "turns the item left or right, in degrees.", -180, 180, 1, "°", null,
                        () -> s.vmRotY, v -> s.vmRotY = v, defaults.vmRotY);
                slider("Roll", "rolls the item sideways, in degrees.", -180, 180, 1, "°", null,
                        () -> s.vmRotZ, v -> s.vmRotZ = v, defaults.vmRotZ);
                toggle("No switch dip", "stops the item from dipping down when you switch slots or use it.",
                        () -> s.vmNoEquipDip, v -> s.vmNoEquipDip = v, defaults.vmNoEquipDip);
                header("Swing");
                cycle("Swing style",
                        "how your hand swings. smooth eases in and out, snappy moves fast at the start, no swing keeps the item still. only you see it.",
                        DofiSettings.SwingStyle.values(), w -> w.label, () -> s.swingStyle, v -> s.swingStyle = v, defaults.swingStyle);
                slider("Swing length", "how long one swing takes. 1 = normal, 2 = twice as slow, 0.5 = twice as fast. only changes how it looks.",
                        0.25, 4, 0.05, "x", null, () -> s.swingSlowness, v -> s.swingSlowness = v, defaults.swingSlowness);
            }
        }
    }

    // ------------------------------------------------------------------ row builders

    private void header(String name) {
        list.addRow(new RowListWidget.Row(List.of(new Label(rowW(), Mono.bold(name).formatted(Formatting.AQUA)))));
    }

    private void row(ClickableWidget control, String desc, Runnable reset) {
        control.setTooltip(Tooltip.of(Mono.text(desc)));
        ButtonWidget r = ButtonWidget.builder(Mono.text("↺"), b -> {
            reset.run();
            refresh();
        }).dimensions(0, 0, RESET_W, 20).tooltip(Tooltip.of(Mono.text("back to default"))).build();
        List<ClickableWidget> widgets = new ArrayList<>();
        widgets.add(control);
        widgets.add(r);
        list.addRow(new RowListWidget.Row(widgets));
    }

    private int controlW() {
        return rowW() - RESET_W - 2;
    }

    private void toggle(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set, boolean def) {
        ButtonWidget b = ButtonWidget.builder(onOff(name, get.get()), btn -> {
            set.accept(!get.get());
            btn.setMessage(onOff(name, get.get()));
        }).dimensions(0, 0, controlW(), 20).build();
        row(b, desc, () -> set.accept(def));
    }

    private static Text onOff(String name, boolean on) {
        return Mono.text(name + ": ").append(Mono.text(on ? "ON" : "OFF").formatted(on ? Formatting.GREEN : Formatting.RED));
    }

    private <E extends Enum<E>> void cycle(String name, String desc, E[] values, Function<E, String> label,
                                           Supplier<E> get, Consumer<E> set, E def) {
        ButtonWidget b = ButtonWidget.builder(Mono.text(name + ": " + label.apply(get.get())), btn -> {
            set.accept(values[(get.get().ordinal() + 1) % values.length]);
            btn.setMessage(Mono.text(name + ": " + label.apply(get.get())));
        }).dimensions(0, 0, controlW(), 20).build();
        row(b, desc + " (click to change)", () -> set.accept(def));
    }

    private void slider(String name, String desc, double min, double max, double step, String unit,
                        DoubleFunction<String> format, DoubleSupplier get, DoubleConsumer set, double def) {
        ValueSlider slider = new ValueSlider(0, 0, controlW(), 20, name, min, max, step, unit, get.getAsDouble(), set);
        if (format != null) {
            slider.format(format);
        }
        row(slider, desc, () -> set.accept(def));
    }

    private void color(String name, String desc, IntSupplier get, IntConsumer set, int def) {
        Runnable open = () -> client.setScreen(new ColorPickerScreen(this, name, get.getAsInt(), set));
        ButtonWidget b = ButtonWidget.builder(Mono.text(name + ": " + String.format(Locale.ROOT, "#%06X", get.getAsInt())),
                btn -> open.run()).dimensions(0, 0, controlW() - 22, 20).build();
        SwatchButton swatch = new SwatchButton(0, 0, 20, 20, get, open);
        swatch.setTooltip(Tooltip.of(Mono.text("click to pick a colour")));
        b.setTooltip(Tooltip.of(Mono.text(desc)));
        ButtonWidget r = ButtonWidget.builder(Mono.text("↺"), btn -> {
            set.accept(def);
            refresh();
        }).dimensions(0, 0, RESET_W, 20).tooltip(Tooltip.of(Mono.text("back to default"))).build();
        list.addRow(new RowListWidget.Row(List.of(b, swatch, r)));
    }

    // ------------------------------------------------------------------ screen

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, this.title, this.width / 2, 8, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        StatsTracker.saveSettings();
        client.setScreen(parent);
    }

    /** A line of text in the list (section headers). */
    private static final class Label extends ClickableWidget {
        Label(int width, Text text) {
            super(0, 0, width, 20, text);
            this.active = false;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            ctx.drawTextWithShadow(tr, getMessage(), getX() + 2, getY() + 8, 0xFFFFFFFF);
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
}
