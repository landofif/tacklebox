package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.menu.Power;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;


/**
 * Two thin progress bars, no box like the other huds: the fishing level (the vanilla xp bar) and the level of the location you
 * are at ({@link XpEstimate#toNext}: read from the compendium, then moved along by the location xp of quest rewards and chat).
 */
public final class LevelBars {
    /** The location level in brackets is always this orange (minecraft gold), whatever the level. */
    private static final int LOCATION_LEVEL = 0xFFFFAA00;

    private LevelBars() {}

    public static int[] draw(DrawContext ctx, boolean preview) {
        TackleboxSettings s = TackleboxSettings.get();
        return s.levelBarsPos.draw(ctx, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight(), (x, y) -> {
            s.levelBarsX = x;
            s.levelBarsY = y;
            return Foe.scaled(ctx, x, y, s.levelBarsScale, c -> drawAt100(c, preview));
        });
    }

    private static int[] drawAt100(DrawContext ctx, boolean preview) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) {
            return null;
        }
        TackleboxSettings s = TackleboxSettings.get();
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        var loc = Locations.byName(ServerInfo.locationName);
        double[] at = loc == null ? null : XpEstimate.toNext(loc.id());

        String[] label = new String[2];
        double[] fraction = new double[2];
        String[] extra = {"", ""};
        int[] number = new int[2]; // colour of the level inside the brackets, 0 = same as the label
        int rows = 0;
        if (s.levelBarsFishing) {
            label[rows] = "level [" + p.experienceLevel + "]";
            number[rows] = LevelColor.of(p.experienceLevel);
            Double need = s.levelBarsMainXp ? XpEstimate.levelNeed() : null;
            if (need != null) {
                extra[rows] = XpEstimate.abbrevXp(p.experienceProgress * need, s.levelBarsDecimals) + "/" + XpEstimate.abbrevXp(need, s.levelBarsDecimals) + "xp";
            }
            fraction[rows++] = p.experienceProgress;
        }
        if (s.levelBarsLocation && at != null) {
            int level = (int) at[0];
            double next = XpEstimate.need(level + 1);
            boolean max = level >= Power.MAX_LEVEL;
            label[rows] = loc.name() + " [" + level + "]";
            number[rows] = LOCATION_LEVEL;
            extra[rows] = !s.levelBarsXp ? "" : max ? "max" : XpEstimate.abbrevXp(next - at[1], s.levelBarsDecimals) + "/" + XpEstimate.abbrevXp(next, s.levelBarsDecimals) + "lxp";
            fraction[rows++] = max ? 1 : (next - at[1]) / next;
        } else if (s.levelBarsLocation && preview) {
            label[rows++] = "open /comp";
        }
        if (rows == 0) {
            return preview ? new int[]{s.levelBarsX, s.levelBarsY, 100, 20} : null;
        }
        int bar = s.levelBarsBar ? s.levelBarsWidth : 0, top = s.levelBarsY + (s.levelBarsHeading ? 20 : 4);
        int barSpan = s.levelBarsBar ? bar + 6 : 0;
        int labelW = 0, extraW = 0;
        for (int i = 0; i < rows; i++) {
            labelW = Math.max(labelW, tr.getWidth(ServerHud.title(label[i])));
            extraW = Math.max(extraW, tr.getWidth(extra[i]));
        }
        int pctW = s.levelBarsPercent ? tr.getWidth("100%") + 6 : 0;
        int boxW = 8 + labelW + 6 + barSpan + pctW + extraW + 8;
        if (s.levelBarsHeading) {
            Foe.title(ctx, s.levelBarsX, s.levelBarsY, boxW, s.levelBarsAlign, Foe.text("level progress"));
        }
        for (int i = 0; i < rows; i++) {
            int y = top + i * 12, x = s.levelBarsX + 8;
            double f = Math.max(0, Math.min(1, fraction[i]));
            String name = ServerHud.title(label[i]);
            drawLabel(ctx, tr, name, s.levelBarsAlign.x(x, labelW, tr.getWidth(name)), y, number[i]);
            int bx = x + labelW + 6;
            if (s.levelBarsBar) {
                ctx.fill(bx, y + 3, bx + bar, y + 5, Foe.FAINT);
                int fillW = (int) (bar * f);
                for (int c = 0; c < fillW; c++) {
                    ctx.fill(bx + c, y + 3, bx + c + 1, y + 5, Hypr.mix(Hypr.acc1(), Hypr.acc2(), bar > 1 ? c / (float) (bar - 1) : 0f));
                }
            }
            int px = bx + barSpan;
            if (s.levelBarsPercent) {
                String pct = (int) (f * 100) + "%"; // cut, so 99.9% never reads as done
                ctx.drawTextWithShadow(tr, pct, px + pctW - 6 - tr.getWidth(pct), y, Foe.TEXT);
            }
            ctx.drawTextWithShadow(tr, extra[i], px + pctW, y, Foe.DIM);
        }
        return new int[]{s.levelBarsX, s.levelBarsY, boxW, top - s.levelBarsY + rows * 12 + 2};
    }

    /** The label in dim text; the number inside the brackets gets its own colour when there is one. */
    private static void drawLabel(DrawContext ctx, TextRenderer tr, String name, int x, int y, int numberColor) {
        int open = name.indexOf('['), close = name.lastIndexOf(']');
        if (numberColor == 0 || open < 0 || close < open) {
            ctx.drawTextWithShadow(tr, name, x, y, Foe.DIM);
            return;
        }
        String head = name.substring(0, open + 1), number = name.substring(open + 1, close);
        ctx.drawTextWithShadow(tr, head, x, y, Foe.DIM);
        x += tr.getWidth(head);
        ctx.drawTextWithShadow(tr, number, x, y, numberColor);
        ctx.drawTextWithShadow(tr, name.substring(close), x + tr.getWidth(number), y, Foe.DIM);
    }

    public static void renderHud(DrawContext ctx) {
        if (TackleboxSettings.get().levelBars && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }
}
