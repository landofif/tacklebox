package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.mixin.BossBarHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ClientBossBar;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardDisplaySlot;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads what FishOnMC already shows you: the sidebar (wallet, credits, catches, crew, location
 * level range) and the boss bar (time, weather, temperature, location). Read-only, a few times a
 * second.
 */
public final class ServerInfo {
    public static String wallet = "";
    public static String credits = "";
    public static String catches = "";
    public static String catchRate = "";
    public static String crewName = "";
    public static String crewLevel = "";
    public static String locationMin = "";
    public static String locationMax = "";

    public static String time = "";
    public static String ampm = "";
    public static String weather = "";
    public static String temperature = "";
    public static String locationId = "";
    public static String locationName = "";

    private static final Pattern TIME = Pattern.compile("(\\d{1,2}:\\d{2})\\s*(AM|PM)");
    private static final Pattern TEMP = Pattern.compile("(-?\\d+)\\s*°");

    /** FishOnMC location ids and names, from the server's own item data. */
    public static final Map<String, String> LOCATIONS = Map.ofEntries(
            Map.entry("spawn", "Cypress Lake"),
            Map.entry("kenai", "Kenai River"),
            Map.entry("biwa", "Lake Biwa"),
            Map.entry("murray", "Murray River"),
            Map.entry("everglades", "Everglades"),
            Map.entry("keywest", "Key West"),
            Map.entry("toledobend", "Toledo Bend Reservoir"),
            Map.entry("greatlakes", "Great Lakes"),
            Map.entry("danube", "Danube River"),
            Map.entry("oilrig", "Oil Rig"),
            Map.entry("amazon", "Amazon River"),
            Map.entry("mediterranean", "Mediterranean Sea"),
            Map.entry("capecod", "Cape Cod"),
            Map.entry("hawaii", "Hawaii"),
            Map.entry("lofotenislands", "Lofoten Islands"),
            Map.entry("cairns", "Cairns"),
            Map.entry("crewisland", "Crew Island"));

    private static int ticks;

    private ServerInfo() {}

    public static void tick(MinecraftClient client) {
        if (ticks++ % 10 != 0 || client.world == null) {
            return;
        }
        try {
            readSidebar(client);
        } catch (RuntimeException ignored) {
            // a half-sent scoreboard; try again next time
        }
        try {
            readBossBar(client);
        } catch (RuntimeException ignored) {
            // same
        }
    }

    private static void readSidebar(MinecraftClient client) {
        Scoreboard scoreboard = client.world.getScoreboard();
        ScoreboardObjective objective = scoreboard.getObjectiveForSlot(ScoreboardDisplaySlot.SIDEBAR);
        if (objective == null) {
            return;
        }
        for (Team team : scoreboard.getTeams()) {
            String s = team.getPrefix().getString();
            if (s.isEmpty()) continue;
            if (s.contains("ᴡᴀʟʟᴇᴛ") && s.contains("$")) wallet = s.substring(s.indexOf('$') + 1).trim();
            if (s.contains("ᴄʀᴇᴅɪᴛꜱ")) credits = afterLastGlyph(s);
            if (s.contains("ᴄᴀᴛᴄʜ ʀᴀᴛᴇ")) catchRate = afterColon(s);
            else if (s.contains("ᴄᴀᴛᴄʜᴇꜱ")) catches = afterColon(s);
            if (s.contains("ᴄʀᴇᴡ:")) crewName = between(s, '[', ']');
            if (s.contains("┠ ʟᴇᴠᴇʟ")) crewLevel = between(s, '[', ']');
            if (s.contains("┠ ʟᴏᴄᴀᴛɪᴏɴ") && !s.contains("---") && s.contains("/")) {
                String range = afterColon(s);
                int slash = range.indexOf('/');
                locationMin = range.substring(0, slash).trim();
                locationMax = range.substring(slash + 1).trim();
            }
        }
    }

    private static void readBossBar(MinecraftClient client) {
        Map<?, ClientBossBar> bars = ((BossBarHudAccessor) client.inGameHud.getBossBarHud()).tacklebox$getBossBars();
        for (ClientBossBar bar : bars.values()) {
            String s = bar.getName().getString();
            Matcher t = TIME.matcher(s);
            if (!t.find()) {
                continue;
            }
            time = t.group(1);
            ampm = t.group(2);
            Matcher temp = TEMP.matcher(s);
            temperature = temp.find() ? temp.group(1) + "°" : "";
            weather = weatherOf(s);
            locationId = "";
            locationName = "";
            for (Map.Entry<String, String> e : LOCATIONS.entrySet()) {
                if (s.contains(e.getValue())) {
                    locationId = e.getKey();
                    locationName = e.getValue().toLowerCase(java.util.Locale.ROOT); // matched as sent, shown lowercase
                    break;
                }
            }
            return;
        }
    }

    private static String weatherOf(String s) {
        if (s.contains("⚡")) return "thunderstorm";
        if (s.contains("☂")) return "rain";
        if (s.contains("♣")) return "blooming oasis";
        if (s.contains("⭐")) return "fabled";
        if (s.contains("⚠")) return "gold rush";
        if (s.contains("○")) return "moon";
        if (s.contains("☀")) return "clear";
        return "";
    }

    private static String afterColon(String s) {
        int i = s.indexOf(':');
        return i < 0 ? "" : s.substring(i + 1).trim();
    }

    private static String afterLastGlyph(String s) {
        for (int i = s.length() - 1; i >= 0; i--) {
            char c = s.charAt(i);
            if (c >= '' && c <= '') {
                return s.substring(i + 1).trim();
            }
        }
        return afterColon(s);
    }

    private static String between(String s, char open, char close) {
        int a = s.indexOf(open);
        int b = s.lastIndexOf(close);
        return a >= 0 && b > a ? s.substring(a + 1, b).trim() : "";
    }
}
