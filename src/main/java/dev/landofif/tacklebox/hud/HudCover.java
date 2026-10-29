package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.chat.ChatBar;
import dev.landofif.tacklebox.chat.ChatBox;
import dev.landofif.tacklebox.chat.ChatBoxes;
import dev.landofif.tacklebox.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.PlayerListEntry;

import java.util.ArrayList;
import java.util.List;

/**
 * With {@code hideBehindChat} on, a hud that would be drawn under the vanilla chat, a chat box or the player list
 * is left out for that frame, so they do not overlap (split screen). Rectangles are x, y, w, h in gui pixels;
 * the chat and player list ones are close estimates that err on the big side.
 */
public final class HudCover {
    private HudCover() {}

    /** True when the hud at this spot would sit under a chat window or the player list and the option is on. */
    public static boolean covered(int x, int y, int w, int h) {
        TackleboxSettings s = TackleboxSettings.get();
        if (s == null || !s.hideBehindChat || w <= 0 || h <= 0) {
            return false;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        for (int[] r : covers(client, s, sw, sh)) {
            if (overlaps(x, y, w, h, r)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The parts of the rectangle that are not under a chat window or the player list (the whole rectangle when
     * the option is off or nothing overlaps, none when it is all covered).
     */
    public static List<int[]> free(int x, int y, int w, int h) {
        List<int[]> pieces = new ArrayList<>();
        pieces.add(new int[]{x, y, w, h});
        TackleboxSettings s = TackleboxSettings.get();
        if (s == null || !s.hideBehindChat || w <= 0 || h <= 0) {
            return pieces;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        for (int[] c : covers(client, s, client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight())) {
            pieces = subtract(pieces, c);
        }
        return pieces;
    }

    static List<int[]> subtract(List<int[]> pieces, int[] c) {
        List<int[]> out = new ArrayList<>();
        for (int[] p : pieces) {
            if (!overlaps(p[0], p[1], p[2], p[3], c)) {
                out.add(p);
                continue;
            }
            int pr = p[0] + p[2], pb = p[1] + p[3];
            int cl = Math.max(p[0], c[0]), cr = Math.min(pr, c[0] + c[2]);
            int ct = Math.max(p[1], c[1]), cb = Math.min(pb, c[1] + c[3]);
            if (ct > p[1]) out.add(new int[]{p[0], p[1], p[2], ct - p[1]});
            if (cb < pb) out.add(new int[]{p[0], cb, p[2], pb - cb});
            if (cl > p[0]) out.add(new int[]{p[0], ct, cl - p[0], cb - ct});
            if (cr < pr) out.add(new int[]{cr, ct, pr - cr, cb - ct});
        }
        return out;
    }

    /**
     * Runs {@code draw} so that only the part of the hud at {@code x, y, w, h} outside the chat windows shows:
     * once per free piece, cut to it. Once, uncut, when nothing overlaps; once cut to nothing when all of it is covered.
     */
    public static void clipped(DrawContext ctx, int x, int y, int w, int h, Runnable draw) {
        List<int[]> pieces = free(x, y, w, h);
        if (pieces.size() == 1 && pieces.get(0)[2] == w && pieces.get(0)[3] == h) {
            draw.run();
            return;
        }
        if (pieces.isEmpty()) {
            pieces.add(new int[]{0, 0, 0, 0});
        }
        for (int[] p : pieces) {
            ctx.enableScissor(p[0], p[1], p[0] + p[2], p[1] + p[3]);
            try {
                draw.run();
            } finally {
                ctx.disableScissor();
            }
        }
    }

    static boolean overlaps(int x, int y, int w, int h, int[] r) {
        return x < r[0] + r[2] && r[0] < x + w && y < r[1] + r[3] && r[1] < y + h;
    }

    private static List<int[]> covers(MinecraftClient client, TackleboxSettings s, int sw, int sh) {
        List<int[]> out = new ArrayList<>();
        for (ChatBox box : s.chatBoxes) {
            if (box != null && box.enabled && ChatBoxes.shows(box)) {
                out.add(ChatBoxes.contentRect(box, sw, sh));
            }
        }
        ChatHud hud = client.inGameHud.getChatHud();
        ChatHudAccessor acc = (ChatHudAccessor) hud;
        boolean open = ChatBoxes.chatOpen();
        int tick = client.inGameHud.getTicks();
        int shown = 0; // lines on screen: all of them with chat open, else only the ones that have not faded yet
        for (var line : acc.tacklebox$visibleMessages()) {
            if (open || tick - line.addedTime() < 200) {
                shown++;
            }
        }
        shown = Math.min(shown, hud.getVisibleLineCount());
        if (shown > 0) {
            double scale = client.options.getChatScale().getValue();
            int h = (int) Math.ceil(shown * acc.tacklebox$lineHeight() * scale) + 4;
            int bottom = sh - 40 + s.chatY;
            s.chatPos.measure(sw, sh, ChatBar.width(), 0);
            out.add(new int[]{s.chatPos.left(sw), bottom - h, ChatBar.width(), h});
        }
        if (client.options.playerListKey.isPressed() && client.getNetworkHandler() != null) {
            int n = 0;
            int name = 0;
            for (PlayerListEntry e : client.getNetworkHandler().getListedPlayerListEntries()) {
                if (n++ < 80) {
                    name = Math.max(name, client.textRenderer.getWidth(client.inGameHud.getPlayerListHud().getPlayerName(e)));
                }
            }
            n = Math.min(n, 80);
            int cols = 1;
            int rows = n;
            while (rows > 20) {
                cols++;
                rows = (n + cols - 1) / cols;
            }
            int colW = Math.min((name + 24) * cols, sw - 50) / cols;
            int w = colW * cols + (cols - 1) * 5;
            out.add(new int[]{(sw - w) / 2 - 1, 8, w + 2, rows * 9 + 24});
        }
        return out;
    }
}
