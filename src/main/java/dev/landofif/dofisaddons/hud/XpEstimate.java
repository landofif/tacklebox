package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.display.Holograms;
import dev.landofif.dofisaddons.menu.FishMenu;
import dev.landofif.dofisaddons.menu.Locations;
import dev.landofif.dofisaddons.screen.FishMenuScreen;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.util.Mono;
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
    private static final Pattern GAINED = Pattern.compile("^location > (.+?) gained " + NUM + "\\s*xp");

    private static double questXp, questLxp;
    /** Location id to {level, xp into the level}. */
    private static final Map<String, double[]> LOCATION = new HashMap<>();
    private static int baseLevel = -1;
    private static float baseProgress;
    private static double xpSince;
    private static int ticks;

    private XpEstimate() {}

    /** The wiki's location table: xp from level n-1 to n. */
    private static double need(int n) {
        return 250.0 * n * n * n;
    }

    private static double num(String digits, String suffix) {
        return StatsTracker.parseMoney(digits, suffix);
    }

    public static void tick(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p == null) {
            return;
        }
        if (p.experienceLevel != baseLevel || p.experienceProgress < baseProgress) {
            rebase(p);
        } else if (xpSince > 0 && p.experienceProgress - baseProgress >= 0.002f) {
            StatsTracker.allTime().need.put(String.valueOf(baseLevel), xpSince / (p.experienceProgress - baseProgress));
            StatsTracker.saveAllTime();
            rebase(p);
        }
        if (++ticks % 10 == 0) {
            if (client.currentScreen instanceof FishMenuScreen f) {
                scan(f.original());
            } else if (client.currentScreen instanceof HandledScreen<?> screen) {
                scan(screen);
            }
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

    /** Called with the normalised lowercase chat line, for "location > lake biwa gained 4.17k xp". */
    public static void onChat(String lower) {
        Matcher m = GAINED.matcher(lower);
        Locations.Location loc = m.find() ? Locations.byName(m.group(1)) : null;
        double[] at = loc == null ? null : LOCATION.get(loc.id());
        if (at != null) {
            at[1] += num(m.group(2), m.group(3));
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
            if (!quests || lore == null) {
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
    }

    /** Quests until the next fishing level, like "~12", or null while it is not known yet. */
    public static String levelQuests() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        Double need = p == null ? null : StatsTracker.allTime().need.get(String.valueOf(p.experienceLevel));
        return need == null || questXp <= 0 ? null : "~" + (long) Math.ceil((1 - p.experienceProgress) * need / questXp);
    }

    private static double[] here() {
        Locations.Location loc = Locations.byName(ServerInfo.locationName);
        return loc == null ? null : LOCATION.get(loc.id());
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
            w = Math.max(w, tr.getWidth(Mono.text(r[0])) + 12 + tr.getWidth(Mono.text(r[1])));
        }
        w += 16;
        int h = 14 + rows.length * 11;
        var s = StatsTracker.settings();
        S24.panel(ctx, s.questsX, s.questsY, w, h, Mono.text("quests left"));
        for (int i = 0; i < rows.length; i++) {
            int y = s.questsY + 9 + i * 11;
            ctx.drawText(tr, Mono.text(rows[i][0]), s.questsX + 8, y, S24.TEXT_4, false);
            Text v = Mono.text(rows[i][1]);
            ctx.drawText(tr, v, s.questsX + w - 8 - tr.getWidth(v), y, S24.TEXT_1, false);
        }
        return new int[]{s.questsX, s.questsY, w, h};
    }

    public static void renderHud(DrawContext ctx) {
        if (StatsTracker.settings().questsHud && dev.landofif.dofisaddons.trigger.TriggerEngine.isOnFishOnMc()) {
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
