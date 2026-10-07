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
        this.tab = builder.getOrCreateCategory(Foe.text("dofis addons"));
    }

    /** Adds the tab to foecollab's config screen builder and saves our settings with it. */
    public static void add(ConfigBuilder builder) {
        ClothTab t = new ClothTab(builder);
        t.menus();
        t.huds();
        t.bobber();
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
            tab.addEntry(eb.startSubCategory(Foe.text(groupName), new ArrayList<>(group)).setExpanded(false).build());
        }
        group.clear();
    }

    private void header(String name) {
        flush();
        groupName = name;
    }

    private void screens() {
        header("screens");
        for (String[] b : new String[][]{{"chat triggers", "triggers"}, {"chat", "chat"}, {"key commands", "keys"}, {"word swaps", "words"}, {"viewmodel", "viewmodel"}, {"move huds", "move"}}) {
            Text link = Foe.text("open " + b[0]).setStyle(net.minecraft.text.Style.EMPTY.withColor(Formatting.GOLD).withUnderline(true)
                    .withClickEvent(new ClickEvent.RunCommand("dofi " + b[1])));
            group.add(new TextListEntry(Foe.text(b[0]), link));
        }
    }

    private static Text[] tip(String desc) {
        return desc.isEmpty() ? new Text[0] : new Text[]{Foe.sentence(desc)};
    }

    private void toggle(String name, String desc, Supplier<Boolean> get, Consumer<Boolean> set) {
        var e = eb.startBooleanToggle(Foe.text(name), get.get()).setDefaultValue(get.get()).setSaveConsumer(set::accept);
        if (!desc.isEmpty()) {
            e.setTooltip(tip(desc));
        }
        group.add(e.build());
    }

    private <E extends Enum<E>> void cycle(String name, String desc, E[] values, Function<E, String> label,
                                           Supplier<E> get, Consumer<E> set) {
        var e = eb.startEnumSelector(Foe.text(name), values[0].getDeclaringClass(), get.get())
                .setDefaultValue(get.get()).setEnumNameProvider(v -> Foe.text(label.apply((E) v))).setSaveConsumer(set::accept);
        e.setTooltip(tip(desc));
        group.add(e.build());
    }

    private void slider(String name, String desc, double min, double max, double step, String unit,
                        DoubleFunction<String> format, DoubleSupplier get, DoubleConsumer set) {
        int cur = (int) Math.round(get.getAsDouble() / step);
        var e = eb.startIntSlider(Foe.text(name), cur, (int) Math.round(min / step), (int) Math.round(max / step))
                .setDefaultValue(cur)
                .setTextGetter(v -> Text.literal(step >= 1 ? String.valueOf((int) Math.round(v * step)) + unit
                        : String.format(Locale.ROOT, "%.2f", v * step) + unit))
                .setSaveConsumer(v -> set.accept(v * step));
        e.setTooltip(tip(desc));
        group.add(e.build());
    }

    private void color(String name, String desc, IntSupplier get, IntConsumer set) {
        var e = eb.startColorField(Foe.text(name), get.getAsInt()).setDefaultValue(get.getAsInt())
                .setSaveConsumer(v -> set.accept(v & 0xFFFFFF));
        e.setTooltip(tip(desc));
        group.add(e.build());
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
    }

    private void huds() {
        header("hotbar");
        toggle("hotbar background", "the box behind the hotbar slots. off leaves just the items and the selection. move the hotbar with move huds.",
                () -> s.hotbarBackground, v -> s.hotbarBackground = v);
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
