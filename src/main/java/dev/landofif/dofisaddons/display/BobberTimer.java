package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.util.math.Vec3d;
import net.minecraft.entity.projectile.FishingBobberEntity;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

/**
 * Your own bobber's waiting time (a hud element and/or text over the bobber) and the bite notification.
 * Display only. A right click with the bobber out (reel in or catch) hides the hud timer and the bite
 * notification on that very click, before the server answers; the text over the bobber just follows the bobber.
 */
public final class BobberTimer {
    private static final long BITE_GAP_MS = 1500; // the server re-sends BITE! while a fish is hooked

    /** The bobber (entity id) you right clicked; its hud timer and bites stay hidden. */
    private static int reeledId = -1;
    private static long biteAt;
    private static long lastBiteTitle;

    /** Set while the bobber screen is open: the timer shows a running sample. */
    public static boolean preview;

    private BobberTimer() {}

    private static FishingBobberEntity own(MinecraftClient client) {
        return client.player == null ? null : client.player.fishHook;
    }

    /** Right click: if it reels the bobber in, hide the hud timer and bite at once. */
    public static void onUse(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        if (p != null && p.fishHook != null && (p.getMainHandStack().isOf(Items.FISHING_ROD) || p.getOffHandStack().isOf(Items.FISHING_ROD))) {
            reeledId = p.fishHook.getId();
            biteAt = 0;
        }
    }

    public static void tick(MinecraftClient client) {
        if (own(client) == null) {
            biteAt = 0;
        }
    }

    /** The server's title. Returns true if it is the BITE! title and should be hidden. */
    public static boolean onTitle(Text title) {
        DofiSettings s = DofiSettings.get();
        if (title == null || !s.biteOn || !"BITE!".equals(title.getString())) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        FishingBobberEntity hook = own(client);
        long now = System.currentTimeMillis();
        if (hook != null && hook.getId() != reeledId && now - lastBiteTitle > BITE_GAP_MS) {
            biteAt = now;
        }
        lastBiteTitle = now;
        return s.biteHideTitle;
    }

    /** Plays the notification once (the bobber screen's test button). */
    public static void testBite() {
        biteAt = System.currentTimeMillis();
    }

    private static String format(float seconds) {
        DofiSettings s = DofiSettings.get();
        return String.format(java.util.Locale.ROOT, "%." + s.timerDecimals + "f", seconds) + (s.timerSuffix ? "ꜱ" : "");
    }

    // ---------------------------------------------------------------- hud

    public static void renderHud(DrawContext ctx) {
        MinecraftClient client = MinecraftClient.getInstance();
        DofiSettings s = DofiSettings.get();
        FishingBobberEntity hook = own(client);
        boolean live = hook != null && hook.getId() != reeledId;
        if ((live || preview) && (s.timerMode == DofiSettings.TimerMode.HUD || s.timerMode == DofiSettings.TimerMode.BOTH)) {
            drawTimer(ctx, preview && !live);
        }
        if (hook != null && (s.timerMode == DofiSettings.TimerMode.BOBBER || s.timerMode == DofiSettings.TimerMode.BOTH)) {
            drawOverBobber(ctx, client, hook);
        }
        if (s.biteOn && (biteAt != 0 || preview)) {
            drawBite(ctx, false);
        }
    }

    /** Draws the timer; {@code sample} uses a running fake time. Returns its rectangle. */
    public static int[] drawTimer(DrawContext ctx, boolean sample) {
        MinecraftClient client = MinecraftClient.getInstance();
        DofiSettings s = DofiSettings.get();
        float seconds = sample ? System.currentTimeMillis() % 10000 / 1000f : client.player.fishHook.age / 20f + client.getRenderTickCounter().getTickProgress(false) / 20f;
        return centered(ctx, Foe.bold(format(seconds)), s.timerX, s.timerY, s.timerSize / 100f, 1f, 0, 0, 0xFF000000 | s.timerColor, s.timerShadow);
    }

