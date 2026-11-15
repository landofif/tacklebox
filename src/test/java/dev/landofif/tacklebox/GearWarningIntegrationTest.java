package dev.landofif.tacklebox;

import dev.landofif.tacklebox.hud.GearCheck;
import dev.landofif.tacklebox.hud.GearWarning;
import dev.landofif.tacklebox.hud.TextHud;
import dev.landofif.tacklebox.trigger.Matcher;
import com.google.gson.Gson;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real PETS lines from luna's logs through the normalizer into the saved equipped pet, and from there to the warning rows. */
class GearWarningIntegrationTest {
    @AfterEach
    void tearDown() {
        TackleboxSettings.get().equippedPet = "";
        GearCheck.reset();
    }

    @Test
    void theLogsPetLinesDriveTheWarning() {
        TackleboxSettings s = TackleboxSettings.get();
        assertTrue(GearWarning.onChat(Matcher.normalize("PETS » Equipped your Eagle Pet")));
        assertEquals("eagle", s.equippedPet);
        assertFalse(GearWarning.onChat(Matcher.normalize("PETS » Your Eagle Pet is now level 41 → 42 (+1)"))); // nothing new
        assertFalse(GearWarning.onChat(Matcher.normalize("PETS » You cannot move your active pet in your inventory!")));

        // at the great lakes the eagle is right, at the danube it is not
        assertTrue(GearCheck.rows(GearCheck.place("greatlakes"), s.equippedPet, false, List.of()).isEmpty());
        List<TextHud.Row> rows = GearCheck.rows(GearCheck.place("danube"), s.equippedPet, true, List.of());
        assertEquals("wrong pet", rows.get(0).label());
        assertEquals("use wolf · in inventory", rows.get(0).value());

        // a level line of another pet means that one is equipped now (the equip line was missed)
        assertTrue(GearWarning.onChat(Matcher.normalize("PETS » Your Duck Pet is now level 99 → 100 (+1)")));
        assertEquals("duck", s.equippedPet);
        assertTrue(GearWarning.onChat(Matcher.normalize("PETS » Pet unequipped!")));
        assertEquals("", s.equippedPet);
        assertTrue(GearCheck.rows(GearCheck.place("danube"), s.equippedPet, false, List.of()).isEmpty());
    }

    @Test
    void thePetAndTheHudSurviveASaveAndOldSettingsGetTheHudOff() {
        TackleboxSettings s = TackleboxSettings.get();
        GearWarning.onChat(Matcher.normalize("PETS » Equipped your Marsh Rabbit Pet"));
        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        back.sanitize();
        assertEquals("marshrabbit", back.equippedPet);
        assertTrue(back.gearChat);

        TackleboxSettings old = new Gson().fromJson("{\"gearHud\":null,\"equippedPet\":null}", TackleboxSettings.class);
        old.sanitize();
        assertFalse(old.gearHud.on);
        assertEquals(360, old.gearHud.pos.x);
        assertEquals("", old.equippedPet);
    }
}
