package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.ChummerRing;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.trigger.Matcher;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.util.ChatColors;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * What is switched on right now, read from chat lines only (the boss bar and scoreboard are not reliable, luna 2026-10-09): player
 * boosters with a countdown (the line names the length), the chummer, and the weather events and moon that {@link ServerHud}
 * already tracks. Text with a shadow and no box, like the quests left hud.
 */
public final class BoostsHud {
    // BOOSTER » Ewiksen has activated a Global Luck Booster » 2x for 30m! (/tipall)
    private static final Pattern BOOSTER = Pattern.compile("^[^A-Za-z0-9]*BOOSTER [>»] (\\w+) has activated a Global (.+?) Booster [>»] (\\d+)x for (\\d+)([hms])");
    // the /active reply, one line per booster: Global Luck Booster » 2x activated by Bostonfan3334 with 45m 48s remaining. Use /tipall ...
    private static final Pattern ACTIVE = Pattern.compile("^[^A-Za-z0-9]*Global (.+?) Booster [>»] (\\d+)x activated by (\\w+) with ((?:\\d+[hms] ?)+) remaining");
    private static final Pattern PART = Pattern.compile("(\\d+)([hms])");
    // BOOSTER » Global Luck Booster from Ewiksen has expired!
    private static final Pattern EXPIRED = Pattern.compile("^[^A-Za-z0-9]*BOOSTER [>»] Global (.+?) Booster from (\\w+) has expired");
    // CHUMMER » noawah activated a Chummer. (+75 Bite Speed)   and   CHUMMER » Chummer has run out of stock.
    private static final Pattern CHUMMER = Pattern.compile("^[^A-Za-z0-9]*CHUMMER [>»] \\w+ (?:activated|has placed) an? .*?Chummer\\.?(?: \\(([^)]*)\\))?");
    private static final Pattern RUN_OUT = Pattern.compile("^[^A-Za-z0-9]*CHUMMER [>»] Chummer has run out of stock");

    /** The chat line does not say how long a chummer lasts; every one in the logs ran out 15:14 to 15:23 after its line. */
    private static final long CHUMMER_MS = 15 * 60_000L, GRACE = 30_000L;
    /** The green of "Bite Speed:" on a placed chummer's hologram, and the white chat gives player names. */
    private static final int HOLOGRAM_GREEN = 0x41F55B, WHITE = 0xFFFFFF;

    /** Words of a row and the colour chat gave them ({@link ChatColors#NONE}: the hud's own grey, or white for the value). */
    record Part(String text, int color) {}

    /** One row: the parts on the left, then the time left or the bonus on the right. */
    record Row(List<Part> label, Part value) {}

    private record Booster(String type, String times, int typeColor, int timesColor, long end) {}

    /** By "type|player", so the expiry line (it names both) takes out the right one. Kept across servers, they are global. */
    private static final Map<String, Booster> BOOSTERS = new LinkedHashMap<>();
    private static long chummerEnd;
    private static String chummerBonus = "";
    private static int chummerColor = ChatColors.NONE;
    private static long frenzyEnd;
    private static String frenzyPlace = "";
    private static int frenzyColor = ChatColors.NONE, frenzyPlaceColor = ChatColors.NONE;
    private static long frenzySeen;

    private BoostsHud() {}

    /** Milliseconds left of the frenzy the chat announced, 0 when none is running. */
    static long frenzyLeft(long now) {
        return Math.max(0, frenzyEnd - now);
    }

    /** When the chat last announced a frenzy, 0 if never. */
    static long frenzySeen() {
        return frenzySeen;
    }

    static String frenzyPlace() {
        return frenzyPlace;
    }

    public static void onChat(Text message) {
        String s = Matcher.normalize(message.getString());
        long now = System.currentTimeMillis();
        EventLines.Frenzy frenzy = EventLines.frenzy(s);
        if (frenzy != null) {
            frenzyEnd = now + frenzy.remainingMs();
            frenzySeen = now;
            frenzyPlace = Locations.shown(frenzy.place()); // as shown; the colour is looked up by the name as sent
            frenzyColor = ChatColors.of(message, "Fish Frenzy"); // the alert's "Fish at X" line has only "Fish"
            frenzyPlaceColor = ChatColors.of(message, frenzy.place());
            return;
        }
        if (EventLines.frenzyEnded(s)) {
            frenzyEnd = 0;
            return;
        }
        java.util.regex.Matcher m = BOOSTER.matcher(s);
        if (m.find()) {
            String type = m.group(2);
            BOOSTERS.put(type + "|" + m.group(1), new Booster(type, m.group(3) + "x", ChatColors.of(message, type + " Booster"),
                    ChatColors.of(message, m.group(3) + "x for"), now + millis(m.group(4) + m.group(5))));
            return;
        }
        m = ACTIVE.matcher(s);
        if (m.find()) {
            String type = m.group(1);
            BOOSTERS.put(type + "|" + m.group(3), new Booster(type, m.group(2) + "x", ChatColors.of(message, type + " Booster"),
                    ChatColors.of(message, m.group(2) + "x activated"), now + millis(m.group(4))));
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
            chummerColor = ChatColors.of(message, chummerBonus.isEmpty() ? "Chummer" : chummerBonus);
        }
    }

