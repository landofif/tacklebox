package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.compat.FoeConfig;
import dev.landofif.dofisaddons.compat.FoeScreens;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Language;

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
 * One settings screen for both mods: foecollabs settings (read from its config, see compat/FoeConfig) and
 * the mods own, as a list of categories on the left and plain label + control rows on the right. foecollabs
 * own settings screen opens this one instead. Every change applies right away and is saved when the screen
 * closes. The search box looks through every category of both.
 */
public class SettingsScreen extends DofiScreen {
    /** The mods own categories. */
    private enum Group {
        GENERAL("general"), HUDS("huds"), BOBBER("bobber"), WORLD("world"), VIEWMODEL("viewmodel");

        final String label;

        Group(String label) {
            this.label = label;
        }
    }

    private static final int ROW_H = 24;
    private static final int SIDE_W = 120;
    private static final int LEFT = 14;
    private static final int TOP = 28;

    private final Screen parent;
    private final DofiSettings s = StatsTracker.settings();
    /** The category shown: "f:" and a foecollab category, or "d:" and one of the mods own. */
    private String key;
    private String query = "";
    private TextFieldWidget search;
    private RowListWidget list;
    private boolean searching;
    /** The section headers above the row being built (by depth), and which of them have a row already. */
    private final String[] path = new String[4];
    private final boolean[] shown = new boolean[4];

    public SettingsScreen(Screen parent) {
        super(Foe.text("settings"));
        this.parent = parent;
        List<String> foe = FoeConfig.available() ? foeCategories() : List.of();
        this.key = foe.isEmpty() ? "d:" + Group.GENERAL.label : "f:" + foe.get(0);
    }

    /** Opened on the mods own settings. */
    public static SettingsScreen dofis(Screen parent) {
        SettingsScreen screen = new SettingsScreen(parent);
        screen.key = "d:" + Group.GENERAL.label;
        return screen;
    }

