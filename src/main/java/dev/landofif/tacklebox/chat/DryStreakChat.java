package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.hud.CryptidHud;
import dev.landofif.tacklebox.hud.Cryptids;
import dev.landofif.tacklebox.util.RichText;
import net.minecraft.client.MinecraftClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Feeds {@link DryStreaks} your catches and the chat, and prints its lines. The counting always runs; a line only shows for a
 * kind switched on ({@code dryOn}). Lines wait for the next tick so they come after the server's own line.
 */
public final class DryStreakChat {
    private static final List<String> pending = new ArrayList<>();

    private DryStreakChat() {}

    /** A chat line ({@code s} normalized): a drop of yours ends its streak. True when something changed (to be saved). */
    public static boolean onChat(String s, long now) {
        DryStreaks.Drop d = DryStreaks.parse(s);
        if (d == null) {
            return false;
        }
        TackleboxSettings set = TackleboxSettings.get();
        String line = DryStreaks.onDrop(set.dryCount, set.dryOn, set.dryText, d, now);
        if (line != null) {
            pending.add(line);
        }
        return true;
    }

    /** An own catch, with the fish's name for the line. */
    public static void onCatch(String name, String variant, String size, long now) {
        TackleboxSettings set = TackleboxSettings.get();
        Cryptids.State state = CryptidHud.state(now);
        pending.addAll(DryStreaks.onCatch(set.dryCount, set.dryOn, set.dryText, name, variant, size,
                state.moon().toLowerCase(Locale.ROOT).startsWith("blood"), !state.storm().isEmpty(), now));
    }

    /** The lines waiting to be printed; emptied by the call. */
    public static List<String> take() {
        List<String> out = new ArrayList<>(pending);
        pending.clear();
        return out;
    }

    public static void tick(MinecraftClient client) {
        if (pending.isEmpty() || client.player == null) {
            return;
        }
        for (String line : take()) {
            client.player.sendMessage(RichText.parse(line, 0xFFFFFF).toText(), false);
        }
    }
}
