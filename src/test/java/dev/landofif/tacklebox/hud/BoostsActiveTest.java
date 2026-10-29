package dev.landofif.tacklebox.hud;

import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The /active reply (luna's log of 2026-10-29) puts each running booster on the hud with the time the server says is left. */
class BoostsActiveTest {
    @AfterEach
    void clear() {
        for (String type : List.of("Money", "XP", "Luck", "Scale")) {
            BoostsHud.onChat(Text.literal("BOOSTER » Global " + type + " Booster from Bostonfan3334 has expired!"));
        }
        BoostsHud.onChat(Text.literal("BOOSTER » Global Event Booster from digl has expired!"));
    }

    private static List<String> labels() {
        return BoostsHud.rows().stream().map(r -> r.label().get(0).text() + " " + r.label().get(1).text()).toList();
    }

    private static String value(String label) {
        for (BoostsHud.Row r : BoostsHud.rows()) {
            if (r.label().get(0).text().equals(label)) {
                return r.value().text();
            }
        }
        throw new AssertionError("no row " + label);
    }

    /** Seconds left in a row under an hour ("45:47"). */
    private static long secondsLeft(String label) {
        String[] p = value(label).split(":");
        return Long.parseLong(p[0]) * 60 + Long.parseLong(p[1]);
    }

    @Test
    void replyLinesBecomeRows() {
        BoostsHud.onChat(Text.literal("Global Money Booster » 2x activated by Bostonfan3334 with 45m 47s remaining. Use /tipall to gain an extra 500 XP and $500!"));
        BoostsHud.onChat(Text.literal("Global Event Booster » 2x activated by digl with 46m 59s remaining. Use /tipall to gain an extra 500 XP and $500!"));
        assertEquals(List.of("Money booster 2x", "Event booster 2x"), labels());
        long money = secondsLeft("Money booster");
        assertTrue(money > 45 * 60 + 45 && money <= 45 * 60 + 47, "money left " + money);
    }

    @Test
    void hoursAndSecondsOnly() {
        BoostsHud.onChat(Text.literal("Global Luck Booster » 3x activated by Bostonfan3334 with 1h 2m 3s remaining. Use /tipall to gain an extra 500 XP and $500!"));
        BoostsHud.onChat(Text.literal("Global XP Booster » 2x activated by Bostonfan3334 with 30s remaining. Use /tipall to gain an extra 500 XP and $500!"));
        assertEquals("1h 02m", value("Luck booster")); // the hud shows minutes above an hour
        long xp = secondsLeft("XP booster");
        assertTrue(xp > 27 && xp <= 30, "xp left " + xp);
        assertEquals(List.of("Luck booster 3x", "XP booster 2x"), labels());
    }

    @Test
    void askingTwiceCorrectsTheTimerWithoutADuplicate() {
        BoostsHud.onChat(Text.literal("BOOSTER » Bostonfan3334 has activated a Global Scale Booster » 2x for 60m! (/tipall)"));
        BoostsHud.onChat(Text.literal("Global Scale Booster » 2x activated by Bostonfan3334 with 45m 47s remaining. Use /tipall to gain an extra 500 XP and $500!"));
        BoostsHud.onChat(Text.literal("Global Scale Booster » 2x activated by Bostonfan3334 with 45m 40s remaining. Use /tipall to gain an extra 500 XP and $500!"));
        assertEquals(List.of("Scale booster 2x"), labels());
        long scale = secondsLeft("Scale booster");
        assertTrue(scale > 45 * 60 + 38 && scale <= 45 * 60 + 40, "scale left " + scale);
    }

    @Test
    void expiryLineStillRemovesIt() {
        BoostsHud.onChat(Text.literal("Global Money Booster » 2x activated by Bostonfan3334 with 45m 47s remaining. Use /tipall to gain an extra 500 XP and $500!"));
        BoostsHud.onChat(Text.literal("BOOSTER » Global Money Booster from Bostonfan3334 has expired!"));
        assertEquals(List.of(), labels());
    }

    @Test
    void playerChatQuotingTheReplyIsIgnored() {
        BoostsHud.onChat(Text.literal("[70]  Giorno_Giovano » Global Money Booster » 2x activated by Bostonfan3334 with 45m 47s remaining."));
        assertEquals(List.of(), labels());
    }
}
