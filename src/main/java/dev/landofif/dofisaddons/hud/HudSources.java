package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.display.BobberTimer;
import dev.landofif.dofisaddons.hud.HudEngine.ValueResult;
import dev.landofif.dofisaddons.stats.StatBook;
import dev.landofif.dofisaddons.stats.StatNames;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.world.ClientWorld;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Every {@code %source.path%} value a custom HUD line can use. A value that isn't available right
 * now (no data yet, not on the server) is "absent", which hides the line that used it.
 *
 * <p>The {@code stats.*} and {@code stats_data.*} names match FOECollab, so HUDs shared from there
 * import and keep working.</p>
 */
public final class HudSources {
    static final Map<String, Function<String[], ValueResult>> SOURCES = Map.ofEntries(
            Map.entry("session", guard(p -> book(StatsTracker.session(), p))),
            Map.entry("alltime", guard(p -> book(StatsTracker.allTime(), p))),
            Map.entry("bobber", guard(HudSources::bobber)),
            Map.entry("player", guard(HudSources::player)),
            Map.entry("world", guard(HudSources::world)),
            Map.entry("time", guard(HudSources::time)),
            Map.entry("boss_bar", guard(HudSources::bossBar)),
            Map.entry("location", guard(HudSources::location)),
            Map.entry("scoreboard", guard(HudSources::scoreboard)),
            Map.entry("tab", guard(HudSources::tab)),
            Map.entry("stats", guard(HudSources::foeStats)),
            Map.entry("stats_data", guard(HudSources::foeStatsData)));

    /** One entry in the "Insert value" picker. */
    public record Entry(String category, String token, String description) {}

    /** What the picker lists, grouped and in a sensible order. */
    public static final List<Entry> CATALOG = buildCatalog();

    private HudSources() {}

    private static Function<String[], ValueResult> guard(Function<String[], ValueResult> source) {
        return params -> {
            try {
                return source.apply(params);
            } catch (RuntimeException e) {
                return ValueResult.absent();
            }
        };
    }

    private static String first(String[] p) {
        return p.length > 0 ? p[0] : "";
    }

    // ------------------------------------------------------------------ session / alltime

    /**
     * %session.fish%, %alltime.rarity.mythical%, %alltime.since.rarity.mythical%, ...
     * Numbers come nicely formatted ("38.5K"); add _raw to the last part for a plain number
     * you can do maths with (%session.value_raw%).
     */
    private static ValueResult book(StatBook b, String[] p) {
        if (p.length == 0) {
            return ValueResult.absent();
        }
        String last = p[p.length - 1];
        boolean raw = last.endsWith("_raw");
        if (raw) {
            p = p.clone();
            p[p.length - 1] = last.substring(0, last.length() - 4);
        }
        String key = p[0];
        return switch (key) {
            case "fish" -> num(b.fish, raw, false);
            case "per_hour" -> b.perHour() > 0 ? num(b.perHour(), raw, true) : ValueResult.absent();
            case "time" -> raw ? ValueResult.str(String.valueOf(b.activeMillis / 1000)) : ValueResult.str(StatNames.duration(b.activeMillis));
            case "value" -> num(b.value, raw, true);
            case "xp" -> num(b.xp, raw, true);
            case "sold" -> num(b.moneyEarned, raw, true);
            case "quests" -> num(b.questsCompleted, raw, false);
            case "pets" -> num(b.pets, raw, false);
            case "shards" -> num(b.shards, raw, false);
            case "lightning" -> num(b.lightningBottles, raw, false);
            case "capsules" -> num(b.infusionCapsules, raw, false);
            case "rarity" -> p.length > 1 ? num(b.count(b.rarity, p[1]), raw, false) : ValueResult.absent();
            case "size" -> p.length > 1 ? num(b.count(b.size, p[1]), raw, false) : ValueResult.absent();
            case "variant" -> p.length > 1 ? num(b.count(b.variant, p[1]), raw, false) : ValueResult.absent();
            case "species" -> p.length > 1 ? num(b.count(b.species, p[1]), raw, false) : ValueResult.absent();
            case "since" -> {
                if (p.length < 2) yield ValueResult.absent();
                String k = p.length >= 3 ? p[1] + ":" + p[2] : p[1];
                long s = b.since(k);
                yield s < 0 ? ValueResult.absent() : num(s, raw, false);
            }
            case "rarity_bar" -> ValueResult.str(rarityBar(b));
            case "heaviest" -> heaviest(b);
            default -> ValueResult.absent();
        };
    }

