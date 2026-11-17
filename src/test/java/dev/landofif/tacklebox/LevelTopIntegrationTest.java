package dev.landofif.tacklebox;

import dev.landofif.tacklebox.hud.LevelBars;
import dev.landofif.tacklebox.hud.XpEstimate;
import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/** Lake Biwa at the top level: its compendium tooltip, then location xp from chat, to the level hud's row (luna, 2026-11-17). */
class LevelTopIntegrationTest {
    @AfterEach
    void tearDown() {
        TriggerEngine.assumeFishOnMc = false;
    }

    @Test
    void biwaAtTheTopIsTwentyWithTheXpPastTheMax() {
        String id = Locations.byName("Lake Biwa").id();
        // the compendium location item as luna's screenshot shows it
        XpEstimate.read(id, FishMenu.info(String.join("\n", "Lake Biwa", "Level: [20] (14.42M/2.32M)", "Catches: 7,955", "Runes Discovered: (15/15)")));
        TriggerEngine.assumeFishOnMc = true;
        XpEstimate.onChat(Text.literal("LOCATION » Lake Biwa gained 4.17K xp"));

        double[] at = XpEstimate.toNext(id);
        assertEquals(20, (int) at[0], "never 21");
        assertEquals(14_424_170, XpEstimate.need(21) - at[1], 0.5, "the chat xp adds to what is past the max");
        assertEquals("14.4M", LevelBars.locationXp(at, false));
        assertEquals("14.42M", LevelBars.locationXp(at, true));
    }
}
