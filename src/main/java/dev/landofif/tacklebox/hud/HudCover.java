package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.chat.ChatBar;
import dev.landofif.tacklebox.chat.ChatBox;
import dev.landofif.tacklebox.chat.ChatBoxes;
import dev.landofif.tacklebox.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
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

    static boolean overlaps(int x, int y, int w, int h, int[] r) {
        return x < r[0] + r[2] && r[0] < x + w && y < r[1] + r[3] && r[1] < y + h;
    }

    private static List<int[]> covers(MinecraftClient client, TackleboxSettings s, int sw, int sh) {
        List<int[]> out = new ArrayList<>();
        for (ChatBox box : s.chatBoxes) {
            if (box != null && box.enabled && ChatBoxes.shows(box)) {
                out.add(ChatBoxes.rect(box, sw, sh));
            }
        }
        ChatHud hud = client.inGameHud.getChatHud();
        if (!((ChatHudAccessor) hud).tacklebox$visibleMessages().isEmpty()) {
            double scale = client.options.getChatScale().getValue();
            boolean open = ChatBoxes.chatOpen();
            int h = (int) Math.ceil(ChatHud.getHeight(open ? client.options.getChatHeightFocused().getValue()
                    : client.options.getChatHeightUnfocused().getValue()) * scale) + 4;
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
