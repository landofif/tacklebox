package dev.landofif.tacklebox.display;

import com.google.gson.Gson;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.screen.SettingsPages;
import dev.landofif.tacklebox.screen.SettingsPages.Tile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The water rings and the big catch moment from the settings to what shows: switched on in their groups, a fish swims to
 * your bobber (its particles through the same owner rule and countdown as BobberTimer.onParticle) and leaves a wake and
 * quicker bite rings, BITE! adds the bigger ring, and a catch through CatchWatch leaves a ring where the bobber was in the
 * fish's colour, with the glow for a mythical; with a full inventory the server's subtitle stands in for the fish.
 */
class CatchEffectsIntegrationTest {
    private static final Path SRC = Path.of("src/main/java/dev/landofif/tacklebox");
    private static final long T = 5_000_000;
    private static final double WATER = 62.889;

    @BeforeEach
    void fresh() {
        defaults();
        WaterRings.reset();
        BigCatch.clear();
        BiteCountdown.onBite(0); // other tests' bites must not hold the countdown back
        BiteCountdown.noteActionBar(false, 0);
    }

    @AfterEach
    void off() {
        defaults();
        TackleboxSettings.get().biteOn = true;
        WaterRings.reset();
        BigCatch.clear();
        BiteCountdown.onBite(0);
        SoundQueue.stopAll();
    }

    /** Both groups as a new install has them: off, with every row on. */
    private static void defaults() {
        TackleboxSettings s = TackleboxSettings.get();
        s.waterFx = false;
        s.waterBiteRings = s.waterWake = s.waterCatchRing = s.waterSparkles = true;
        s.waterOpacity = 60;
        s.waterColor = 0xD8F0FF;
        s.bigCatch = false;
        s.bigFabled = s.bigTrophy = s.bigMythical = s.bigGigantic = true;
        s.bigGlow = s.bigShine = s.bigSound = true;
        s.bigGlowOpacity = 35;
    }

    private static Tile tile(String title) {
        return SettingsPages.build().stream().filter(p -> p.name().equals("fishing")).findFirst().orElseThrow()
                .tiles().stream().filter(t -> t.title().equals(title)).findFirst().orElseThrow();
    }

    /** Switches both groups on the way the settings screen does, by their switch in the list of groups. */
    private static void switchOn() {
        TackleboxSettings s = TackleboxSettings.get();
        assertFalse(s.waterFx, "off until switched on");
        assertFalse(s.bigCatch, "off until switched on");
        tile("water").main().click(0, 0);
        tile("big catches").main().click(0, 0);
        assertTrue(s.waterFx);
        assertTrue(s.bigCatch);
    }

    private static NbtCompound fish(UUID id, UUID catcher, String rarity, String variant, String size) {
        NbtCompound n = new NbtCompound();
        n.putIntArray("id", Uuids.toIntArray(id));
        n.putIntArray("catcher", Uuids.toIntArray(catcher));
        n.putString("fish", "atlantic_cod");
        n.putString("rarity", rarity);
        n.putString("variant", variant);
        n.putString("size", size);
        n.putString("location", "med");
        return n;
    }

    private static List<WaterRings.Ring> rings(float to) {
        return WaterRings.rings().stream().filter(r -> r.to() == to).toList();
    }

    @Test
    void settingsSurviveASaveAndOldFilesStayOff() {
        TackleboxSettings old = new Gson().fromJson("{\"biteOn\": true}", TackleboxSettings.class);
        old.sanitize();
        assertFalse(old.waterFx);
        assertFalse(old.bigCatch);
        assertTrue(old.waterBiteRings && old.waterWake && old.waterCatchRing && old.waterSparkles, "the rows stay on");
        assertTrue(old.bigFabled && old.bigTrophy && old.bigMythical && old.bigGigantic && old.bigGlow && old.bigShine && old.bigSound);
        assertEquals(60, old.waterOpacity);
        assertEquals(0xD8F0FF, old.waterColor);
        assertEquals(35, old.bigGlowOpacity);

        TackleboxSettings s = new Gson().fromJson("{\"waterFx\": true, \"waterWake\": false, \"waterOpacity\": 0, \"waterColor\": -1,"
                + " \"bigCatch\": true, \"bigGigantic\": false, \"bigSound\": false, \"bigGlowOpacity\": 500}", TackleboxSettings.class);
        s.sanitize();
        TackleboxSettings again = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        again.sanitize();
        assertTrue(again.waterFx);
        assertFalse(again.waterWake);
        assertTrue(again.waterBiteRings);
        assertEquals(5, again.waterOpacity, "clamped");
        assertEquals(0xFFFFFF, again.waterColor);
        assertTrue(again.bigCatch);
        assertFalse(again.bigGigantic);
        assertFalse(again.bigSound);
        assertEquals(100, again.bigGlowOpacity, "clamped");
    }

