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
    private static final int W = 110;

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

        Foe.title(ctx, s.levelBarsX, s.levelBarsY, Foe.text("level progress"));
        int y = s.levelBarsY + 20;
        bar(ctx, tr, s.levelBarsX + 8, y, "level " + p.experienceLevel, p.experienceProgress);
        y += 17;
        if (at != null) {
            int level = (int) at[0];
            double next = XpEstimate.need(level + 1);
            boolean max = level >= Power.MAX_LEVEL;
            bar(ctx, tr, s.levelBarsX + 8, y, loc.name() + " " + level + (max ? " (max)" : ""), max ? 1 : (next - at[1]) / next);
            y += 17;
        } else if (preview) {
            bar(ctx, tr, s.levelBarsX + 8, y, "open /comp", 0);
            y += 17;
        }
        return new int[]{s.levelBarsX, s.levelBarsY, W + 16, y - s.levelBarsY + 2};
    }

    private static void bar(DrawContext ctx, TextRenderer tr, int x, int y, String label, double fraction) {
        fraction = Math.max(0, Math.min(1, fraction));
        ctx.drawTextWithShadow(tr, Foe.text(label), x, y, Foe.DIM);
        String pct = (int) (fraction * 100) + "%"; // cut, so 99.9% never reads as done
        ctx.drawTextWithShadow(tr, pct, x + W - tr.getWidth(pct), y, Foe.TEXT);
        ctx.fill(x, y + 10, x + W, y + 12, Foe.FAINT);
        ctx.fillGradient(x, y + 10, x + (int) (W * fraction), y + 12, Hypr.acc1(), Hypr.acc2());
    }

    public static void renderHud(DrawContext ctx) {
        if (DofiSettings.get().levelBars && TriggerEngine.isOnFishOnMc()) {
            draw(ctx, false);
        }
    }
}
