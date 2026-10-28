package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.display.ChummerRing;
import dev.landofif.tacklebox.util.ChatColors;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Each kind of active hud row takes the colours of the chat line it came from (formats of luna's logs, colours made up). The fish
 * frenzy row is checked in {@link FrenzyHudIntegrationTest}: a frenzy line here would leave its "last seen" time behind for that test.
 */
class BoostsHudColorsTest {
    private static final int GREY = 0x555555, WHITE = 0xFFFFFF, YELLOW = 0xFFFF55, GREEN = 0x55FF55, GOLD = 0xFFAA00, BLUE = 0x7B9CFF, RED = 0xE0413A;

    static Text line(Object... parts) {
        MutableText t = Text.empty();
        for (int i = 0; i < parts.length; i += 2) {
            MutableText p = Text.literal((String) parts[i]);
            if (parts[i + 1] != null) {
                int c = (Integer) parts[i + 1];
                p = p.styled(st -> st.withColor(c));
            }
            t.append(p);
        }
        return t;
    }

    /** Both chat handlers, in the order {@code Tacklebox} calls them. */
    static void chat(Text line) {
        ServerHud.onChat(line);
        BoostsHud.onChat(line);
    }

    static BoostsHud.Row row(String name) {
        for (BoostsHud.Row r : BoostsHud.rows()) {
            if (r.label().get(0).text().equals(name)) {
                return r;
            }
        }
        return null;
    }

    @AfterEach
    void clear() {
        BoostsHud.onChat(Text.literal("BOOSTER » Global Scale Booster from Rexblis1905 has expired!"));
        BoostsHud.reset();
        ServerHud.reset();
    }

    @Test
    void boosterNameAndTimesInTheirColoursTimerUncoloured() {
        chat(line("BOOSTER", YELLOW, " » ", GREY, "Rexblis1905", WHITE, " has activated a ", WHITE, "Global Scale Booster", BLUE, " » ", GREY,
                "2x", GOLD, " for 60m! ", WHITE, "(/tipall)", GREY));
        BoostsHud.Row r = row("Scale booster");
        assertNotNull(r);
        assertEquals(List.of(new BoostsHud.Part("Scale booster", BLUE), new BoostsHud.Part("2x", GOLD)), r.label());
        assertEquals(ChatColors.NONE, r.value().color());
    }

    @Test
    void weatherNameAndBonus() {
        chat(line("STORM", YELLOW, " » A ", WHITE, "Thunderstorm", YELLOW, " has formed! ", WHITE, "(+100 Bite Speed)", GREEN));
        assertEquals(new BoostsHud.Row(List.of(new BoostsHud.Part("Thunderstorm", YELLOW)), new BoostsHud.Part("+100 Bite Speed", GREEN)), row("Thunderstorm"));
    }

    @Test
    void goldRushKeepsTheColourOfTheShortenedBonus() {
        chat(line("GOLD RUSH", GOLD, " » A ", WHITE, "Gold Rush", GOLD, " has begun! ", WHITE, "(Base Shard Chance → 1 in 50 25)", GREEN));
        assertEquals(new BoostsHud.Row(List.of(new BoostsHud.Part("Gold Rush", GOLD)), new BoostsHud.Part("Shards → 1 in 25", GREEN)), row("Gold Rush"));
    }

    @Test
    void moonNameAndBonus() {
        chat(line("BLUE MOON", BLUE, " » A ", WHITE, "Blue Moon", BLUE, " has risen. ", WHITE, "(2x Fishing XP)", GREEN));
        assertEquals(new BoostsHud.Row(List.of(new BoostsHud.Part("Blue Moon", BLUE)), new BoostsHud.Part("2x Fishing XP", GREEN)), row("Blue Moon"));
        chat(line("BLUE MOON", BLUE, " » The Blue Moon has set.", WHITE));
        assertEquals(null, row("Blue Moon"));
    }

    @Test
    void chummerFromChatInTheColourOfItsBonus() {
        chat(line("CHUMMER", GREEN, " » ", GREY, "meowfih", WHITE, " activated a ", GREEN, "", null, " Chummer (+130 Bite Speed)", GREEN));
        BoostsHud.Row r = row("chummer +130 Bite Speed");
        assertNotNull(r);
        assertEquals(GREEN, r.label().get(0).color());
    }

    @Test
    void fabledHeaderColourAndPlace() {
        chat(line("FABLED FISH EVENT", RED));
        chat(line("Visit the ", WHITE, "Cypress Lake", YELLOW, " to catch the fabled fish!", WHITE));
        assertEquals(new BoostsHud.Row(List.of(new BoostsHud.Part("Fabled Event", RED)), new BoostsHud.Part("at Cypress Lake", YELLOW)), row("Fabled Event"));
    }

    @Test
    void chummerFromItsHologramInTheHologramGreenOwnerWhite() throws ReflectiveOperationException {
        // only ChummerRing.tick fills the list, from the world's text displays
        java.lang.reflect.Field detected = ChummerRing.class.getDeclaredField("detected");
        detected.setAccessible(true);
        detected.set(null, List.of(new ChummerRing.Detected("meowfih", 75, System.currentTimeMillis() + 600_000)));
        try {
            BoostsHud.Row r = row("chummer +75");
            assertNotNull(r);
            assertEquals(List.of(new BoostsHud.Part("chummer +75", 0x41F55B), new BoostsHud.Part("meowfih", WHITE)), r.label());
            assertEquals(ChatColors.NONE, r.value().color());
        } finally {
            detected.set(null, List.of());
        }
    }

    @Test
    void linesWithoutColoursStayGrey() {
        chat(Text.literal("STORM » A Thunderstorm has formed! (+100 Bite Speed)"));
        assertEquals(new BoostsHud.Row(List.of(new BoostsHud.Part("Thunderstorm", ChatColors.NONE)), new BoostsHud.Part("+100 Bite Speed", ChatColors.NONE)),
                row("Thunderstorm"));
    }
}
