package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.compat.FoeScreens;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.ui.Hypr;
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
 * The mods settings as pages of tiles (see {@link SettingsScreen}): a page is a workspace, a tile one section of it.
 * Each row is a label and one control that applies its value right away.
 */
public final class SettingsPages {
    public record Page(String name, List<Tile> tiles) {}

    public record Tile(String title, Text heading, List<Row> rows) {}

    /** A setting: a label, a tooltip and a control drawn at the right of the row. */
    public abstract static class Row {
        public static final int H = 14;
        public final String label, desc;
        public final String name;
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
                Hypr.rrect(c, cx, cy + 1, 24, 12, 6, Hypr.horizontal(Hypr.ACC1, Hypr.ACC2, cx, 24, fade * on), 8);
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
            Hypr.rrect(c, trackX, cy + 5, knob - trackX + 4, 4, 2, Hypr.horizontal(Hypr.ACC1, Hypr.ACC2, trackX, TRACK, fade), 8);
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
                Hypr.rrect(c, cx, cy, w, H, H / 2, Hypr.horizontal(Hypr.ACC1, Hypr.ACC2, cx, w, fade), 8);
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

    private final DofiSettings s = DofiSettings.get();
    private final List<Page> pages = new ArrayList<>();
    private final List<Tile> tiles = new ArrayList<>();
    private final List<Row> rows = new ArrayList<>();
    private String pageName, tileName;

    private SettingsPages() {}

    /** A fresh set of pages reading and writing the live settings. */
    public static List<Page> build() {
        SettingsPages p = new SettingsPages();
        p.page("menus");
        p.menus();
        p.page("huds");
        p.huds();
        p.serverHud();
        p.page("bobber");
        p.bobber();
        p.page("world");
        p.chat();
        p.world();
        p.page("screens");
        p.screens();
        p.page(null);
        return p.pages;
    }

    private void page(String name) {
        header(null);
        if (pageName != null) {
            pages.add(new Page(pageName, new ArrayList<>(tiles)));
        }
        tiles.clear();
        pageName = name;
    }

    private void header(String name) {
        if (tileName != null && !rows.isEmpty()) {
            tiles.add(new Tile(tileName, Foe.bold(tileName), new ArrayList<>(rows)));
        }
        rows.clear();
        tileName = name;
    }

    private void screens() {
        header("screens");
        link("chat triggers", TriggerListScreen::new);
        link("chat", ChatFilterScreen::new);
        link("key commands", KeyCommandScreen::new);
        link("word swaps", WordSwapScreen::new);
        link("bait picker", BaitScreen::new);
        link("power", PowerScreen::new);
        link("bobber timer", BobberScreen::new);
        link("viewmodel", ViewmodelScreen::new);
        link("move huds", FoeScreens::move);
    }