    /** "60m" or "1h 2m 3s" in milliseconds. */
    private static long millis(String length) {
        long ms = 0;
        java.util.regex.Matcher m = PART.matcher(length);
        while (m.find()) {
            long unit = switch (m.group(2)) {
                case "h" -> 3_600_000L;
                case "m" -> 60_000L;
                default -> 1000L;
            };
            ms += Long.parseLong(m.group(1)) * unit;
        }
        return ms;
    }

    /** The boosters stay (they are global, the /active sent on game start fills them in), the chummer is per server. */
    public static void reset() {
        chummerEnd = 0;
    }

    /** Each row's words in the colours chat gives them (luna 2026-10-28), so the timers stay white. */
    static List<Row> rows() {
        long now = System.currentTimeMillis();
        List<Row> rows = new ArrayList<>();
        BOOSTERS.values().removeIf(b -> b.end() < now);
        for (Booster b : BOOSTERS.values()) {
            rows.add(new Row(List.of(new Part(b.type() + " booster", b.typeColor()), new Part(b.times(), b.timesColor())), timer(b.end() - now)));
        }
        List<ChummerRing.Detected> holograms = ChummerRing.detected();
        for (ChummerRing.Detected d : holograms) {
            rows.add(new Row(List.of(new Part("chummer" + (d.bite() >= 0 ? " +" + d.bite() : ""), HOLOGRAM_GREEN), new Part(d.owner(), WHITE)),
                    d.endMs() > 0 ? timer(Math.max(0, d.endMs() - now)) : new Part("", ChatColors.NONE)));
        }
        // the chat line is the fallback for a chummer whose hologram the client does not track
        if (holograms.isEmpty() && chummerEnd + GRACE > now) {
            rows.add(new Row(List.of(new Part(("chummer " + chummerBonus).trim(), chummerColor)), timer(Math.max(0, chummerEnd - now))));
        }
        if (frenzyEnd > now) {
            rows.add(new Row(List.of(new Part("fish frenzy", frenzyColor), new Part(frenzyPlace, frenzyPlaceColor)), timer(frenzyEnd - now)));
        }
        rows.addAll(ServerHud.activeEvents());
        Row moon = ServerHud.activeMoon();
        if (moon != null) {
            rows.add(moon);
        }
        return rows;
    }

    private static Part timer(long ms) {
        return new Part(ServerHud.clock(ms), ChatColors.NONE);
    }

    /** The parts in small capitals with a space between them, each in its chat colour. */
    static MutableText text(List<Part> parts) {
        MutableText out = Text.empty();
        for (Part p : parts) {
            if (p.text().isEmpty()) {
                continue;
            }
            if (!out.getSiblings().isEmpty()) {
                out.append(" ");
            }
            MutableText t = Foe.text(p.text());
            out.append(p.color() == ChatColors.NONE ? t : t.styled(st -> st.withColor(p.color())));
        }
        return out;
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}; with preview an empty hud still shows a line so it can be moved. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        TackleboxSettings s = TackleboxSettings.get();
        return s.boostsPos.draw(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), (x, y) -> {
            s.boostsX = x;
            s.boostsY = y;
            return Foe.scaled(ctx, x, y, s.boostsScale, c -> drawAt100(c, preview));
        });
    }

    private static int[] drawAt100(DrawContext ctx, boolean preview) {
        List<Row> rows = rows();
        if (rows.isEmpty()) {
            if (!preview) {
                return null;
            }
            rows.add(new Row(List.of(new Part("nothing active", ChatColors.NONE)), new Part("", ChatColors.NONE)));
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = 0;
        for (Row r : rows) {
            w = Math.max(w, tr.getWidth(text(r.label())) + (r.value().text().isEmpty() ? 0 : 12 + tr.getWidth(text(List.of(r.value())))));
        }
        w += 16;
        TackleboxSettings s = TackleboxSettings.get();
        int top = s.boostsHeading ? 20 : 4, h = top + 5 + rows.size() * 11;
        if (s.boostsHeading) {
            Foe.title(ctx, s.boostsX, s.boostsY, w, s.boostsAlign, Foe.text("active"));
        }
        for (int i = 0; i < rows.size(); i++) {
            int y = s.boostsY + top + i * 11;
            Text l = text(rows.get(i).label()), v = text(List.of(rows.get(i).value()));
            int lx = s.boostsX + 8, vx = s.boostsX + w - 8 - tr.getWidth(v); // left keeps the values in a column on the right
            if (s.boostsAlign != TackleboxSettings.HudAlign.LEFT) {
                lx = s.boostsAlign.x(s.boostsX + 8, w - 16, tr.getWidth(l) + (v.getString().isEmpty() ? 0 : 12 + tr.getWidth(v)));
                vx = lx + tr.getWidth(l) + 12;
            }
            ctx.drawTextWithShadow(tr, l, lx, y, Foe.DIM);
            ctx.drawTextWithShadow(tr, v, vx, y, Foe.TEXT);
        }
        return new int[]{s.boostsX, s.boostsY, w, h};
    }

    public static void renderHud(DrawContext ctx) {
        if (TackleboxSettings.get().boostsHud && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }
}
