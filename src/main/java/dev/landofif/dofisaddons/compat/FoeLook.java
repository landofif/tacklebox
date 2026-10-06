package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.ui.S24;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

import java.util.ArrayList;
import java.util.List;

/**
 * foecollab's huds in the system24 look. foecollab draws every hud inside one hud element
 * ({@code foecollab:main_hud}); the mod wraps that element and, while it runs, turns each black
 * background fill into a system24 panel: the hud panel colour, a 1px border, and a title that sits on
 * the top edge (foecollab's themed title) gets a gap in the border like the mods own huds. foecollab's
 * theme frame textures are skipped. In foecollab's own screens its black buttons with white outlines
 * become system24 buttons (with "restyle all screens"). Nothing of foecollab is compiled against; only
 * its hud id and package name are used.
 */
public final class FoeLook {
    private static final Identifier MAIN_HUD = Identifier.of("foecollab", "main_hud");
    private static final int GAP = 3;

    /** A panel whose border is drawn once the hud is done, so titles drawn later can cut into it. */
    private record Panel(Matrix3x2f pose, int x1, int y1, int x2, int y2, int border, List<float[]> gaps) {}

    private static boolean inHud;
    private static boolean drawing;
    private static final List<Panel> PANELS = new ArrayList<>();

    private FoeLook() {}

    public static void register() {
        if (!FabricLoader.getInstance().isModLoaded("foecollab")) {
            return;
        }
        // after every mod's init, so foecollab's element is registered whatever order mods load in
        ClientLifecycleEvents.CLIENT_STARTED.register(client ->
                HudElementRegistry.replaceElement(MAIN_HUD, original -> (ctx, tick) -> render(original, ctx, tick)));
    }

    private static void render(HudElement original, DrawContext ctx, RenderTickCounter tick) {
        DofiSettings s = StatsTracker.settings();
        if (s == null || !s.foeHuds) {
            original.render(ctx, tick);
            return;
        }
        inHud = true;
        try {
            original.render(ctx, tick);
        } finally {
            inHud = false;
            flush(ctx);
        }
    }

    /** True while foecollab's huds draw and the mod restyles them. */
    public static boolean active() {
        return inHud && !drawing;
    }

    private static boolean screenDrawing;

    /** Called around every screen's render; true (and restyling on) for one of foecollab's screens. */
    public static boolean beginScreen(Screen screen) {
        if (screen.getClass().getName().startsWith("io.github.foecollab.")) {
            screenDrawing = true;
            return true;
        }
        return false;
    }

    public static void endScreen() {
        screenDrawing = false;
    }

    /** True while one of foecollab's own screens draws and every screen is restyled. */
    public static boolean inScreen() {
        return screenDrawing && !inHud && !drawing && S24.everywhere();
    }

    /**
     * A fill in one of foecollab's screens. Its buttons are black boxes with white outlines that turn
     * orange on hover; those become system24 buttons. Returns true when it was drawn recoloured.
     */
    public static boolean screenFill(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int alpha = color >>> 24;
        int rgb = color & 0xFFFFFF;
        boolean thin = Math.abs(x2 - x1) <= 1 || Math.abs(y2 - y1) <= 1;
        int out;
        if (thin && alpha == 0xFF && rgb == 0xFFFFFF) {
            out = S24.BORDER;
        } else if (thin && alpha == 0xFF && rgb == 0xFFAA00) {
            out = S24.ACCENT;
        } else if (!thin && alpha > 0 && rgb == 0) {
            out = S24.alpha(Math.max(alpha, 0xD0), S24.BG_2);
        } else if (!thin && alpha > 0 && alpha < 0xFF && rgb == 0xFFFFFF) {
            out = S24.alpha(Math.max(alpha, 0xD0), S24.BG_1);
        } else {
            return false;
        }
        drawing = true;
        try {
            ctx.fill(x1, y1, x2, y2, out);
        } finally {
            drawing = false;
        }
        return true;
    }

