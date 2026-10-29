package dev.landofif.tacklebox.chat;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.option.KeyBinding;

/**
 * The chat peek key: while it is held with no screen open, vanilla chat and every chat box look the
 * way they do with chat open (all lines, none faded, the open height, the scroll bar) and the mouse
 * wheel scrolls vanilla chat instead of the hotbar. Letting go goes back to the newest line.
 * The looks come from ChatHudMixin (focused) and {@link ChatBoxes#chatOpen}, the wheel from MouseMixin.
 */
public final class ChatPeek {
    /** Registered in Tacklebox, unbound by default. */
    public static KeyBinding key;
    private static boolean active;

    private ChatPeek() {}

    /** True while the key is held and no screen is open. */
    public static boolean active() {
        return active;
    }

    /** Vanilla's "is chat open" check, true while peeking too. */
    public static boolean focused(boolean focused) {
        return focused || active;
    }

    public static void tick(MinecraftClient client) {
        update(key != null && key.isPressed(), client.currentScreen != null, client.currentScreen instanceof ChatScreen, ChatPeek::toBottom);
    }

    /** Letting go (or opening a screen) ends the peek; it scrolls back down unless chat itself opened. */
    static void update(boolean down, boolean screenOpen, boolean chatScreen, Runnable toBottom) {
        boolean was = active;
        active = down && !screenOpen;
        if (was && !active && !chatScreen) {
            toBottom.run();
        }
    }

    private static void toBottom() {
        MinecraftClient.getInstance().inGameHud.getChatHud().resetScroll();
        ChatBoxes.scrollToBottom();
    }

    /** Lines one wheel notch scrolls, like ChatScreen: 7, or 1 with shift. */
    public static int lines(double amount, boolean shift) {
        double a = Math.clamp(amount, -1, 1);
        return (int) (shift ? a : a * 7);
    }
}
