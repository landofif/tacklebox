package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Asks Hyprland to focus the game window when the server's BITE! title arrives, so the player can
 * switch back in time. Hyprland 0.56 reads dispatch as Lua and matches the whole title. Only focuses; every fishing input stays with the player. The window has no
 * class under Prism, so it is matched by title.
 */
public final class BiteFocus {
    static final long COOLDOWN_MS = 2000;
    static final List<String> COMMAND = List.of("hyprctl", "dispatch", "hl.dsp.focus({ window = 'initialtitle:^Minecraft.*' })");

    private static long lastFocus;

    private BiteFocus() {
    }

    /** True when a bite at {@code now} should focus the window, given the last focus time. */
    static boolean due(long now, long last) {
        return last == 0 || now - last >= COOLDOWN_MS;
    }

    /** Hyprland warps the cursor to the window centre on every focus, so a window that already has focus is left alone. */
    static boolean wanted(boolean windowFocused, long now, long last) {
        return !windowFocused && due(now, last);
    }

    public static void onTitle(Text title) {
        if (title == null || !TackleboxSettings.get().biteFocus || !"BITE!".equals(title.getString())) {
            return;
        }
        long now = System.currentTimeMillis();
        if (!wanted(MinecraftClient.getInstance().isWindowFocused(), now, lastFocus)) {
            return;
        }
        lastFocus = now;
        run();
    }

    private static void run() {
        try {
            new ProcessBuilder(COMMAND).redirectErrorStream(true).redirectOutput(ProcessBuilder.Redirect.DISCARD).start();
        } catch (Exception e) {
            Tacklebox.LOGGER.warn("bite focus: could not run hyprctl", e);
        }
    }
}