    /** Draws the bite notification; {@code sample} (the mover) skips the animation. Returns its rectangle. */
    public static int[] drawBite(DrawContext ctx, boolean sample) {
        DofiSettings s = DofiSettings.get();
        float t = sample ? 1 : (System.currentTimeMillis() - biteAt) / 1000f;
        float alpha = 1, scale = 1, dx = 0, dy = 0;
        if (!sample) {
            float out = Math.min(0.25f, (float) s.biteSeconds / 3);
            if (t > s.biteSeconds) {
                if (!preview) {
                    biteAt = 0;
                }
                return new int[]{0, 0, 0, 0};
            }
            if (s.biteAnim != DofiSettings.BiteAnim.NONE && t > s.biteSeconds - out) {
                alpha = (float) (s.biteSeconds - t) / out;
            }
            float in = MathHelper.clamp(t / 0.25f, 0, 1);
            float ease = 1 - (1 - in) * (1 - in);
            switch (s.biteAnim) {
                case FADE -> alpha *= in;
                case POP -> scale = in >= 1 ? 1 : 0.4f + 0.6f * ease + 0.2f * (float) Math.sin(in * Math.PI);
                case SLIDE -> {
                    dy = -24 * (1 - ease);
                    alpha *= in;
                }
                case SHAKE -> dx = (float) Math.sin(t * 70) * 8 * Math.max(0, 1 - t / 0.5f);
                case PULSE -> scale = 1 + 0.1f * (float) Math.sin(t * 12);
                default -> {
                }
            }
        }
        if (alpha <= 0.02f) {
            return new int[]{0, 0, 0, 0};
        }
        return centered(ctx, Foe.bold(s.biteText), s.biteX, s.biteY, s.biteSize / 100f * scale, alpha, dx, dy, 0xFF000000 | s.biteColor, s.biteShadow);
    }

    private static int[] centered(DrawContext ctx, Text text, double xPct, double yPct, float scale, float alpha,
                                  float dx, float dy, int argb, boolean shadow) {
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        return drawAt(ctx, text, (float) (sw * xPct / 100) + dx, (float) (sh * yPct / 100) + dy, scale, alpha, argb, shadow);
    }

    private static int[] drawAt(DrawContext ctx, Text text, float cx, float cy, float scale, float alpha, int argb, boolean shadow) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        int w = tr.getWidth(text), h = 9;
        int a = Math.max(4, (int) (alpha * 255));
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, cy);
        ctx.getMatrices().scale(scale, scale);
        ctx.drawText(tr, text, -w / 2, -h / 2, (a << 24) | (argb & 0xFFFFFF), shadow);
        ctx.getMatrices().popMatrix();
        return new int[]{Math.round(cx - w * scale / 2), Math.round(cy - h * scale / 2), Math.round(w * scale), Math.round(h * scale)};
    }

    // ---------------------------------------------------------------- over the bobber

    /**
     * The timer over the bobber, drawn in the hud at the bobber's projected spot: text drawn in the world is
     * flushed by layer with the models, so a player model (a hat) could cover it. The size follows the distance.
     */
    private static void drawOverBobber(DrawContext ctx, MinecraftClient client, FishingBobberEntity hook) {
        DofiSettings s = DofiSettings.get();
        float tick = client.getRenderTickCounter().getTickProgress(false);
        Camera camera = client.gameRenderer.getCamera();
        Vec3d p = hook.getLerpedPos(tick).add(0, s.timerHeight, 0).subtract(camera.getCameraPos());
        // the camera's unit vectors: horizontalPlane looks ahead, diagonalPlane is to the left, verticalPlane up
        Vec3d forward = new Vec3d(camera.getHorizontalPlane());
        Vec3d left = new Vec3d(camera.getDiagonalPlane());
        Vec3d up = new Vec3d(camera.getVerticalPlane());
        double depth = p.dotProduct(forward);
        if (depth <= 0.05) {
            return; // behind you
        }
        double tan = Math.tan(Math.toRadians(client.options.getFov().getValue()) / 2);
        double aspect = (double) client.getWindow().getFramebufferWidth() / client.getWindow().getFramebufferHeight();
        double a = p.dotProduct(left) / (depth * tan * aspect); // +1 is the left edge of the screen
        double b = p.dotProduct(up) / (depth * tan); // +1 is the top
        int sw = ctx.getScaledWindowWidth(), sh = ctx.getScaledWindowHeight();
        float pixelsPerBlock = (float) (sh / (2 * tan * depth));
        float scale = Math.max(0.4f, Math.min(8f, 0.025f * s.timerWorldSize / 100f * pixelsPerBlock));
        drawAt(ctx, Foe.bold(format((hook.age + tick) / 20f)), (float) (sw * (0.5 - a / 2)), (float) (sh * (0.5 - b / 2)),
                scale, 1f, 0xFF000000 | s.timerColor, s.timerShadow);
    }
}
