package dev.landofif.dofisaddons.hud;

import java.util.ArrayList;
import java.util.List;

/** Ready-made HUDs to start from. Every one is just lines you can edit. */
public final class HudPresets {
    public record Preset(String name, String description, CustomHud hud) {}

    private HudPresets() {}

    public static List<Preset> all() {
        return List.of(
                new Preset("Blank", "an empty box to fill yourself", blank()),
                new Preset("All-time stats", "fish, rate, time, value, rarities and drops since you installed the mod", allTime()),
                new Preset("Session stats", "the same, for this session only (resets when you relaunch)", session()),
                new Preset("Dry streaks", "fish since your last mythical, legendary, pet, shard...", dry()),
                new Preset("Where & when", "location, in-game time, weather and temperature", where()),
                new Preset("Wallet", "money, credits, level and crew from the sidebar", wallet()),
                new Preset("Level", "your fishing level and progress bar, replaces the vanilla xp bar", level()),
                new Preset("Bobber timer", "how long your line has been out (also see settings)", bobber()));
    }

    private static CustomHud make(String name, double x, double y, String... lines) {
        CustomHud h = new CustomHud();
        h.name = name;
        h.x = x;
        h.y = y;
        h.lines = new ArrayList<>(List.of(lines));
        return h;
    }

    public static CustomHud blank() {
        return make("New HUD", 2, 30, "&b&lMy HUD", "&7Label%tab%&fvalue", "&7Edit these lines!");
    }

    public static CustomHud session() {
        return make("Session stats", 1, 55,
                "&b&lThis session",
                "&7Fish%tab%&f%session.fish%",
                "&7Per hour%tab%&f%session.per_hour%",
                "&7Fishing%tab%&f%session.time%",
                "&7Value%tab%&f$%session.value%",
                "%session.rarity_bar%",
                "&7Pets%tab%&f%session.pets%",
                "&7Shards%tab%&f%session.shards%",
                "&7Since mythical%tab%&f%alltime.since.rarity.mythical%");
    }

    public static CustomHud allTime() {
        return make("All-time stats", 1, 28,
                "&6&lAll-time",
                "&7Fish%tab%&f%alltime.fish%",
                "&7Per hour%tab%&f%alltime.per_hour%",
                "&7Fishing%tab%&f%alltime.time%",
                "&7Value%tab%&f$%alltime.value%",
                "&7Sold%tab%&f$%alltime.sold%",
                "%alltime.rarity_bar%",
                "&7Pets%tab%&f%alltime.pets%",
                "&7Shards%tab%&f%alltime.shards%",
                "&7Heaviest%tab%&f%alltime.heaviest%");
    }

    public static CustomHud dry() {
        CustomHud h = make("Dry streaks", 99, 30,
                "&c&lFish since last",
                "&#C93832Mythical%tab%&f%alltime.since.rarity.mythical%",
                "&#D98103Legendary%tab%&f%alltime.since.rarity.legendary%",
                "&#CE2326Fabled%tab%&f%alltime.since.variant.fabled%",
                "&#AF3333Gigantic%tab%&f%alltime.since.size.gigantic%",
                "&7Pet%tab%&f%alltime.since.pet%",
                "&7Shard%tab%&f%alltime.since.shard%");
        h.align = CustomHud.Align.RIGHT;
        return h;
    }

    public static CustomHud where() {
        CustomHud h = make("Where & when", 50, 1,
                "&a%boss_bar.location%",
                "&7%boss_bar.time%  &f%boss_bar.weather%  &7%boss_bar.temperature%");
        h.align = CustomHud.Align.CENTER;
        return h;
    }

    public static CustomHud wallet() {
        CustomHud h = make("Wallet", 99, 70,
                "&e&lWallet",
                "&7Money%tab%&f$%scoreboard.wallet%",
                "&7Credits%tab%&f%scoreboard.credits%",
                "&7Level%tab%&f%player.level% &8(%player.xp_progress%%)",
                "&7Crew%tab%&f%scoreboard.crew_name% &8lvl %scoreboard.crew_level%");
        h.align = CustomHud.Align.RIGHT;
        return h;
    }

    public static CustomHud level() {
        CustomHud h = make("Level", 50, 86,
                "&7lvl &l%player.level_colored% %player.level_bar.16% &7%player.xp_progress%%");
        h.align = CustomHud.Align.CENTER;
        h.background = 0;
        return h;
    }

    /** Off by default: the bobber timer already shows above the bobber. */
    public static CustomHud bobber() {
        CustomHud h = make("Bobber timer (HUD)", 50, 58, "&f%bobber.time%s");
        h.align = CustomHud.Align.CENTER;
        h.background = 0;
        h.enabled = false;
        return h;
    }
}
