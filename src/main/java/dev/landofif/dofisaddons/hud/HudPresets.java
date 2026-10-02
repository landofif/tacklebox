package dev.landofif.dofisaddons.hud;

import java.util.ArrayList;
import java.util.List;

/** Ready-made HUDs to start from. Every one is just lines you can edit. */
public final class HudPresets {
    public record Preset(String name, String description, CustomHud hud) {}

    private HudPresets() {}

    public static List<Preset> all() {
        return List.of(
                new Preset("blank", "an empty box to fill yourself", blank()),
                new Preset("all-time stats", "fish, rate, time, value, rarities and drops since you installed the mod", allTime()),
                new Preset("session stats", "the same, for this session only (resets when you relaunch)", session()),
                new Preset("dry streaks", "fish since your last mythical, legendary, pet, shard...", dry()),
                new Preset("where & when", "location, in-game time, weather and temperature", where()),
                new Preset("wallet", "money, credits, level and crew from the sidebar", wallet()),
                new Preset("level", "your fishing level and progress bar, replaces the vanilla xp bar", level()),
                new Preset("bobber timer", "how long your line has been out (also see settings)", bobber()),
                new Preset("bait", "the bait on your rod and its uses left, while you hold the rod", bait()),
                new Preset("events", "weather and world events with an estimated time left", events()));
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
        return make("new hud", 2, 30, "&b&lmy hud", "&7label%tab%&fvalue", "&7edit these lines!");
    }

    public static CustomHud session() {
        return make("session stats", 1, 55,
                "&b&lthis session",
                "&7fish%tab%&f%session.fish%",
                "&7per hour%tab%&f%session.per_hour%",
                "&7fishing%tab%&f%session.time%",
                "&7value%tab%&f$%session.value%",
                "%session.rarity_bar%",
                "&7pets%tab%&f%session.pets%",
                "&7shards%tab%&f%session.shards%",
                "&7since mythical%tab%&f%alltime.since.rarity.mythical%");
    }

    public static CustomHud allTime() {
        return make("all-time stats", 1, 28,
                "&6&lall-time",
                "&7fish%tab%&f%alltime.fish%",
                "&7per hour%tab%&f%alltime.per_hour%",
                "&7fishing%tab%&f%alltime.time%",
                "&7value%tab%&f$%alltime.value%",
                "&7sold%tab%&f$%alltime.sold%",
                "%alltime.rarity_bar%",
                "&7pets%tab%&f%alltime.pets%",
                "&7shards%tab%&f%alltime.shards%",
                "&7heaviest%tab%&f%alltime.heaviest%");
    }

    public static CustomHud dry() {
        CustomHud h = make("dry streaks", 99, 30,
                "&c&lfish since last",
                "&#c93832mythical%tab%&f%alltime.since.rarity.mythical%",
                "&#d98103legendary%tab%&f%alltime.since.rarity.legendary%",
                "&#ce2326fabled%tab%&f%alltime.since.variant.fabled%",
                "&#af3333gigantic%tab%&f%alltime.since.size.gigantic%",
                "&7pet%tab%&f%alltime.since.pet%",
                "&7shard%tab%&f%alltime.since.shard%");
        h.align = CustomHud.Align.RIGHT;
        return h;
    }

    public static CustomHud where() {
        CustomHud h = make("where & when", 50, 1,
                "&a%boss_bar.location%",
                "&7%boss_bar.time%  &f%boss_bar.weather%  &7%boss_bar.temperature%");
        h.align = CustomHud.Align.CENTER;
        return h;
    }

    public static CustomHud wallet() {
        CustomHud h = make("wallet", 99, 70,
                "&e&lwallet",
                "&7money%tab%&f$%scoreboard.wallet%",
                "&7credits%tab%&f%scoreboard.credits%",
                "&7level%tab%&f%player.level% &8(%player.xp_progress%%)",
                "&7crew%tab%&f%scoreboard.crew_name% &8lvl %scoreboard.crew_level%");
        h.align = CustomHud.Align.RIGHT;
        return h;
    }

    public static CustomHud level() {
        CustomHud h = make("level", 50, 86,
                "&7lvl &l%player.level_colored% %player.level_bar.16% &7%player.xp_progress%%");
        h.align = CustomHud.Align.CENTER;
        h.background = 0;
        return h;
    }

    public static CustomHud bait() {
        return make("bait", 1, 55,
                "&b&lbait",
                "%bait.name_colored%",
                "&7uses%tab%%bait.uses%");
    }

    /** Each line hides itself while its event is off, so only what is on shows. */
    public static CustomHud events() {
        CustomHud h = make("events", 99, 16,
                "&b&levents",
                "&#55ffffrain%tab%&f%event.rain%",
                "&7thunderstorm%tab%&f%event.thunderstorm%",
                "&5supercell%tab%&f%event.supercell%",
                "&arainbow%tab%&f%event.rainbow%",
                "&6gold rush%tab%&f%event.gold_rush%",
                "&ablooming oasis%tab%&f%event.blooming_oasis%",
                "&ffull moon%tab%&f%event.full_moon%",
                "&bblue moon%tab%&f%event.blue_moon%",
                "&esuper moon%tab%&f%event.super_moon%",
                "&cblood moon%tab%&f%event.blood_moon%");
        h.align = CustomHud.Align.RIGHT;
        return h;
    }

    /** Off by default: the bobber timer already shows above the bobber. */
    public static CustomHud bobber() {
        CustomHud h = make("bobber timer (hud)", 50, 58, "&f%bobber.time%s");
        h.align = CustomHud.Align.CENTER;
        h.background = 0;
        h.enabled = false;
        return h;
    }
}