    /** A fill from foecollab's huds. Returns true when it was drawn as a panel instead. */
    public static boolean fill(DrawContext ctx, int x1, int y1, int x2, int y2, int color) {
        int alpha = color >>> 24;
        int left = Math.min(x1, x2), right = Math.max(x1, x2);
        int top = Math.min(y1, y2), bottom = Math.max(y1, y2);
        if (color == 0xFFFFFFFF && (right - left <= 1 || bottom - top <= 1)) {
            drawing = true; // a plain white outline: the hud border colour instead
            try {
                ctx.fill(left, top, right, bottom, 0xFF000000 | S24.HUD_BORDER);
            } finally {
                drawing = false;
            }
            return true;
        }
        if ((color & 0xFFFFFF) != 0 || alpha == 0 || right - left < 4 || bottom - top < 4) {
            return false; // only the black backgrounds; bars and highlights keep their colour
        }
        alpha = S24.scale(alpha, StatsTracker.settings().hudOpacity);
        drawing = true;
        try {
            ctx.fill(left, top, right, bottom, alpha << 24 | (S24.HUD_PANEL & 0xFFFFFF));
        } finally {
            drawing = false;
        }
        int border = Math.min(255, Math.round(alpha * 1.4f)) << 24 | (S24.HUD_BORDER & 0xFFFFFF);
        PANELS.add(new Panel(new Matrix3x2f(ctx.getMatrices()), left, top, right, bottom, border, new ArrayList<>()));
        return true;
    }

    /** Text from foecollab's huds: a title lying across a panel's top edge leaves a gap in it. */
    public static void text(DrawContext ctx, TextRenderer tr, OrderedText text, int x, int y) {
        if (PANELS.isEmpty()) {
            return;
        }
        Matrix3x2f pose = ctx.getMatrices();
        Vector2f a = pose.transformPosition(x, y, new Vector2f());
        Vector2f b = pose.transformPosition(x + tr.getWidth(text), y + tr.fontHeight, new Vector2f());
        for (int i = PANELS.size() - 1; i >= 0; i--) {
            Panel p = PANELS.get(i);
            Matrix3x2f inv = p.pose().invert(new Matrix3x2f());
            Vector2f la = inv.transformPosition(a, new Vector2f());
            Vector2f lb = inv.transformPosition(b, new Vector2f());
            float top = Math.min(la.y, lb.y), bottom = Math.max(la.y, lb.y);
            float left = Math.min(la.x, lb.x), right = Math.max(la.x, lb.x);
            if (top <= p.y1() && bottom >= p.y1() + 1 && right > p.x1() && left < p.x2()) {
                p.gaps().add(new float[]{left - GAP, right + GAP});
                return;
            }
        }
    }

    /** Theme frame textures from foecollab, skipped while restyling. */
    public static boolean skipTexture(Identifier sprite) {
        return sprite.getNamespace().equals("foecollab") && sprite.getPath().startsWith("themes/");
    }

    private static void flush(DrawContext ctx) {
        if (PANELS.isEmpty()) {
            return;
        }
        drawing = true;
        ctx.getMatrices().pushMatrix();
        try {
            for (Panel p : PANELS) {
                ctx.getMatrices().set(p.pose());
                int c = p.border();
                topEdge(ctx, p, c);
                ctx.fill(p.x1(), p.y2() - 1, p.x2(), p.y2(), c);
                ctx.fill(p.x1(), p.y1() + 1, p.x1() + 1, p.y2() - 1, c);
                ctx.fill(p.x2() - 1, p.y1() + 1, p.x2(), p.y2() - 1, c);
            }
        } finally {
            ctx.getMatrices().popMatrix();
            drawing = false;
            PANELS.clear();
        }
    }

    private static void topEdge(DrawContext ctx, Panel p, int color) {
        float from = p.x1();
        List<float[]> gaps = new ArrayList<>(p.gaps());
        gaps.sort((g, h) -> Float.compare(g[0], h[0]));
        for (float[] gap : gaps) {
            if (gap[0] > from) {
                ctx.fill(Math.round(from), p.y1(), Math.round(Math.min(gap[0], p.x2())), p.y1() + 1, color);
            }
            from = Math.max(from, gap[1]);
        }
        if (from < p.x2()) {
            ctx.fill(Math.round(from), p.y1(), p.x2(), p.y1() + 1, color);
        }
    }
}
