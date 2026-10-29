package dev.landofif.tacklebox.util;

import dev.landofif.tacklebox.trigger.Matcher;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/** Chat lines built like luna's screenshots of 2026-10-28 (the hex values are close to them, not read from the server). */
class ChatColorsTest {
    /** Pairs of text and colour (null: none). */
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

    @Test
    void wordsTakeTheColourOfTheirFirstLetter() {
        Text l = line("BOOSTER", 0xFFFF55, " » ", 0x555555, "Rexblis1905", 0xFFFFFF, " has activated a ", 0xAAAAAA,
                "Global Event Booster", 0xD4F06B, " » ", 0x555555, "2x", 0xFFAA00, " for 60m!", 0xFFFFFF);
        assertEquals(0xD4F06B, ChatColors.of(l, "Event Booster"));
        assertEquals(0xFFAA00, ChatColors.of(l, "2x for"));
        assertEquals(0xFFFF55, ChatColors.of(l, "BOOSTER"));
    }

    @Test
    void colourOfTheParentCounts() {
        Text l = Text.literal("CHUMMER » ").formatted(Formatting.GREEN).append(Text.literal("meowfih activated a Chummer (+75 Bite Speed)"));
        assertEquals(0x55FF55, ChatColors.of(l, "+75 Bite Speed"));
    }

    @Test
    void wordsTheNormalizedLineChangedFallBackToTheirFirstWord() {
        // normalize drops the pack glyph, so the bonus as the hud has it is not in the line as sent
        Text l = line("GOLD RUSH", 0xFFAA00, " » A Gold Rush has begun! ", 0xFFFFFF, "(Base Shard Chance → 1 in 50 25)", 0x55FF55);
        String normalized = Matcher.normalize(l.getString());
        String bonus = normalized.substring(normalized.indexOf('(') + 1, normalized.indexOf(')'));
        assertEquals("Base Shard Chance → 1 in 50 25", bonus);
        assertEquals(0x55FF55, ChatColors.of(l, bonus));
    }

    @Test
    void missingWordsAndPlainLinesHaveNoColour() {
        Text l = line("STORM", 0xFFFF55, " » A ", 0xFFFFFF, "Thunderstorm", 0xFFFF55, " has formed!", 0xFFFFFF);
        assertEquals(ChatColors.NONE, ChatColors.of(l, "Rainbow"));
        assertEquals(ChatColors.NONE, ChatColors.of(l, ""));
        assertEquals(ChatColors.NONE, ChatColors.of(l, null));
        assertEquals(ChatColors.NONE, ChatColors.of(Text.literal("STORM » A Thunderstorm has formed!"), "Thunderstorm"));
    }
}
