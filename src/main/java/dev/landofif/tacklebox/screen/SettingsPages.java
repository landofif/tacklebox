package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.display.DisplayCull;
import dev.landofif.tacklebox.display.NearbyPlayers;
import dev.landofif.tacklebox.display.RodIdle;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import java.util.function.DoubleSupplier;
import java.util.function.Function;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;
import java.util.function.Supplier;

/**
 * The mods settings as pages of groups (see {@link SettingsScreen}); a group is a {@link Tile}.
 * Each row is a label and one control that applies its value right away.
 */
public final class SettingsPages {
    /** {@code icon} is the path of a vanilla item, drawn next to the name. */
    public record Page(String name, String icon, List<Tile> tiles) {
        /** A page of links only (tools): its one group's rows are listed where other pages list their groups. */
        public boolean links() {
            return tiles.size() == 1 && tiles.get(0).rows().stream().allMatch(r -> r.icon != null);
        }
    }

    /** A group of a page. {@code main} is its on/off switch, shown next to its name in the list of groups, or null. */
    public record Tile(String title, String icon, Text heading, List<Row> rows, Row main) {}

    /** A setting: a label, a tooltip and a control drawn at the right of the row. */
    public abstract static class Row {
        public static final int H = 14;
        public final String label, desc;
        public final String name;
        /** The item drawn before a link on the tools page, or null. */
        public String icon;
        public float hot;
        /** Where the row was drawn last, for hit tests. */
        public int x, y, w;

        Row(String name, String desc) {
            this.name = name;
            this.label = Foe.smallCaps(name);
            this.desc = desc;
        }

        public abstract int width(TextRenderer tr);

        /** The control with its top left at {@code cx, cy}, {@link #H} tall. */
        public abstract void draw(DrawContext c, TextRenderer tr, int cx, int cy, float fade, boolean hover, float dt);

        public void click(double mx, int button) {}

        public void drag(double mx) {}

        public boolean drags() {
            return false;
        }

        static void pill(DrawContext c, int cx, int cy, int w, float fade, boolean hover) {
            Hypr.rrect(c, cx, cy, w, H, H / 2, Hypr.alpha(hover ? Hypr.OVERLAY : Hypr.SURFACE, fade));
        }
    }

    private static final class Toggle extends Row {
        private final BooleanSupplier get;
        private final Consumer<Boolean> set;
        private float on = -1;

        Toggle(String name, String desc, BooleanSupplier get, Consumer<Boolean> set) {
            super(name, desc);
            this.get = get;
            this.set = set;
        }

        @Override
        public int width(TextRenderer tr) {
            return 24;
        }

