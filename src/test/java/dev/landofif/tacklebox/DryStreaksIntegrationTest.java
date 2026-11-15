package dev.landofif.tacklebox;

import dev.landofif.tacklebox.chat.DryStreakChat;
import dev.landofif.tacklebox.chat.DryStreaks;
import dev.landofif.tacklebox.hud.CryptidHud;
import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.trigger.Matcher;
import com.google.gson.Gson;
import net.minecraft.text.Text;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/** Real chat lines from luna's logs and catches through the dry streak messages, with the moon from the server hud's chat state. */
class DryStreaksIntegrationTest {
    @BeforeEach
    @AfterEach
    void clean() {
        TackleboxSettings s = TackleboxSettings.get();
        s.dryOn.clear();
        s.dryText.clear();
        s.dryCount.clear();
        ServerHud.reset();
        DryStreaks.reset();
        DryStreakChat.take();
    }

    private static boolean chat(String line, long now) {
        ServerHud.onChat(Text.literal(line));
        CryptidHud.onChat(Matcher.normalize(line), now);
        return DryStreakChat.onChat(Matcher.normalize(line), now);
    }

    @Test
    void aShardAfterThreeCatchesSaysThreeInTheDefaultText() {
        TackleboxSettings s = TackleboxSettings.get();
        s.dryOn.add(DryStreaks.SHARD);
        long t = 1_800_000_000_000L;
        DryStreakChat.onCatch("largemouth bass", "normal", "adult", t);
        DryStreakChat.onCatch("largemouth bass", "normal", "adult", t + 20_000);
        DryStreakChat.onCatch("largemouth bass", "normal", "adult", t + 40_000);
        assertTrue(chat("RARE CATCH! You pulled a 2x Subtropical Shard", t + 40_100));
        assertEquals(List.of("&atacklebox &8» &b2x Subtropical Shard &7after &f3 &7catches"), DryStreakChat.take());
        assertTrue(DryStreakChat.take().isEmpty());
        assertEquals(0, s.dryCount.get("shard"));
        assertEquals(3, s.dryCount.get("pet"));

        // the pet row is off: its drop resets the count and says nothing; a quoted line is not a drop
        assertTrue(chat("PET DROP! You pulled a  Eagle Pet", t + 40_200));
        assertFalse(chat("[382]   Realstranden » PET DROP! You pulled a Common Bear Pet", t + 40_300));
        assertTrue(DryStreakChat.take().isEmpty());
        assertEquals(0, s.dryCount.get("pet"));
    }

    @Test
    void capsulesCountOnlyWhileTheBloodMoonIsUpAndTheTextCanBeEdited() {
        TackleboxSettings s = TackleboxSettings.get();
        s.dryOn.add(DryStreaks.CAPSULE);
        s.dryText.put(DryStreaks.CAPSULE, "%item% took %streak%");
        long t = 1_800_000_000_000L;
        DryStreakChat.onCatch("cod", "normal", "adult", t);
        chat("BLOOD MOON » A Blood Moon has risen! (+120 Bite Speed)", t + 1000);
        DryStreakChat.onCatch("cod", "normal", "adult", t + 20_000);
        DryStreakChat.onCatch("cod", "normal", "adult", t + 40_000);
        chat("BLOOD MOON » The Blood Moon has set.", t + 50_000);
        DryStreakChat.onCatch("cod", "normal", "adult", t + 60_000);
        assertEquals(2, s.dryCount.get("capsule"));
        chat("BLOOD MOON » A Blood Moon has risen! (+120 Bite Speed)", t + 100_000);
        assertTrue(chat("RARE CATCH! You pulled 1x Infusion Capsule", t + 120_000)); // before its catch is seen
        DryStreakChat.onCatch("cod", "normal", "adult", t + 120_100);
        assertEquals(List.of("Infusion Capsule took 3"), DryStreakChat.take());
        assertEquals(0, s.dryCount.get("capsule"));
    }

    @Test
    void countsAndTextsSurviveASaveAndOldSettingsGetEmptyOnes() {
        TackleboxSettings s = TackleboxSettings.get();
        s.dryOn.add(DryStreaks.GIGANTIC);
        s.dryText.put(DryStreaks.PET, "pet %streak%");
        DryStreakChat.onCatch("cod", "albino", "adult", 1000);
        TackleboxSettings back = new Gson().fromJson(new Gson().toJson(s), TackleboxSettings.class);
        back.sanitize();
        assertTrue(back.dryOn.contains(DryStreaks.GIGANTIC));
        assertEquals("pet %streak%", back.dryText.get(DryStreaks.PET));
        assertEquals(1, back.dryCount.get("gigantic"));
        assertEquals(0, back.dryCount.get("variant:albino"));

        TackleboxSettings old = new Gson().fromJson("{\"dryOn\":null,\"dryText\":null,\"dryCount\":null}", TackleboxSettings.class);
        old.sanitize();
        assertTrue(old.dryOn.isEmpty());
        assertEquals(DryStreaks.KINDS.get(0).template(), DryStreaks.template(old.dryText, DryStreaks.PET));
        assertTrue(old.dryCount.isEmpty());
    }
}
