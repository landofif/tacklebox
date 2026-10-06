package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.compat.FoeConfig;
import dev.landofif.dofisaddons.compat.FoeScreens;
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
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
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
        MENUS("menus"), HUDS("huds"), BOBBER("bobber"), WORLD("world"), VIEWMODEL("viewmodel");

        final String label;

        Group(String label) {
            this.label = label;
        }
    }

    private static final DoubleFunction<String> PCT = v -> String.format(Locale.ROOT, "%.0f%%", v);
    private static final int ROW_H = 24;
    private static final int LEFT = 14;
    private static final int TOP = 28;

    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();
    /** The category shown: "f:" and a foecollab category, or "d:" and one of the mods own. */
    private String key;
    /** The section of a foecollab category picked in the dropdown under it, or null for the whole category. */
    private String sub;
    /** Grey headings the player folded (by their path), so the rows under them are left out. */
    private final Set<String> collapsed = new HashSet<>();
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
        this.key = foe.isEmpty() ? "d:" + Group.MENUS.label : "f:" + foe.get(0);
    }

    /** Opened on the mods own settings. */
    public static SettingsScreen dofis(Screen parent) {
        SettingsScreen screen = new SettingsScreen(parent);
        screen.key = "d:" + Group.MENUS.label;
        return screen;
    }

    @Override
    protected void init() {
        int bottom = height - 36;
        int sideW = Math.max(84, Math.min(120, width / 4)); // narrower on small windows, so the rows keep their room
        RowListWidget side = new RowListWidget(client, LEFT, TOP, sideW, bottom - TOP, 22);
        List<String> foe = FoeConfig.available() ? foeCategories() : List.of();
        if (!foe.isEmpty()) {
            side.addRow(new RowListWidget.Row(List.of(new Label(side.getRowWidth(), Foe.text("foecollab").withColor(Foe.DIM & 0xFFFFFF), 14))));
            for (String c : foe) {
                side.addRow(new RowListWidget.Row(List.of(category(side, "f:" + c, c))));
                if (("f:" + c).equals(key)) { // the picked category opens a dropdown of its sections, if it is long
                    for (String section : foeSections(c, (bottom - TOP - 24) / ROW_H)) {
                        side.addRow(new RowListWidget.Row(List.of(new Label(6, Text.empty(), 20),
                                pick(side, section, section.equals(sub) && query.isBlank(), 8, () -> sub = section))));
                    }
                }
            }
        }
        side.addRow(new RowListWidget.Row(List.of(new Label(side.getRowWidth(), Foe.text("dofis addons").withColor(Foe.DIM & 0xFFFFFF), 14))));
        for (Group g : Group.values()) {
            side.addRow(new RowListWidget.Row(List.of(category(side, "d:" + g.label, g.label))));
        }
        side.addRow(new RowListWidget.Row(List.of(new Label(side.getRowWidth(), Foe.text("screens").withColor(Foe.DIM & 0xFFFFFF), 14))));
        side.addRow(new RowListWidget.Row(List.of(screenButton(side, "chat triggers", () -> new TriggerListScreen(this)))));
        side.addRow(new RowListWidget.Row(List.of(screenButton(side, "chat", () -> new ChatFilterScreen(this)))));
        side.addRow(new RowListWidget.Row(List.of(screenButton(side, "key commands", () -> new KeyCommandScreen(this)))));
        addDrawableChild(side);

        int lx = LEFT + sideW + 8;
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

        int bw = Math.min(100, (width - LEFT * 2 - 8) / 3);
        int x = (width - (bw * 3 + 8)) / 2;
        addDrawableChild(ButtonWidget.builder(Foe.text("move huds"), b -> FoeScreens.openMover(this))
                .dimensions(x, height - 30, bw, 20)
                .tooltip(Tooltip.of(Foe.sentence("foecollabs hud mover. the quests left hud and the chat boxes are in it too"))).build());
        ButtonWidget menu = ButtonWidget.builder(Foe.text("foecollab menu"), b -> open(FoeScreens.main(this)))
                .dimensions(x + bw + 4, height - 30, bw, 20)
                .tooltip(Tooltip.of(Foe.sentence("custom huds, custom buttons, pet calculator and more"))).build();
        menu.active = FoeConfig.available();
        addDrawableChild(menu);
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(x + (bw + 4) * 2, height - 30, bw, 20).build());
    }

    /** A category. The picked one is orange and cannot be clicked again. */
    private ButtonWidget category(RowListWidget side, String id, String label) {
        return pick(side, label, id.equals(key) && sub == null && query.isBlank(), 0, () -> {
            key = id;
            sub = null;
        });
    }

    private ButtonWidget pick(RowListWidget side, String label, boolean picked, int indent, Runnable choose) {
        ButtonWidget b = ButtonWidget.builder(picked ? Foe.text(label).withColor(Foe.ACCENT & 0xFFFFFF) : Foe.text(label), btn -> {
            choose.run();
            clearAndInit();
        }).dimensions(0, 0, side.getRowWidth() - indent, 20).build();
        b.active = !picked;
        return b;
    }

    /** Opens one of the mods own screens from the list. */
    private ButtonWidget screenButton(RowListWidget side, String label, Supplier<Screen> screen) {
        return ButtonWidget.builder(Foe.text(label), btn -> open(screen.get())).dimensions(0, 0, side.getRowWidth(), 20).build();
    }

    private void open(Screen screen) {
        if (screen != null) {
            DofiSettings.save();
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
            foeRows(null, null);
            for (Group g : Group.values()) {
                header(g.label, 0);
                fill(g);
            }
        } else if (key.startsWith("f:")) {
            foeRows(key.substring(2), sub);
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
            case MENUS -> menus();
            case HUDS -> huds();
            case BOBBER -> bobber();
            case WORLD -> world();
            case VIEWMODEL -> viewmodel();
        }
    }

    // ------------------------------------------------------------------ the mods settings

    private void menus() {
        header("menus");
        toggle("simple menus", "chests, your inventory and fishonmc menus with no box and no slot boxes, just the items. hides the menu art from the server pack.",
                () -> s.flatMenus, v -> s.flatMenus = v);
        toggle("flat buttons", "every button as a flat see-through fill instead of the bevelled texture, in the game menus too.",
                () -> s.flatButtons, v -> s.flatButtons = v);
        toggle("rarity circles", "a see-through circle in the rarity colour behind items in your inventory. foecollabs corner mark has its own setting.",
                () -> s.rarityCircles, v -> s.rarityCircles = v);
        toggle("fishonmc menu", "the main menu and /travel as a screen with big icons you can rearrange. off gives the chest back.",
                () -> s.fishMenu, v -> s.fishMenu = v);
        slider("menu size", "how much of the screen the fishonmc menu fills.", 40, 100, 1, "%", PCT,
                () -> s.menuSize, v -> s.menuSize = (int) Math.round(v));
        header("presets");
        toggle("presets screen", "/presets as a wardrobe of cards with player models. off gives the chest back.",
                () -> s.presetsMenu, v -> s.presetsMenu = v);
        slider("presets size", "how much of the screen the presets screen fills.", 40, 100, 1, "%", PCT,
                () -> s.presetsSize, v -> s.presetsSize = (int) Math.round(v));
        toggle("other page boxes", "draws the card boxes behind the dimmed presets of the other page. off shows only the models.",
                () -> s.presetsOtherBg, v -> s.presetsOtherBg = v);
    }

    private void huds() {
        header("quests left");
        toggle("quests left hud", "quests until your next level and the next level of your location. it learns from /menu, /quests and the compendium. move it with move huds.",
                () -> s.questsHud, v -> s.questsHud = v);
    }

    private void bobber() {
        header("bobber");
        toggle("still bobber", "keeps your bobber still on the water. it still dips on a bite.",
                () -> s.stillBobber, v -> s.stillBobber = v);
        toggle("smooth cast", "on a laggy connection your cast flies smoothly instead of jumping back. the landing spot stays exact.",
                () -> s.smoothCast, v -> s.smoothCast = v);
        toggle("hide other bobbers", "hides other players bobbers and their lines.",
                () -> s.hideOtherBobbers, v -> s.hideOtherBobbers = v);
        header("skin");
        cycle("bobber skin", "swaps your bobber for a skin. only you see it.",
                DofiSettings.BobberSkin.values(), k -> k.label, () -> s.bobberSkin, v -> s.bobberSkin = v);
        toggle("skin every bobber", "uses your skin on other players bobbers too, on your screen only.",
                () -> s.skinEveryone, v -> s.skinEveryone = v);
        header("line");
        toggle("custom line colour", "colours your own fishing line.",
                () -> s.customLineColor, v -> s.customLineColor = v);
        color("line colour", "the colour of your fishing line.",
                () -> s.lineColor, v -> s.lineColor = v);
    }

    private void world() {
        header("nametags");
        toggle("hide my nametag", "hides the rank, name and power tag over your own head. others still see it.",
                () -> s.hideOwnNametag, v -> s.hideOwnNametag = v);
        toggle("hide all nametags", "hides every players nametag, the servers and the vanilla one.",
                () -> s.hideAllNametags, v -> s.hideAllNametags = v);
        header("clutter");
        toggle("hide xp popups", "hides the +66xp line above the hotbar after a catch.",
                () -> s.hideXpPopups, v -> s.hideXpPopups = v);
        toggle("flat leaves", "draws leaves flat like vanilla instead of the packs 3d leaves. reloads resources.",
                () -> s.flatLeaves, v -> {
                    if (s.flatLeaves != v) {
                        s.flatLeaves = v;
                        client.reloadResources();
                    }
                });
    }

    private void viewmodel() {
        header("held item");
        toggle("custom viewmodel", "changes the size and place of your held item. off is vanilla.",
                () -> s.viewmodel, v -> s.viewmodel = v);
        slider("size", "how big the held item is. 1 = normal.", 0.1, 3, 0.01, "x", null,
                () -> s.vmScale, v -> s.vmScale = v);
        slider("left / right", "moves the item sideways. mirrored for your off hand.", -1, 1, 0.01, "", null,
                () -> s.vmX, v -> s.vmX = v);
        slider("up / down", "moves the item up or down.", -1, 1, 0.01, "", null,
                () -> s.vmY, v -> s.vmY = v);
        slider("forward / back", "moves the item closer or further away.", -1, 1, 0.01, "", null,
                () -> s.vmZ, v -> s.vmZ = v);
        slider("tilt", "tilts the item forward or back, in degrees.", -180, 180, 1, "°", null,
                () -> s.vmRotX, v -> s.vmRotX = v);
        slider("turn", "turns the item left or right, in degrees.", -180, 180, 1, "°", null,
                () -> s.vmRotY, v -> s.vmRotY = v);
        slider("roll", "rolls the item sideways, in degrees.", -180, 180, 1, "°", null,
                () -> s.vmRotZ, v -> s.vmRotZ = v);
        toggle("no switch dip", "stops the dip when you switch slots or use the item.",
                () -> s.vmNoEquipDip, v -> s.vmNoEquipDip = v);
        header("swing");
        cycle("swing style", "how your hand swings. no swing only turns the item in place.",
                DofiSettings.SwingStyle.values(), w -> w.label, () -> s.swingStyle, v -> s.swingStyle = v);
        slider("swing length", "how long one swing takes. 1 = normal, 2 = twice as slow.",
                0.25, 4, 0.01, "x", null, () -> s.swingSlowness, v -> s.swingSlowness = v);
        slider("swing turn", "with no swing, how far the item turns. 100% is vanilla, 0% keeps it still.",
                0, 100, 1, "%", PCT, () -> s.swingTurn, v -> s.swingTurn = v);
    }

    // ------------------------------------------------------------------ foecollab

    private static List<String> foeCategories() {
        return FoeConfig.nodes().stream().filter(n -> n instanceof FoeConfig.Header h && h.depth() == 0)
                .map(n -> ((FoeConfig.Header) n).label()).toList();
    }

    /**
     * The sections of a category that earn a dropdown in the sidebar: at least two, and only when the
     * category has more rows than fit on screen (a short one needs no scrolling, so no dropdown).
     */
    private static List<String> foeSections(String category, int fits) {
        List<String> out = new ArrayList<>();
        int rows = 0;
        String current = null;
        for (FoeConfig.Node node : FoeConfig.nodes()) {
            if (node instanceof FoeConfig.Header h) {
                if (h.depth() == 0) {
                    current = h.label();
                } else if (category.equals(current)) {
                    rows++;
                    if (h.depth() == 1) {
                        out.add(h.label());
                    }
                }
            } else if (category.equals(current)) {
                rows += ((FoeConfig.Option) node).prefix() == null || ((FoeConfig.Option) node).prefix().isEmpty() ? 1 : 2;
            }
        }
        return rows > fits && out.size() >= 2 ? out : List.of();
    }

    /**
     * foecollabs settings as rows: one category, or every category while searching ({@code category} null),
     * all of it or only one of its sections ({@code section}).
     */
    private void foeRows(String category, String section) {
        String current = null;
        String currentSection = null;
        for (FoeConfig.Node node : FoeConfig.nodes()) {
            if (node instanceof FoeConfig.Header h) {
                if (h.depth() == 0) {
                    current = h.label();
                    currentSection = null;
                } else if (h.depth() == 1) {
                    currentSection = h.label();
                }
                if ((category == null || category.equals(current)) && (section == null || section.equals(currentSection))) {
                    header(h.label(), h.depth());
                }
            } else {
                FoeConfig.Option o = (FoeConfig.Option) node;
                if (o.depth() == 1) {
                    currentSection = null; // a plain option of the category, outside any section
                }
                if ((category == null || category.equals(current)) && (section == null || section.equals(currentSection))) {
                    foeOption(o);
                }
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
        if (!searching && depth > 0 && !folded(depth)) {
            emit(depth);
        }
    }

    private String pathKey(int depth) {
        StringBuilder k = new StringBuilder();
        for (int i = 1; i <= depth; i++) {
            if (path[i] != null) {
                k.append('/').append(path[i]);
            }
        }
        return k.toString();
    }

    /** Whether a grey heading above depth {@code below} is folded, so what sits under it is left out. */
    private boolean folded(int below) {
        for (int d = 2; d < below && d < path.length; d++) {
            if (path[d] != null && collapsed.contains(pathKey(d))) {
                return true;
            }
        }
        return false;
    }

    private void emit(int depth) {
        shown[depth] = true;
        // headings sit low in their row: tight to the rows below them, with room above (grouping by proximity)
        if (depth <= 1 || searching) {
            Text text = Foe.text(path[depth]);
            list.addRow(new RowListWidget.Row(List.of(new Label(rowW(), text.copy().withColor(depth <= 1 ? Foe.TITLE & 0xFFFFFF : Foe.DIM & 0xFFFFFF), ROW_H, true, null))));
            return;
        }
        String k = pathKey(depth); // a grey heading folds when clicked
        boolean closed = collapsed.contains(k);
        Text text = Foe.text((closed ? "▸ " : "▾ ") + path[depth]).withColor(Foe.DIM & 0xFFFFFF);
        list.addRow(new RowListWidget.Row(List.of(new Label(rowW(), text, ROW_H, true, () -> {
            if (!collapsed.remove(k)) {
                collapsed.add(k);
            }
            refresh();
        }))));
    }

    private boolean matches(String name, String desc) {
        String q = query.trim().toLowerCase(Locale.ROOT);
        if (name.toLowerCase(Locale.ROOT).contains(q) || desc.toLowerCase(Locale.ROOT).contains(q)) {
            return true;
        }
        for (String h : path) {
            if (h != null && h.toLowerCase(Locale.ROOT).contains(q)) {
                return true;
            }
        }
        return false;
    }

    /** A label on the left, the control on the right; a hover tooltip explains it. */
    private void row(String name, String desc, ClickableWidget control, ClickableWidget... more) {
        if (!searching && folded(path.length)) {
            return;
        }
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
            Tooltip tip = Tooltip.of(Foe.sentence(desc));
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
        swatch.setTooltip(Tooltip.of(Foe.sentence("click to pick a colour")));
        row(name, desc, b, swatch);
    }

    // ------------------------------------------------------------------ screen

    @Override
    public void close() {
        DofiSettings.save();
        if (FoeConfig.available()) {
            FoeConfig.save();
        }
        client.setScreen(parent);
    }

    /** A line of text in a list: row labels and section titles. */
    private static final class Label extends ClickableWidget {
        private final boolean low;
        /** What a click does (folding a heading), or null for plain text. */
        private final Runnable onClick;

        Label(int width, Text text, int height) {
            this(width, text, height, false, null);
        }

        Label(int width, Text text, int height, boolean low, Runnable onClick) {
            super(0, 0, width, height, text);
            this.low = low;
            this.onClick = onClick;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            Text text = onClick != null && isHovered() ? getMessage().copy().withColor(Foe.ACCENT & 0xFFFFFF) : getMessage();
            ctx.drawTextWithShadow(tr, Language.getInstance().reorder(tr.trimToWidth(text, width - 4)), getX() + 2, low ? getY() + height - 12 : getY() + (height - 8) / 2, 0xFFFFFFFF);
        }

        @Override
        public boolean mouseClicked(net.minecraft.client.gui.Click click, boolean doubled) {
            if (onClick == null || !isMouseOver(click.x(), click.y())) {
                return false;
            }
            playDownSound(net.minecraft.client.MinecraftClient.getInstance().getSoundManager());
            onClick.run();
            return true;
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
            appendDefaultNarrations(builder);
        }
    }
}
