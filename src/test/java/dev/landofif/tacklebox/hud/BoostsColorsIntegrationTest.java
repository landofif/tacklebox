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

/**
 * luna's screenshots of 2026-10-28, chat to drawn hud: the booster, storm and gold rush lines go through both chat handlers and
 * come out as the small capitals the hud draws, each word in its chat colour and the timer without one (drawn white).
 */
class BoostsColorsIntegrationTest {
    private static final int GREY = 0x555555, WHITE = 0xFFFFFF, YELLOW = 0xFFFF55, GREEN = 0x55FF55, GOLD = 0xFFAA00, LIME = 0xD4F06B, BLUE = 0x7B9CFF;

    @AfterEach
    void clear() {
        BoostsHud.onChat(Text.literal("BOOSTER » Global Event Booster from Rexblis1905 has expired!"));
        BoostsHud.onChat(Text.literal("BOOSTER » Global Scale Booster from Rexblis1905 has expired!"));
        ServerHud.reset();
    }

    private static int color(Text drawn, String words) {
        return ChatColors.of(drawn, Foe.smallCaps(words));
    }

    @Test
    void screenshotLinesToDrawnRows() {
        for (String type : List.of("Event", "Scale")) {
            chat(line("BOOSTER", YELLOW, " » ", GREY, "Rexblis1905", WHITE, " has activated a ", WHITE, "Global " + type + " Booster", type.equals("Event") ? LIME : BLUE,
                    " » ", GREY, "2x", GOLD, " for 60m! ", WHITE, "(/tipall)", GREY));
        }
        chat(line("STORM", YELLOW, " » A ", WHITE, "Thunderstorm", YELLOW, " has formed! ", WHITE, "(+100 Bite Speed)", GREEN));
        chat(line("GOLD RUSH", GOLD, " » A ", WHITE, "Gold Rush", GOLD, " has begun! ", WHITE, "(Base Shard Chance → 1 in 50 25)", GREEN));

        List<BoostsHud.Row> rows = BoostsHud.rows();
        assertEquals(List.of("Event booster", "Scale booster", "Thunderstorm", "Gold Rush"), rows.stream().map(r -> r.label().get(0).text()).toList());

        Text event = BoostsHud.text(rows.get(0).label()), scale = BoostsHud.text(rows.get(1).label());
        assertEquals(Foe.smallCaps("Event booster 2x"), event.getString());
        assertEquals(LIME, color(event, "event booster"));
        assertEquals(GOLD, color(event, "2x"));
        assertEquals(BLUE, color(scale, "scale booster"));
        Text timer = BoostsHud.text(List.of(rows.get(0).value()));
        assertEquals(ChatColors.NONE, ChatColors.of(timer, timer.getString()));

        Text storm = BoostsHud.text(rows.get(2).label()), stormBonus = BoostsHud.text(List.of(rows.get(2).value()));
        assertEquals(YELLOW, color(storm, "thunderstorm"));
        assertEquals(Foe.smallCaps("+100 Bite Speed"), stormBonus.getString());
        assertEquals(GREEN, color(stormBonus, "+100 bite speed"));
        assertEquals(GREEN, color(BoostsHud.text(List.of(rows.get(3).value())), "shards"));
    }
}
