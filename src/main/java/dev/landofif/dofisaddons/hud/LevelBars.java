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

/**
 * Two thin progress bars, no box like the other huds: the fishing level (the vanilla xp bar) and the level of the location you
 * are at ({@link XpEstimate#toNext}: read from the compendium, then moved along by the location xp of quest rewards and chat).
 */
public final class LevelBars {
    private static final int BAR = 80;

    private LevelBars() {}

    public static int[] draw(DrawContext ctx, boolean preview) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) {
            return null;
        }
        DofiSettings s = DofiSettings.get();
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        var loc = Locations.byName(ServerInfo.locationName);
        double[] at = loc == null ? null : XpEstimate.toNext(loc.id());

        String[] label = {"level [" + p.experienceLevel + "]", null};
        double[] fraction = {p.experienceProgress, 0};
        String[] extra = {"", ""};
        int rows = 1;
        if (at != null) {
            int level = (int) at[0];
            double next = XpEstimate.need(level + 1);
            boolean max = level >= Power.MAX_LEVEL;
            label[1] = loc.name() + " level [" + level + "]";
            fraction[1] = max ? 1 : (next - at[1]) / next;
            extra[1] = max ? "max" : XpEstimate.abbrevXp(next - at[1]) + "/" + XpEstimate.abbrevXp(next) + "lxp";
            rows = 2;
        } else if (preview) {
            label[1] = "open /comp";
            rows = 2;
        }
        int labelW = 0, extraW = 0;
        for (int i = 0; i < rows; i++) {
            labelW = Math.max(labelW, tr.getWidth(Foe.text(label[i])));
            extraW = Math.max(extraW, tr.getWidth(extra[i]));
        }
        int pctW = tr.getWidth("100%");
        Foe.title(ctx, s.levelBarsX, s.levelBarsY, Foe.text("level progress"));
        for (int i = 0; i < rows; i++) {
            int y = s.levelBarsY + 20 + i * 12, x = s.levelBarsX + 8;
            double f = Math.max(0, Math.min(1, fraction[i]));
            ctx.drawTextWithShadow(tr, Foe.text(label[i]), x, y, Foe.DIM);
            int bx = x + labelW + 6;
            ctx.fill(bx, y + 3, bx + BAR, y + 5, Foe.FAINT);
            ctx.fillGradient(bx, y + 3, bx + (int) (BAR * f), y + 5, Hypr.acc1(), Hypr.acc2());
            String pct = (int) (f * 100) + "%"; // cut, so 99.9% never reads as done
            int px = bx + BAR + 6;
            ctx.drawTextWithShadow(tr, pct, px + pctW - tr.getWidth(pct), y, Foe.TEXT);
            ctx.drawTextWithShadow(tr, extra[i], px + pctW + 6, y, Foe.DIM);
        }
        return new int[]{s.levelBarsX, s.levelBarsY, 8 + labelW + 6 + BAR + 6 + pctW + (extraW > 0 ? 6 + extraW : 0) + 8, 20 + rows * 12 + 2};
    }

    public static void renderHud(DrawContext ctx) {
        if (DofiSettings.get().levelBars && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }
}
