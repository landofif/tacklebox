package dev.landofif.tacklebox.trigger;

import dev.landofif.tacklebox.display.SoundQueue;
import dev.landofif.tacklebox.display.TitleOverlay;
import dev.landofif.tacklebox.util.RichText;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** With "chat colours" on, captured words keep the colours the chat line gave them, and the title's own style comes back after them. */
class TriggerColorsTest {
    private static final int GOLD = 0xFFAA00, AQUA = 0x55FFFF, GREEN = 0x55FF55, WHITE = 0xFFFFFF;

    @AfterEach
    void tearDown() {
        TriggerEngine.playSound = SoundQueue::play;
        TriggerStore.all().clear();
        TitleOverlay.clear();
    }

    /** A line made of (text, colour) pairs; a null colour means none. */
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

    /** Each character of a drawn text with its colour and whether it is bold: "a:ffaa00:b". */
    static List<String> chars(Text text) {
        List<String> out = new ArrayList<>();
        text.visit((style, s) -> {
            for (char c : s.toCharArray()) {
                TextColor color = style.getColor();
                out.add(c + ":" + (color == null ? "none" : String.format("%06x", color.getRgb())) + (style.isBold() ? ":b" : ""));
            }
            return Optional.empty();
        }, Style.EMPTY);
        return out;
    }

    @Test
    void lineColorsFollowTheNormalisedLine() {
        // a glyph and a double space are dropped by Matcher.normalize, so the colours have to move with the characters
        Text text = line("", WHITE, "RARE  CATCH! ", GOLD, "You pulled a 1x ", WHITE, "Subtropical Shard", GREEN);
        assertEquals("RARE CATCH! You pulled a 1x Subtropical Shard", Matcher.normalize(text.getString()));
        int[] colors = TriggerEngine.lineColors(text);
        assertEquals("RARE CATCH! You pulled a 1x Subtropical Shard".length(), colors.length);
        assertEquals(GOLD, colors[0]);
        assertEquals(GOLD, colors[5]); // the C, after the collapsed space
        assertEquals(WHITE, colors[12]);
        assertEquals(GREEN, colors[28]);
        assertEquals(GREEN, colors[colors.length - 1]);
        assertEquals(-1, TriggerEngine.lineColors(Text.literal("plain"))[0]);
    }

    @Test
    void colourCodeResetsBoldInRichText() {
        // what makes the restore needed: &#RRGGBB drops bold like vanilla colour codes do
        assertEquals(List.of("a:ffffff:b", "b:55ffff"), chars(RichText.parse("&la&#55ffffb", WHITE).toText()));
        assertEquals(new RichText.Codes("&r", "&l"), RichText.codesAtEnd("&lpet "));
        assertEquals(new RichText.Codes("&#55ffff", "&o"), RichText.codesAtEnd("&a&l&#55ffff&o x &&l"));
        assertEquals(new RichText.Codes("&r", ""), RichText.codesAtEnd("&a&lx&r"));
    }

    @Test
    void colouredValueKeepsBoldAndHandsTheTitleColourBack() {
        Map<String, String> values = Map.of("item", "Ab");
        Map<String, int[]> colors = Map.of("item", new int[]{AQUA, -1});
        String filled = TriggerEngine.fill("&l{item}!", values, colors);
        assertEquals(List.of("A:55ffff:b", "b:ffaa00:b", "!:ffaa00:b"), chars(RichText.parse(filled, GOLD).toText()));

        String lower = TriggerEngine.fill("&l{item.lower} &oend", values, colors);
        assertEquals(List.of("a:55ffff:b", "b:ffaa00:b", " :ffaa00:b", "e:ffaa00:b", "n:ffaa00:b", "d:ffaa00:b"),
                chars(RichText.parse(lower, GOLD).toText()).subList(0, 6));
        assertEquals("ab end", RichText.parse(lower, GOLD).toText().getString());

        // after a colour code in the template, that colour comes back
        String after = TriggerEngine.fill("&#123456x{item}y", values, Map.of("item", new int[]{AQUA, AQUA}));
        assertEquals(List.of("x:123456", "A:55ffff", "b:55ffff", "y:123456"), chars(RichText.parse(after, GOLD).toText()));
    }

    @Test
    void ampersandInAColouredValueStaysText() {
        String filled = TriggerEngine.fill("{x}", Map.of("x", "&l"), Map.of("x", new int[]{AQUA, AQUA}));
        assertEquals("&l", RichText.parse(filled, WHITE).toText().getString());
    }

    private static Trigger rare(boolean chatColors) {
        Trigger t = TriggerPresets.rareCatch();
        t.fishOnMcOnly = false;
        t.title.chatColors = chatColors;
        t.totem.enabled = false;
        TriggerStore.all().clear();
        TriggerStore.all().add(t);
        TriggerEngine.playSound = s -> {};
        TitleOverlay.clear();
        return t;
    }

    @Test
    void engineColoursTheCaptureOnlyWhenTheToggleIsOn() {
        Text text = line("RARE CATCH! ", GOLD, "You pulled a 1x ", WHITE, "Subtropical Shard", GREEN);
        rare(true);
        TriggerEngine.onGameMessage(text, false);
        List<String> subtitle = chars(TitleOverlay.shown().get(0).subtitle());
        assertEquals("s:55ff55", subtitle.get(0));
        assertEquals("subtropical shard", TitleOverlay.shown().get(0).subtitle().getString());

        rare(false);
        TriggerEngine.onGameMessage(text, false);
        assertEquals("s:bbbbbb", chars(TitleOverlay.shown().get(0).subtitle()).get(0), "the subtitle colour of the trigger");

        rare(true);
        TriggerEngine.onGameMessage(Text.literal("RARE CATCH! You pulled 1x Infusion Capsule"), false); // no colours in the line
        assertEquals("i:bbbbbb", chars(TitleOverlay.shown().get(0).subtitle()).get(0));
    }

    @Test
    void savedTriggersKeepTheirLookPresetsTakeChatColours() {
        Trigger saved = TriggerStore.GSON.fromJson("{\"name\":\"old\",\"title\":{\"title\":\"&lhi\"}}", Trigger.class);
        saved.sanitize();
        assertFalse(saved.title.chatColors);
        assertFalse(new Trigger().title.chatColors);
        for (TriggerPresets.Preset p : TriggerPresets.all()) {
            assertTrue(p.create().get().title.chatColors, p.name());
        }
    }
}
