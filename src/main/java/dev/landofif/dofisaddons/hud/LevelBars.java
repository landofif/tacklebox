package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.menu.Locations;
import dev.landofif.dofisaddons.menu.Power;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.ui.Hypr;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;

import java.util.Map;

/**
 * Two thin progress bars, no box like the other huds: the fishing level (the vanilla xp bar) and the level of the location you
 * are at ({@link XpEstimate#toNext}: read from the compendium, then moved along by the location xp of quest rewards and chat).
 */
public final class LevelBars {
    /** The one word of a location that matters; locations not listed keep their full name. */
    private static final Map<String, String> SHORT = Map.ofEntries(
            Map.entry("cypress", "cypress"), Map.entry("kenai", "kenai"), Map.entry("biwa", "biwa"), Map.entry("murray", "murray"),
            Map.entry("everglades", "everglades"), Map.entry("keywest", "key"), Map.entry("toledobend", "toledo"),
            Map.entry("greatlakes", "great lakes"), Map.entry("danube", "danube"), Map.entry("oilrig", "rig"),
            Map.entry("amazon", "amazon"), Map.entry("mediterranean", "med"), Map.entry("capecod", "cape"),
            Map.entry("nile", "nile"), Map.entry("hawaii", "hawaii"), Map.entry("lofoten", "lofoten"), Map.entry("cairns", "cairns"));

    private LevelBars() {}

    public static int[] draw(DrawContext ctx, boolean preview) {
        DofiSettings s = DofiSettings.get();
        return Foe.scaled(ctx, s.levelBarsX, s.levelBarsY, s.levelBarsScale, c -> drawAt100(c, preview));
    }

    private static int[] drawAt100(DrawContext ctx, boolean preview) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) {
            return null;
        }
        DofiSettings s = DofiSettings.get();
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        var loc = Locations.byName(ServerInfo.locationName);
        double[] at = loc == null ? null : XpEstimate.toNext(loc.id());

        String[] label = new String[2];
        double[] fraction = new double[2];
        String[] extra = {"", ""};
        int rows = 0;
        if (s.levelBarsFishing) {
            label[rows] = "level [" + p.experienceLevel + "]";
            fraction[rows++] = p.experienceProgress;
        }
        if (s.levelBarsLocation && at != null) {
            int level = (int) at[0];
            double next = XpEstimate.need(level + 1);
            boolean max = level >= Power.MAX_LEVEL;
            label[rows] = SHORT.getOrDefault(loc.id(), loc.name()) + " [" + level + "]";
            extra[rows] = !s.levelBarsXp ? "" : max ? "max" : XpEstimate.abbrevXp(next - at[1]) + "/" + XpEstimate.abbrevXp(next) + "lxp";
            fraction[rows++] = max ? 1 : (next - at[1]) / next;
        } else if (s.levelBarsLocation && preview) {
            label[rows++] = "open /comp";
        }
        if (rows == 0) {
            return preview ? new int[]{s.levelBarsX, s.levelBarsY, 100, 20} : null;
        }
        int bar = s.levelBarsWidth, top = s.levelBarsY + (s.levelBarsHeading ? 20 : 4);
        int labelW = 0, extraW = 0;
        for (int i = 0; i < rows; i++) {
            labelW = Math.max(labelW, tr.getWidth(ServerHud.title(label[i])));
            extraW = Math.max(extraW, tr.getWidth(extra[i]));
        }
        int pctW = s.levelBarsPercent ? tr.getWidth("100%") + 6 : 0;
        int boxW = 8 + labelW + 6 + bar + 6 + pctW + extraW + 8;
        if (s.levelBarsHeading) {
            Foe.title(ctx, s.levelBarsX, s.levelBarsY, boxW, s.levelBarsAlign, Foe.text("level progress"));
        }
        for (int i = 0; i < rows; i++) {
            int y = top + i * 12, x = s.levelBarsX + 8;
            double f = Math.max(0, Math.min(1, fraction[i]));
            String name = ServerHud.title(label[i]);
            ctx.drawTextWithShadow(tr, name, s.levelBarsAlign.x(x, labelW, tr.getWidth(name)), y, Foe.DIM);
            int bx = x + labelW + 6;
            ctx.fill(bx, y + 3, bx + bar, y + 5, Foe.FAINT);
            int fillW = (int) (bar * f);
            for (int c = 0; c < fillW; c++) {
                ctx.fill(bx + c, y + 3, bx + c + 1, y + 5, Hypr.mix(Hypr.acc1(), Hypr.acc2(), bar > 1 ? c / (float) (bar - 1) : 0f));
            }
            int px = bx + bar + 6;
            if (s.levelBarsPercent) {
                String pct = (int) (f * 100) + "%"; // cut, so 99.9% never reads as done
                ctx.drawTextWithShadow(tr, pct, px + pctW - 6 - tr.getWidth(pct), y, Foe.TEXT);
            }
            ctx.drawTextWithShadow(tr, extra[i], px + pctW, y, Foe.DIM);
        }
        return new int[]{s.levelBarsX, s.levelBarsY, boxW, top - s.levelBarsY + rows * 12 + 2};
    }

    public static void renderHud(DrawContext ctx) {
        if (DofiSettings.get().levelBars && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }
}
