package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.display.Holograms;
import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.menu.Locations;
import dev.landofif.dofisaddons.menu.Power;
import dev.landofif.dofisaddons.screen.FishMenuScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.font.TextRenderer;
import dev.landofif.dofisaddons.util.Glyphs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Estimates how many quests are left until your next fishing level and your next location level.
 * Quest rewards come from the /quests tooltips ("16.15K-23.07Kxp", "3.12K-4.17Klxp", the middle of
 * each range). Location xp per level is the wiki table (250 * n^3). Fishing xp per level is not
 * published, so it is learned from the xp bar: popup xp divided by how far the bar moved.
 */
public final class XpEstimate {
    private static final String NUM = "([0-9][0-9.,]*)\\s*([kmb]?)";
    private static final Pattern REWARD = Pattern.compile(NUM + "(?:\\s*-\\s*" + NUM + ")?\\s*(l?)xp", Pattern.CASE_INSENSITIVE);
    private static final Pattern POPUP = Pattern.compile("\\+" + NUM + "(l?)xp", Pattern.CASE_INSENSITIVE);
    /** The quest reward line "+ 3.35Klxp" (location xp, for the location you are at). */
    private static final Pattern LXP_LINE = Pattern.compile("^\\s*\\+\\s*" + NUM + "\\s*lxp\\s*$");
    private static final Pattern GAINED = Pattern.compile("^location > (.+?) gained " + NUM + "\\s*xp");

    /** The profile item in /menu: "Fishing Level [274]" then "(1.02M/3.73M)". */
    private static final Pattern PROFILE = Pattern.compile("fishing level \\[(\\d+)\\]\\s*\\(\\s*" + NUM + "\\s*/\\s*" + NUM + "\\s*\\)", Pattern.CASE_INSENSITIVE);

    private static final Pattern STATS = Pattern.compile("(Bite Speed|Reel Speed|Prospect|(?<!pet )Luck|Scale)\\W{0,3}([\\d,.]+[KkMm]?(?: \\([^)]*\\))?)", Pattern.CASE_INSENSITIVE); // Glyphs.plain lowercases

    private static double questXp, questLxp;
    /** {level, xp the level takes} from the /menu profile, or null until it was opened. */
    private static double[] profile;
    /** Location id to {level, xp into the level}. */
    private static final Map<String, double[]> LOCATION = new HashMap<>();
    private static boolean loaded;
    private static int baseLevel = -1;
    private static float baseProgress;
    private static double xpSince;
    private static int ticks;

    private XpEstimate() {}

    /** The wiki's location table: xp from level n-1 to n. */
    public static double need(int n) {
        return 250.0 * n * n * n;
    }

