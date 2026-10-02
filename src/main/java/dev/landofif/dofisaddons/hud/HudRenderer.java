package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Draws custom HUDs: a soft dark box with the resolved lines in the mono font. Lines are
 * re-resolved a few times a second, not every frame. A line with a value that isn't available
 * right now is left out, so a box only shows what makes sense at the moment.
 */
public final class HudRenderer {
    private static final int PAD = 4;
    private static final int LINE_H = 10;
    private static final long REFRESH_MS = 200;

    private static final Map<CustomHud, List<Text>> CACHE = new IdentityHashMap<>();
    private static long cachedAt;

    private HudRenderer() {}

    public static void invalidate() {
        cachedAt = 0;
    }

    /** Resolves a HUD's lines. Lines with missing values are dropped; a blank line is a spacer. */
    public static List<Text> resolve(CustomHud hud) {
        List<Text> out = new ArrayList<>();
        boolean any = false;
        for (String template : hud.lines) {
            if (template == null) continue;
            HudEngine.ParseResult parsed = HudEngine.parse(colorCodes(template));
            if (!parsed.complete()) continue;
            MutableText line = Text.empty().setStyle(Mono.style(net.minecraft.text.Style.EMPTY)).append(parsed.text());
            out.add(line);
            if (!parsed.text().getString().isBlank()) any = true;
        }
        return any ? out : List.of();
    }

    /** "&a" and "&#RRGGBB" to the § form the engine reads; "&&" stays a literal "&". */
    static String colorCodes(String s) {
        return s.replace("&&", "\u0000").replace('&', '§').replace('\u0000', '&');
    }

    public static void renderAll(DrawContext ctx) {
        long now = System.currentTimeMillis();
        if (now - cachedAt > REFRESH_MS) {
            CACHE.clear();
            for (CustomHud hud : HudStore.all()) {
                if (hud.enabled) CACHE.put(hud, resolve(hud));
            }
            cachedAt = now;
        }
        for (CustomHud hud : HudStore.all()) {
            List<Text> lines = CACHE.get(hud);
            if (hud.enabled && lines != null && !lines.isEmpty()) {
                draw(ctx, hud, lines, false);
            }
        }
    }

    /** Draws one box and returns its on-screen rectangle {x, y, w, h}. */
    public static int[] draw(DrawContext ctx, CustomHud hud, List<Text> rawLines, boolean highlight) {
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        float scale = (float) hud.scale;
        // A box with a background doesn't need a shadow; text floating over the world gets a soft one.
        boolean shadow = hud.background < 25;
        List<Text> lines = new ArrayList<>(rawLines.size());
        for (Text t : rawLines) lines.add(Mono.prepare(t, scale, shadow));

        int contentW = 0;
        for (Text t : lines) contentW = Math.max(contentW, tr.getWidth(t));
        int boxW = contentW + PAD * 2;
        int boxH = Math.max(1, lines.size()) * LINE_H + PAD * 2 - 2;

        int sw = ctx.getScaledWindowWidth();
        int sh = ctx.getScaledWindowHeight();
        int w = Math.round(boxW * scale);
        int h = Math.round(boxH * scale);
        int anchorX = (int) Math.round(hud.x / 100.0 * sw);
        int x = switch (hud.align) {
            case LEFT -> anchorX;
            case CENTER -> anchorX - w / 2;
            case RIGHT -> anchorX - w;
        };
        int y = (int) Math.round(hud.y / 100.0 * sh);
        x = Math.max(0, Math.min(sw - w, x));
        y = Math.max(0, Math.min(sh - h, y));

        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(x, y);
        ctx.getMatrices().scale(scale, scale);
        int alpha = (int) (hud.background / 100.0 * 255) << 24;
        if (alpha != 0) {
            ctx.fill(0, 0, boxW, boxH, alpha);
        }
        if (highlight) {
            int c = 0xFF55FFFF;
            ctx.fill(0, 0, boxW, 1, c);
            ctx.fill(0, boxH - 1, boxW, boxH, c);
            ctx.fill(0, 0, 1, boxH, c);
            ctx.fill(boxW - 1, 0, boxW, boxH, c);
        }
        int ty = PAD;
        for (Text t : lines) {
            int tw = tr.getWidth(t);
            int tx = switch (hud.align) {
                case LEFT -> PAD;
                case CENTER -> PAD + (contentW - tw) / 2;
                case RIGHT -> PAD + contentW - tw;
            };
            ctx.drawTextWithShadow(tr, t, tx, ty, 0xFFFFFFFF); // mono runs carry their own shadow setting
            ty += LINE_H;
        }
        ctx.getMatrices().popMatrix();
        return new int[]{x, y, w, h};
    }
}
