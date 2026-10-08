package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.ui.Foe;
import me.shedaniel.clothconfig2.api.AbstractConfigListEntry;
import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import me.shedaniel.clothconfig2.gui.entries.TextListEntry;
import net.minecraft.text.ClickEvent;
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
 * The mods settings as a "dofis addons" tab in foecollab's own cloth config screen (added by
 * {@code ConfigScreenProviderMixin}). Each section is a folding group; values are written when the screen is saved.
 */
public final class ClothTab {
    private static final DoubleFunction<String> PCT = null; // kept so the rows below read like they always did
    private final DofiSettings s = DofiSettings.get();
    private final ConfigEntryBuilder eb;
    private final ConfigCategory tab;
    private final List<AbstractConfigListEntry> group = new ArrayList<>();
    private String groupName;

    private ClothTab(ConfigBuilder builder) {
        this.eb = builder.entryBuilder();
        this.tab = builder.getOrCreateCategory(title("dofis addons"));
    }

    /** Adds the tab to foecollab's config screen builder and saves our settings with it. */
    public static void add(ConfigBuilder builder) {
        ClothTab t = new ClothTab(builder);
        t.menus();
        t.huds();
        t.bobber();
        t.chat();
        t.world();
        t.screens();
        t.flush();
        Runnable saving = builder.getSavingRunnable();
        builder.setSavingRunnable(() -> {
            if (saving != null) {
                saving.run();
            }
            DofiSettings.save();
        });
    }

    private void flush() {
        if (groupName != null && !group.isEmpty()) {
            tab.addEntry(eb.startSubCategory(title(groupName).formatted(Formatting.BOLD, Formatting.WHITE), new ArrayList<>(group)).setExpanded(false).build());
        }
        group.clear();
    }

    private void header(String name) {
        flush();
        groupName = name;
    }

    private void screens() {
        header("screens");
        link("chat triggers", dev.landofif.dofisaddons.screen.TriggerListScreen::new);
        link("chat", dev.landofif.dofisaddons.screen.ChatFilterScreen::new);
        link("key commands", dev.landofif.dofisaddons.screen.KeyCommandScreen::new);
        link("word swaps", dev.landofif.dofisaddons.screen.WordSwapScreen::new);
        link("bait picker", dev.landofif.dofisaddons.screen.BaitScreen::new);
        link("bobber timer", dev.landofif.dofisaddons.screen.BobberScreen::new);
        link("viewmodel", dev.landofif.dofisaddons.screen.ViewmodelScreen::new);
        link("move huds", FoeScreens::move);
    }

    /** A row with an open button for one of the mods screens; it returns to the config screen when closed. */
    private void link(String name, Function<net.minecraft.client.gui.screen.Screen, net.minecraft.client.gui.screen.Screen> screen) {
        group.add(ButtonEntry.action(title(name), title("open"), () -> {
            var mc = net.minecraft.client.MinecraftClient.getInstance();
            net.minecraft.client.gui.screen.Screen next = screen.apply(mc.currentScreen);
            if (next != null) {
                mc.setScreen(next);
            }
        }));
    }

    /** Title Case in the normal font (the group headings are bold), like foecollab's own labels (not our small capitals). */
    private static net.minecraft.text.MutableText title(String text) {
        StringBuilder b = new StringBuilder();
        for (String w : text.split(" ")) {
            if (!w.isEmpty()) {
                b.append(b.length() > 0 ? " " : "").append(Character.toUpperCase(w.charAt(0))).append(w.substring(1));
            }
        }
        return Text.literal(b.toString());
    }

    private static Text[] tip(String desc) {
        return desc.isEmpty() ? new Text[0] : new Text[]{Foe.sentence(desc)};
    }

    private void toggle(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
        var e = eb.startBooleanToggle(title(name), get.get()).setDefaultValue(get.get()).setSaveConsumer(set::accept);
        if (!desc.isEmpty()) {
            e.setTooltip(tip(desc));
        }
        group.add(e.build());
    }

