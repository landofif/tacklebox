package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.compat.FoeConfig;
import dev.landofif.dofisaddons.compat.FoeScreens;
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
import net.minecraft.client.gui.widget.TextFieldWidget;
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
        GENERAL("general"), LOOK("look"), BOBBER("bobber"), WORLD("world"), VIEWMODEL("viewmodel"), FOECOLLAB("foecollab");

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
    private TextFieldWidget search;
    private String query = "";
    private RowListWidget list;
    private int left;
    private int contentW;

    public SettingsScreen(Screen parent) {
        super(Mono.text("dofis addons · settings"));
        this.parent = parent;
    }

    /** Opened on the foecollab tab: stands in for foecollab's own cloth config screen. */
    public static SettingsScreen foecollab(Screen parent) {
        SettingsScreen screen = new SettingsScreen(parent);
        screen.tab = Tab.FOECOLLAB;
        return screen;
    }

    @Override
    protected void init() {
        contentW = Math.min(400, width - 20);
        left = (width - contentW) / 2;

        List<Tab> shown = new ArrayList<>(List.of(Tab.values()));
        if (!FoeConfig.available()) {
            shown.remove(Tab.FOECOLLAB);
        }
        int tabs = shown.size();
        int tabW = (contentW - 4 * (tabs - 1)) / tabs;
        int i = 0;
        for (Tab t : shown) {
            ButtonWidget b = ButtonWidget.builder(Mono.text(t.label), btn -> {
                tab = t;
                clearAndInit();
            }).dimensions(left + i * (tabW + 4), 22, tabW, 18).build();
            b.active = t != tab;
            addDrawableChild(b);
            i++;
        }

        int listTop = tab == Tab.FOECOLLAB ? 70 : 46;
        list = new RowListWidget(client, left, listTop, contentW, height - listTop - 34, ROW_H);
        addDrawableChild(list);
        if (tab == Tab.FOECOLLAB) {
            // a search box above the list, lined up with its rows: foecollab has a few hundred options
            int x = list.getRowLeft();
            int w = list.getRowWidth();
            int bw = 92;
            search = new TextFieldWidget(textRenderer, x, 46, w - 2 * (bw + 4), 18, Mono.text("search"));
            search.setMaxLength(64);
            search.setText(query);
            search.setPlaceholder(Mono.text("search foecollab settings").formatted(Formatting.DARK_GRAY));
            search.setChangedListener(v -> {
                query = v;
                refresh();
                list.setScrollY(0);
            });
            addDrawableChild(search);
            addDrawableChild(ButtonWidget.builder(Mono.text("foecollab menu"), b -> open(FoeScreens.main(this)))
                    .dimensions(x + w - 2 * bw - 4, 46, bw, 18)
                    .tooltip(Tooltip.of(Mono.text("foecollabs own menu: custom huds, custom buttons, pet calculator and more"))).build());
            addDrawableChild(ButtonWidget.builder(Mono.text("move huds"), b -> open(FoeScreens.move(this)))
                    .dimensions(x + w - bw, 46, bw, 18)
                    .tooltip(Tooltip.of(Mono.text("drag foecollabs huds into place"))).build());
        }
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
                cycle("flavor", "the colour set: system24, caelestia, catppuccin, nord, tokyo night, rose pine, everforest or vencord. changes the mod, the restyled screens and the huds.",
                        dev.landofif.dofisaddons.ui.Flavor.values(), f -> f.label, () -> s.flavor, v -> {
                            s.flavor = v;
                            S24.apply(v);
                            refresh(); // headers and swatches pick up the new colours
                        }, defaults.flavor);
                cycle("style", "system24: square panels with thin borders and labels in the top edge. caelestia: rounded borderless boxes like caelestia shell, best with the caelestia flavor.",
                        DofiSettings.Style.values(), v -> v.label, () -> s.style, v -> {
                            s.style = v;
                            refresh();
                        }, defaults.style);
                toggle("restyle all screens",
                        "draws buttons, sliders, text fields, lists, screen backgrounds and tooltips in every screen in the system24 style, not just in the mods own screens.",
                        () -> s.s24Everywhere, v -> s.s24Everywhere = v, defaults.s24Everywhere);
                toggle("restyle menus",
                        "draws chests, your inventory and fishonmc menus as flat panels with flat slots. this hides the menu art from the fishonmc resource pack.",
                        () -> s.s24Menus, v -> s.s24Menus = v, defaults.s24Menus);
                toggle("restyle hotbar", "flat boxes for the hotbar, with an accent border on the selected slot.",
                        () -> s.s24Hotbar, v -> s.s24Hotbar = v, defaults.s24Hotbar);
                toggle("restyle foecollab huds",
                        "draws foecollabs hud boxes as system24 panels in your flavor. pick any clean theme in foecollab to get the titles set into the top border.",
                        () -> s.foeHuds, v -> s.foeHuds = v, defaults.foeHuds);
                header("font");
                toggle("mono font everywhere",
                        "draws all text in the game, chat and menus included, with dm mono like the rest of the mod. icons from the fishonmc pack keep their own font.",
                        () -> s.monoEverywhere, v -> s.monoEverywhere = v, defaults.monoEverywhere);
                header("title screen");
                cycle("title screen",
                        "which title screen shows when the game starts: the system24 one from dofis addons with a join fishonmc button, foecollabs, or the minecraft one.",
                        titleChoices(), c -> c.label, () -> s.titleChoice, v -> s.titleChoice = v, DofiSettings.TitleChoice.DOFIS);
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
                header("bite");
                toggle("bite message", "shows a short message in the middle of the screen when your own fish bites.",
                        () -> s.biteMessage, v -> s.biteMessage = v, defaults.biteMessage);
                text("message text", "what the bite message says.", () -> s.biteMessageText, v -> s.biteMessageText = v, defaults.biteMessageText);
                color("message colour", "the colour of the bite message.",
                        () -> s.biteMessageColor, v -> s.biteMessageColor = v, defaults.biteMessageColor);
                header("bobber");
                toggle("still bobber", "holds your bobber still on the water instead of bobbing up and down. it still dips when a fish bites.",
                        () -> s.stillBobber, v -> s.stillBobber = v, defaults.stillBobber);
                toggle("smooth cast", "on a laggy connection your cast can jump back and fly the path again. this shows your own cast flying smoothly. where it lands stays exact.",
                        () -> s.smoothCast, v -> s.smoothCast = v, defaults.smoothCast);
                toggle("hide other bobbers", "hides other players bobbers and their lines, on your screen only.",
                        () -> s.hideOtherBobbers, v -> s.hideOtherBobbers = v, defaults.hideOtherBobbers);
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
            case WORLD -> {
                header("nametags");
                toggle("hide my nametag", "hides the rank, name and power tag the server puts over your own head. other players still see it.",
                        () -> s.hideOwnNametag, v -> s.hideOwnNametag = v, defaults.hideOwnNametag);
                toggle("hide all nametags", "hides every players nametag on your screen, the servers and the vanilla one.",
                        () -> s.hideAllNametags, v -> s.hideAllNametags = v, defaults.hideAllNametags);
                header("clutter");
                toggle("hide xp popups", "hides the +66xp line above the hotbar after each catch.",
                        () -> s.hideXpPopups, v -> s.hideXpPopups = v, defaults.hideXpPopups);
                toggle("flat leaves", "draws leaves flat like vanilla instead of the 3d leaves from the fishonmc pack. reloads resources when you change it.",
                        () -> s.flatLeaves, v -> {
                            if (s.flatLeaves != v) {
                                s.flatLeaves = v;
                                client.reloadResources();
                            }
                        }, defaults.flatLeaves);
                header("tooltips");
                toggle("short fish tooltips", "leaves out the latin name, value, ecosystem and the kg / cm part of caught fish tooltips.",
                        () -> s.shortFishTooltips, v -> s.shortFishTooltips = v, defaults.shortFishTooltips);
                toggle("short pet tooltips", "shows maxed stats once (+175 instead of +175/+175) and leaves out empty skin, item and trail slots.",
                        () -> s.shortPetTooltips, v -> s.shortPetTooltips = v, defaults.shortPetTooltips);
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
                slider("swing turn", "with no swing, how far the item turns. 100% is the full vanilla turn, 0% keeps it still.",
                        0, 100, 1, "%", v -> String.format(Locale.ROOT, "%.0f%%", v), () -> s.swingTurn, v -> s.swingTurn = v, defaults.swingTurn);
            }
            case FOECOLLAB -> foeRows();
        }
    }

    private DofiSettings.TitleChoice[] titleChoices() {
        if (FoeConfig.available()) {
            return DofiSettings.TitleChoice.values();
        }
        return new DofiSettings.TitleChoice[]{DofiSettings.TitleChoice.DOFIS, DofiSettings.TitleChoice.MINECRAFT};
    }

    private void open(Screen screen) {
        if (screen != null) {
            StatsTracker.saveSettings();
            FoeConfig.save();
            client.setScreen(screen);
        }
    }

    // ------------------------------------------------------------------ foecollab

    /** foecollab's own settings as rows (compat/FoeConfig), filtered by the search box. */
    private void foeRows() {
        String q = query.trim().toLowerCase(Locale.ROOT);
        List<FoeConfig.Header> path = new ArrayList<>(); // the open categories and sections, by depth
        int emitted = 0; // how many of them already have a row
        int shown = 0;
        for (FoeConfig.Node node : FoeConfig.nodes()) {
            if (node instanceof FoeConfig.Header h) {
                while (path.size() > h.depth()) path.removeLast();
                path.add(h);
                emitted = Math.min(emitted, path.size() - 1);
                continue;
            }
            FoeConfig.Option o = (FoeConfig.Option) node;
            if (!q.isEmpty() && !foeMatches(o, path, q)) {
                continue;
            }
            for (; emitted < path.size(); emitted++) {
                FoeConfig.Header h = path.get(emitted);
                if (h.depth() == 0) {
                    header(h.label());
                } else {
                    subheader(h.label());
                }
            }
            foeOption(o);
            shown++;
        }
        if (shown == 0) {
            subheader(q.isEmpty() ? "foecollab has no settings to show" : "nothing matches " + q);
        }
    }

    private static boolean foeMatches(FoeConfig.Option o, List<FoeConfig.Header> path, String q) {
        if (o.label().contains(q) || (o.tooltip() != null && o.tooltip().contains(q))) {
            return true;
        }
        for (FoeConfig.Header h : path) {
            if (h.label().contains(q)) return true;
        }
        return false;
    }

    private void foeOption(FoeConfig.Option o) {
        if (o.prefix() != null && !o.prefix().isEmpty()) {
            subheader(o.prefix());
        }
        String name = o.label();
        String desc = o.tooltip() == null ? "" : o.tooltip();
        switch (o.kind()) {
            case BOOL -> toggle(name, desc, () -> (Boolean) o.get(), o::set, (Boolean) o.def());
            case SLIDER -> slider(name, desc, o.min(), o.max(), 1, "", v -> String.valueOf((int) Math.round(v)),
                    () -> ((Number) o.get()).doubleValue(), v -> o.set((int) Math.round(v)), ((Number) o.def()).doubleValue());
            case COLOR -> color(name, desc, () -> (Integer) o.get() & 0xFFFFFF,
                    v -> o.set(((Integer) o.get() & 0xFF000000) | (v & 0xFFFFFF)), (Integer) o.def() & 0xFFFFFF);
            case INT -> text(name, desc, () -> String.valueOf(o.get()), v -> {
                try {
                    o.set(Integer.parseInt(v.trim()));
                } catch (NumberFormatException ignored) {
                    // keep the old value until the field holds a number
                }
            }, String.valueOf(o.def()));
            case LONG -> text(name, desc, () -> String.valueOf(o.get()), v -> {
                try {
                    o.set(Long.parseLong(v.trim()));
                } catch (NumberFormatException ignored) {
                    // keep the old value until the field holds a number
                }
            }, String.valueOf(o.def()));
            case STRING -> text(name, desc, () -> String.valueOf(o.get()), o::set, String.valueOf(o.def()));
            case ENUM -> {
                Enum<?>[] values = o.constants();
                ButtonWidget b = ButtonWidget.builder(Mono.text(name + ": " + FoeConfig.enumLabel(o.get())), btn -> {
                    Enum<?> now = (Enum<?>) o.get();
                    o.set(values[((now == null ? -1 : now.ordinal()) + 1) % values.length]);
                    btn.setMessage(Mono.text(name + ": " + FoeConfig.enumLabel(o.get())));
                }).dimensions(0, 0, controlW(), 20).build();
                row(b, desc.isEmpty() ? "click to change" : desc + " (click to change)", () -> o.set(o.def()));
            }
        }
    }

    // ------------------------------------------------------------------ row builders

    private void header(String name) {
        list.addRow(new RowListWidget.Row(List.of(new Label(rowW(), Mono.bold(name).withColor(S24.ACCENT & 0xFFFFFF)))));
    }

    /** A smaller grey header for a section inside a category. */
    private void subheader(String name) {
        list.addRow(new RowListWidget.Row(List.of(new Label(rowW(), Mono.text(name).withColor(S24.TEXT_3 & 0xFFFFFF)))));
    }

    private void row(ClickableWidget control, String desc, Runnable reset) {
        if (!desc.isEmpty()) {
            control.setTooltip(Tooltip.of(Mono.text(desc)));
        }
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

    private void text(String name, String desc, Supplier<String> get, Consumer<String> set, String def) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, controlW(), 20, Mono.text(name));
        field.setMaxLength(64);
        field.setText(get.get());
        field.setPlaceholder(Mono.text(name).formatted(Formatting.DARK_GRAY));
        field.setChangedListener(set);
        row(field, desc, () -> set.accept(def));
    }

    private void color(String name, String desc, IntSupplier get, IntConsumer set, int def) {
        Runnable open = () -> client.setScreen(new ColorPickerScreen(this, name, get.getAsInt(), set));
        ButtonWidget b = ButtonWidget.builder(Mono.text(name + ": " + String.format(Locale.ROOT, "#%06x", get.getAsInt())),
                btn -> open.run()).dimensions(0, 0, controlW() - 22, 20).build();
        SwatchButton swatch = new SwatchButton(0, 0, 20, 20, get, open);
        swatch.setTooltip(Tooltip.of(Mono.text("click to pick a colour")));
        if (!desc.isEmpty()) {
            b.setTooltip(Tooltip.of(Mono.text(desc)));
        }
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
        if (FoeConfig.available()) {
            FoeConfig.save();
        }
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
