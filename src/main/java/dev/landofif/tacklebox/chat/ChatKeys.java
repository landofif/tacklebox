package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.hud.ServerInfo;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;

import java.util.Locale;

/**
 * Keys in the chat field (hooked in ChatScreenMixin): Escape closes chat even with command
 * suggestions showing, and Tab right after "here" swaps the word for where you are.
 */
public final class ChatKeys {
    private static final String INSTANCE = "ɪɴꜱᴛᴀɴᴄᴇ";

    private ChatKeys() {}

    /** Keys the command suggestions get first. Not Escape: it would only hide them, one press should close chat. */
    public static boolean toSuggestions(KeyInput input) {
        return !input.isEscape();
    }

    /** The chat field after a Tab: its text and where the cursor goes. */
    public record Edit(String text, int cursor) {}

    /**
     * "here" just before the cursor (a whole word, any case) becomes "Kenai River (instance 3)",
     * or just the location outside an instance. Null when there is nothing to swap.
     */
    public static Edit here(String text, int cursor, String location, String instance) {
        if (location == null || location.isBlank() || cursor < 4 || cursor > text.length()) {
            return null;
        }
        int start = cursor - 4;
        if (!text.substring(start, cursor).toLowerCase(Locale.ROOT).equals("here")
                || (start > 0 && !Character.isWhitespace(text.charAt(start - 1)))) {
            return null;
        }
        String place = instance == null || instance.isBlank() ? location : location + " (instance " + instance + ")";
        return new Edit(text.substring(0, start) + place + text.substring(cursor), start + place.length());
    }

    /** The instance from the tab list footer ("ɪɴꜱᴛᴀɴᴄᴇ 3 (…"): its number, or "" when you are in none. */
    public static String instanceOf(String footer) {
        if (footer == null) {
            return "";
        }
        int at = footer.indexOf(INSTANCE);
        int end = footer.lastIndexOf('(');
        if (at < 0 || end < at + INSTANCE.length()) {
            return "";
        }
        String raw = footer.substring(at + INSTANCE.length(), end).trim();
        String digits = raw.replaceAll("\\D", "");
        return digits.isEmpty() ? raw : digits;
    }

    /** The Tab swap with what the boss bar and tab list say right now; null off fishonmc or with no location. */
    public static Edit here(String text, int cursor) {
        if (!TriggerEngine.isOnFishOnMc()) {
            return null;
        }
        Text footer = MinecraftClient.getInstance().inGameHud.getPlayerListHud().footer;
        return here(text, cursor, ServerInfo.LOCATIONS.get(ServerInfo.locationId), instanceOf(footer == null ? null : footer.getString()));
    }
}
