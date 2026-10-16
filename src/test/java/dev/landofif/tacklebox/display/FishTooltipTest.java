package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The tooltip of luna's Camouflage Grouper (dump of 2026-10-16, built the way the client had it after foecollab). */
class FishTooltipTest {
    private static final String WINDOW = " 퀃 ";
    private static final Style WHITE = Style.EMPTY.withColor(Formatting.WHITE);
    private static final Style LABEL = Style.EMPTY.withColor(0xCBCED3);
    private static final Style GRAY = Style.EMPTY.withColor(Formatting.DARK_GRAY);
    private static final Style TAGS = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("foecollab", "tags")));

    private static Text seg(String text, Style style) {
        return Text.literal(text).setStyle(style);
    }

    /** A lore line the way vanilla styles it: the window prefix first, then the server's own segments. */
    private static Text line(String prefix, Text... content) {
        MutableText t = Text.empty().setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(false));
        t.append(seg(prefix, WHITE));
        for (Text c : content) {
            t.append(c);
        }
        return t;
    }

    private static Text labelled(String label, Text... value) {
        List<Text> content = new ArrayList<>();
        content.add(seg(label, LABEL));
        content.addAll(List.of(value));
        return line(WINDOW + "  ", content.toArray(new Text[0]));
    }

    private static Text separator() {
        return line(WINDOW + "   ", seg(" ".repeat(47), GRAY.withStrikethrough(true)));
    }

    /** foecollab swaps the rarity mark for its own font and rebuilds the line without vanilla's outer style. */
    private static Text iconLine() {
        return Text.empty().append(seg(WINDOW + "   ", WHITE)).append(seg("", TAGS)).append(Text.literal(" "));
    }

    private static List<Text> fish() {
        List<Text> in = new ArrayList<>();
        in.add(seg("Camouflage Grouper", Style.EMPTY.withColor(Formatting.WHITE)));                                      // 0 the item name
        in.add(line(" 퀁    ", Text.literal("Camouflage Grouper")));            // 1 header
        in.add(iconLine());                                                                                              // 2
        in.add(line(WINDOW + "  ", seg(" ᴇᴘɪɴᴇᴘʜᴇʟᴜs ᴘᴏʟʏᴘʜᴇᴋᴀᴅɪᴏɴ", GRAY))); // 3 latin
        in.add(separator());                                                                                             // 4
        in.add(labelled(" ᴠᴀʟᴜᴇ: ", seg("$37.56", Style.EMPTY.withColor(Formatting.GREEN))));    // 5 value
        in.add(labelled(" ɴᴀᴛᴜʀᴇ: ", seg("sᴀssʏ", Style.EMPTY.withColor(0xFF5DFE)))); // 6 nature
        in.add(labelled(" ʟᴏᴄᴀᴛɪᴏɴ: ", seg("ᴄᴀɪʀɴꜱ", Style.EMPTY.withColor(0xA1C2FB)))); // 7 location
        in.add(labelled(" sɪᴢᴇ: ", seg("ᴀᴅᴜʟᴛ ", Style.EMPTY.withColor(0x1C7DA0)), seg("♂", Style.EMPTY.withColor(Formatting.AQUA)))); // 8 size
        in.add(labelled(" ᴡᴇɪɢʜᴛ: ", seg("0.17lb ", Style.EMPTY.withColor(0xE7FFF5)), seg("(0.08kg)", GRAY)));    // 9 weight
        in.add(labelled(" ʟᴇɴɢᴛʜ: ", seg("7.11in ", Style.EMPTY.withColor(0xE7FFF5)), seg("(18.06cm)", GRAY)));    // 10 length
        in.add(separator());                                                                                             // 11
        in.add(labelled(" ꜰɪsʜ ɢʀᴏᴜᴘ: ", seg("ɢʀᴏᴜᴘᴇʀs", Style.EMPTY.withColor(0xC6F7AA)))); // 12 group
        in.add(labelled(" ʟɪꜰᴇsᴛʏʟᴇ: ", seg("ʀᴇᴇꜰ", Style.EMPTY.withColor(0xC8F573)))); // 13 lifestyle
        in.add(labelled(" ᴇᴄᴏsʏsᴛᴇᴍ: ", seg("ɴᴀᴛɪᴠᴇ", Style.EMPTY.withColor(0x49C65C)))); // 14 ecosystem
        in.add(separator());                                                                                             // 15
        in.add(labelled(" Angler: ", seg("meowfih", Style.EMPTY.withColor(0x80B3C6))));                                  // 16
        in.add(labelled(" Date: ", seg("10/16/2026", Style.EMPTY.withColor(Formatting.GOLD))));                          // 17
        in.add(labelled(" Rod: ", seg("Meanmouth", Style.EMPTY.withColor(0x87D35B))));                                   // 18
        in.add(line(" 퀄  "));                                                   // 19 end of the window
        return in;
    }

    private static List<String> plain(List<Text> lines) {
        return lines.stream().map(Glyphs::plain).toList();
    }

    private static boolean carriesFont(Text line, String glyph, StyleSpriteSource font) {
        boolean[] found = {false};
        line.visit((style, s) -> {
            if (s.contains(glyph) && font.equals(style.getFont())) {
                found[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        return found[0];
    }

    @Test
    void realFishGoesFrom20LinesTo14() {
        List<Text> in = fish();
        List<Text> lines = new ArrayList<>(in);
        FishTooltip.shorten(lines);
        assertEquals(14, lines.size());
        assertEquals(List.of("camouflage grouper", "camouflage grouper", plain(in).get(6), plain(in).get(7), plain(in).get(8),
                "0.17lb/7.11in", "", plain(in).get(12), plain(in).get(13), "", plain(in).get(16), plain(in).get(17), plain(in).get(18), ""), plain(lines));
    }

    @Test
    void linesThatStayAreTheOriginalObjects() {
        List<Text> in = fish();
        List<Text> lines = new ArrayList<>(in);
        FishTooltip.shorten(lines);
        assertSame(in.get(0), lines.get(0));   // the item name
        for (int i : new int[] {6, 7, 8}) {
            assertTrue(lines.contains(in.get(i)));
        }
        assertSame(in.get(19), lines.get(13)); // end of the window
    }

    @Test
    void rarityMarkMovesToTheNameKeepingItsFont() {
        List<Text> lines = fish();
        FishTooltip.shorten(lines);
        assertTrue(carriesFont(lines.get(1), "", new StyleSpriteSource.Font(Identifier.of("foecollab", "tags"))));
        assertTrue(lines.get(1).getString().startsWith(" 퀁")); // still the window's top
    }

    @Test
    void variantGlyphOnTheMarkLineComesAlong() {
        List<Text> lines = fish();
        lines.set(2, Text.empty().append(seg(WINDOW + "   ", WHITE)).append(Text.literal(" ")).append(Text.literal("")));
        FishTooltip.shorten(lines);
        assertTrue(lines.get(1).getString().contains(""));
        assertTrue(lines.get(1).getString().endsWith(""));
    }

    @Test
    void everyLineKeepsItsWindowPrefix() {
        List<Text> lines = fish();
        FishTooltip.shorten(lines);
        for (Text t : lines.subList(1, lines.size())) {
            assertTrue(t.getString().startsWith(" "), t.getString());
        }
    }

    @Test
    void metricTextIsGoneFromWeightAndLength() {
        List<Text> lines = fish();
        FishTooltip.shorten(lines);
        String all = String.join("|", lines.stream().map(Text::getString).toList());
        assertFalse(all.contains("kg"));
        assertFalse(all.contains("cm"));
        assertTrue(all.contains("0.17lb/7.11in"));
    }

    @Test
    void weightAloneJustLosesItsKilograms() {
        List<Text> lines = new ArrayList<>(List.of(fish().get(0), fish().get(1), fish().get(9), fish().get(19)));
        FishTooltip.shorten(lines);
        assertEquals(List.of("camouflage grouper", "camouflage grouper", "weight: 0.17lb", ""), plain(lines));
    }

    @Test
    void runningTwiceChangesNothingMore() {
        List<Text> once = fish();
        FishTooltip.shorten(once);
        List<Text> twice = new ArrayList<>(once);
        FishTooltip.shorten(twice);
        assertEquals(once.size(), twice.size());
        for (int i = 0; i < once.size(); i++) {
            assertSame(once.get(i), twice.get(i));
        }
    }

    @Test
    void linesAfterTheWindowStayForAdvancedTooltips() {
        List<Text> in = fish();
        in.add(Text.literal("minecraft:cod"));
        in.add(Text.literal("22 component(s)"));
        List<Text> lines = new ArrayList<>(in);
        FishTooltip.shorten(lines);
        assertSame(in.get(20), lines.get(lines.size() - 2));
        assertSame(in.get(21), lines.get(lines.size() - 1));
    }

    @Test
    void aTooltipWithoutTheServersWindowIsLeftAlone() {
        List<Text> in = List.of(Text.literal("Some Item"), Text.literal("a plain lore line"), Text.literal("weight: 1lb (0.45kg)"));
        List<Text> lines = new ArrayList<>(in);
        FishTooltip.shorten(lines);
        assertEquals(in, lines);
    }

    @Test
    void unknownLabelsAreLeftAlone() {
        List<Text> in = List.of(fish().get(0), fish().get(1), labelled(" something else: ", Text.literal("x")), fish().get(19));
        List<Text> lines = new ArrayList<>(in);
        FishTooltip.shorten(lines);
        assertEquals(in, lines);
    }

    @Test
    void onlyAFishWithTheFishKeyCounts() {
        NbtCompound fish = new NbtCompound();
        fish.putString("fish", "camouflagegrouper");
        assertTrue(FishTooltip.isFish(fish));
        fish.putBoolean("shopitem", true);
        assertFalse(FishTooltip.isFish(fish));
        assertFalse(FishTooltip.isFish(new NbtCompound()));
    }
}
