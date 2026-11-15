package dev.landofif.tacklebox;

import dev.landofif.tacklebox.menu.FishMenu;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.menu.TravelMap;
import dev.landofif.tacklebox.util.Glyphs;
import com.google.gson.Gson;
import net.minecraft.text.Text;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/** The item names of luna's /travel dump (2026-10-29) through the location lookup onto the map, the commands, and the setting. */
class TravelMapIntegrationTest {
    private static List<String> dumpNames() throws Exception {
        String dump = new String(TravelMapIntegrationTest.class.getResourceAsStream("/dumps/travel-20261029.txt").readAllBytes(), StandardCharsets.UTF_8);
        List<String> names = new ArrayList<>();
        Matcher m = Pattern.compile("(?m)^  name=\"([^\"]*)\"").matcher(dump);
        while (m.find()) {
            names.add(m.group(1));
        }
        return names;
    }

    @Test
    void everyPlaceOfTheTravelDumpGetsItsOwnSpotOnTheMap() throws Exception {
        List<String> ids = new ArrayList<>();
        for (String name : dumpNames()) {
            Locations.Location loc = Locations.byName(Glyphs.plain(Text.literal(name)));
            if (loc != null) {
                ids.add(loc.id());
            }
        }
        assertEquals(17, ids.size()); // "Return" and "Close" are no places
        assertEquals(17, ids.stream().distinct().count());
        assertTrue(ids.contains("toledobend")); // "Toledo Bend Reservoir *FRENZY!*" in the dump

        double[][] at = TravelMap.place(ids, new TravelMap.View(), 12, 30, 616, 232, 19);
        for (int i = 0; i < at.length; i++) {
            assertNotNull(at[i], ids.get(i));
            assertEquals(i, TravelMap.hit(at, at[i][0], at[i][1], 9), ids.get(i));
        }
        // cypress lake and toledo bend are the same spot in the world: apart on screen, still next to each other
        double[] cypress = at[ids.indexOf("cypress")], toledo = at[ids.indexOf("toledobend")];
        double d = Math.hypot(cypress[0] - toledo[0], cypress[1] - toledo[1]);
        assertTrue(d >= 18.9 && d < 45, "distance " + d);
    }

    @Test
    void theTravelCommandsAreTtTravelAndTurbotravel() {
        assertTrue(FishMenu.isTravelCommand("tt"));
        assertTrue(FishMenu.isTravelCommand("travel"));
        assertTrue(FishMenu.isTravelCommand("TurboTravel "));
        assertFalse(FishMenu.isTravelCommand("tpa meowfih"));
        assertFalse(FishMenu.isTravelCommand("stats"));
    }

    @Test
    void theMapIsOffUntilSwitchedOnAndSurvivesASave() {
        TackleboxSettings old = new Gson().fromJson("{}", TackleboxSettings.class);
        old.sanitize();
        assertFalse(old.travelMap);
        old.travelMap = true;
        assertTrue(new Gson().fromJson(new Gson().toJson(old), TackleboxSettings.class).travelMap);
    }
}
