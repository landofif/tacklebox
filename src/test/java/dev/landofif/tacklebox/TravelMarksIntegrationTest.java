package dev.landofif.tacklebox;

import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.menu.Locations;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** The fabled event's chat lines (docs/fishonmc-research.md section 4) through the server hud to the colour of the dump's Cypress Lake item. */
class TravelMarksIntegrationTest {
    private static int cypress() {
        return Locations.mark("Cypress Lake", ServerHud.fabledPlace());
    }

    @Test
    void fabledStartMarksThePlaceUntilTheFishIsCaught() {
        ServerHud.onChat(Text.literal("FABLED FISH EVENT"));
        ServerHud.onChat(Text.literal("Caught by someone"));
        assertEquals(0, cypress());

        ServerHud.onChat(Text.literal("FABLED FISH EVENT"));
        ServerHud.onChat(Text.literal("Visit the Cypress Lake to"));
        ServerHud.onChat(Text.literal("try and catch it!"));
        assertEquals(Locations.FABLED, cypress());
        assertEquals(0, Locations.mark("Kenai River", ServerHud.fabledPlace()));
        assertEquals(Locations.FRENZY, Locations.mark("Cypress Lake *FRENZY!*", ServerHud.fabledPlace()));

        ServerHud.onChat(Text.literal("FABLED FISH EVENT"));
        ServerHud.onChat(Text.literal("Caught by Freddy_2005"));
        assertEquals(0, cypress());
    }
}