    private static ValueResult num(double v, boolean raw, boolean compact) {
        if (raw) {
            return ValueResult.str(v == Math.rint(v) ? String.valueOf((long) v) : String.format(Locale.ROOT, "%.2f", v));
        }
        return ValueResult.str(compact ? StatNames.compact(v) : StatNames.number((long) v));
    }

    private static String rarityBar(StatBook b) {
        StringBuilder sb = new StringBuilder();
        for (StatNames.Kind k : StatNames.RARITIES) {
            long n = b.count(b.rarity, k.id());
            if (n == 0 && k.id().equals("special")) continue;
            if (!sb.isEmpty()) sb.append(' ');
            sb.append(String.format("&#%06X", k.color())).append(k.name().charAt(0)).append(StatNames.compact(n));
        }
        return sb.toString().replace('&', '§');
    }

    private static ValueResult heaviest(StatBook b) {
        return b.heaviest.entrySet().stream()
                .max((x, y) -> Float.compare(x.getValue().weight, y.getValue().weight))
                .map(e -> ValueResult.str(StatNames.pretty(e.getKey()) + " " + String.format(Locale.ROOT, "%.1flb", e.getValue().weight)))
                .orElse(ValueResult.absent());
    }

    // ------------------------------------------------------------------ live sources

    private static ValueResult bobber(String[] p) {
        return switch (first(p)) {
            case "time" -> BobberTimer.isOut() ? ValueResult.str(String.format(Locale.ROOT, "%.1f", BobberTimer.seconds())) : ValueResult.absent();
            case "bite" -> ValueResult.bool(BobberTimer.hasBite());
            default -> ValueResult.absent();
        };
    }

    private static ValueResult player(String[] p) {
        ClientPlayerEntity player = MinecraftClient.getInstance().player;
        if (player == null) {
            return ValueResult.absent();
        }
        return switch (first(p)) {
            case "name" -> ValueResult.str(player.getName().getString());
            case "health" -> ValueResult.str(String.valueOf((int) player.getHealth()));
            case "level", "xp_level" -> ValueResult.str(String.valueOf(player.experienceLevel));
            case "xp_progress" -> ValueResult.str(String.format(Locale.ROOT, "%.1f", player.experienceProgress * 100f));
            case "level_colored" -> ValueResult.comp(LevelInfo.coloredLevel());
            case "level_bar" -> ValueResult.comp(LevelInfo.bar(p.length > 1 ? Math.max(4, Math.min(60, Integer.parseInt(p[1]))) : 20));
            case "next_level" -> ValueResult.str(String.valueOf(player.experienceLevel + 1));
            case "x" -> ValueResult.str(String.valueOf(player.getBlockX()));
            case "y" -> ValueResult.str(String.valueOf(player.getBlockY()));
            case "z" -> ValueResult.str(String.valueOf(player.getBlockZ()));
            case "yaw" -> ValueResult.str(String.valueOf((int) player.getYaw()));
            case "pitch" -> ValueResult.str(String.valueOf((int) player.getPitch()));
            case "direction" -> ValueResult.str(direction(player.getYaw()));
            case "held_item" -> player.getMainHandStack().isEmpty()
                    ? ValueResult.absent()
                    : ValueResult.str(player.getMainHandStack().getName().getString());
            default -> ValueResult.absent();
        };
    }

    private static ValueResult world(String[] p) {
        ClientWorld world = MinecraftClient.getInstance().world;
        if (world == null) {
            return ValueResult.absent();
        }
        return switch (first(p)) {
            case "time" -> ValueResult.str(String.valueOf(world.getTimeOfDay() % 24000L));
            case "day" -> ValueResult.str(String.valueOf(world.getTimeOfDay() / 24000L));
            case "raining" -> ValueResult.bool(world.isRaining());
            case "thundering" -> ValueResult.bool(world.isThundering());
            default -> ValueResult.absent();
        };
    }

