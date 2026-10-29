package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.display.OwnFish;
import dev.landofif.tacklebox.display.ParticleSwitches;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Hide nearby players, only my fish and particle switches: old settings keep them off, choices survive a save. */
class CrowdsIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepEverythingShown() {
        TackleboxSettings s = load("{\"swingSlowness\": 2.0}");
        assertFalse(s.hideNearPlayers);
        assertEquals(4, s.nearPlayersRange);
        assertFalse(s.ownFishOnly);
        assertEquals(List.of(), s.hiddenParticles);
        assertEquals(List.of(), load("{\"hiddenParticles\": null}").hiddenParticles);
    }

    @Test
    void nearbyPlayersRangeIsClampedAndSaved() {
        TackleboxSettings s = load("{\"hideNearPlayers\": true, \"nearPlayersRange\": 99}");
        assertEquals(12, s.nearPlayersRange);
        TackleboxSettings again = load(new Gson().toJson(s));
        assertTrue(again.hideNearPlayers);
        assertEquals(12, again.nearPlayersRange);
    }

    @Test
    void onlyMyFishDropsANeighboursFishAtACrowdedDock() {
        TackleboxSettings s = load(new Gson().toJson(load("{\"ownFishOnly\": true}")));
        double[] mine = {0, 0}, left = {-2.5, 0}, right = {2.5, 0};
        List<double[]> dock = List.of(mine, left, right);
        double[] toRight = OwnFish.owner(2.5, -1.5, 1, 0, dock, b -> b[0], b -> b[1]);
        double[] toMine = OwnFish.owner(0, -1.5, 1, 0, dock, b -> b[0], b -> b[1]);
        assertSame(right, toRight);
        assertSame(mine, toMine);
        assertTrue(OwnFish.drops(s.ownFishOnly, toRight, mine));
        assertFalse(OwnFish.drops(s.ownFishOnly, toMine, mine));
    }

    @Test
    void switchedOffParticlesSurviveASave() {
        TackleboxSettings s = load("{}");
        ParticleSwitches.toggle(s.hiddenParticles, "minecraft:rain");
        ParticleSwitches.toggle(s.hiddenParticles, "minecraft:firework");
        TackleboxSettings again = load(new Gson().toJson(s));
        assertEquals(List.of("minecraft:rain", "minecraft:firework"), again.hiddenParticles);
        assertEquals(List.of("minecraft:firework", "minecraft:rain"), ParticleSwitches.list(List.of(), again.hiddenParticles));
    }
}