    @Test
    void aFishSwimmingInLeavesAWakeAndQuickerRingsThenTheBiteRing() {
        switchOn();
        TackleboxSettings s = TackleboxSettings.get();
        assertEquals(TackleboxSettings.TimerStyle.UP, s.timerStyle, "the rings need no countdown on the timer");
        assertTrue(WaterRings.wantsApproach(s));
        double[] mine = {0, 0}, other = {0, 6};
        List<double[]> bobbers = List.of(mine, other);
        TreeSet<Long> born = new TreeSet<>();
        int particles = 0;
        for (int k = 0; k < 38; k++) { // a fish 3.8 blocks out swims in at 2 blocks a second, one particle a tick
            long t = T + k * 50L;
            double r = 3.8 - k * 0.1;
            WaterRings.onBobber(0, WATER, 0, t);
            // the same as BobberTimer.onParticle: the owner rule, then the countdown and the wake
            double[] owner = OwnFish.owner(-r, 0, 0, 1, bobbers, b -> b[0], b -> b[1]);
            if (owner == mine) {
                particles++;
                BiteCountdown.onApproach(r, t);
                WaterRings.onWake(-r, WATER, 0, t);
            }
            // a neighbour's fish at the same time: not ours, so no wake for it
            assertEquals(other, OwnFish.owner(2, 6, 0, 1, bobbers, b -> b[0], b -> b[1]));
            // the same as WaterRingRenderer.tick
            WaterRings.tick(t, BiteCountdown.active(t), BiteCountdown.seconds(t));
            rings(1.0f).forEach(ring -> born.add(ring.born()));
        }
        List<Long> biteRings = new ArrayList<>(born);
        assertEquals(38, particles, "every particle was our fish's");

        List<WaterRings.Ring> wake = rings(0.6f);
        assertTrue(wake.size() >= 3, "a wake: " + wake.size());
        for (int i = 1; i < wake.size(); i++) {
            assertTrue(Math.abs(wake.get(i).x() - wake.get(i - 1).x()) >= 0.35 - 1e-9, "spaced out");
            assertTrue(wake.get(i).born() - wake.get(i - 1).born() >= 120);
        }
        assertTrue(wake.stream().allMatch(r -> r.z() == 0 && r.x() < 0 && r.y() == WATER), "behind our own fish only");

        assertTrue(biteRings.size() >= 3, "bite rings: " + biteRings);
        assertTrue(biteRings.get(0) >= T + 150, "only once the countdown is sure (4 particles)");
        long firstGap = biteRings.get(1) - biteRings.get(0), lastGap = biteRings.get(biteRings.size() - 1) - biteRings.get(biteRings.size() - 2);
        assertTrue(firstGap > lastGap, "quicker near the bite: " + biteRings);
        assertTrue(lastGap >= 250, "never closer than 0.25 s");
        assertTrue(WaterRings.rings().stream().allMatch(r -> r.rgb() == 0xD8F0FF), "the water colour");

        // BITE! through the bobber timer (the bite notification off, so it needs no game)
        s.biteOn = false;
        long now = System.currentTimeMillis();
        WaterRings.onBobber(0, WATER, 0, now);
        assertFalse(BobberTimer.onTitle(Text.literal("BITE!")));
        List<WaterRings.Ring> big = rings(1.4f);
        assertEquals(1, big.size(), "one bigger ring on the bite");
        assertEquals(0, big.get(0).x());
        assertFalse(BiteCountdown.active(now + 50), "the rings stop: the countdown is over");
        int all = WaterRings.rings().size();
        WaterRings.tick(now + 50, BiteCountdown.active(now + 50), BiteCountdown.seconds(now + 50));
        assertTrue(WaterRings.rings().size() <= all, "no new bite rings after the bite");
    }

    @Test
    void aCatchLeavesARingWhereTheBobberWasAndAMythicalGlows() {
        switchOn();
        UUID me = UUID.randomUUID();
        NbtCompound old = fish(UUID.randomUUID(), me, "common", "normal", "adult");
        NbtCompound caught = fish(UUID.randomUUID(), me, "mythical", "normal", "large");
        WaterRings.onBobber(12.5, WATER, -7.5, T - 300); // the bobber's last tick: it is gone once the fish is caught

        CatchWatch.onTitle("", List.of(old), me, T);
        assertNull(BigCatch.current());
        CatchWatch.Fish found = CatchWatch.check(List.of(old, caught), me, T + 50);
        assertNotNull(found);
        // what CatchWatch.tick does with the fish item it found
        CatchWatch.effects(caught.getString("rarity").orElse(""), found.variant(), found.size(), T + 50);

        WaterRings.Ring ring = rings(1.5f).get(0);
        assertEquals(12.5, ring.x());
        assertEquals(WATER, ring.y());
        assertEquals(-7.5, ring.z());
        assertEquals(0xC93832, ring.rgb(), "mythical red");
        assertEquals(BigCatch.Kind.MYTHICAL, BigCatch.current());
        assertEquals(0.35f, BigCatch.glowAlpha(T + 250), 1e-6, "the edge glow at its peak");
        assertEquals(0, BigCatch.glowAlpha(T + 700), "and gone");
        assertTrue(BigCatch.shineStart(T) >= T, "the catch title shown at the catch gets the shine");

        CatchWatch.fallback(T + 800);
        assertEquals(1, WaterRings.rings().size(), "once per catch: the subtitle does not add another");

        CatchWatch.onTitle("", List.of(old, caught), me, T + 9000);
        assertNull(BigCatch.current(), "the next catch title ends the moment");
        assertEquals(-1, BigCatch.shineStart(T + 9000));
    }