    private static ValueResult time(String[] p) {
        LocalDateTime now = LocalDateTime.now();
        int hour = now.getHour();
        return switch (first(p)) {
            case "hour" -> ValueResult.str(String.format("%02d", hour));
            case "minute" -> ValueResult.str(String.format("%02d", now.getMinute()));
            case "second" -> ValueResult.str(String.format("%02d", now.getSecond()));
            case "clock" -> ValueResult.str(String.format("%02d:%02d", hour, now.getMinute()));
            case "clock12" -> ValueResult.str(String.format("%d:%02d %s", ((hour + 11) % 12) + 1, now.getMinute(), hour < 12 ? "AM" : "PM"));
            case "date" -> ValueResult.str(String.format("%04d-%02d-%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth()));
            default -> ValueResult.absent();
        };
    }

    private static ValueResult bossBar(String[] p) {
        return switch (first(p)) {
            case "time" -> ValueResult.of(ServerInfo.time.isEmpty() ? "" : ServerInfo.time + " " + ServerInfo.ampm);
            case "weather" -> ValueResult.of(ServerInfo.weather);
            case "temperature" -> ValueResult.of(ServerInfo.temperature);
            case "location" -> ValueResult.of(ServerInfo.locationName);
            default -> ValueResult.absent();
        };
    }

    private static ValueResult location(String[] p) {
        return switch (first(p)) {
            case "name" -> ValueResult.of(ServerInfo.locationName);
            case "id" -> ValueResult.of(ServerInfo.locationId);
            case "fish" -> ServerInfo.locationId.isEmpty() ? ValueResult.absent()
                    : num(StatsTracker.session().count(StatsTracker.session().location, ServerInfo.locationId), false, false);
            default -> ValueResult.absent();
        };
    }

    private static ValueResult scoreboard(String[] p) {
        return switch (first(p)) {
            case "level" -> {
                ClientPlayerEntity player = MinecraftClient.getInstance().player;
                yield player == null ? ValueResult.absent() : ValueResult.str(String.valueOf(player.experienceLevel));
            }
            case "wallet", "money" -> ValueResult.of(ServerInfo.wallet);
            case "credits" -> ValueResult.of(ServerInfo.credits);
            case "catches" -> ValueResult.of(ServerInfo.catches);
            case "catch_rate" -> ValueResult.of(ServerInfo.catchRate);
            case "crew_name" -> ValueResult.of(ServerInfo.crewName);
            case "crew_level" -> ValueResult.of(ServerInfo.crewLevel);
            case "location_min" -> ValueResult.of(ServerInfo.locationMin);
            case "location_max" -> ValueResult.of(ServerInfo.locationMax);
            default -> ValueResult.absent();
        };
    }

    private static ValueResult tab(String[] p) {
        MinecraftClient client = MinecraftClient.getInstance();
        return switch (first(p)) {
            case "count" -> client.getNetworkHandler() == null
                    ? ValueResult.absent()
                    : ValueResult.str(String.valueOf(client.getNetworkHandler().getPlayerList().size()));
            default -> ValueResult.absent();
        };
    }

    // ------------------------------------------------------------------ FOECollab names

    private static ValueResult foeStats(String[] p) {
        StatBook s = StatsTracker.session();
        StatBook a = StatsTracker.allTime();
        return switch (first(p)) {
            case "fish_caught" -> raw(s.fish);
            case "all_fish_caught" -> raw(a.fish);
            case "total_xp" -> raw((long) s.xp);
            case "all_total_xp" -> raw((long) a.xp);
            case "total_value" -> raw((long) s.value);
            case "all_total_value" -> raw((long) a.value);
            case "pets" -> raw(s.pets);
            case "all_pets" -> raw(a.pets);
            case "shards" -> raw(s.shards);
            case "all_shards" -> raw(a.shards);
            case "quests" -> raw(s.questsCompleted);
            case "all_quests" -> raw(a.questsCompleted);
            case "lightning_bottles" -> raw(s.lightningBottles);
            case "infusion_capsules" -> raw(s.infusionCapsules);
            case "fish_time" -> {
                long sec = s.activeMillis / 1000;
                yield ValueResult.str(String.format("%02d:%02d:%02d", sec / 3600, (sec / 60) % 60, sec % 60));
            }
            case "fish_time_seconds" -> raw(s.activeMillis / 1000);
            default -> ValueResult.absent();
        };
    }

    /** %stats_data.data.fish.rarity.mythical.count%, %stats_data.data.pet.dry_streak%, ... (all-time). */
    private static ValueResult foeStatsData(String[] p) {
        StatBook a = StatsTracker.allTime();
        if (p.length < 3 || !p[0].equals("data")) {
            return ValueResult.absent();
        }
        return switch (p[1]) {
            case "fish" -> {
                if (p[2].equals("total")) yield raw(a.fish);
                if (p.length < 5) yield ValueResult.absent();
                Map<String, Long> map = switch (p[2]) {
                    case "rarity" -> a.rarity;
                    case "size" -> a.size;
                    case "variant" -> a.variant;
                    default -> null;
                };
                if (map == null) yield ValueResult.absent();
                yield switch (p[4]) {
                    case "count" -> raw(a.count(map, p[3]));
                    case "dry_streak" -> raw(Math.max(0, a.since(p[2] + ":" + p[3])));
                    default -> ValueResult.absent();
                };
            }
            case "pet" -> switch (p[2]) {
                case "total", "both" -> raw(a.pets);
                case "dry_streak" -> raw(Math.max(0, a.since("pet")));
                default -> ValueResult.absent();
            };
            case "item" -> {
                if (p.length < 4) yield ValueResult.absent();
                String key = switch (p[2]) {
                    case "armorShard", "shard" -> "shard";
                    case "lightningBottle", "lightning_bottle" -> "lightning";
                    case "infusionCapsule", "infusion_capsule" -> "capsule";
                    default -> null;
                };
                if (key == null) yield ValueResult.absent();
                long count = switch (key) {
                    case "shard" -> a.shards;
                    case "lightning" -> a.lightningBottles;
                    default -> a.infusionCapsules;
                };
                yield switch (p[3]) {
                    case "count", "both" -> raw(count);
                    case "dry_streak" -> raw(Math.max(0, a.since(key)));
                    default -> ValueResult.absent();
                };
            }
            default -> ValueResult.absent();
        };
    }

    private static ValueResult raw(long v) {
        return ValueResult.str(String.valueOf(v));
    }

    private static String direction(float yaw) {
        return switch (Math.floorMod(Math.round(yaw / 90f), 4)) {
            case 0 -> "S";
            case 1 -> "W";
            case 2 -> "N";
            default -> "E";
        };
    }

    // ------------------------------------------------------------------ picker catalog

    private static List<Entry> buildCatalog() {
        List<Entry> c = new ArrayList<>();
        for (String scope : new String[]{"session", "alltime"}) {
            String cat = scope.equals("session") ? "this session" : "all-time";
            c.add(new Entry(cat, "%" + scope + ".fish%", "fish caught"));
            c.add(new Entry(cat, "%" + scope + ".per_hour%", "fish per hour of fishing"));
            c.add(new Entry(cat, "%" + scope + ".time%", "time spent fishing"));
            c.add(new Entry(cat, "%" + scope + ".value%", "total value of fish caught"));
            c.add(new Entry(cat, "%" + scope + ".xp%", "xp from fish"));
            c.add(new Entry(cat, "%" + scope + ".sold%", "money from selling fish"));
            c.add(new Entry(cat, "%" + scope + ".rarity_bar%", "coloured count per rarity"));
            c.add(new Entry(cat, "%" + scope + ".pets%", "pets dropped"));
            c.add(new Entry(cat, "%" + scope + ".shards%", "shards"));
            c.add(new Entry(cat, "%" + scope + ".lightning%", "lightning in a bottle"));
            c.add(new Entry(cat, "%" + scope + ".capsules%", "infusion capsules"));
            c.add(new Entry(cat, "%" + scope + ".quests%", "quests completed"));
            c.add(new Entry(cat, "%" + scope + ".heaviest%", "heaviest fish"));
            for (StatNames.Kind k : StatNames.RARITIES) {
                c.add(new Entry(cat, "%" + scope + ".rarity." + k.id() + "%", k.name() + " fish"));
            }
            for (StatNames.Kind k : StatNames.SIZES) {
                c.add(new Entry(cat, "%" + scope + ".size." + k.id() + "%", k.name() + " fish"));
            }
            for (StatNames.Kind k : StatNames.VARIANTS) {
                c.add(new Entry(cat, "%" + scope + ".variant." + k.id() + "%", k.name() + " fish"));
            }
        }
        String dry = "Fish since last (dry streak)";
        for (StatNames.Kind k : StatNames.RARITIES) {
            c.add(new Entry(dry, "%alltime.since.rarity." + k.id() + "%", "since last " + k.name()));
        }
        for (StatNames.Kind k : StatNames.SIZES) {
            c.add(new Entry(dry, "%alltime.since.size." + k.id() + "%", "since last " + k.name()));
        }
        for (StatNames.Kind k : StatNames.VARIANTS) {
            c.add(new Entry(dry, "%alltime.since.variant." + k.id() + "%", "since last " + k.name()));
        }
        c.add(new Entry(dry, "%alltime.since.pet%", "since last pet"));
        c.add(new Entry(dry, "%alltime.since.shard%", "since last shard"));
        c.add(new Entry(dry, "%alltime.since.lightning%", "since last lightning in a bottle"));
        c.add(new Entry(dry, "%alltime.since.capsule%", "since last infusion capsule"));

        String server = "FishOnMC";
        c.add(new Entry(server, "%boss_bar.location%", "where you are"));
        c.add(new Entry(server, "%boss_bar.time%", "in-game time"));
        c.add(new Entry(server, "%boss_bar.weather%", "weather / event"));
        c.add(new Entry(server, "%boss_bar.temperature%", "temperature"));
        c.add(new Entry(server, "%location.fish%", "fish caught here this session"));
        c.add(new Entry(server, "%scoreboard.wallet%", "wallet"));
        c.add(new Entry(server, "%scoreboard.credits%", "credits"));
        c.add(new Entry(server, "%scoreboard.catches%", "catches (server count)"));
        c.add(new Entry(server, "%scoreboard.catch_rate%", "catch rate"));
        c.add(new Entry(server, "%scoreboard.crew_name%", "crew name"));
        c.add(new Entry(server, "%scoreboard.crew_level%", "crew level"));
        c.add(new Entry(server, "%scoreboard.location_min%", "location min level"));
        c.add(new Entry(server, "%scoreboard.location_max%", "location max level"));
        c.add(new Entry(server, "%tab.count%", "players online"));

        String you = "You & time";
        c.add(new Entry(you, "%bobber.time%", "seconds your line has been out"));
        c.add(new Entry(you, "%player.level%", "fishing level"));
        c.add(new Entry(you, "%player.level_colored%", "fishing level in its level colour"));
        c.add(new Entry(you, "%player.level_bar%", "progress bar to the next level (add .30 for 30 characters long)"));
        c.add(new Entry(you, "%player.next_level%", "next level"));
        c.add(new Entry(you, "%player.xp_progress%", "% to next level"));
        c.add(new Entry(you, "%player.x%", "x"));
        c.add(new Entry(you, "%player.y%", "y"));
        c.add(new Entry(you, "%player.z%", "z"));
        c.add(new Entry(you, "%player.direction%", "facing (n/e/s/w)"));
        c.add(new Entry(you, "%time.clock%", "real time (24h)"));
        c.add(new Entry(you, "%time.clock12%", "real time (12h)"));
        c.add(new Entry(you, "%time.date%", "todays date"));

        String fn = "Logic & maths";
        c.add(new Entry(fn, "%condition.(<session.fish_raw>>100)%", "show the line only if true"));
        c.add(new Entry(fn, "%round.(<expression.(<session.value_raw>\\/<session.fish_raw>)>,1)%", "maths: average value per fish"));
        c.add(new Entry(fn, "%not.(<bobber.bite>)%", "show the line only if false"));
        return List.copyOf(c);
    }
}