    private <E extends Enum<E>> void cycle(String name, String desc, E[] values, Function<E, String> label,
                                           Supplier<E> get, Consumer<E> set) {
        var e = eb.startEnumSelector(title(name), values[0].getDeclaringClass(), get.get())
                .setDefaultValue(get.get()).setEnumNameProvider(v -> title(label.apply((E) v))).setSaveConsumer(set::accept);
        e.setTooltip(tip(desc));
        group.add(e.build());
    }

    private void slider(String name, String desc, double min, double max, double step, String unit,
                        DoubleFunction<String> format, DoubleSupplier get, DoubleConsumer set) {
        int cur = (int) Math.round(get.getAsDouble() / step);
        var e = eb.startIntSlider(title(name), cur, (int) Math.round(min / step), (int) Math.round(max / step))
                .setDefaultValue(cur)
                .setTextGetter(v -> Text.literal(step >= 1 ? String.valueOf((int) Math.round(v * step)) + unit
                        : String.format(Locale.ROOT, "%.2f", v * step) + unit))
                .setSaveConsumer(v -> set.accept(v * step));
        e.setTooltip(tip(desc));
        group.add(e.build());
    }

    private void color(String name, String desc, IntSupplier get, IntConsumer set) {
        ButtonEntry e = ButtonEntry.color(title(name), get, set);
        e.setTooltipSupplier(() -> java.util.Optional.of(tip(desc)));
        group.add(e);
    }

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
        header("vaults");
        toggle("vault overview", "/pv as all seven personal vaults on one screen with a search. a vault shows what it held when you last opened it. off gives the chest back.",
                () -> s.vaultMenu, v -> s.vaultMenu = v);
    }

    private void huds() {
        header("hotbar");
        cycle("hotbar look", "what sits behind the hotbar slots: nothing, the vanilla sprite, a see-through fill, a thin outline or both. the vanilla sprite only fits 20 pixel spacing.",
                DofiSettings.BoxLook.values(), k -> k.label, () -> s.hotbarLook, v -> s.hotbarLook = v);
        slider("hotbar spacing", "the distance between hotbar slots in pixels. 20 is vanilla, 16 puts the items right next to each other. not used by the vanilla sprite look.", 16, 24, 1, "", PCT,
                () -> s.hotbarSpacing, v -> s.hotbarSpacing = (int) Math.round(v));
        slider("hotbar size", "how big the hotbar is. it grows from the bottom middle.", 50, 200, 5, "%", PCT,
                () -> s.hotbarScale, v -> s.hotbarScale = (int) Math.round(v));
        cycle("selected slot", "how the slot you hold is marked: the vanilla frame, a round ring, a simple line under the item, or nothing.",
                DofiSettings.SelectionStyle.values(), k -> k.label, () -> s.hotbarSelection, v -> s.hotbarSelection = v);
        color("selected colour", "the colour of the round ring and the simple line. the frame keeps its own.",
                () -> s.hotbarSelectColor, v -> s.hotbarSelectColor = v);
        toggle("offhand box", "the box next to the hotbar for your offhand item.",
                () -> s.hotbarOffhand, v -> s.hotbarOffhand = v);
        slider("equipment size", "how big foecollabs armour and rod parts row is. move it with move huds.", 50, 200, 5, "%", PCT,
                () -> s.equipScale, v -> s.equipScale = (int) Math.round(v));
        cycle("equipment place", "where the armour and rod parts row sits: above the hotbar, or level with it on its left or right. you can still drag it from there.",
                DofiSettings.EquipPlace.values(), k -> k.label, () -> s.equipPlace, v -> s.equipPlace = v);
        cycle("equipment look", "what sits behind foecollabs armour and rod parts: nothing, foecollabs own squares, or a fill, outline or both around each group of three.",
                DofiSettings.BoxLook.values(), k -> k.label, () -> s.equipLook, v -> s.equipLook = v);
        slider("equipment spacing", "the distance between equipment slots in pixels. 22 is foecollabs own. not used by its own squares look.", 16, 26, 1, "", PCT,
                () -> s.equipSpacing, v -> s.equipSpacing = (int) Math.round(v));
        color("box outline colour", "the colour of the thin outline around the hotbar and equipment boxes.",
                () -> s.boxOutlineColor, v -> s.boxOutlineColor = v);
        slider("box fill opacity", "how dark the see-through fill behind the hotbar and equipment is.", 0, 100, 5, "%", PCT,
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
