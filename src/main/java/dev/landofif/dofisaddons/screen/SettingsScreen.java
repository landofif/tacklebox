package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.ui.DofiScreen;
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
public class SettingsScreen extends DofiScreen {
    private enum Tab {
        GENERAL("general"), LOOK("look"), BOBBER("bobber"), VIEWMODEL("viewmodel");

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
        super(Mono.text("dofis addons · settings"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        contentW = Math.min(360, width - 20);
        left = (width - contentW) / 2;

        int tabs = Tab.values().length;
        int tabW = (contentW - 4 * (tabs - 1)) / tabs;
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

        addDrawableChild(ButtonWidget.builder(Mono.text("done"), b -> close())
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
                header("level");
                toggle("hide vanilla xp bar",
                        "on fishonmc the xp bar shows your fishing level. this hides it and its number so the level hud can take its place. add the level hud from custom huds.",
                        () -> s.hideVanillaXp, v -> s.hideVanillaXp = v, defaults.hideVanillaXp);
                header("stats");
                slider("pause fishing clock after",
                        "the time fishing clock (and fish per hour) stops when you havent caught anything for this long, so afk time doesnt count.",
                        10, 600, 10, "s", null,
                        () -> s.pauseAfterSeconds, v -> s.pauseAfterSeconds = (int) v, defaults.pauseAfterSeconds);
            }
            case LOOK -> {
                header("system24");
                toggle("restyle all screens",
                        "draws buttons, sliders, text fields, lists, screen backgrounds and tooltips in every screen in the system24 style, not just in the mods own screens.",
                        () -> s.s24Everywhere, v -> s.s24Everywhere = v, defaults.s24Everywhere);
                toggle("restyle menus",
                        "draws chests, your inventory and fishonmc menus as flat panels with flat slots. this hides the menu art from the fishonmc resource pack.",
                        () -> s.s24Menus, v -> s.s24Menus = v, defaults.s24Menus);
                toggle("restyle hotbar", "flat boxes for the hotbar, with an accent border on the selected slot.",
                        () -> s.s24Hotbar, v -> s.s24Hotbar = v, defaults.s24Hotbar);
                header("font");
                toggle("mono font everywhere",
                        "draws all text in the game, chat and menus included, with dm mono like the rest of the mod. icons from the fishonmc pack keep their own font.",
                        () -> s.monoEverywhere, v -> s.monoEverywhere = v, defaults.monoEverywhere);
                header("title screen");
                toggle("dofis title screen",
                        "replaces the minecraft title screen with a system24 one that has a button to join fishonmc straight away.",
                        () -> s.titleScreen, v -> s.titleScreen = v, defaults.titleScreen);
            }
            case BOBBER -> {
                header("timer");
                cycle("bobber timer",
                        "shows how long your line has been in the water. changes colour as the wait gets long, and says bite! when something bites. display only.",
                        DofiSettings.BobberMode.values(), m -> m.label, () -> s.bobberTimer, v -> s.bobberTimer = v, defaults.bobberTimer);
                color("timer colour", "the colour of the timer while the wait is still short.",
                        () -> s.bobberColor, v -> s.bobberColor = v, defaults.bobberColor);
                slider("warn after", "the timer switches to the warn colour after this many seconds. 0 = never.",
                        0, 60, 1, "s", v -> v == 0 ? "never" : String.format(Locale.ROOT, "%.0fs", v),
                        () -> s.bobberWarnSeconds, v -> s.bobberWarnSeconds = v, defaults.bobberWarnSeconds);
                color("warn colour", "the colour of the timer once it passes the warn time.",
                        () -> s.bobberWarnColor, v -> s.bobberWarnColor = v, defaults.bobberWarnColor);
                slider("slow after", "the timer switches to the slow colour after this many seconds. 0 = never.",
                        0, 60, 1, "s", v -> v == 0 ? "never" : String.format(Locale.ROOT, "%.0fs", v),
                        () -> s.bobberSlowSeconds, v -> s.bobberSlowSeconds = v, defaults.bobberSlowSeconds);
                color("slow colour", "the colour of the timer once it passes the slow time.",
                        () -> s.bobberSlowColor, v -> s.bobberSlowColor = v, defaults.bobberSlowColor);
                color("bite colour", "the colour of the timer once something bites.",
                        () -> s.bobberBiteColor, v -> s.bobberBiteColor = v, defaults.bobberBiteColor);
                toggle("show bite!", "puts bite! in front of the time when something bites. the colour still changes when this is off.",
                        () -> s.bobberShowBite, v -> s.bobberShowBite = v, defaults.bobberShowBite);
                toggle("bold timer", "draws the timer in the medium face instead of the light one.",
                        () -> s.bobberBold, v -> s.bobberBold = v, defaults.bobberBold);
                slider("decimals", "how many digits after the point. 0 shows whole seconds.",
                        0, 2, 1, "", v -> String.format(Locale.ROOT, "%.0f", v),
                        () -> s.bobberDecimals, v -> s.bobberDecimals = (int) Math.round(v), defaults.bobberDecimals);
                slider("timer size", "how big the timer is. 1 = normal.", 0.5, 3, 0.05, "x", null,
                        () -> s.bobberScale, v -> s.bobberScale = v, defaults.bobberScale);
                slider("height above bobber", "how many blocks above the bobber the timer floats. above bobber mode only.",
                        0, 3, 0.05, " blocks", null, () -> s.bobberHeight, v -> s.bobberHeight = v, defaults.bobberHeight);
                slider("background", "how solid the box behind the timer above the bobber is. 0 = no box.",
                        0, 100, 1, "%", v -> String.format(Locale.ROOT, "%.0f%%", v),
                        () -> s.bobberBacking, v -> s.bobberBacking = (int) Math.round(v), defaults.bobberBacking);
                toggle("show through walls", "keeps the timer above the bobber visible behind blocks and water.",
                        () -> s.bobberThroughWalls, v -> s.bobberThroughWalls = v, defaults.bobberThroughWalls);
                slider("crosshair offset", "how far below the crosshair the timer sits. under crosshair mode only. negative is above.",
                        -60, 60, 1, "", v -> String.format(Locale.ROOT, "%.0f", v),
                        () -> s.bobberCrosshairY, v -> s.bobberCrosshairY = (int) Math.round(v), defaults.bobberCrosshairY);
                header("skin");
                cycle("bobber skin", "swaps the look of your bobber. only you see it, other players still see the normal one.",
                        DofiSettings.BobberSkin.values(), k -> k.label, () -> s.bobberSkin, v -> s.bobberSkin = v, defaults.bobberSkin);
                toggle("skin every bobber", "also uses your skin for other players bobbers, on your screen only.",
                        () -> s.skinEveryone, v -> s.skinEveryone = v, defaults.skinEveryone);
                header("line");
                toggle("custom line colour", "colours your own fishing line. other lines stay normal.",
                        () -> s.customLineColor, v -> s.customLineColor = v, defaults.customLineColor);
                color("line colour", "the colour of your fishing line. click to pick it.",
                        () -> s.lineColor, v -> s.lineColor = v, defaults.lineColor);
            }
            case VIEWMODEL -> {
                header("held item");
                toggle("custom viewmodel", "changes how big your held item is and where it sits on screen. turn off to go back to normal.",
                        () -> s.viewmodel, v -> s.viewmodel = v, defaults.viewmodel);
                slider("size", "how big the held item is. 1 = normal.", 0.1, 3, 0.05, "x", null,
                        () -> s.vmScale, v -> s.vmScale = v, defaults.vmScale);
                slider("left / right", "moves the item sideways. mirrored for your off hand.", -1, 1, 0.05, "", null,
                        () -> s.vmX, v -> s.vmX = v, defaults.vmX);
                slider("up / down", "moves the item up or down.", -1, 1, 0.05, "", null,
                        () -> s.vmY, v -> s.vmY = v, defaults.vmY);
                slider("forward / back", "moves the item closer or further away. negative is further.", -1, 1, 0.05, "", null,
                        () -> s.vmZ, v -> s.vmZ = v, defaults.vmZ);
                slider("tilt", "tilts the item forward or back, in degrees.", -180, 180, 1, "°", null,
                        () -> s.vmRotX, v -> s.vmRotX = v, defaults.vmRotX);
                slider("turn", "turns the item left or right, in degrees.", -180, 180, 1, "°", null,
                        () -> s.vmRotY, v -> s.vmRotY = v, defaults.vmRotY);
                slider("roll", "rolls the item sideways, in degrees.", -180, 180, 1, "°", null,
                        () -> s.vmRotZ, v -> s.vmRotZ = v, defaults.vmRotZ);
                toggle("no switch dip", "stops the item from dipping down when you switch slots or use it.",
                        () -> s.vmNoEquipDip, v -> s.vmNoEquipDip = v, defaults.vmNoEquipDip);
                header("swing");
                cycle("swing style",
                        "how your hand swings. smooth eases in and out, snappy moves fast at the start, no swing only turns the item around its own centre without moving it. only you see it.",
                        DofiSettings.SwingStyle.values(), w -> w.label, () -> s.swingStyle, v -> s.swingStyle = v, defaults.swingStyle);
                slider("swing length", "how long one swing takes. 1 = normal, 2 = twice as slow, 0.5 = twice as fast. only changes how it looks.",
                        0.25, 4, 0.05, "x", null, () -> s.swingSlowness, v -> s.swingSlowness = v, defaults.swingSlowness);
            }
        }
    }

    // ------------------------------------------------------------------ row builders

    private void header(String name) {
        list.addRow(new RowListWidget.Row(List.of(new Label(rowW(), Mono.bold(name).withColor(S24.ACCENT & 0xFFFFFF)))));
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
        return Mono.text(name + ": ").append(Mono.text(on ? "on" : "off").formatted(on ? Formatting.GREEN : Formatting.RED));
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
        ButtonWidget b = ButtonWidget.builder(Mono.text(name + ": " + String.format(Locale.ROOT, "#%06x", get.getAsInt())),
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