    public static double num(String digits, String suffix) {
        double v;
        try {
            v = Double.parseDouble(digits.replace(",", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
        return switch (suffix.toUpperCase(Locale.ROOT)) {
            case "K" -> v * 1_000;
            case "M" -> v * 1_000_000;
            case "B" -> v * 1_000_000_000;
            default -> v;
        };
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null) {
            return;
        }
        if (!loaded) { // what earlier sessions read is kept in the settings file
            loaded = true;
            var s = DofiSettings.get();
            questXp = s.questXp;
            questLxp = s.questLxp;
            profile = s.profile;
            LOCATION.putAll(s.locLevels);
            s.locLevels = LOCATION;
        }
        if (p.experienceLevel != baseLevel || p.experienceProgress < baseProgress) {
            rebase(p);
        } else if (xpSince > 0 && p.experienceProgress - baseProgress >= 0.002f) {
            DofiSettings.get().need.put(String.valueOf(baseLevel), xpSince / (p.experienceProgress - baseProgress));
            DofiSettings.save();
            rebase(p);
        }
        HandledScreen<?> open = client.currentScreen instanceof FishMenuScreen f ? f.original()
                : client.currentScreen instanceof HandledScreen<?> screen ? screen : null;
        if (open != null) {
            dev.landofif.dofisaddons.menu.Compendium.scan(open); // every tick: pages flipped faster than the half second below were missed
        }
        if (++ticks % 10 == 0 && open != null) {
            scan(open);
            Power.scan(open);
        }
    }

    private static void rebase(ClientPlayerEntity p) {
        baseLevel = p.experienceLevel;
        baseProgress = p.experienceProgress;
        xpSince = 0;
    }

    /** Called for every action bar message, before the popup is hidden. */
    public static void onPopup(String raw) {
        Matcher m = POPUP.matcher(Holograms.fold(raw));
        while (m.find()) {
            if (m.group(3).isEmpty()) {
                xpSince += num(m.group(1), m.group(2));
            }
        }
    }

    /** Every game chat line: "location > lake biwa gained 4.17k xp" moves the location level along. */
    public static void onChat(net.minecraft.text.Text message) {
        if (!TriggerEngine.isOnFishOnMc()) {
            return;
        }
        String lower = dev.landofif.dofisaddons.trigger.Matcher.normalize(message.getString()).toLowerCase(Locale.ROOT);
        Matcher m = GAINED.matcher(lower);
        Locations.Location loc = m.find() ? Locations.byName(m.group(1)) : null;
        double[] at = loc == null ? null : LOCATION.get(loc.id());
        if (at != null) {
            at[1] += num(m.group(2), m.group(3));
            return;
        }
        m = LXP_LINE.matcher(lower);
        at = m.find() ? here() : null;
        if (at != null) {
            at[1] += num(m.group(1), m.group(2));
        }
    }

    private static void scan(HandledScreen<?> screen) {
        boolean quests = "quests".equals(Glyphs.menuName(screen.getTitle()));
        double xp = 0, lxp = 0;
        int xpN = 0, lxpN = 0;
        for (Slot slot : screen.getScreenHandler().slots) {
            ItemStack stack = slot.getStack();
            if (stack.isEmpty() || slot.inventory instanceof net.minecraft.entity.player.PlayerInventory) {
                continue;
            }
            Locations.Location loc = Locations.byName(Glyphs.plain(stack.getName()));
            FishMenu.Info info = loc == null ? null : FishMenu.info(stack);
            if (info != null && info.level() >= 0 && info.progress() >= 0) {
                LOCATION.put(loc.id(), new double[]{info.level(), info.progress() * need(info.level() + 1)});
            }
            LoreComponent lore = stack.get(DataComponentTypes.LORE);
            if (lore == null) {
                continue;
            }
            String text = lore.lines().stream().map(Glyphs::plain).collect(java.util.stream.Collectors.joining(" "));
            Matcher pm = PROFILE.matcher(text);
            if (pm.find()) {
                Matcher st = STATS.matcher(text);
                while (st.find()) { // the speeds and prospect sit in the same item, the server hud shows them
                    DofiSettings.get().profileStats.put(st.group(1).toLowerCase(Locale.ROOT), st.group(2));
                }
                profile = new double[]{Integer.parseInt(pm.group(1)), num(pm.group(4), pm.group(5))};
            }
            if (!quests) {
                continue;
            }
            for (Text line : lore.lines()) {
                Matcher m = REWARD.matcher(line.getString());
                if (!m.find()) {
                    continue;
                }
                double lo = num(m.group(1), m.group(2));
                double mid = m.group(3) == null ? lo : (lo + num(m.group(3), m.group(4))) / 2;
                if (m.group(5).isEmpty()) {
                    xp += mid;
                    xpN++;
                } else {
                    lxp += mid;
                    lxpN++;
                }
            }
        }
        if (xpN > 0) {
            questXp = xp / xpN;
        }
        if (lxpN > 0) {
            questLxp = lxp / lxpN;
        }
        var s = DofiSettings.get();
        s.questXp = questXp;
        s.questLxp = questLxp;
        s.profile = profile;
    }

    /** 3350 as "3.35K", for the xp shown next to the location bar. */
    public static String abbrevXp(double v) {
        return v >= 1_000_000 ? String.format(Locale.ROOT, "%.2fM", v / 1_000_000) : v >= 1000 ? String.format(Locale.ROOT, "%.2fK", v / 1000) : String.valueOf((long) v);
    }

    /** Quests until the next fishing level, like "~12", or null while it is not known yet. */
    public static String levelQuests() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p != null && profile != null && p.experienceLevel == (int) profile[0] && questXp > 0) {
            return "~" + (long) Math.ceil((1 - p.experienceProgress) * profile[1] / questXp);
        }
        Double need = p == null ? null : DofiSettings.get().need.get(String.valueOf(p.experienceLevel));
        return need == null || questXp <= 0 ? null : "~" + (long) Math.ceil((1 - p.experienceProgress) * need / questXp);
    }