    @Test
    void aFullInventoryGoesByTheServersSubtitle() {
        switchOn();
        UUID me = UUID.randomUUID();
        long t = T + 100_000;
        WaterRings.onBobber(3.5, WATER, 4.5, t - 50);
        CatchWatch.onSubtitle(" Atlantic Cod", t - 20); // the subtitle can come first
        CatchWatch.onTitle("", List.of(), me, t);
        assertNull(CatchWatch.check(List.of(), me, t + 100), "the fish dropped on the ground");
        CatchWatch.fallback(t + 400);
        assertTrue(WaterRings.rings().isEmpty(), "waiting for the fish item a little");
        CatchWatch.fallback(t + 500);
        assertEquals(0xC93832, rings(1.5f).get(0).rgb());
        assertEquals(BigCatch.Kind.MYTHICAL, BigCatch.current());
        CatchWatch.fallback(t + 600);
        assertEquals(1, WaterRings.rings().size(), "once");

        long u = t + 10_000;
        WaterRings.onBobber(3.5, WATER, 4.5, u);
        CatchWatch.onTitle("", List.of(), me, u);
        CatchWatch.onSubtitle("  Atlantic Cod", u + 300); // a common trophy, the subtitle after the title
        CatchWatch.fallback(u + 500);
        WaterRings.Ring common = rings(1.5f).get(1);
        assertEquals(0xFFFFFF, common.rgb(), "a common: faint white");
        assertEquals(0.3f, common.strength(), 1e-6);
        assertEquals(BigCatch.Kind.TROPHY, BigCatch.current());

        long v = u + 10_000;
        WaterRings.onBobber(3.5, WATER, 4.5, v);
        CatchWatch.onTitle("", List.of(), me, v);
        CatchWatch.fallback(v + 600);
        CatchWatch.fallback(v + 1001);
        CatchWatch.onSubtitle(" Atlantic Cod", v + 1500); // too late to be this catch's
        CatchWatch.fallback(v + 1600);
        assertEquals(2, WaterRings.rings().size(), "no fish and no subtitle: nothing");
        assertNull(BigCatch.current());
    }

    @Test
    void theGroupsHaveTheirRowsAndSwitches() {
        assertEquals("water rings", tile("water").main().name);
        assertEquals("prismarine_crystals", tile("water").icon());
        assertEquals(List.of("water rings", "bite rings", "fish wake", "catch ring", "catch sparkles", "water ring opacity", "water ring colour"),
                tile("water").rows().stream().map(r -> r.name).toList());
        assertEquals("big catch moment", tile("big catches").main().name);
        assertEquals("nether_star", tile("big catches").icon());
        assertEquals(List.of("big catch moment", "fabled catches", "trophy catches", "mythical catches", "gigantic catches", "edge glow",
                "edge glow strength", "title shine", "big catch sound"), tile("big catches").rows().stream().map(r -> r.name).toList());
    }

    @Test
    void itIsWiredIntoTheGame() throws IOException {
        String main = Files.readString(SRC.resolve("Tacklebox.java"));
        assertTrue(main.contains("WaterRingRenderer.register()") && main.contains("WaterRingRenderer.tick(client)")
                && main.contains("WaterRings.reset()") && main.contains("BigCatch.renderHud(context)"));
        assertTrue(Files.readString(SRC.resolve("display/WaterRingRenderer.java")).contains("WorldRenderEvents.END_MAIN.register("),
                "drawn after the water");
        String watch = Files.readString(SRC.resolve("display/CatchWatch.java"));
        assertTrue(watch.contains("effects(rarity, f.variant(), f.size(), now)") && watch.contains("fallback(now)"));
        String timer = Files.readString(SRC.resolve("display/BobberTimer.java"));
        assertTrue(timer.contains("WaterRings.onWake(") && timer.contains("WaterRings.onBite(now)"));
        assertTrue(Files.readString(SRC.resolve("mixin/InGameHudOverlayMixin.java")).contains("CatchWatch.onSubtitle("));
        assertTrue(Files.readString(SRC.resolve("display/CatchTitle.java")).contains("BigCatch.shineStart(shownAt)"));
        assertTrue(Files.exists(Path.of("src/main/resources/assets/tacklebox/textures/gui/edge_glow.png")));
    }
}
