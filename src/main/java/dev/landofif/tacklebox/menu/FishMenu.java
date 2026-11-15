package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.screen.FishMenuScreen;
import dev.landofif.tacklebox.util.Glyphs;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.util.List;
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
    private static final Set<String> BUTTON_MENUS = Set.of("stats", "crew", "presets", "event");
    private static final Set<String> COMMANDS = Set.of("travel", "turbotravel", "tt", "stats", "crew", "presets", "event");
    /** The commands that open the travel menu; /tt is the server's short form of /turbotravel. */
    private static final Set<String> TRAVEL = Set.of("travel", "turbotravel", "tt");
    /** The instance list ("Instance 4 (14/75)"), which shares the crew menu's title. */
    private static final Pattern INSTANCE = Pattern.compile("instance \\d+\\b.*");
    private static final Pattern LEVEL = Pattern.compile("(?:level|lvl)\\D{0,8}(\\d+)");
    private static final Pattern PERCENT = Pattern.compile("(\\d+(?:[.,]\\d+)?)\\s*%");
    private static final Pattern FRACTION = Pattern.compile("([\\d,.]+)\\s*([km]?)\\s*/\\s*([\\d,.]+)\\s*([km]?)");

    /** Set when the player asked for the chest gui, so the next screen is not swapped. */
    public static boolean bypass;
    private static long commandAt;
    private static long travelAt;

    private FishMenu() {}

    /** Notes when the player types a menu command, because the travel menus title is only art. */
    public static void register() {
        ClientSendMessageEvents.COMMAND.register(command -> {
            String name = command.trim().split("\\s+")[0].toLowerCase(Locale.ROOT);
            if (COMMANDS.contains(name)) {
                commandAt = Util.getMeasuringTimeMs();
            }
            travelAt = isTravelCommand(command) ? Util.getMeasuringTimeMs() : 0;
        });
    }

    /** A typed command (no slash) that opens the travel menu. */
    public static boolean isTravelCommand(String command) {
        return TRAVEL.contains(command.trim().split("\\s+")[0].toLowerCase(Locale.ROOT));
    }

    /**
     * The travel menu the player just asked for by command, when the travel map is on (screen/TravelMapScreen). Asked before
     * {@link #wants}; "tile menu" on the map opens the tiles itself.
     */
    public static boolean wantsMap(Screen screen) {
        if (!(screen instanceof GenericContainerScreen) || bypass || Util.getMeasuringTimeMs() - travelAt >= 4000) {
            return false;
        }
        TackleboxSettings s = TackleboxSettings.get();
        if (s == null || !s.travelMap || !dev.landofif.tacklebox.trigger.TriggerEngine.isOnFishOnMc()) {
            return false;
        }
        travelAt = 0;
        commandAt = 0;
        return true;
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
        TackleboxSettings s = TackleboxSettings.get();
        var server = MinecraftClient.getInstance().getCurrentServerEntry();
        if (s == null || !(Vaults.number(screen) > 0 ? s.vaultMenu : isPresets(screen) ? s.presetsMenu : s.fishMenu) || server == null || !server.address.toLowerCase(Locale.ROOT).contains("fishonmc")) {
            return false;
        }
        if (Vaults.number(screen) > 0) {
            return true;
        }
        if (Glyphs.menuName(screen.getTitle()).equals("main menu")) {
            return false; // luna, 2026-10-30: /menu stays the server's chest, made bigger by chestScale
        }
        boolean typed = Util.getMeasuringTimeMs() - commandAt < 4000;
        commandAt = 0;
        // a button inside one of our menus (quests, then difficulty, then tier) stays in our screen too
        Screen open = MinecraftClient.getInstance().currentScreen;
        boolean chained = open instanceof FishMenuScreen || open instanceof dev.landofif.tacklebox.screen.TravelMapScreen;
        String title = Glyphs.plain(screen.getTitle()).toLowerCase(Locale.ROOT);
        boolean needsInventory = title.contains("merchant") || title.contains("vault") || title.contains("shop")
                || title.contains("pet menu") || title.contains("armor")
                || screen.getTitle().getString().chars().anyMatch(c -> c >= 0xD541 && c <= 0xD546); // the auction house pages (title art only) keep their chest art
        return typed || BUTTON_MENUS.contains(Glyphs.menuName(screen.getTitle())) || (chained && !needsInventory);
    }

    /** The instance list, from its plain item names: it stays the server's chest like /menu. */
    public static boolean isInstanceList(List<String> plainNames) {
        return plainNames.stream().anyMatch(n -> INSTANCE.matcher(n).matches());
    }

    /**
     * The menus that share the crew title {@code 핒} (U+D552), named by their plain item names: the collections overview
     * ("basses collection"), a location leveling page ("location leveling data"), otherwise the crew menu itself.
     */
    public static String crewTitleMenu(List<String> plainNames) {
        if (plainNames.stream().anyMatch(n -> n.endsWith(" collection"))) {
            return "collections";
        }
        if (plainNames.contains("location leveling data")) {
            return "location leveling";
        }
        return "crew";
    }

    /** Whose profile item this is, from its plain lore ("profile of meowfih"), or null when it is no profile. */
    public static String profileOwner(List<String> plainLore) {
        for (String line : plainLore) {
            if (line.startsWith("profile of ")) {
                return line.substring(11).trim();
            }
        }
        return null;
    }

    /** A menu with someone else's profile ({@code /stats <player>}): nothing in it is yours to remember. */
    public static boolean othersProfile(List<List<String>> plainLoreOfItems, String ownName) {
        for (List<String> lore : plainLoreOfItems) {
            String owner = profileOwner(lore);
            if (owner != null && !owner.equalsIgnoreCase(ownName)) {
                return true;
            }
        }
        return false;
    }

    /** {@link #othersProfile} for an open menu. */
    public static boolean othersProfile(net.minecraft.client.gui.screen.ingame.HandledScreen<?> screen) {
        var client = MinecraftClient.getInstance();
        if (client.player == null) {
            return false;
        }
        List<List<String>> lore = new java.util.ArrayList<>();
        for (var slot : screen.getScreenHandler().slots) {
            if (!(slot.inventory instanceof net.minecraft.entity.player.PlayerInventory)) {
                lore.add(slot.getStack().getOrDefault(DataComponentTypes.LORE, LoreComponent.DEFAULT).lines().stream().map(Glyphs::plain).toList());
            }
        }
        return othersProfile(lore, client.player.getName().getString());
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