    @Override
    protected void init() {
        int bottom = height - 36;
        RowListWidget side = new RowListWidget(client, LEFT, TOP, SIDE_W, bottom - TOP, 22);
        List<String> foe = FoeConfig.available() ? foeCategories() : List.of();
        if (!foe.isEmpty()) {
            side.addRow(new RowListWidget.Row(List.of(new Label(side.getRowWidth(), Foe.text("foecollab").withColor(Foe.DIM & 0xFFFFFF), 14))));
            for (String c : foe) {
                side.addRow(new RowListWidget.Row(List.of(category(side, "f:" + c, c))));
            }
        }
        side.addRow(new RowListWidget.Row(List.of(new Label(side.getRowWidth(), Foe.text("dofis addons").withColor(Foe.DIM & 0xFFFFFF), 14))));
        for (Group g : Group.values()) {
            side.addRow(new RowListWidget.Row(List.of(category(side, "d:" + g.label, g.label))));
        }
        addDrawableChild(side);

        int lx = LEFT + SIDE_W + 8;
        int lw = width - LEFT - lx;
        search = new TextFieldWidget(textRenderer, lx, TOP, lw - 14, 18, Foe.text("search"));
        search.setMaxLength(64);
        search.setText(query);
        search.setPlaceholder(Foe.text("search every setting").formatted(Formatting.DARK_GRAY));
        search.setChangedListener(v -> {
            query = v;
            refresh();
            list.setScrollY(0);
        });
        addDrawableChild(search);
        list = new RowListWidget(client, lx, TOP + 24, lw, bottom - TOP - 24, ROW_H);
        addDrawableChild(list);
        fillRows();

        int bw = 100;
        int x = (width - (bw * 3 + 8)) / 2;
        addDrawableChild(ButtonWidget.builder(Foe.text("move huds"), b -> FoeScreens.openMover(this))
                .dimensions(x, height - 30, bw, 20)
                .tooltip(Tooltip.of(Foe.text("foecollabs hud mover. the quests left hud and the chat boxes are in it too"))).build());
        ButtonWidget menu = ButtonWidget.builder(Foe.text("foecollab menu"), b -> open(FoeScreens.main(this)))
                .dimensions(x + bw + 4, height - 30, bw, 20)
                .tooltip(Tooltip.of(Foe.text("custom huds, custom buttons, pet calculator and more"))).build();
        menu.active = FoeConfig.available();
        addDrawableChild(menu);
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(x + (bw + 4) * 2, height - 30, bw, 20).build());
    }

    private ButtonWidget category(RowListWidget side, String id, String label) {
        ButtonWidget b = ButtonWidget.builder(Foe.text(label), btn -> {
            key = id;
            clearAndInit();
        }).dimensions(0, 0, side.getRowWidth(), 20).build();
        b.active = !id.equals(key) || !query.isBlank();
        return b;
    }

    private void open(Screen screen) {
        if (screen != null) {
            StatsTracker.saveSettings();
            FoeConfig.save();
            client.setScreen(screen);
        }
    }

    /** Rebuilds the rows without losing the scroll position. */
    private void refresh() {
        double scroll = list.getScrollY();
        list.clearRows();
        fillRows();
        list.setScrollY(scroll);
    }

    private int rowW() {
        return list.getRowWidth();
    }

    private int controlW() {
        return rowW() * 11 / 20;
    }

    private void fillRows() {
        query = query == null ? "" : query;
        searching = !query.isBlank();
        java.util.Arrays.fill(path, null);
        java.util.Arrays.fill(shown, false);
        if (searching) {
            foeRows(null);
            for (Group g : Group.values()) {
                header(g.label, 0);
                fill(g);
            }
        } else if (key.startsWith("f:")) {
            foeRows(key.substring(2));
        } else {
            for (Group g : Group.values()) {
                if (key.equals("d:" + g.label)) {
                    fill(g);
                }
            }
        }
        if (list.children().isEmpty()) {
            list.addRow(new RowListWidget.Row(List.of(new Label(rowW(), Foe.text("nothing matches " + query.trim()).withColor(Foe.DIM & 0xFFFFFF), 20))));
        }
    }

    private void fill(Group g) {
        switch (g) {
            case GENERAL -> general();
            case HUDS -> huds();
            case BOBBER -> bobber();
            case WORLD -> world();
            case VIEWMODEL -> viewmodel();
        }
    }

    // ------------------------------------------------------------------ the mods settings

    private void general() {
        header("level", 1);
        toggle("hide vanilla xp bar",
                "on fishonmc the xp bar shows your fishing level. this hides it and its number so foecollabs level hud can take its place.",
                () -> s.hideVanillaXp, v -> s.hideVanillaXp = v);
        header("stats", 1);
        slider("pause fishing clock after",
                "the time fishing clock (and fish per hour) stops when you havent caught anything for this long, so afk time doesnt count.",
                10, 600, 10, "s", null,
                () -> s.pauseAfterSeconds, v -> s.pauseAfterSeconds = (int) v);
        header("menus", 1);
        toggle("simple menus",
                "draws chests, your inventory and fishonmc menus as one box with no slot boxes. this hides the menu art from the fishonmc resource pack.",
                () -> s.flatMenus, v -> s.flatMenus = v);
        toggle("fishonmc menu",
                "shows the main menu (book in the 9th slot, /menu) and /travel as a screen of dofis addons with big icons you can rearrange. turn it off to get the chest back.",
                () -> s.fishMenu, v -> s.fishMenu = v);
        slider("menu size", "how much of the screen the fishonmc menu takes.", 40, 100, 1, "%", v -> String.format(Locale.ROOT, "%.0f%%", v),
                () -> s.menuSize, v -> s.menuSize = (int) Math.round(v));
        toggle("presets screen",
                "shows /presets as a wardrobe of cards with player models. turn it off to get the chest back.",
                () -> s.presetsMenu, v -> s.presetsMenu = v);
        slider("presets size", "how much of the screen the presets screen takes.", 40, 100, 1, "%", v -> String.format(Locale.ROOT, "%.0f%%", v),
                () -> s.presetsSize, v -> s.presetsSize = (int) Math.round(v));
        toggle("presets other page background", "draws the card boxes behind the dimmed presets of the other page. off by default, so only the models show.",
                () -> s.presetsOtherBg, v -> s.presetsOtherBg = v);
    }

    private void huds() {
        header("huds", 1);
        toggle("quests left hud",
                "a small box with the estimated quests until your next level and the next level of the location you are at. it learns as you play and reads the /quests menu and the compendium locations. move it in foecollabs hud mover.",
                () -> s.questsHud, v -> s.questsHud = v);
        slider("hud opacity", "how see-through the mods huds are: quests left and the chat boxes.", 10, 100, 5, "%", v -> String.format(Locale.ROOT, "%.0f%%", v),
                () -> s.hudOpacity, v -> s.hudOpacity = (int) Math.round(v));
        slider("screen opacity", "how see-through the box of the mods screens is, this one too.", 20, 100, 5, "%", v -> String.format(Locale.ROOT, "%.0f%%", v),
                () -> s.screenOpacity, v -> s.screenOpacity = (int) Math.round(v));
    }

    private void bobber() {
            header("timer");
            cycle("bobber timer",
                    "shows how long your line has been in the water. changes colour as the wait gets long, and says bite! when something bites. display only.",
                    DofiSettings.BobberMode.values(), m -> m.label, () -> s.bobberTimer, v -> s.bobberTimer = v);
            color("timer colour", "the colour of the timer while the wait is still short.",
                    () -> s.bobberColor, v -> s.bobberColor = v);
            slider("warn after", "the timer switches to the warn colour after this many seconds. 0 = never.",
                    0, 60, 1, "s", v -> v == 0 ? "never" : String.format(Locale.ROOT, "%.0fs", v),
                    () -> s.bobberWarnSeconds, v -> s.bobberWarnSeconds = v);
            color("warn colour", "the colour of the timer once it passes the warn time.",
                    () -> s.bobberWarnColor, v -> s.bobberWarnColor = v);
            slider("slow after", "the timer switches to the slow colour after this many seconds. 0 = never.",
                    0, 60, 1, "s", v -> v == 0 ? "never" : String.format(Locale.ROOT, "%.0fs", v),
                    () -> s.bobberSlowSeconds, v -> s.bobberSlowSeconds = v);
            color("slow colour", "the colour of the timer once it passes the slow time.",
                    () -> s.bobberSlowColor, v -> s.bobberSlowColor = v);
            color("bite colour", "the colour of the timer once something bites.",
                    () -> s.bobberBiteColor, v -> s.bobberBiteColor = v);
            toggle("show bite!", "puts bite! in front of the time when something bites. the colour still changes when this is off.",
                    () -> s.bobberShowBite, v -> s.bobberShowBite = v);
            slider("decimals", "how many digits after the point. 0 shows whole seconds.",
                    0, 2, 1, "", v -> String.format(Locale.ROOT, "%.0f", v),
                    () -> s.bobberDecimals, v -> s.bobberDecimals = (int) Math.round(v));
            slider("timer size", "how big the timer is. 1 = normal.", 0.5, 3, 0.05, "x", null,
                    () -> s.bobberScale, v -> s.bobberScale = v);
            slider("height above bobber", "how many blocks above the bobber the timer floats. above bobber mode only.",
                    0, 3, 0.05, " blocks", null, () -> s.bobberHeight, v -> s.bobberHeight = v);
            slider("background", "how solid the box behind the timer above the bobber is. 0 = no box.",
                    0, 100, 1, "%", v -> String.format(Locale.ROOT, "%.0f%%", v),
                    () -> s.bobberBacking, v -> s.bobberBacking = (int) Math.round(v));
            toggle("show through walls", "keeps the timer above the bobber visible behind blocks and water.",
                    () -> s.bobberThroughWalls, v -> s.bobberThroughWalls = v);
            slider("crosshair offset", "how far below the crosshair the timer sits. under crosshair mode only. negative is above.",
                    -60, 60, 1, "", v -> String.format(Locale.ROOT, "%.0f", v),
                    () -> s.bobberCrosshairY, v -> s.bobberCrosshairY = (int) Math.round(v));
            header("bite");
            toggle("bite message", "shows a short message in the middle of the screen when your own fish bites.",
                    () -> s.biteMessage, v -> s.biteMessage = v);
            text("message text", "what the bite message says.", () -> s.biteMessageText, v -> s.biteMessageText = v);
            color("message colour", "the colour of the bite message.",
                    () -> s.biteMessageColor, v -> s.biteMessageColor = v);
            header("bobber");
            toggle("still bobber", "holds your bobber still on the water instead of bobbing up and down. it still dips when a fish bites.",
                    () -> s.stillBobber, v -> s.stillBobber = v);
            toggle("smooth cast", "on a laggy connection your cast can jump back and fly the path again. this shows your own cast flying smoothly. where it lands stays exact.",
                    () -> s.smoothCast, v -> s.smoothCast = v);
            toggle("hide other bobbers", "hides other players bobbers and their lines, on your screen only.",
                    () -> s.hideOtherBobbers, v -> s.hideOtherBobbers = v);
            header("skin");
            cycle("bobber skin", "swaps the look of your bobber. only you see it, other players still see the normal one.",
                    DofiSettings.BobberSkin.values(), k -> k.label, () -> s.bobberSkin, v -> s.bobberSkin = v);
            toggle("skin every bobber", "also uses your skin for other players bobbers, on your screen only.",
                    () -> s.skinEveryone, v -> s.skinEveryone = v);
            header("line");
            toggle("custom line colour", "colours your own fishing line. other lines stay normal.",
                    () -> s.customLineColor, v -> s.customLineColor = v);
            color("line colour", "the colour of your fishing line. click to pick it.",
                    () -> s.lineColor, v -> s.lineColor = v);
    }

    private void world() {
            header("nametags");
            toggle("hide my nametag", "hides the rank, name and power tag the server puts over your own head. other players still see it.",
                    () -> s.hideOwnNametag, v -> s.hideOwnNametag = v);
            toggle("hide all nametags", "hides every players nametag on your screen, the servers and the vanilla one.",
                    () -> s.hideAllNametags, v -> s.hideAllNametags = v);
            header("clutter");
            toggle("hide xp popups", "hides the +66xp line above the hotbar after each catch.",
                    () -> s.hideXpPopups, v -> s.hideXpPopups = v);
            toggle("flat leaves", "draws leaves flat like vanilla instead of the 3d leaves from the fishonmc pack. reloads resources when you change it.",
                    () -> s.flatLeaves, v -> {
                        if (s.flatLeaves != v) {
                            s.flatLeaves = v;
                            client.reloadResources();
                        }
                    });
            header("tooltips");
            toggle("short fish tooltips", "leaves out the latin name, value, ecosystem and the kg / cm part of caught fish tooltips.",
                    () -> s.shortFishTooltips, v -> s.shortFishTooltips = v);
            toggle("short pet tooltips", "shows maxed stats once (+175 instead of +175/+175) and leaves out empty skin, item and trail slots.",
                    () -> s.shortPetTooltips, v -> s.shortPetTooltips = v);
    }

    private void viewmodel() {
            header("held item");
            toggle("custom viewmodel", "changes how big your held item is and where it sits on screen. turn off to go back to normal.",
                    () -> s.viewmodel, v -> s.viewmodel = v);
            slider("size", "how big the held item is. 1 = normal.", 0.1, 3, 0.05, "x", null,
                    () -> s.vmScale, v -> s.vmScale = v);
            slider("left / right", "moves the item sideways. mirrored for your off hand.", -1, 1, 0.05, "", null,
                    () -> s.vmX, v -> s.vmX = v);
            slider("up / down", "moves the item up or down.", -1, 1, 0.05, "", null,
                    () -> s.vmY, v -> s.vmY = v);
            slider("forward / back", "moves the item closer or further away. negative is further.", -1, 1, 0.05, "", null,
                    () -> s.vmZ, v -> s.vmZ = v);
            slider("tilt", "tilts the item forward or back, in degrees.", -180, 180, 1, "°", null,
                    () -> s.vmRotX, v -> s.vmRotX = v);
            slider("turn", "turns the item left or right, in degrees.", -180, 180, 1, "°", null,
                    () -> s.vmRotY, v -> s.vmRotY = v);
            slider("roll", "rolls the item sideways, in degrees.", -180, 180, 1, "°", null,
                    () -> s.vmRotZ, v -> s.vmRotZ = v);
            toggle("no switch dip", "stops the item from dipping down when you switch slots or use it.",
                    () -> s.vmNoEquipDip, v -> s.vmNoEquipDip = v);
            header("swing");
            cycle("swing style",
                    "how your hand swings. smooth eases in and out, snappy moves fast at the start, no swing only turns the item around its own centre without moving it. only you see it.",
                    DofiSettings.SwingStyle.values(), w -> w.label, () -> s.swingStyle, v -> s.swingStyle = v);
            slider("swing length", "how long one swing takes. 1 = normal, 2 = twice as slow, 0.5 = twice as fast. only changes how it looks.",
                    0.25, 4, 0.05, "x", null, () -> s.swingSlowness, v -> s.swingSlowness = v);
            slider("swing turn", "with no swing, how far the item turns. 100% is the full vanilla turn, 0% keeps it still.",
                    0, 100, 1, "%", v -> String.format(Locale.ROOT, "%.0f%%", v), () -> s.swingTurn, v -> s.swingTurn = v);
    }

    // ------------------------------------------------------------------ foecollab

    private static List<String> foeCategories() {
        return FoeConfig.nodes().stream().filter(n -> n instanceof FoeConfig.Header h && h.depth() == 0)
                .map(n -> ((FoeConfig.Header) n).label()).toList();
    }

    /** foecollabs settings as rows: one category, or every category while searching ({@code category} null). */
    private void foeRows(String category) {
        String current = null;
        for (FoeConfig.Node node : FoeConfig.nodes()) {
            if (node instanceof FoeConfig.Header h) {
                if (h.depth() == 0) {
                    current = h.label();
                }
                if (category == null || category.equals(current)) {
                    header(h.label(), h.depth());
                }
            } else if (category == null || category.equals(current)) {
                foeOption((FoeConfig.Option) node);
            }
        }
    }

    private void foeOption(FoeConfig.Option o) {
        if (o.prefix() != null && !o.prefix().isEmpty()) {
            header(o.prefix(), o.depth());
        }
        String name = o.label();
        String desc = o.tooltip() == null ? "" : o.tooltip();
        switch (o.kind()) {
            case BOOL -> toggle(name, desc, () -> (Boolean) o.get(), o::set);
            case SLIDER -> slider(name, desc, o.min(), o.max(), 1, "", v -> String.valueOf((int) Math.round(v)),
                    () -> ((Number) o.get()).doubleValue(), v -> o.set((int) Math.round(v)));
            case COLOR -> color(name, desc, () -> (Integer) o.get() & 0xFFFFFF,
                    v -> o.set(((Integer) o.get() & 0xFF000000) | (v & 0xFFFFFF)));
            case INT -> text(name, desc, () -> String.valueOf(o.get()), v -> {
                try {
                    o.set(Integer.parseInt(v.trim()));
                } catch (NumberFormatException ignored) {
                    // keep the old value until the field holds a number
                }
            });
            case LONG -> text(name, desc, () -> String.valueOf(o.get()), v -> {
                try {
                    o.set(Long.parseLong(v.trim()));
                } catch (NumberFormatException ignored) {
                    // keep the old value until the field holds a number
                }
            });
            case STRING -> text(name, desc, () -> String.valueOf(o.get()), o::set);
            case ENUM -> {
                Enum<?>[] values = o.constants();
                ButtonWidget b = ButtonWidget.builder(Foe.text(FoeConfig.enumLabel(o.get())), btn -> {
                    Enum<?> now = (Enum<?>) o.get();
                    o.set(values[((now == null ? -1 : now.ordinal()) + 1) % values.length]);
                    btn.setMessage(Foe.text(FoeConfig.enumLabel(o.get())));
                }).dimensions(0, 0, controlW(), 20).build();
                row(name, desc.isEmpty() ? "click to change" : desc + " (click to change)", b);
            }
        }
    }

    // ------------------------------------------------------------------ row builders

    /** A section title. While searching it only shows once one of the rows under it matches. */
    private void header(String name) {
        header(name, 1);
    }

    private void header(String name, int depth) {
        depth = Math.min(depth, path.length - 1);
        path[depth] = name;
        shown[depth] = false;
        for (int i = depth + 1; i < path.length; i++) {
            path[i] = null;
        }
        if (!searching && depth > 0) {
            emit(depth);
        }
    }

    private void emit(int depth) {
        shown[depth] = true;
        Text text = Foe.text(path[depth]);
        list.addRow(new RowListWidget.Row(List.of(new Label(rowW(), depth <= 1 ? text.copy().withColor(Foe.TITLE & 0xFFFFFF) : text.copy().withColor(Foe.DIM & 0xFFFFFF), 20))));
    }

    private boolean matches(String name, String desc) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (name.contains(q) || desc.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (String h : path) {
            if (h != null && h.contains(q)) {
                return true;
            }
        }
        return false;
    }

    /** A label on the left, the control on the right; a hover tooltip explains it. */
    private void row(String name, String desc, ClickableWidget control, ClickableWidget... more) {
        if (searching) {
            if (!matches(name, desc)) {
                return;
            }
            for (int i = 0; i < path.length; i++) {
                if (path[i] != null && !shown[i]) {
                    emit(i);
                }
            }
        }
        Label label = new Label(rowW() - controlW() - 2, Foe.text(name), 20);
        List<ClickableWidget> widgets = new ArrayList<>(List.of(label, control));
        widgets.addAll(List.of(more));
        if (!desc.isEmpty()) {
            Tooltip tip = Tooltip.of(Foe.text(desc));
            label.setTooltip(tip);
            control.setTooltip(tip);
        }
        list.addRow(new RowListWidget.Row(widgets));
    }

    private static Text onOff(boolean on) {
        return Foe.text(on ? "on" : "off").formatted(on ? Formatting.GREEN : Formatting.RED);
    }

    private void toggle(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
        ButtonWidget b = ButtonWidget.builder(onOff(get.get()), btn -> {
            set.accept(!get.get());
            btn.setMessage(onOff(get.get()));
        }).dimensions(0, 0, controlW(), 20).build();
        row(name, desc, b);
    }

    private <E extends Enum<E>> void cycle(String name, String desc, E[] values, Function<E, String> label,
                                           Supplier<E> get, Consumer<E> set) {
        ButtonWidget b = ButtonWidget.builder(Foe.text(label.apply(get.get())), btn -> {
            set.accept(values[(get.get().ordinal() + 1) % values.length]);
            btn.setMessage(Foe.text(label.apply(get.get())));
        }).dimensions(0, 0, controlW(), 20).build();
        row(name, desc + " (click to change)", b);
    }

    private void slider(String name, String desc, double min, double max, double step, String unit,
                        DoubleFunction<String> format, DoubleSupplier get, DoubleConsumer set) {
        ValueSlider slider = new ValueSlider(0, 0, controlW(), 20, "", min, max, step, unit, get.getAsDouble(), set);
        if (format != null) {
            slider.format(format);
        }
        row(name, desc, slider);
    }

    private void text(String name, String desc, Supplier<String> get, Consumer<String> set) {
        TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, controlW(), 20, Foe.text(name));
        field.setMaxLength(64);
        field.setText(get.get());
        field.setChangedListener(set);
        row(name, desc, field);
    }

    private void color(String name, String desc, IntSupplier get, IntConsumer set) {
        Runnable open = () -> client.setScreen(new ColorPickerScreen(this, name, get.getAsInt(), set));
        ButtonWidget b = ButtonWidget.builder(Foe.text(String.format(Locale.ROOT, "#%06x", get.getAsInt())),
                btn -> open.run()).dimensions(0, 0, controlW() - 22, 20).build();
        SwatchButton swatch = new SwatchButton(0, 0, 20, 20, get, open);
        swatch.setTooltip(Tooltip.of(Foe.text("click to pick a colour")));
        row(name, desc, b, swatch);
    }

    // ------------------------------------------------------------------ screen

    @Override
    public void close() {
        StatsTracker.saveSettings();
        if (FoeConfig.available()) {
            FoeConfig.save();
        }
        client.setScreen(parent);
    }

    /** A line of text in a list: row labels and section titles. */
    private static final class Label extends ClickableWidget {
        Label(int width, Text text, int height) {
            super(0, 0, width, height, text);
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            ctx.drawTextWithShadow(tr, Language.getInstance().reorder(tr.trimToWidth(getMessage(), width - 4)), getX() + 2, getY() + (height - 8) / 2, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
            return false;
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
}
