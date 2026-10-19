package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * ctrl + f in chat: what you type in the chat box hides every message (vanilla chat and the boxes)
 * that does not contain it. ctrl + f again or closing the chat leaves it. Enter does not send while it is on.
 */
public final class ChatSearch {
    private static boolean active;
    private static String query = "";

    private ChatSearch() {}

    public static boolean active() {
        return active;
    }

    /** True when a message should be hidden: search is on and the message does not contain the text. */
    public static boolean hides(Text message) {
        return active && !query.isEmpty() && !message.getString().toLowerCase(Locale.ROOT).contains(query);
    }

    /** ctrl + f. Starts the search with what is already typed, or ends it. Returns whether it is on now. */
    public static boolean toggle(String typed) {
        active = !active;
        query = active ? clean(typed) : "";
        apply();
        return active;
    }

    /** The chat box text changed while searching. */
    public static void typed(String text) {
        if (active) {
            query = clean(text);
            apply();
        }
    }

    public static void leave() {
        if (active) {
            active = false;
            query = "";
            apply();
        }
    }

    private static String clean(String text) {
        return text.toLowerCase(Locale.ROOT).trim();
    }

    private static void apply() {
        ChatBoxes.refilter();
        ChatHud hud = MinecraftClient.getInstance().inGameHud.getChatHud();
        ((ChatHudAccessor) hud).tacklebox$refresh();
        hud.resetScroll();
    }
}