        @Override
        public void draw(DrawContext c, TextRenderer tr, int cx, int cy, float fade, boolean hover, float dt) {
            float target = get.getAsBoolean() ? 1 : 0;
            on = on < 0 ? target : Hypr.approach(on, target, dt, 22);
            if (on < 0.98f) { // settled toggles are one layer, not two
                Hypr.rrect(c, cx, cy + 1, 24, 12, 6, Hypr.alpha(Hypr.OVERLAY, fade));
            }
            if (on > 0.02f) {
                Hypr.rrect(c, cx, cy + 1, 24, 12, 6, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), cx, 24, fade * on), 8);
            }
            Hypr.rrect(c, cx + 2 + Math.round(on * 12), cy + 3, 8, 8, 4, Hypr.alpha(0xFFF5F5FF, fade));
        }

        @Override
        public void click(double mx, int button) {
            set.accept(!get.getAsBoolean());
        }
    }

    private static final class Cycle<E extends Enum<E>> extends Row {
        private final E[] values;
        private final Function<E, String> text;
        private final Supplier<E> get;
        private final Consumer<E> set;
        private int cw = -1;

        Cycle(String name, String desc, E[] values, Function<E, String> text, Supplier<E> get, Consumer<E> set) {
            super(name, desc);
            this.values = values;
            this.text = text;
            this.get = get;
            this.set = set;
        }

        @Override
        public int width(TextRenderer tr) {
            if (cw < 0) {
                for (E v : values) {
                    cw = Math.max(cw, tr.getWidth(Foe.smallCaps(text.apply(v))) + 14);
                }
            }
            return cw;
        }

        @Override
        public void draw(DrawContext c, TextRenderer tr, int cx, int cy, float fade, boolean hover, float dt) {
            pill(c, cx, cy, width(tr), fade, hover);
            String s = Foe.smallCaps(text.apply(get.get()));
            c.drawTextWithShadow(tr, s, cx + (width(tr) - tr.getWidth(s)) / 2, cy + 3, Hypr.alpha(Hypr.TEXT, fade));
        }

        @Override
        public void click(double mx, int button) {
            set.accept(values[Math.floorMod(get.get().ordinal() + (button == 1 ? -1 : 1), values.length)]);
        }
    }

    private static final class Slider extends Row {
        private static final int TRACK = 70;
        private final double min, max, step;
        private final String unit;
        private final DoubleSupplier get;
        private final DoubleConsumer set;
        private float shown = -1;
        private int trackX;

        Slider(String name, String desc, double min, double max, double step, String unit, DoubleSupplier get, DoubleConsumer set) {
            super(name, desc);
            this.min = min;
            this.max = max;
            this.step = step;
            this.unit = unit;
            this.get = get;
            this.set = set;
        }

        @Override
        public int width(TextRenderer tr) {
            return 32 + TRACK;
        }

        @Override
        public void draw(DrawContext c, TextRenderer tr, int cx, int cy, float fade, boolean hover, float dt) {
            double v = get.getAsDouble();
            float target = (float) ((v - min) / (max - min));
            shown = shown < 0 ? target : Hypr.approach(shown, target, dt, 24);
            String s = step >= 1 ? (int) Math.round(v) + unit : String.format(Locale.ROOT, "%.2f", v) + unit;
            c.drawTextWithShadow(tr, s, cx + 26 - tr.getWidth(s), cy + 3, Hypr.alpha(Hypr.SUB, fade));
            trackX = cx + 32;
            Hypr.rrect(c, trackX, cy + 5, TRACK, 4, 2, Hypr.alpha(Hypr.OVERLAY, fade));
            int knob = trackX + Math.round(Hypr.clamp01(shown) * (TRACK - 8));
            Hypr.rrect(c, trackX, cy + 5, knob - trackX + 4, 4, 2, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), trackX, TRACK, fade), 8);
            Hypr.rrect(c, knob, cy + 3, 8, 8, 4, Hypr.alpha(0xFFF5F5FF, fade));
        }

        @Override
        public void click(double mx, int button) {
            drag(mx);
        }

        @Override
        public void drag(double mx) {
            double frac = Math.max(0, Math.min(1, (mx - trackX - 4) / (TRACK - 8)));
            double v = min + Math.round(frac * (max - min) / step) * step;
            set.accept(Math.max(min, Math.min(max, v)));
        }

        @Override
        public boolean drags() {
            return true;
        }
    }

    private static final class Color extends Row {
        private final IntSupplier get;
        private final IntConsumer set;

        Color(String name, String desc, IntSupplier get, IntConsumer set) {
            super(name, desc);
            this.get = get;
            this.set = set;
        }

        @Override
        public int width(TextRenderer tr) {
            return 72;
        }

        @Override
        public void draw(DrawContext c, TextRenderer tr, int cx, int cy, float fade, boolean hover, float dt) {
            int rgb = get.getAsInt() & 0xFFFFFF;
            c.drawTextWithShadow(tr, String.format(Locale.ROOT, "#%06x", rgb), cx, cy + 3, Hypr.alpha(Hypr.SUB, fade));
            Hypr.rrect(c, cx + 44, cy, 28, H, 7, Hypr.alpha(hover ? Hypr.TEXT : Hypr.OVERLAY, fade));
            Hypr.rrect(c, cx + 45, cy + 1, 26, H - 2, 6, Hypr.alpha(0xFF000000 | rgb, fade));
        }

        @Override
        public void click(double mx, int button) {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(new ColorPickerScreen(mc.currentScreen, name, get.getAsInt(), rgb -> set.accept(rgb & 0xFFFFFF)));
        }
    }

    private static final class Action extends Row {
        private final String button;
        private final Runnable press;

        Action(String name, String desc, String button, Runnable press) {
            super(name, desc);
            this.button = Foe.smallCaps(button);
            this.press = press;
        }

        @Override
        public int width(TextRenderer tr) {
            return tr.getWidth(button) + 14;
        }

        @Override
        public void draw(DrawContext c, TextRenderer tr, int cx, int cy, float fade, boolean hover, float dt) {
            int w = width(tr);
            if (hover) {
                Hypr.rrect(c, cx, cy, w, H, H / 2, Hypr.horizontal(Hypr.acc1(), Hypr.acc2(), cx, w, fade), 8);
            } else {
                pill(c, cx, cy, w, fade, false);
            }
            c.drawTextWithShadow(tr, button, cx + 7, cy + 3, Hypr.alpha(hover ? Hypr.CRUST : Hypr.TEXT, fade));
        }

        @Override
        public void click(double mx, int button) {
            press.run();
        }
    }

    private final TackleboxSettings s = TackleboxSettings.get();
    private final List<Page> pages = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private String pageName, pageIcon, tileName, tileIcon;
    private Row main;

    private SettingsPages() {}

    /** A fresh set of pages reading and writing the live settings. */
    public static List<Page> build() {
        SettingsPages p = new SettingsPages();
        p.page("look", "brush");
        p.lookPage();
        p.page("menus", "book");
        p.menusPage();
        p.page("huds", "spyglass");
        p.hudsPage();
        p.page("inventory", "bundle");
        p.inventoryPage();
        p.page("fishing", "fishing_rod");
        p.fishingPage();
        p.page("chat", "writable_book");
        p.chatPage();
        p.page("world", "oak_sapling");
        p.worldPage();
        p.page("tools", "iron_pickaxe");
        p.toolsPage();
        p.page(null, null);
        return p.pages;
    }

    private void page(String name, String icon) {
        header(null, null);
        if (pageName != null) {
            pages.add(new Page(pageName, pageIcon, new ArrayList<>(tiles)));
        }
        tiles.clear();
        pageName = name;
        pageIcon = icon;
    }

    private void header(String name, String icon) {
        if (tileName != null && !rows.isEmpty()) {
            tiles.add(new Tile(tileName, tileIcon, Foe.bold(tileName), new ArrayList<>(rows), main));
        }
        rows.clear();
        main = null;
        tileName = name;
        tileIcon = icon;
    }

    private void dropToggle(dev.landofif.tacklebox.trigger.RareDrops.Row r, String part, java.util.Set<String> off) {
        toggle(r.label() + ": " + part, "the " + part + " for " + r.desc(),
                () -> !off.contains(r.key()), v -> {
                    if (v) off.remove(r.key());
                    else off.add(r.key());
                });
    }

    /** A row that opens one of the mods screens; closing it returns here. */
    private void link(String name, String desc, Function<Screen, Screen> screen) {
        action(name, desc, "open", () -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            Screen next = screen.apply(mc.currentScreen);
            if (next != null) {
                mc.setScreen(next);
            }
        });
    }

    /** A link on the tools page, with its item. */
    private void tool(String name, String icon, String desc, Function<Screen, Screen> screen) {
        link(name, desc, screen);
        rows.get(rows.size() - 1).icon = icon;
    }

    /** The row just added switches the whole tile on and off, so the grid shows it on the tile. */
    private void main() {
        main = rows.get(rows.size() - 1);
    }

    private void action(String name, String desc, String button, Runnable press) {
        rows.add(new Action(name, desc, button, press));
    }

    private void toggle(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
        rows.add(new Toggle(name, desc, get::get, set));
    }

    private <E extends Enum<E>> void cycle(String name, String desc, E[] values, Function<E, String> label,
                                           Supplier<E> get, Consumer<E> set) {
        rows.add(new Cycle<>(name, desc, values, label, get, set));
    }

    private void slider(String name, String desc, double min, double max, double step, String unit,
                        DoubleSupplier get, DoubleConsumer set) {
        rows.add(new Slider(name, desc, min, max, step, unit, get, set));
    }

    private void color(String name, String desc, IntSupplier get, IntConsumer set) {
        rows.add(new Color(name, desc, get, set));
    }

    private void lookPage() {
        header("accent", "cyan_dye");
        color("accent colour", "the first colour of the gradient on focused tiles, hovered buttons, toggles and sliders in every screen of the mod.",
                () -> s.accent1, v -> s.accent1 = v);
        color("accent colour 2", "the second colour of that gradient. use the same as the first for a flat accent.",
                () -> s.accent2, v -> s.accent2 = v);
        header("pause menu", "oak_door");
        toggle("ask before disconnect", "the disconnect button in the pause menu asks yes or no first.",
                () -> s.confirmDisconnect, v -> s.confirmDisconnect = v);
        main();
        header("hotbar", "item_frame");
        cycle("hotbar look", "what sits behind the hotbar slots: nothing, the vanilla sprite, a see-through fill, a thin outline or both. the vanilla sprite only fits 20 pixel spacing.",
                TackleboxSettings.BoxLook.values(), k -> k.label, () -> s.hotbarLook, v -> s.hotbarLook = v);
        slider("hotbar spacing", "the distance between hotbar slots in pixels. 20 is vanilla, 16 puts the items right next to each other. not used by the vanilla sprite look.", 16, 24, 1, "",
                () -> s.hotbarSpacing, v -> s.hotbarSpacing = (int) Math.round(v));
        slider("hotbar size", "how big the hotbar is. it grows from the bottom middle.", 50, 200, 5, "%",
                () -> s.hotbarScale, v -> s.hotbarScale = (int) Math.round(v));
        cycle("selected slot", "how the slot you hold is marked: the vanilla frame, a round ring, a simple line under the item, or nothing.",
                TackleboxSettings.SelectionStyle.values(), k -> k.label, () -> s.hotbarSelection, v -> s.hotbarSelection = v);
        color("selected colour", "the colour of the round ring and the simple line. the frame keeps its own.",
                () -> s.hotbarSelectColor, v -> s.hotbarSelectColor = v);
        toggle("offhand box", "the box next to the hotbar for your offhand item.",
                () -> s.hotbarOffhand, v -> s.hotbarOffhand = v);
        color("box outline colour", "the colour of the thin outline around the hotbar and equipment boxes.",
                () -> s.boxOutlineColor, v -> s.boxOutlineColor = v);
        slider("box fill opacity", "how strong the accent gradient fill behind the hotbar and equipment is. 0 is see-through.", 0, 100, 5, "%",
                () -> s.boxOpacity, v -> s.boxOpacity = (int) Math.round(v));
    }

    private void menusPage() {
        header("menus", "cod");
        toggle("simple menus", "chests, your inventory and fishonmc menus with no box and no slot boxes, just the items. hides the menu art from the server pack.",
                () -> s.flatMenus, v -> s.flatMenus = v);
        color("menu background", "the colour behind chests, your inventory and fishonmc menus when simple menus is on.",
                () -> s.menuBgColor, v -> s.menuBgColor = v);
        slider("menu background opacity", "how strong that colour is. 0 is see-through.", 0, 100, 5, "%",
                () -> s.menuBgOpacity, v -> s.menuBgOpacity = (int) Math.round(v));
        slider("menu picture opacity", "how strong your own picture is behind the menus. 0 hides it.", 0, 100, 5, "%",
                () -> s.menuImageOpacity, v -> s.menuImageOpacity = (int) Math.round(v));
        action("menu picture", "the picture shown behind the menus, config/tacklebox/menu.png. opens the folder.", "open folder", () -> {
            try {
                java.nio.file.Files.createDirectories(dev.landofif.tacklebox.display.MenuBackground.IMAGE.getParent());
            } catch (java.io.IOException ignored) {
            }
            net.minecraft.util.Util.getOperatingSystem().open(dev.landofif.tacklebox.display.MenuBackground.IMAGE.getParent());
        });
        toggle("flat buttons", "every button as a flat see-through fill instead of the bevelled texture, in the game menus too.",
                () -> s.flatButtons, v -> s.flatButtons = v);
        toggle("rarity circles", "a see-through circle in the rarity colour behind items in your inventory. foecollabs corner mark has its own setting.",
                () -> s.rarityCircles, v -> s.rarityCircles = v);
        cycle("rarity shape", "the rarity mark as a circle behind the item, or a square around it.",
                TackleboxSettings.RarityShape.values(), e -> e.label, () -> s.rarityShape, v -> s.rarityShape = v);
        slider("rarity opacity", "how strong the rarity circles and squares are. 0 hides them.", 0, 100, 5, "%",
                () -> s.rarityOpacity, v -> s.rarityOpacity = (int) Math.round(v));
        toggle("square gradient", "the rarity square fades toward its top, clear at the top. only for the square shape.",
                () -> s.rarityGradient, v -> s.rarityGradient = v);
        toggle("fishonmc menu", "the main menu, /travel, quests, stats and crew as one dark panel with tabs and rows. off gives the chest back.",
                () -> s.fishMenu, v -> s.fishMenu = v);
        slider("menu size", "how much of the screen the fishonmc menu fills.", 40, 100, 1, "%",
                () -> s.menuSize, v -> s.menuSize = (int) Math.round(v));
        toggle("travel map", "/tt, /travel and /turbotravel open as a world map: every place sits where it is in the world, left click travels, right click shows its species, the wheel zooms and a drag moves the map. a button on it shows the tile menu instead. off gives the tiles or the chest back.",
                () -> s.travelMap, v -> s.travelMap = v);
        header("presets", "armor_stand");
        toggle("presets screen", "/presets as a wardrobe of cards with player models. off gives the chest back.",
                () -> s.presetsMenu, v -> s.presetsMenu = v);
        main();
        slider("presets size", "how much of the screen the presets screen fills.", 40, 100, 1, "%",
                () -> s.presetsSize, v -> s.presetsSize = (int) Math.round(v));
        toggle("other page boxes", "draws the card boxes behind the dimmed presets of the other page. off shows only the models.",
                () -> s.presetsOtherBg, v -> s.presetsOtherBg = v);
        header("chest menus", "chest_minecart");
        slider("chest menu scale", "the gui scale of server chest menus while one is open, in whole steps like the games gui scale. the hud behind it changes with it. 0 keeps the games scale.", 0, 6, 1, "",
                () -> s.chestScale, v -> s.chestScale = (int) Math.round(v));
        toggle("page wheel", "the mouse wheel turns the pages of paged server menus, like the compendium and the auction house: down clicks next, up clicks back. not in quest menus.",
                () -> s.pageWheel, v -> s.pageWheel = v);
        header("vaults", "iron_door");
        toggle("vault overview", "/pv as all seven personal vaults on one screen with a search. a vault shows what it held when you last opened it. off gives the chest back.",
                () -> s.vaultMenu, v -> s.vaultMenu = v);
        main();
        header("tooltips", "oak_sign");
        toggle("short fish tooltips", "a caught fish with no scientific name, value or ecosystem line, no kg or cm, weight and length on one line and the rarity mark next to the name. only changes what you see.",
                () -> s.shortFishTooltips, v -> s.shortFishTooltips = v);
        toggle("short pet tooltips", "a pet with no line for its kind, no equipped skin, item or trail line when nothing is equipped, no right click hint, and the rarity mark next to the name. only changes what you see.",
                () -> s.shortPetTooltips, v -> s.shortPetTooltips = v);
        toggle("scroll tall tooltips", "a tooltip taller than the screen starts at its top and the mouse wheel scrolls it.",
                () -> s.tooltipScroll, v -> s.tooltipScroll = v);
        toggle("pet profit marks", "in the auction house, a pet you could sell to the pet store for more than it costs gets a green tint, and every pet gets a line with what the store pays. only common and rare pets, the other rarities are not known yet. only changes what you see.",
                () -> s.petProfitMarks, v -> s.petProfitMarks = v);
    }

    private void hudsPage() {
        header("quests left", "map");
        toggle("quests left hud", "quests until your next level and the next level of your location. it learns from /menu, /quests and the compendium. move it with move huds.",
                () -> s.questsHud, v -> s.questsHud = v);
        main();
        toggle("quests left heading", "the quests left title above the hud.", () -> s.questsHeading, v -> s.questsHeading = v);
        cycle("quests left align", "how the text of the quests left hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.questsAlign, v -> s.questsAlign = v);
        header("level progress", "experience_bottle");
        toggle("level progress hud", "two thin bars: your fishing level (from the vanilla xp bar) and the level of the location you are at. the location bar starts when you open /comp once and then follows the location xp of quest rewards and chat. move it with move huds.",
                () -> s.levelBars, v -> s.levelBars = v);
        main();
        toggle("level progress heading", "the green level progress title above the bars.",
                () -> s.levelBarsHeading, v -> s.levelBarsHeading = v);
        toggle("level progress fishing line", "the fishing level bar.",
                () -> s.levelBarsFishing, v -> s.levelBarsFishing = v);
        toggle("level progress location line", "the bar for the location you are at.",
                () -> s.levelBarsLocation, v -> s.levelBarsLocation = v);
        toggle("level progress bar", "the thin bar itself. off leaves just the level, the percent and the location xp as text.",
                () -> s.levelBarsBar, v -> s.levelBarsBar = v);
        toggle("level progress percent", "the percent after each bar.",
                () -> s.levelBarsPercent, v -> s.levelBarsPercent = v);
        toggle("level progress location xp", "the location xp into the level over what it needs, after the location bar.",
                () -> s.levelBarsXp, v -> s.levelBarsXp = v);
        toggle("level progress main xp", "the xp into the fishing level over what it needs, after the fishing bar.",
                () -> s.levelBarsMainXp, v -> s.levelBarsMainXp = v);
        toggle("level progress decimals", "decimal places in the location xp, 367K instead of 367.27K.",
                () -> s.levelBarsDecimals, v -> s.levelBarsDecimals = v);
        slider("level progress bar width", "how long the bars are, in pixels.", 40, 200, 10, "",
                () -> s.levelBarsWidth, v -> s.levelBarsWidth = (int) v);
        cycle("level progress align", "how the text of the level progress hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.levelBarsAlign, v -> s.levelBarsAlign = v);
        header("active boosts", "firework_rocket");
        toggle("active boosts hud", "what is on right now: player boosters with a countdown, the chummer, and the weather events and moon. read from chat lines only. the server hud shows the events and moon too, switch its weather and moon lines off if you only want them here. move it with move huds.",
                () -> s.boostsHud, v -> s.boostsHud = v);
        main();
        toggle("active boosts heading", "the active title above the boosts.", () -> s.boostsHeading, v -> s.boostsHeading = v);
        cycle("active boosts align", "how the text of the active boosts hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.boostsAlign, v -> s.boostsAlign = v);
        header("event", "nether_star");
        toggle("event hud", "the points, the next threshold with its rewards, your placement and your missions of the running event. the points and thresholds are read from /event when you open it and then follow the chat. move it with move huds.",
                () -> s.eventHud, v -> s.eventHud = v);
        main();
        toggle("event heading", "the event's name above the hud.", () -> s.eventHeading, v -> s.eventHeading = v);
        cycle("event align", "how the text of the event hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.eventAlign, v -> s.eventAlign = v);
        toggle("event missions", "lists the missions that are not done yet and the repeatable ones on the event hud, coloured like the menu, with the points earned and the points one catch gives. read from the mission book in /event and then follow the chat.",
                () -> s.eventMissions, v -> s.eventMissions = v);
        header("mini event", "glowstone_dust");
        toggle("mini event hud", "the running mini event: when it ends, when the daily missions reroll, and your missions with their progress. while this is on the mod sends /minievent by itself every 10 minutes and a few seconds after a catch and hides the answer, and counts your catches in between. off sends nothing. move it with move huds.",
                () -> s.miniEventHud.on, v -> s.miniEventHud.on = v);
        main();
        toggle("mini event heading", "the mini event's name above the hud.", () -> s.miniEventHud.heading, v -> s.miniEventHud.heading = v);
        cycle("mini event align", "how the text of the mini event hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.miniEventHud.align, v -> s.miniEventHud.align = v);
        header("cryptids", "ender_eye");
        toggle("cryptids hud", "what can be caught where right now (moons, rainbow, storms, fabled, and the cryptids of the in-game clock) with the odds and the drops, then when the next moon and clock window come. read from chat and the clock in the top bar. move it with move huds.",
                () -> s.cryptidHud, v -> s.cryptidHud = v);
        main();
        toggle("cryptids heading", "the cryptids title above the hud.", () -> s.cryptidHeading, v -> s.cryptidHeading = v);
        cycle("cryptids align", "how the text of the cryptids hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.cryptidAlign, v -> s.cryptidAlign = v);
        toggle("cryptids popups", "a title when one of the events below starts, saying where to go and what for. these are not triggers, nothing needs adding in /triggers.",
                () -> s.cryptidPopups, v -> s.cryptidPopups = v);
        for (String[] k : new String[][]{{"moons", "rougarou at toledo and bessie at the great lakes during any moon, and the infusion capsule in a blood moon."},
                {"rainbow", "garaboncias at the danube."}, {"storms", "lightning in a bottle anywhere, 1/500 in a thunderstorm and 1/250 in a supercell."},
                {"fabled", "the fabled fish at the place the chat names."}, {"bigfoot", "cypress between 1 and 3 am in-game."},
                {"titanoboa", "the amazon between 2 and 4 am in-game."}, {"bessie", "the great lakes between 4 and 6 am in-game."}}) {
            toggle("cryptids: " + k[0], "shows " + k[0] + " on the cryptids hud and its popup: " + k[1],
                    () -> !s.cryptidOff.contains(k[0]), v -> {
                        if (v) s.cryptidOff.remove(k[0]);
                        else s.cryptidOff.add(k[0]);
                    });
        }
        header("timers", "clock");
        toggle("timers hud", "countdowns to the next tackle shop restock (every 4 hours) and the cosmetic store reset (midnight utc), and your votes of today (back to 0 at midnight utc). a title shows on every restock and reset whether this hud is on or not. move it with move huds.",
                () -> s.timersHud.on, v -> s.timersHud.on = v);
        main();
        toggle("timers heading", "the timers title above the hud.", () -> s.timersHud.heading, v -> s.timersHud.heading = v);
        cycle("timers align", "how the text of the timers hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.timersHud.align, v -> s.timersHud.align = v);
        toggle("timers votes", "the votes line: how many of the 7 vote sites you voted on today, counted from the vote reward lines in chat. it goes back to 0 at midnight utc.",
                () -> s.timersVotes, v -> s.timersVotes = v);
        header("wrong gear", "barrier");
        toggle("wrong gear warning", "a red warning on the hud while your equipped pet is not the pet of the place you are at, or your armor is for another climate. it names the right pet and says if it is in your inventory. nothing at the cypress lake spawn. the pet is learned from the chat line when you equip it. move it with move huds.",
                () -> s.gearHud.on, v -> s.gearHud.on = v);
        main();
        toggle("wrong gear heading", "the warning title above the hud.", () -> s.gearHud.heading, v -> s.gearHud.heading = v);
        toggle("wrong gear chat line", "one chat line a moment after you arrive somewhere with the wrong pet or armor. only you see it.",
                () -> s.gearChat, v -> s.gearChat = v);
        cycle("wrong gear align", "how the text of the wrong gear warning lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.gearHud.align, v -> s.gearHud.align = v);
        header("server hud", "oak_hanging_sign");
        toggle("server hud", "a sidebar styled hud: time, location, wallet and credits, the tackle shop and moon timers, and the weather events and moon that are active. move and resize it with move huds.",
                () -> s.serverHud, v -> s.serverHud = v);
        main();
        cycle("server hud align", "how the text of the server hud (the title stays centred) lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.serverHudAlign, v -> s.serverHudAlign = v);
        link("choose lines", "which lines the server hud shows, in what order.", ServerHudScreen::new);
        header("now playing", "music_disc_cat");
        toggle("now playing hud", "shows what is playing on your computer (spotify, youtube in a browser, anything that works with playerctl): title, artist and the time. linux only, needs playerctl installed. shows only, it never controls the player. move it with move huds.",
                () -> s.nowPlaying, v -> s.nowPlaying = v);
        main();
        toggle("now playing heading", "the green now playing title above the track.",
                () -> s.nowPlayingHeading, v -> s.nowPlayingHeading = v);
        toggle("now playing album art", "the cover next to the text. it is fetched once per track from the address the player gives (spotify, youtube thumbnails).",
                () -> s.nowPlayingArt, v -> s.nowPlayingArt = v);
        toggle("now playing artist", "the artist or channel under the title.",
                () -> s.nowPlayingArtist, v -> s.nowPlayingArtist = v);
        toggle("now playing progress bar", "a thin bar showing how far into the track you are. not shown for live streams.",
                () -> s.nowPlayingBar, v -> s.nowPlayingBar = v);
        toggle("now playing time", "the time played and the length of the track.",
                () -> s.nowPlayingTime, v -> s.nowPlayingTime = v);
        cycle("now playing align", "how the text of the now playing hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.nowPlayingAlign, v -> s.nowPlayingAlign = v);
    }

    private void inventoryPage() {
        header("equipment", "iron_chestplate");
        toggle("equipment hud", "your chestplate, leggings and boots and the reel, pole and line of your rod as two rounded groups above the hotbar. off brings back foecollabs own row.",
                () -> s.equipHud, v -> s.equipHud = v);
        main();
        slider("equipment size", "how big foecollabs armour and rod parts row is. move it with move huds.", 50, 200, 5, "%",
                () -> s.equipScale, v -> s.equipScale = (int) Math.round(v));
        cycle("equipment place", "where the armour and rod parts row sits: above the hotbar, or level with it on its left or right. you can still drag it from there.",
                TackleboxSettings.EquipPlace.values(), k -> k.label, () -> s.equipPlace, v -> s.equipPlace = v);
        cycle("equipment look", "what sits behind foecollabs armour and rod parts: nothing, foecollabs own squares, or a fill, outline or both around each group of three.",
                TackleboxSettings.BoxLook.values(), k -> k.label, () -> s.equipLook, v -> s.equipLook = v);
        slider("equipment spacing", "the distance between equipment slots in pixels. 22 is foecollabs own. not used by its own squares look.", 16, 26, 1, "",
                () -> s.equipSpacing, v -> s.equipSpacing = (int) Math.round(v));
        toggle("equipment circles", "a circle in the rarity colour behind each equipment item, instead of foecollabs bar under it.",
                () -> s.equipCircle, v -> s.equipCircle = v);
        toggle("equipment colour", "uses one colour for the circle or bar instead of the rarity colour.",
                () -> s.equipRecolor, v -> s.equipRecolor = v);
        color("equipment colour pick", "the colour used when equipment colour is on.",
                () -> s.equipColor, v -> s.equipColor = v);
        header("inventory hud", "hopper");
        toggle("inventory hud", "your inventory on the hud without opening it: the 27 slots above the hotbar as three rows of nine, with the item counts. display only. hidden while a chest or the inventory is open. move and resize it with move huds.",
                () -> s.inventoryHud.on, v -> s.inventoryHud.on = v);
        main();
        toggle("inventory hud slots", "a faint dark square behind every slot of the inventory hud, so empty slots can be told apart.",
                () -> s.inventoryHudCells, v -> s.inventoryHudCells = v);
        header("slots left", "shulker_shell");
        toggle("slots left hud", "how many inventory slots are still empty (hotbar and inventory, 36 in all), shown once that is at or under the number below. red at 0. move it with move huds.",
                () -> s.slotsLeftHud.on, v -> s.slotsLeftHud.on = v);
        main();
        toggle("slots left heading", "the inventory title above the hud.", () -> s.slotsLeftHud.heading, v -> s.slotsLeftHud.heading = v);
        slider("slots left shows at", "the slots left hud shows once this many or fewer slots are empty. 36 shows it always, 0 only when the inventory is full.", 0, 36, 1, "",
                () -> s.slotsLeftAt, v -> s.slotsLeftAt = (int) Math.round(v));
        cycle("slots left align", "how the text of the slots left hud lines up inside it: left, centre or right.",
                TackleboxSettings.HudAlign.values(), k -> k.label, () -> s.slotsLeftHud.align, v -> s.slotsLeftHud.align = v);
    }

    private void fishingPage() {
        header("bobber", "slime_ball");
        toggle("still bobber", "keeps your bobber still on the water. it still dips on a bite.",
                () -> s.stillBobber, v -> s.stillBobber = v);
        toggle("smooth cast", "on a laggy connection your cast flies smoothly instead of jumping back. the landing spot stays exact.",
                () -> s.smoothCast, v -> s.smoothCast = v);
        toggle("hide other bobbers", "hides other players bobbers and their lines.",
                () -> s.hideOtherBobbers, v -> s.hideOtherBobbers = v);
        slider("rod not cast after", "how long your rod can be in hand with no bobber out before the rod not cast trigger fires. add that trigger from the presets in chat triggers. it never casts for you.",
                RodIdle.MIN_SECONDS, RodIdle.MAX_SECONDS, 5, "s", () -> s.rodIdleSeconds, v -> s.rodIdleSeconds = (int) v);
        header("skin", "pufferfish");
        cycle("bobber skin", "swaps your bobber for a skin. only you see it.",
                TackleboxSettings.BobberSkin.values(), k -> k.label, () -> s.bobberSkin, v -> s.bobberSkin = v);
        toggle("skin every bobber", "uses your skin on other players bobbers too, on your screen only.",
                () -> s.skinEveryone, v -> s.skinEveryone = v);
        header("line", "string");
        toggle("custom line colour", "colours your own fishing line.",
                () -> s.customLineColor, v -> s.customLineColor = v);
        main();
        color("line colour", "the colour of your fishing line.",
                () -> s.lineColor, v -> s.lineColor = v);
        header("water", "prismarine_crystals");
        toggle("water rings", "thin rings on the water at your own bobber: quicker ones while a fish swims in, a small wake behind the fish and one ring where you caught it. they grow a little and fade. only your own bobber and fish.",
                () -> s.waterFx, v -> s.waterFx = v);
        main();
        toggle("bite rings", "rings at your bobber while a fish swims to it, coming quicker as it gets close, and a bigger one on the bite. they follow the same fish particles as the bite countdown.",
                () -> s.waterBiteRings, v -> s.waterBiteRings = v);
        toggle("fish wake", "small fainter rings behind the fish as it swims to your bobber.",
                () -> s.waterWake, v -> s.waterWake = v);
        toggle("catch ring", "one ring where your bobber was when you catch a fish, in its rarity colour. commons get a faint white one.",
                () -> s.waterCatchRing, v -> s.waterCatchRing = v);
        toggle("catch sparkles", "a few slow dust sparkles in the rarity colour where you caught a legendary fish or better. switching off the dust particle hides them too.",
                () -> s.waterSparkles, v -> s.waterSparkles = v);
        slider("water ring opacity", "how strong the rings are when they start.", 5, 100, 5, "%",
                () -> s.waterOpacity, v -> s.waterOpacity = (int) Math.round(v));
        color("water ring colour", "the colour of the bite rings and the wake. the catch ring takes the fish's rarity colour.",
                () -> s.waterColor, v -> s.waterColor = v);
        header("catch title", "tropical_fish");
        toggle("catch title", "when you catch a fish: its picture, under it the rarity badge and the name, under that the size with weight and length. the lines come from the fish in your inventory, so with a full inventory only the server's own line shows. move it with move huds.",
                () -> s.catchTitle, v -> s.catchTitle = v);
        main();
        toggle("catch title hides the server's", "hides the server's own big catch title and its subtitle while the catch title is on. foecollab's catch title is switched off in foecollab's own settings.",
                () -> s.catchHideTitle, v -> s.catchHideTitle = v);
        toggle("catch title picture", "the picture of the caught fish on top, the same one the server shows.",
                () -> s.catchImage, v -> s.catchImage = v);
        slider("catch title picture size", "how tall the picture is. 100 is 32 pixels.", 25, 300, 5, "%",
                () -> s.catchImageSize, v -> s.catchImageSize = (int) Math.round(v));
        toggle("catch title rarity", "the rarity badge before the name.",
                () -> s.catchRarity, v -> s.catchRarity = v);
        toggle("catch title variant", "the badge of a fish that is not normal (albino, melanistic, trophy, fabled, event variants) before the name.",
                () -> s.catchVariant, v -> s.catchVariant = v);
        toggle("catch title name", "the name of the fish.",
                () -> s.catchName, v -> s.catchName = v);
        toggle("catch title name in rarity colour", "the name in the colour of its rarity instead of the text colour.",
                () -> s.catchNameRarity, v -> s.catchNameRarity = v);
        toggle("catch title size class", "baby, juvenile, adult, large or gigantic on the second line, in its own colour.",
                () -> s.catchSize, v -> s.catchSize = v);
        toggle("catch title weight", "the weight in lb on the second line.",
                () -> s.catchWeight, v -> s.catchWeight = v);
        toggle("catch title length", "the length in inches on the second line.",
                () -> s.catchLength, v -> s.catchLength = v);
        color("catch title text colour", "the colour of the name and of the weight and length.",
                () -> s.catchColor, v -> s.catchColor = v);
        toggle("catch title shadow", "a shadow behind the text. the pictures never get one.",
                () -> s.catchShadow, v -> s.catchShadow = v);
        slider("catch title seconds", "how long the catch title stays.", 0.5, 10, 0.5, "s",
                () -> s.catchSeconds, v -> s.catchSeconds = v);
        cycle("catch title animation", "how the catch title comes in.",
                TackleboxSettings.BiteAnim.values(), k -> k.label, () -> s.catchAnim, v -> s.catchAnim = v);
        slider("catch title size", "how big the whole catch title is. scrolling it in move huds does the same.", 50, 400, 10, "%",
                () -> s.catchScale, v -> s.catchScale = (int) Math.round(v));
        header("big catches", "nether_star");
        toggle("big catch moment", "when you catch a fabled, trophy, mythical or gigantic fish: a soft glow in its colour at the edges of the screen, one light sweep over the catch title's name and a sound. nothing moves or flashes.",
                () -> s.bigCatch, v -> s.bigCatch = v);
        main();
        toggle("fabled catches", "fabled fish get the moment. a fish that is several kinds gets the first one that is on: fabled, trophy, mythical, gigantic.",
                () -> s.bigFabled, v -> s.bigFabled = v);
        toggle("trophy catches", "trophy fish get the moment.",
                () -> s.bigTrophy, v -> s.bigTrophy = v);
        toggle("mythical catches", "mythical fish get the moment.",
                () -> s.bigMythical, v -> s.bigMythical = v);
        toggle("gigantic catches", "gigantic fish get the moment. with a full inventory the size is not known, so they get none.",
                () -> s.bigGigantic, v -> s.bigGigantic = v);
        toggle("edge glow", "a soft glow in the fish's colour that fades in and out at the edges of the screen.",
                () -> s.bigGlow, v -> s.bigGlow = v);
        slider("edge glow strength", "how strong the edge glow gets at its peak.", 5, 100, 5, "%",
                () -> s.bigGlowOpacity, v -> s.bigGlowOpacity = (int) Math.round(v));
        toggle("title shine", "one light band sweeps across the name on the catch title. needs the catch title on.",
                () -> s.bigShine, v -> s.bigShine = v);
        toggle("big catch sound", "a sound for each kind: a deep one for fabled, a bell chord for trophy, level up for mythical, an amethyst chime for gigantic.",
                () -> s.bigSound, v -> s.bigSound = v);
        header("rare drops", "prismarine_shard");
        toggle("rare drops", "your own pet drops and rare catches (shards, lightning in a bottle, infusion capsules, prospecting amulets, bigfoot items) show a title, pop their item in the totem animation and play a sound. these are built in, not triggers. other players drops never show.",
                () -> s.rareDrops, v -> s.rareDrops = v);
        main();
        for (dev.landofif.tacklebox.trigger.RareDrops.Row r : dev.landofif.tacklebox.trigger.RareDrops.ROWS) {
            dropToggle(r, "title", s.dropTitleOff);
            dropToggle(r, "totem pop", s.dropPopOff);
            dropToggle(r, "sound", s.dropSoundOff);
        }
        header("chummer", "cod_bucket");
        toggle("chummer range ring", "draws a thin ring on the ground at the range of every placed chummer your client can see, 20 blocks out. read from the chummer's hologram, not from chat.",
                () -> s.chummerRing, v -> s.chummerRing = v);
        main();
        color("ring colour", "the colour of the chummer range ring.",
                () -> s.chummerRingColor, v -> s.chummerRingColor = v);
    }

    private void chatPage() {
        header("chat", "paper");
        toggle("hide login messages", "hides the welcome lines fishonmc prints every time you join: the instance, the fish and shader pack lines, the welcome, help, rules and discord lines.",
                () -> s.hideLoginChat, v -> s.hideLoginChat = v);
        toggle("hide empty lines", "hides chat lines with nothing in them, the blank gaps the server sends around some messages. announcements lose their spacing lines too.",
                () -> s.hideEmptyChat, v -> s.hideEmptyChat = v);
        toggle("hide huds behind chat", "huds that would sit under the chat, a chat box or the player list are hidden while they overlap. off by default.",
                () -> s.hideBehindChat, v -> s.hideBehindChat = v);
        header("dry streaks", "dried_kelp");
        for (dev.landofif.tacklebox.chat.DryStreaks.Kind k : dev.landofif.tacklebox.chat.DryStreaks.KINDS) {
            toggle("dry streak: " + k.label(), "a chat line when you get " + k.desc() + ", saying how many catches it took. only you see it.",
                    () -> s.dryOn.contains(k.key()), v -> {
                        if (v) s.dryOn.add(k.key());
                        else s.dryOn.remove(k.key());
                    });
        }
        link("dry streak texts", "what each dry streak line says, with %item% and %streak%.", DryStreakScreen::new);
    }

    private void worldPage() {
        header("nametags", "name_tag");
        toggle("hide my nametag", "hides the rank, name and power tag over your own head. others still see it.",
                () -> s.hideOwnNametag, v -> s.hideOwnNametag = v);
        toggle("hide all nametags", "hides every players nametag, the servers and the vanilla one.",
                () -> s.hideAllNametags, v -> s.hideAllNametags = v);
        header("clutter", "cobweb");
        toggle("hide xp popups", "hides the +66xp line above the hotbar after a catch.",
                () -> s.hideXpPopups, v -> s.hideXpPopups = v);
        toggle("flat leaves", "draws leaves flat like vanilla instead of the packs 3d leaves. reloads resources.",
                () -> s.flatLeaves, v -> {
                    if (s.flatLeaves != v) {
                        s.flatLeaves = v;
                        net.minecraft.client.MinecraftClient.getInstance().reloadResources();
                    }
                });
        toggle("black sky", "makes the sky flat black, always midnight to your eyes: no sun, moon, stars, sunsets or rain. the server's real time and weather are untouched.",
                () -> s.blackSky, v -> s.blackSky = v);
        toggle("hide block outline", "hides the outline around the block you are looking at.",
                () -> s.hideBlockOutline, v -> s.hideBlockOutline = v);
        link("particles", "switch off particle types, like rain splashes. lists every particle seen since the game started. a switched off type is hidden for everyone, your own too.", ParticleScreen::new);
        header("crowds", "totem_of_undying");
        toggle("hide nearby players", "while your line is out, players near you or your bobber are not drawn, nor their nametags. crewmates and friends from foecollab stay. only on your screen.",
                () -> s.hideNearPlayers, v -> s.hideNearPlayers = v);
        slider("nearby range", "how close a player has to be to you or your bobber to be hidden, in blocks.", NearbyPlayers.MIN_RANGE, NearbyPlayers.MAX_RANGE, 1, " blocks",
                () -> s.nearPlayersRange, v -> s.nearPlayersRange = (int) v);
        toggle("only my fish", "hides the fish particles swimming to other players bobbers, so only the fish coming to yours shows.",
                () -> s.ownFishOnly, v -> s.ownFishOnly = v);
        header("lag", "redstone");
        toggle("hide far displays", "stops drawing the servers item, block and text displays (spawn decor, shop signs, holograms) beyond the range below. at spawn this can double your fps. nearby ones stay.",
                () -> s.hideFarDisplays, v -> s.hideFarDisplays = v);
        main();
        slider("display range", "how far away displays still get drawn, in blocks.", DisplayCull.MIN_RANGE, DisplayCull.MAX_RANGE, 2, " blocks",
                () -> s.farDisplayRange, v -> s.farDisplayRange = (int) v);
        header("screenshots", "painting");
        toggle("screenshot preview", "after f2 the screenshot goes to your clipboard and shows in the top right for a few seconds. open chat to click open, which shows it in your image viewer. linux only, needs wl-copy. nothing is uploaded.",
                () -> s.screenshotPreview, v -> s.screenshotPreview = v);
        main();
    }

    private void toolsPage() {
        header("tools", "iron_pickaxe");
        tool("chat triggers", "bell", "", TriggerListScreen::new);
        tool("chat", "paper", "", ChatFilterScreen::new);
        tool("key commands", "tripwire_hook", "", KeyCommandScreen::new);
        tool("command aliases", "knowledge_book", "", AliasScreen::new);
        tool("word swaps", "feather", "", WordSwapScreen::new);
        tool("bait picker", "wheat_seeds", "", BaitScreen::new);
        tool("power", "blaze_powder", "", PowerScreen::new);
        tool("pet prices", "emerald", "what pets sell for between players, by rating and rarity, from the community's pet price sheet. downloaded at most once a day.", PetPricesScreen::new);
        tool("bobber timer", "repeater", "", BobberScreen::new);
        tool("viewmodel", "iron_sword", "", ViewmodelScreen::new);
        tool("move huds", "lead", "", dev.landofif.tacklebox.hud.HudMover::new);
    }
}
