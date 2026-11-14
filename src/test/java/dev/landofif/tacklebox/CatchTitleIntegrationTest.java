package dev.landofif.tacklebox;

import com.google.gson.Gson;
import dev.landofif.tacklebox.display.CatchTitle;
import dev.landofif.tacklebox.display.CatchWatch;
import dev.landofif.tacklebox.screen.SettingsPages;
import dev.landofif.tacklebox.screen.SettingsPages.Tile;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;
import net.minecraft.util.Uuids;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The catch title from the settings tile to the lines on screen: switched on in the settings, the server's catch title and
 * subtitle come in, the catch watch finds the new fish of yours, and the title says its rarity, name, size, weight and length.
 */
class CatchTitleIntegrationTest {
    private static final Path SRC = Path.of("src/main/java/dev/landofif/tacklebox");

    @AfterEach
    void off() {
        TackleboxSettings.get().catchTitle = false;
        CatchTitle.clear();
    }

    private static NbtCompound fish(UUID id, UUID catcher) {
        NbtCompound n = new NbtCompound();
        n.putIntArray("id", Uuids.toIntArray(id));
        n.putIntArray("catcher", Uuids.toIntArray(catcher));
        n.putString("fish", "sockeye_salmon");
        n.putString("rarity", "epic");
        n.putString("variant", "albino");
        n.putString("size", "gigantic");
        n.putString("location", "kenai");
        n.putFloat("weight", 8.26f);
        n.putFloat("length", 27.04f);
        return n;
    }

    private static String text(List<CatchTitle.Seg> line) {
        return line.stream().map(CatchTitle.Seg::text).collect(Collectors.joining());
    }

    @Test
    void aCatchShowsItsPictureAndTwoLines() {
        TackleboxSettings s = TackleboxSettings.get();
        Tile tile = SettingsPages.build().stream().filter(p -> p.name().equals("huds")).findFirst().orElseThrow()
                .tiles().stream().filter(t -> t.title().equals("catch title")).findFirst().orElseThrow();
        assertEquals("catch title", tile.main().name);
        assertFalse(s.catchTitle, "off until switched on");
        tile.main().click(0, 0);
        assertTrue(s.catchTitle);

        UUID me = UUID.randomUUID();
        NbtCompound old = fish(UUID.randomUUID(), me);
        NbtCompound caught = fish(UUID.randomUUID(), me);
        Text picture = Text.literal("");
        long now = 50_000;

        assertFalse(CatchTitle.onSubtitle(Text.literal(" Sockeye Salmon"), now - 5));
        CatchWatch.onTitle(picture.getString(), List.of(old), me, now);
        assertTrue(CatchTitle.onTitle(picture, now), "the server's own title is hidden");
        assertNull(CatchTitle.view(now + 20).info());

        CatchWatch.Fish found = CatchWatch.check(List.of(old, caught), me, now + 100);
        assertNotNull(found);
        CatchTitle.onFish(caught, "Sockeye Salmon", now + 100);

        CatchTitle.View view = CatchTitle.view(now + 120);
        assertEquals(picture, view.image());
        assertNull(view.fallback());
        assertEquals(" ꜱᴏᴄᴋᴇʏᴇ ꜱᴀʟᴍᴏɴ", text(CatchTitle.line1(view.info(), s)));
        assertEquals("ɢɪɢᴀɴᴛɪᴄ 8.3ʟʙ / 27.0ɪɴ", text(CatchTitle.line2(view.info(), s)));
        s.catchVariant = true;
        assertEquals("  ꜱᴏᴄᴋᴇʏᴇ ꜱᴀʟᴍᴏɴ", text(CatchTitle.line1(view.info(), s)));
        s.catchVariant = false;

        assertNull(CatchTitle.view(now + 3001), "gone after its three seconds");
    }

    @Test
    void settingsSurviveASaveAndOldFilesStayOff() {
        TackleboxSettings old = new Gson().fromJson("{\"biteOn\": true}", TackleboxSettings.class);
        old.sanitize();
        assertFalse(old.catchTitle);
        assertTrue(old.catchHideTitle);
        assertEquals(TackleboxSettings.BiteAnim.POP, old.catchAnim);

        TackleboxSettings s = new Gson().fromJson("{\"catchTitle\": true, \"catchX\": 80.5, \"catchY\": 12, \"catchScale\": 150,"
                + " \"catchImageSize\": 9000, \"catchSeconds\": 0, \"catchAnim\": \"gone\", \"catchVariant\": true, \"catchColor\": -1}", TackleboxSettings.class);
        s.sanitize();
        TackleboxSettings again = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        again.sanitize();
        assertTrue(again.catchTitle);
        assertTrue(again.catchVariant);
        assertEquals(80.5, again.catchX);
        assertEquals(12, again.catchY);
        assertEquals(150, again.catchScale);
        assertEquals(300, again.catchImageSize, "clamped");
        assertEquals(0.5, again.catchSeconds, "clamped");
        assertEquals(TackleboxSettings.BiteAnim.POP, again.catchAnim, "an unknown animation is the default");
        assertEquals(0xFFFFFF, again.catchColor);
    }

    @Test
    void itIsDrawnAsAHudMovedInTheMoverAndFedByTheCatchWatch() throws IOException {
        String main = Files.readString(SRC.resolve("Tacklebox.java"));
        assertTrue(main.contains("display.CatchTitle.renderHud(context)"));
        String mover = Files.readString(SRC.resolve("hud/HudMover.java"));
        assertTrue(mover.contains("if (s.catchTitle)") && mover.contains("CatchTitle.draw(ctx, true)"));
        assertTrue(Files.readString(SRC.resolve("display/CatchWatch.java")).contains("CatchTitle.onFish("));
        String mixin = Files.readString(SRC.resolve("mixin/InGameHudOverlayMixin.java"));
        assertTrue(mixin.contains("CatchTitle.onTitle(") && mixin.contains("CatchTitle.onSubtitle("));
    }
}
