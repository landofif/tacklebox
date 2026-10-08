package dev.landofif.dofisaddons.chat;

import dev.landofif.dofisaddons.mixin.ChatHudAccessor;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.hud.ChatHud;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;

/**
 * The vanilla chat's scrolling: it eases to where you scroll (the lines slide, with one extra line drawn so
 * there is no gap), and the thin non-clickable scroll bar is replaced by a thicker one you can drag.
 * The chat render is run with the field {@code scrolledLines} set to the whole line we are at and put
 * back afterwards, so vanilla still sees (and changes) the real target.
 */
public final class ChatBar {
    private static final int WIDTH = 5;
    private static final int MIN_THUMB = 12;

    /** The scroll position being drawn, in lines (between whole numbers while it eases). */
    public static float shown;
    /** One more line is drawn while it is between two (see ChatHudMixin). */
    public static boolean extra;
    /** The chat pose without the easing shift, for mapping the mouse into chat space. */
    public static final Matrix3x2f pose = new Matrix3x2f();
    public static float shift;

    private static ChatHud hud;
    private static int target, lines, count;
    private static long at;
    private static boolean valid, dragging;
    private static float barX, top, bottom, thumbH, grab;

    private ChatBar() {}

    public static void begin(ChatHud chat) {
        hud = chat;
        ChatHudAccessor acc = (ChatHudAccessor) chat;
        target = acc.dofisaddons$scrolled();
        count = acc.dofisaddons$visibleMessages().size();
        extra = false;
        lines = chat.getVisibleLineCount();
        long now = System.nanoTime();
        double dt = Math.min(0.25, (now - at) / 1e9);
        at = now;
        shown += (float) ((target - shown) * (1 - Math.exp(-dt * 18)));
        if (dragging || Math.abs(target - shown) < 0.01f) {
            shown = target;
        }
        shown = Math.max(0, Math.min(shown, Math.max(target, 0)));
        int whole = (int) shown;
        float frac = shown - whole;
        acc.dofisaddons$setScrolled(whole);
        extra = frac > 0.001f && whole + lines < count;
        shift = frac * acc.dofisaddons$lineHeight();
        valid = false;
    }

    public static void end() {
        if (hud != null) {
            ((ChatHudAccessor) hud).dofisaddons$setScrolled(target);
        }
        extra = false;
    }

    /** New lines arrive while you are scrolled up: vanilla moves the target with them, so move what is shown too. */
    public static void added(int before, int after) {
        if (before > 0 && after > before) {
            shown += after - before;
        }
    }

    /** Draws the bar in place of vanilla's (called instead of its first fill; {@code y1} is the bottom of vanilla's thumb). */
    public static void draw(ChatHud.Backend backend, int x1, int y1) {
        ChatHudAccessor acc = (ChatHudAccessor) hud;
        int h = acc.dofisaddons$lineHeight();
        int vanillaDrawn = lines + (extra ? 1 : 0);
        float baseY = y1 + (int) ((long) acc.dofisaddons$scrolled() * vanillaDrawn * h / count);
        float vp = lines * h;
        bottom = baseY;
        top = bottom - vp;
        barX = x1 + 1;
        thumbH = Math.min(vp, Math.max(MIN_THUMB, vp * lines / count));
        valid = true;
        float thumbTop = thumbTop();
        int dy = Math.round(shift); // drawn inside the shifted pose
        int color = dragging ? Foe.ACCENT : acc.dofisaddons$unread() ? 0xFFCC3333 : 0xB0FFFFFF;
        backend.fill((int) barX, Math.round(top) - dy, (int) barX + WIDTH, Math.round(bottom) - dy, 0x50000000);
        backend.fill((int) barX, Math.round(thumbTop) - dy, (int) barX + WIDTH, Math.round(thumbTop + thumbH) - dy, color);
    }

    private static float thumbTop() {
        float max = Math.max(1, count - lines);
        float frac = Math.max(0, Math.min(1, shown / max));
        return top + (1 - frac) * (bottom - top - thumbH);
    }

    private static Vector2f local(double mx, double my) {
        return new Matrix3x2f(pose).invert().transformPosition((float) mx, (float) my, new Vector2f());
    }

    public static boolean press(double mx, double my) {
        if (!valid || count <= lines) {
            return false;
        }
        Vector2f l = local(mx, my);
        if (l.x < barX - 3 || l.x > barX + WIDTH + 3 || l.y < top || l.y > bottom) {
            return false;
        }
        float t = thumbTop();
        grab = l.y >= t && l.y <= t + thumbH ? l.y - t : thumbH / 2;
        dragging = true;
        apply(l.y);
        return true;
    }

    /** Called every frame with the chat open. */
    public static void tick(double mx, double my, boolean held) {
        if (!dragging) {
            return;
        }
        if (!held) {
            dragging = false;
            return;
        }
        apply(local(mx, my).y);
    }

    private static void apply(float y) {
        float range = Math.max(1, bottom - top - thumbH);
        float frac = Math.max(0, Math.min(1, 1 - (y - grab - top) / range));
        ChatHudAccessor acc = (ChatHudAccessor) hud;
        hud.scroll(Math.round(frac * (count - lines)) - acc.dofisaddons$scrolled()); // clamps, and clears the unread mark at the bottom
        shown = acc.dofisaddons$scrolled();
    }
}