    private void link(String name, Function<Screen, Screen> screen) {
        link(name, "", screen);
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

    private void menus() {
        header("menus");
        toggle("simple menus", "chests, your inventory and fishonmc menus with no box and no slot boxes, just the items. hides the menu art from the server pack.",
                () -> s.flatMenus, v -> s.flatMenus = v);
        color("menu background", "the colour behind chests, your inventory and fishonmc menus when simple menus is on.",
                () -> s.menuBgColor, v -> s.menuBgColor = v);
        slider("menu background opacity", "how strong that colour is. 0 is see-through.", 0, 100, 5, "%",
                () -> s.menuBgOpacity, v -> s.menuBgOpacity = (int) Math.round(v));
        slider("menu picture opacity", "how strong your own picture is behind the menus. 0 hides it.", 0, 100, 5, "%",
                () -> s.menuImageOpacity, v -> s.menuImageOpacity = (int) Math.round(v));
        action("menu picture", "the picture shown behind the menus, config/dofisaddons/menu.png. opens the folder.", "open folder", () -> {
            try {
                java.nio.file.Files.createDirectories(dev.landofif.dofisaddons.display.MenuBackground.IMAGE.getParent());
            } catch (java.io.IOException ignored) {
            }
            net.minecraft.util.Util.getOperatingSystem().open(dev.landofif.dofisaddons.display.MenuBackground.IMAGE.getParent());
        });
        toggle("flat buttons", "every button as a flat see-through fill instead of the bevelled texture, in the game menus too.",
                () -> s.flatButtons, v -> s.flatButtons = v);
        toggle("rarity circles", "a see-through circle in the rarity colour behind items in your inventory. foecollabs corner mark has its own setting.",
                () -> s.rarityCircles, v -> s.rarityCircles = v);
        toggle("fishonmc menu", "the main menu and /travel as a screen with big icons you can rearrange. off gives the chest back.",
                () -> s.fishMenu, v -> s.fishMenu = v);
        slider("menu size", "how much of the screen the fishonmc menu fills.", 40, 100, 1, "%",
                () -> s.menuSize, v -> s.menuSize = (int) Math.round(v));
        header("presets");
        toggle("presets screen", "/presets as a wardrobe of cards with player models. off gives the chest back.",
                () -> s.presetsMenu, v -> s.presetsMenu = v);
        slider("presets size", "how much of the screen the presets screen fills.", 40, 100, 1, "%",
                () -> s.presetsSize, v -> s.presetsSize = (int) Math.round(v));
        toggle("other page boxes", "draws the card boxes behind the dimmed presets of the other page. off shows only the models.",
                () -> s.presetsOtherBg, v -> s.presetsOtherBg = v);
        header("vaults");
        toggle("vault overview", "/pv as all seven personal vaults on one screen with a search. a vault shows what it held when you last opened it. off gives the chest back.",
                () -> s.vaultMenu, v -> s.vaultMenu = v);
    }

    private void huds() {
        header("hotbar");
        cycle("hotbar look", "what sits behind the hotbar slots: nothing, the vanilla sprite, a see-through fill, a thin outline or both. the vanilla sprite only fits 20 pixel spacing.",
                DofiSettings.BoxLook.values(), k -> k.label, () -> s.hotbarLook, v -> s.hotbarLook = v);
        slider("hotbar spacing", "the distance between hotbar slots in pixels. 20 is vanilla, 16 puts the items right next to each other. not used by the vanilla sprite look.", 16, 24, 1, "",
                () -> s.hotbarSpacing, v -> s.hotbarSpacing = (int) Math.round(v));
        slider("hotbar size", "how big the hotbar is. it grows from the bottom middle.", 50, 200, 5, "%",
                () -> s.hotbarScale, v -> s.hotbarScale = (int) Math.round(v));
        cycle("selected slot", "how the slot you hold is marked: the vanilla frame, a round ring, a simple line under the item, or nothing.",
                DofiSettings.SelectionStyle.values(), k -> k.label, () -> s.hotbarSelection, v -> s.hotbarSelection = v);
        color("selected colour", "the colour of the round ring and the simple line. the frame keeps its own.",
                () -> s.hotbarSelectColor, v -> s.hotbarSelectColor = v);
        toggle("offhand box", "the box next to the hotbar for your offhand item.",
                () -> s.hotbarOffhand, v -> s.hotbarOffhand = v);
        header("equipment");
        slider("equipment size", "how big foecollabs armour and rod parts row is. move it with move huds.", 50, 200, 5, "%",
                () -> s.equipScale, v -> s.equipScale = (int) Math.round(v));
        cycle("equipment place", "where the armour and rod parts row sits: above the hotbar, or level with it on its left or right. you can still drag it from there.",
                DofiSettings.EquipPlace.values(), k -> k.label, () -> s.equipPlace, v -> s.equipPlace = v);
        cycle("equipment look", "what sits behind foecollabs armour and rod parts: nothing, foecollabs own squares, or a fill, outline or both around each group of three.",
                DofiSettings.BoxLook.values(), k -> k.label, () -> s.equipLook, v -> s.equipLook = v);
        slider("equipment spacing", "the distance between equipment slots in pixels. 22 is foecollabs own. not used by its own squares look.", 16, 26, 1, "",
                () -> s.equipSpacing, v -> s.equipSpacing = (int) Math.round(v));
        color("box outline colour", "the colour of the thin outline around the hotbar and equipment boxes.",
                () -> s.boxOutlineColor, v -> s.boxOutlineColor = v);
        slider("box fill opacity", "how dark the see-through fill behind the hotbar and equipment is.", 0, 100, 5, "%",
                () -> s.boxOpacity, v -> s.boxOpacity = (int) Math.round(v));
        toggle("equipment circles", "a circle in the rarity colour behind each equipment item, instead of foecollabs bar under it.",
                () -> s.equipCircle, v -> s.equipCircle = v);
        toggle("equipment colour", "uses one colour for the circle or bar instead of the rarity colour.",
                () -> s.equipRecolor, v -> s.equipRecolor = v);
        color("equipment colour pick", "the colour used when equipment colour is on.",
                () -> s.equipColor, v -> s.equipColor = v);
        header("quests left");
        toggle("quests left hud", "quests until your next level and the next level of your location. it learns from /menu, /quests and the compendium. move it with move huds.",
                () -> s.questsHud, v -> s.questsHud = v);
    }

    private void serverHud() {
        header("active boosts");
        toggle("active boosts hud", "what is on right now: player boosters with a countdown, the chummer, and the weather events and moon. read from chat lines only. the server hud shows the events and moon too, switch its weather and moon lines off if you only want them here. move it with move huds.",
                () -> s.boostsHud, v -> s.boostsHud = v);
        header("now playing");
        toggle("now playing hud", "shows what is playing on your computer (spotify, youtube in a browser, anything that works with playerctl): title, artist and the time. linux only, needs playerctl installed. shows only, it never controls the player. move it with move huds.",
                () -> s.nowPlaying, v -> s.nowPlaying = v);
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
        header("server hud");
        toggle("server hud", "a sidebar styled hud: time, location, wallet and credits, the tackle shop and moon timers, and the weather events and moon that are active. move and resize it with move huds.",
                () -> s.serverHud, v -> s.serverHud = v);
        link("choose lines", "which lines the server hud shows, in what order.", ServerHudScreen::new);
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

    private void chat() {
        header("chat");
        toggle("hide login messages", "hides the welcome lines fishonmc prints every time you join: the instance, the fish and shader pack lines, the welcome, help, rules and discord lines.",
                () -> s.hideLoginChat, v -> s.hideLoginChat = v);
        toggle("hide empty lines", "hides chat lines with nothing in them, the blank gaps the server sends around some messages. announcements lose their spacing lines too.",
                () -> s.hideEmptyChat, v -> s.hideEmptyChat = v);
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
                        net.minecraft.client.MinecraftClient.getInstance().reloadResources();
                    }
                });
    }

}
