package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.util.ChatColors;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static dev.landofif.tacklebox.hud.BoostsHudColorsTest.chat;
import static dev.landofif.tacklebox.hud.BoostsHudColorsTest.line;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * luna's /active reply of 2026-10-29 through both chat handlers to the drawn hud: five rows in small capitals, type and multiplier
 * in their chat colours (made up here), the timers uncoloured, and the startup reply after a restart leaves no row twice.
 */
class BoostsActiveIntegrationTest {
    private static final int WHITE = 0xFFFFFF, GREY = 0xAAAAAA, GOLD = 0xFFAA00, BLUE = 0x7B9CFF;

    @AfterEach
    void clear() {
        for (String type : List.of("Money", "XP", "Luck", "Scale")) {
            BoostsHud.onChat(Text.literal("BOOSTER » Global " + type + " Booster from Bostonfan3334 has expired!"));
        }
        BoostsHud.onChat(Text.literal("BOOSTER » Global Event Booster from digl has expired!"));
        ServerHud.reset();
    }

    private static void reply(String type, String owner, String left) {
        chat(line("Global " + type + " Booster", BLUE, " » ", GREY, "2x", GOLD, " activated by ", WHITE, owner, WHITE, " with " + left + " remaining. ", WHITE,
                "Use /tipall to gain an extra 500 XP and $500!", GREY));
    }

    @Test
    void activeReplyToDrawnRows() {
        for (int round = 0; round < 2; round++) { // the startup /active and one typed by hand
            reply("Money", "Bostonfan3334", "45m 47s");
            reply("XP", "Bostonfan3334", "45m 47s");
            reply("Luck", "Bostonfan3334", "45m 48s");
            reply("Scale", "Bostonfan3334", "45m 47s");
            reply("Event", "digl", "46m 59s");
        }
        List<BoostsHud.Row> rows = BoostsHud.rows();
        assertEquals(List.of("Money booster", "XP booster", "Luck booster", "Scale booster", "Event booster"),
                rows.stream().map(r -> r.label().get(0).text()).toList());

        Text event = BoostsHud.text(rows.get(4).label());
        assertEquals(Foe.smallCaps("Event booster 2x"), event.getString());
        assertEquals(BLUE, ChatColors.of(event, Foe.smallCaps("event booster")));
        assertEquals(GOLD, ChatColors.of(event, Foe.smallCaps("2x")));
        Text timer = BoostsHud.text(List.of(rows.get(4).value()));
        assertEquals(ChatColors.NONE, ChatColors.of(timer, timer.getString()));
        assertTrue(timer.getString().matches(Foe.smallCaps("46:5") + "\\d"), timer.getString()); // 46m 59s less the test's own time
    }
}
