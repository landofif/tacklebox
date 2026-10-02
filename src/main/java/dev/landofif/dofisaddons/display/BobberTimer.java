package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.util.Mono;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * Shows how long your line has been in the water, floating above your bobber (or under the
 * crosshair). Changes colour as the wait gets long, and shows "bite!" when the server announces a bite.
 * Colours, size, height and the rest are in the bobber tab of the settings.
 * Display only: it never reels or casts.
 */
public final class BobberTimer {
    private static int hookId = -1;
    private static long castAt;
    private static long biteAt;

    private BobberTimer() {}

    public static boolean isOut() {
        return castAt != 0;
    }

    public static double seconds() {
        return castAt == 0 ? 0 : (System.currentTimeMillis() - castAt) / 1000.0;
    }

    public static boolean hasBite() {
        return castAt != 0 && biteAt >= castAt;
    }

    public static void tick(MinecraftClient client) {
        FishingBobberEntity hook = client.player == null ? null : client.player.fishHook;
        if (hook == null) {
            hookId = -1;
            castAt = 0;
            return;
        }
        if (hook.getId() != hookId) {
            hookId = hook.getId();
            castAt = System.currentTimeMillis();
            biteAt = 0;
        }
    }

    /** FishOnMC sends a "BITE!" title when something bites. */
    public static void onTitle(Text title) {
        if (castAt != 0 && title != null && title.getString().toUpperCase(Locale.ROOT).contains("BITE")) {
            biteAt = System.currentTimeMillis();
        }
    }

    private static Text label() {
        DofiSettings s = StatsTracker.settings();
        double secs = seconds();
        String time = String.format(Locale.ROOT, "%." + s.bobberDecimals + "fs", secs);
        int color;
        if (hasBite()) {
            color = s.bobberBiteColor;
            if (s.bobberShowBite) {
                time = "bite! " + time;
            }
        } else if (s.bobberSlowSeconds > 0 && secs >= s.bobberSlowSeconds) {
            color = s.bobberSlowColor;
        } else if (s.bobberWarnSeconds > 0 && secs >= s.bobberWarnSeconds) {
            color = s.bobberWarnColor;
        } else {
            color = s.bobberColor;
        }
        MutableText t = Mono.text(time).withColor(color);
        return s.bobberBold || hasBite() && s.bobberShowBite ? t.formatted(Formatting.BOLD) : t;
    }

    private static boolean active(DofiSettings.BobberMode mode) {
        return isOut() && StatsTracker.settings().bobberTimer == mode && TriggerEngine.isOnFishOnMc();
    }

    /** Under-crosshair mode. */
    public static void renderHud(DrawContext ctx) {
        if (!active(DofiSettings.BobberMode.UNDER_CROSSHAIR)) {
            return;
        }
        DofiSettings s = StatsTracker.settings();
        float scale = (float) s.bobberScale;
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Text t = Mono.prepare(label(), scale, true);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(ctx.getScaledWindowWidth() / 2f, ctx.getScaledWindowHeight() / 2f + s.bobberCrosshairY);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawTextWithShadow(tr, t, -tr.getWidth(t) / 2, 0, 0xFFFFFFFF);
        ctx.getMatrices().popMatrix();
    }

    /** Above-bobber mode: a small label that always faces you. */
    public static void renderWorld(WorldRenderContext context) {
        if (!active(DofiSettings.BobberMode.ABOVE_BOBBER)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        FishingBobberEntity hook = client.player == null ? null : client.player.fishHook;
        if (hook == null) {
            return;
        }
        DofiSettings s = StatsTracker.settings();
        float scale = (float) s.bobberScale;
        float delta = client.getRenderTickCounter().getTickProgress(true);
        Vec3d pos = hook.getLerpedPos(delta);
        Vec3d camera = context.worldState().cameraRenderState.pos;
        TextRenderer tr = client.textRenderer;
        // Seen up close in the world, so rasterise generously; it has its own backing instead of a shadow.
        Text text = Mono.prepare(label(), Math.round(4 * scale), false);

        MatrixStack matrices = new MatrixStack();
        matrices.translate(pos.x - camera.x, pos.y - camera.y + s.bobberHeight, pos.z - camera.z);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());
        matrices.scale(0.025f * scale, -0.025f * scale, 0.025f * scale);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float x = -tr.getWidth(text) / 2f;
        int backing = Math.round(s.bobberBacking * 2.55f) << 24;
        tr.draw(text, x, 0, 0xFFFFFFFF, false, matrix, context.consumers(),
                s.bobberThroughWalls ? TextRenderer.TextLayerType.SEE_THROUGH : TextRenderer.TextLayerType.NORMAL,
                backing, 0xF000F0);
    }
}
