package dev.landofif.dofisaddons.menu;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.screen.FishMenuScreen;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.util.Glyphs;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Which chest screens the mods own menu takes over (the main menu and /travel, /turbotravel), and what it
 * reads off their items. The player's own click on a button is the only thing sent to the server.
 */
public final class FishMenu {
    /** Level and progress to the next level read off a travel item (-1 when the tooltip had none). */
    public record Info(int level, float progress) {}

    /** Menus that are only buttons, named by their art glyph (see util/Glyphs). The others (sell, vault, shops, pets, armor) need your inventory. */
    private static final Set<String> BUTTON_MENUS = Set.of("main menu", "stats", "quests", "crew", "presets");
    private static final Set<String> COMMANDS = Set.of("menu", "travel", "turbotravel", "quests", "stats", "crew", "presets");
    private static final Pattern LEVEL = Pattern.compile("(?:level|lvl)\\D{0,8}(\\d+)");
    private static final Pattern PERCENT = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*%");
    private static final Pattern FRACTION = Pattern.compile("([\\d,.]+)\\s*([km]?)\\s*/\\s*([\\d,.]+)\\s*([km]?)");

    /** Set when the player asked for the chest gui, so the next screen is not swapped. */
    public static boolean bypass;
    private static long commandAt;

    private FishMenu() {}

    /** Notes when the player types a menu command, because the travel menus title is only art. */
    public static void register() {
        ClientSendMessageEvents.COMMAND.register(command -> {
            String name = command.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
            if (COMMANDS.contains(name)) {
                commandAt = Util.getMeasuringTimeMs();
            }
        });
    }

    public static boolean isPresets(Screen screen) {
        return Glyphs.menuName(screen.getTitle()).equals("presets");
    }

    public static boolean wants(Screen screen) {
        if (!(screen instanceof GenericContainerScreen)) {
            return false;
        }
        if (bypass) {
            bypass = false;
            return false;
        }
        DofiSettings s = StatsTracker.settings();
        var server = MinecraftClient.getInstance().getCurrentServerEntry();
        if (s == null || !(isPresets(screen) ? s.presetsMenu : s.fishMenu) || server == null || !server.address.toLowerCase(Locale.ROOT).contains("fishonmc")) {
            return false;
        }
        boolean typed = Util.getMeasuringTimeMs() - commandAt < 4000;
        commandAt = 0;
        // a button inside one of our menus (quests, then difficulty, then tier) stays in our screen too
        boolean chained = MinecraftClient.getInstance().currentScreen instanceof FishMenuScreen;
        String title = Glyphs.plain(screen.getTitle()).toLowerCase(Locale.ROOT);
        boolean needsInventory = title.contains("merchant") || title.contains("vault") || title.contains("shop")
                || title.contains("pet menu") || title.contains("armor");
        return typed || BUTTON_MENUS.contains(Glyphs.menuName(screen.getTitle())) || (chained && !needsInventory);
    }

    /** Level and progress from an items name and lore, trying "level n", "n%" and "a/b". */
    public static Info info(ItemStack stack) {
        StringBuilder text = new StringBuilder(Glyphs.plain(stack.getName()));
        LoreComponent lore = stack.get(DataComponentTypes.LORE);
        if (lore != null) {
            for (Text line : lore.lines()) {
                text.append('\n').append(Glyphs.plain(line));
            }
        }
        String all = text.toString().toLowerCase(Locale.ROOT);
        Matcher level = LEVEL.matcher(all);
        Matcher percent = PERCENT.matcher(all);
        Matcher fraction = FRACTION.matcher(all);
        float progress = -1;
        if (percent.find()) {
            progress = Float.parseFloat(percent.group(1).replace(',', '.')) / 100f;
        } else if (fraction.find()) {
            double max = number(fraction.group(3), fraction.group(4));
            progress = max > 0 ? (float) (number(fraction.group(1), fraction.group(2)) / max) : -1;
        }
        return new Info(level.find() ? Integer.parseInt(level.group(1)) : -1, Math.min(1f, progress));
    }

    private static double number(String digits, String suffix) {
        double n = Double.parseDouble(digits.replace(",", ""));
        return suffix.equals("k") ? n * 1_000 : suffix.equals("m") ? n * 1_000_000 : n;
    }
}
