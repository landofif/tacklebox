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
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;

import java.util.Locale;

/**
 * Shows how long your line has been in the water, floating above your bobber (or under the
 * crosshair). Turns red after a while, and shows "BITE!" when the server announces a bite.
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
        if (hasBite()) {
            return Mono.text("BITE! " + String.format(Locale.ROOT, "%.1fs", seconds())).formatted(Formatting.RED, Formatting.BOLD);
        }
        double secs = seconds();
        Formatting color = s.bobberSlowSeconds > 0 && secs >= s.bobberSlowSeconds ? Formatting.RED
                : s.bobberSlowSeconds > 0 && secs >= s.bobberSlowSeconds * 0.66 ? Formatting.YELLOW
                : Formatting.WHITE;
        return Mono.text(String.format(Locale.ROOT, "%.1fs", secs)).formatted(color);
    }

    private static boolean active(DofiSettings.BobberMode mode) {
        return isOut() && StatsTracker.settings().bobberTimer == mode && TriggerEngine.isOnFishOnMc();
    }

    /** Under-crosshair mode. */
    public static void renderHud(DrawContext ctx) {
        if (!active(DofiSettings.BobberMode.UNDER_CROSSHAIR)) {
            return;
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Text t = label();
        int x = ctx.getScaledWindowWidth() / 2 - tr.getWidth(t) / 2;
        int y = ctx.getScaledWindowHeight() / 2 + 12;
        ctx.drawTextWithShadow(tr, t, x, y, 0xFFFFFFFF);
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
        float delta = client.getRenderTickCounter().getTickProgress(true);
        Vec3d pos = hook.getLerpedPos(delta);
        Vec3d camera = context.worldState().cameraRenderState.pos;
        TextRenderer tr = client.textRenderer;
        Text text = label();

        MatrixStack matrices = new MatrixStack();
        matrices.translate(pos.x - camera.x, pos.y - camera.y + 0.75, pos.z - camera.z);
        matrices.multiply(client.gameRenderer.getCamera().getRotation());
        matrices.scale(0.025f, -0.025f, 0.025f);
        Matrix4f matrix = matrices.peek().getPositionMatrix();
        float x = -tr.getWidth(text) / 2f;
        tr.draw(text, x, 0, 0xFFFFFFFF, false, matrix, context.consumers(),
                TextRenderer.TextLayerType.SEE_THROUGH, 0x55000000, 0xF000F0);
    }
}
