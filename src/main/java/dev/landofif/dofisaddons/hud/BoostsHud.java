package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.trigger.Matcher;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * What is switched on right now, read from chat lines only (the boss bar and scoreboard are not reliable, l 2026-10-09): player
 * boosters with a countdown (the line names the length), the chummer, and the weather events and moon that {@link ServerHud}
 * already tracks. Text with a shadow and no box, like the quests left hud.
 */
public final class BoostsHud {
    // BOOSTER » Ewiksen has activated a Global Luck Booster » 2x for 30m! (/tipall)
    private static final Pattern BOOSTER = Pattern.compile("^[^A-Za-z0-9]*BOOSTER [>»] (\\w+) has activated a Global (.+?) Booster [>»] (\\d+)x for (\\d+)([hms])");
    // BOOSTER » Global Luck Booster from Ewiksen has expired!
    private static final Pattern EXPIRED = Pattern.compile("^[^A-Za-z0-9]*BOOSTER [>»] Global (.+?) Booster from (\\w+) has expired");
    // CHUMMER » noawah activated a Chummer. (+75 Bite Speed)   and   CHUMMER » Chummer has run out of stock.
    private static final Pattern CHUMMER = Pattern.compile("^[^A-Za-z0-9]*CHUMMER [>»] \\w+ (?:activated|has placed) an? .*?Chummer\\.?(?: \\(([^)]*)\\))?");
    private static final Pattern RUN_OUT = Pattern.compile("^[^A-Za-z0-9]*CHUMMER [>»] Chummer has run out of stock");

    /** The chat line does not say how long a chummer lasts; every one in the logs ran out 15:14 to 15:23 after its line. */
    private static final long CHUMMER_MS = 15 * 60_000L, GRACE = 30_000L;

    private record Booster(String label, long end) {}

    /** By "type|player", so the expiry line (it names both) takes out the right one. Kept across servers, they are global. */
    private static final Map<String, Booster> BOOSTERS = new LinkedHashMap<>();
    private static long chummerEnd;
    private static String chummerBonus = "";

    private BoostsHud() {}

    public static void onChat(Text message) {
        String s = Matcher.normalize(message.getString());
        long now = System.currentTimeMillis();
        java.util.regex.Matcher m = BOOSTER.matcher(s);
        if (m.find()) {
            long unit = switch (m.group(5)) {
                case "h" -> 3_600_000L;
                case "m" -> 60_000L;
                default -> 1000L;
            };
            BOOSTERS.put(m.group(2) + "|" + m.group(1), new Booster(m.group(2) + " booster " + m.group(3) + "x", now + Long.parseLong(m.group(4)) * unit));
            return;
        }
        m = EXPIRED.matcher(s);
        if (m.find()) {
            BOOSTERS.remove(m.group(1) + "|" + m.group(2));
            return;
        }
        if (RUN_OUT.matcher(s).find()) {
            chummerEnd = 0;
            return;
        }
        m = CHUMMER.matcher(s);
        if (m.find()) {
            chummerEnd = now + CHUMMER_MS;
            chummerBonus = m.group(1) == null ? "" : m.group(1);
        }
    }

    /** The boosters stay (they are global and nothing repeats them on joining), the chummer is per server. */
    public static void reset() {
        chummerEnd = 0;
    }

    private static List<String[]> rows() {
        long now = System.currentTimeMillis();
        List<String[]> rows = new ArrayList<>();
        BOOSTERS.values().removeIf(b -> b.end() < now);
        for (Booster b : BOOSTERS.values()) {
            rows.add(new String[]{b.label(), ServerHud.clock(b.end() - now)});
        }
        if (chummerEnd + GRACE > now) {
            rows.add(new String[]{("chummer " + chummerBonus).trim(), ServerHud.clock(Math.max(0, chummerEnd - now))});
        }
        for (String[] e : ServerHud.activeEvents()) {
            rows.add(e);
        }
        String[] moon = ServerHud.activeMoon();
        if (moon != null) {
            rows.add(moon);
        }
        return rows;
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}; with preview an empty hud still shows a line so it can be moved. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        DofiSettings s = DofiSettings.get();
        return Foe.scaled(ctx, s.boostsX, s.boostsY, s.boostsScale, c -> drawAt100(c, preview));
    }

    private static int[] drawAt100(DrawContext ctx, boolean preview) {
        List<String[]> rows = rows();
        if (rows.isEmpty()) {
            if (!preview) {
                return null;
            }
            rows.add(new String[]{"nothing active", ""});
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = 0;
        for (String[] r : rows) {
            w = Math.max(w, tr.getWidth(Foe.text(r[0])) + (r[1].isEmpty() ? 0 : 12 + tr.getWidth(Foe.text(r[1]))));
        }
        w += 16;
        int h = 25 + rows.size() * 11;
        DofiSettings s = DofiSettings.get();
        Foe.title(ctx, s.boostsX, s.boostsY, Foe.text("active"));
        for (int i = 0; i < rows.size(); i++) {
            int y = s.boostsY + 20 + i * 11;
            ctx.drawTextWithShadow(tr, Foe.text(rows.get(i)[0]), s.boostsX + 8, y, Foe.DIM);
            Text v = Foe.text(rows.get(i)[1]);
            ctx.drawTextWithShadow(tr, v, s.boostsX + w - 8 - tr.getWidth(v), y, Foe.TEXT);
        }
        return new int[]{s.boostsX, s.boostsY, w, h};
    }

    public static void renderHud(DrawContext ctx) {
        if (DofiSettings.get().boostsHud && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }
}