    private static double[] here() {
        Locations.Location loc = Locations.byName(ServerInfo.locationName);
        return loc == null ? null : LOCATION.get(loc.id());
    }

    /** Location xp one quest gives, or 0 until /quests was read. */
    public static double questLxp() {
        return questLxp;
    }

    /** {level, xp left to the next one} of a location from the menus plus the xp since, or null while it is unknown. */
    public static double[] toNext(String id) {
        double[] at = LOCATION.get(id);
        if (at == null) {
            return null;
        }
        int level = (int) at[0] + levelsGained(at);
        double into = at[1];
        for (int n = (int) at[0] + 1; n <= level; n++) {
            into -= need(n);
        }
        return new double[]{level, need(level + 1) - into};
    }

    /** The location level seen in the menus, or null. */
    public static String locationLevel() {
        double[] at = here();
        return at == null ? null : String.valueOf((int) at[0] + levelsGained(at));
    }

    /** Quests until the next level of the location you are at, or null while it is not known yet. */
    public static String locationQuests() {
        double[] at = here();
        if (at == null || questLxp <= 0) {
            return null;
        }
        int level = (int) at[0] + levelsGained(at);
        double into = at[1];
        for (int n = (int) at[0] + 1; n <= level; n++) {
            into -= need(n);
        }
        return "~" + (long) Math.ceil((need(level + 1) - into) / questLxp);
    }

    /** Draws the box at its saved spot and returns {x, y, w, h}; with preview it shows dashes for unknown values. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        String level = levelQuests();
        String loc = locationQuests();
        if (p == null || !preview && level == null && loc == null) {
            return null;
        }
        String where = locationLevel();
        String[][] rows = {
                {"to level " + (p.experienceLevel + 1), level == null ? "-" : level},
                {"to " + (ServerInfo.locationName.isEmpty() ? "location" : ServerInfo.locationName) + " level "
                        + (where == null ? "?" : String.valueOf(Integer.parseInt(where) + 1)), loc == null ? "-" : loc}};
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = 0;
        for (String[] r : rows) {
            w = Math.max(w, tr.getWidth(Foe.text(r[0])) + 12 + tr.getWidth(Foe.text(r[1])));
        }
        w += 16;
        int h = 25 + rows.length * 11;
        var s = DofiSettings.get();
        Foe.title(ctx, s.questsX, s.questsY, Foe.text("quests left")); // no box behind it
        for (int i = 0; i < rows.length; i++) {
            int y = s.questsY + 20 + i * 11;
            ctx.drawTextWithShadow(tr, Foe.text(rows[i][0]), s.questsX + 8, y, Foe.DIM);
            Text v = Foe.text(rows[i][1]);
            ctx.drawTextWithShadow(tr, v, s.questsX + w - 8 - tr.getWidth(v), y, Foe.TEXT);
        }
        return new int[]{s.questsX, s.questsY, w, h};
    }

    public static void renderHud(DrawContext ctx) {
        if (DofiSettings.get().questsHud && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, true); // dashes until /quests, /travel and some catches have taught it the numbers
        }
    }

    /** Levels passed by xp that arrived after the menu was last read. */
    private static int levelsGained(double[] at) {
        int n = 0;
        double into = at[1];
        while (into >= need((int) at[0] + n + 1)) {
            into -= need((int) at[0] + ++n);
        }
        return n;
    }
}
