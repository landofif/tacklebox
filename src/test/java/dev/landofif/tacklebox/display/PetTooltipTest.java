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

/** The tooltip of luna's common Duck Pet (dump of 2026-10-16, built the way the client had it after foecollab). */
class PetTooltipTest {
    private static final Style WHITE = Style.EMPTY.withColor(Formatting.WHITE);
    private static final Style LABEL = Style.EMPTY.withColor(0xCBCED3);
    private static final Style GRAY = Style.EMPTY.withColor(Formatting.DARK_GRAY);
    private static final Style TAGS = Style.EMPTY.withFont(new StyleSpriteSource.Font(Identifier.of("foecollab", "tags")));

    private static Text seg(String text, Style style) {
        return Text.literal(text).setStyle(style);
    }

    /** The server's window prefix with the glyph of its row, then the spaces that indent the line. */
    private static String window(String glyph, int spaces) {
        return " " + glyph + " " + " ".repeat(spaces);
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

    private static Text body(int indent, Text... content) {
        return line(window("퀃", indent), content);
    }

    private static Text separator() {
        return body(3, seg(" ".repeat(47), GRAY.withStrikethrough(true)));
    }

    private static Text spacer() {
        return body(1);
    }

    private static Text equipment(String label, String value) {
        return body(3, seg(label, Style.EMPTY.withColor(Formatting.GRAY)), seg(value, Style.EMPTY.withColor(0x666960)));
    }

    private static Text bonus(String label, int labelColor, String value, String percent) {
        return body(4, seg(label, Style.EMPTY.withColor(labelColor)), seg("(", GRAY), seg("+0", Style.EMPTY.withColor(0x93FBE0)),
                seg("/", GRAY), seg(value, Style.EMPTY.withColor(0x31544B)), seg(") ", GRAY), seg(percent, GRAY));
    }

    private static final String LUCK = "ʟᴜᴄᴋ: ";
    private static final String SCALE = "sᴄᴀʟᴇ: ";

    /** Index in the tooltip of each line, as in the dump: 0 is the item name, the lore follows. */
    private static List<Text> pet() {
        Style name = Style.EMPTY.withColor(0xD0D2A0);
        List<Text> in = new ArrayList<>();
        in.add(seg("Duck Pet", name));                                                                                      // 0 item name
        in.add(line(window("퀁", 3), Text.literal("Duck Pet").setStyle(name)));                                         // 1 header
        in.add(Text.empty().append(seg(window("퀃", 3), WHITE)).append(seg("", TAGS)));                          // 2 rarity mark
        in.add(body(3, seg("sᴜʙᴛʀᴏᴘɪᴄᴀʟ ᴘᴇᴛ", GRAY)));   // 3 kind
        in.add(separator());                                                                                                // 4
        in.add(body(3, seg("ᴘᴇᴛ ʟᴇᴠᴇʟ: ", GRAY), seg("[", GRAY),
                seg("0", Style.EMPTY.withColor(Formatting.GREEN)), seg("]", GRAY)));                                        // 5 level
        in.add(body(3, seg(" ".repeat(20), Style.EMPTY.withColor(0x677982).withBold(true).withStrikethrough(true)),
                seg(" (", GRAY), seg("0", Style.EMPTY.withColor(Formatting.AQUA)), seg("/", GRAY),
                seg("1K", Style.EMPTY.withColor(Formatting.BLUE)), seg(")", GRAY)));                                        // 6 xp bar
        in.add(separator());                                                                                                // 7
        in.add(body(3, seg("While in any ", Style.EMPTY.withColor(0xCED5C1)), seg("Subtropical Climate:", Style.EMPTY.withColor(0x47F7D6)))); // 8
        in.add(bonus(LUCK, 0x80DAC3, "+8", "(32%)"));                                                                       // 9
        in.add(bonus(SCALE, 0x4C88F1, "+15", "(60%)"));                                                                     // 10
        in.add(spacer());                                                                                                   // 11
        in.add(body(3, seg("While at ", Style.EMPTY.withColor(0xCED5C1)), seg("Toledo:", Style.EMPTY.withColor(0x99A7D0)))); // 12
        in.add(bonus(LUCK, 0x80DAC3, "+22", "(88%)"));                                                                      // 13
        in.add(bonus(SCALE, 0x4C88F1, "+8", "(32%)"));                                                                      // 14
        in.add(spacer());                                                                                                   // 15
        in.add(body(3, seg("ʀᴀᴛɪɴɢ: ", GRAY), seg("ɢᴏᴏᴅ ", Style.EMPTY.withColor(Formatting.GREEN)),
                seg("[", GRAY), seg("53%", Style.EMPTY.withColor(Formatting.GREEN)), seg("]", GRAY)));                      // 16 rating
        in.add(separator());                                                                                                // 17
        in.add(equipment("Equipped Skin: ", "Default"));                                                                    // 18
        in.add(separator());                                                                                                // 19
        in.add(separator());                                                                                                // 20
        in.add(equipment("Equipped Item: ", "Empty"));                                                                      // 21
        in.add(separator());                                                                                                // 22
        in.add(separator());                                                                                                // 23
        in.add(equipment("Trail Item: ", "Empty"));                                                                         // 24
        in.add(separator());                                                                                                // 25
        in.add(body(2, seg(" Discoverer: ", LABEL), seg("landofif", Style.EMPTY.withColor(0x80B3C6))));                     // 26
        in.add(body(2, seg(" Date: ", LABEL), seg("06/03/2026", Style.EMPTY.withColor(Formatting.GOLD))));                  // 27
        in.add(separator());                                                                                                // 28
        in.add(body(3, seg("ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ ᴘᴇᴛ ᴍᴇɴᴜ", GRAY))); // 29 hint
        in.add(line(window("퀄", 1)));                                                                                  // 30 end of the window
        return in;
    }

    private static List<String> plain(List<Text> lines) {
        return lines.stream().map(Glyphs::plain).toList();
    }

    private static List<Text> shortened(List<Text> in) {
        List<Text> lines = new ArrayList<>(in);
        PetTooltip.shorten(lines);
        return lines;
    }

    @Test
    void realDuckPetGoesFrom31LinesTo18() {
        List<Text> in = pet();
        List<Text> lines = shortened(in);
        assertEquals(18, lines.size());
        List<String> p = plain(in);
        assertEquals(List.of("duck pet", "duck pet", p.get(5), p.get(6), "", p.get(8), p.get(9), p.get(10), "",
                p.get(12), p.get(13), p.get(14), "", p.get(16), "", p.get(26), p.get(27), ""), plain(lines));
    }

    @Test
    void linesThatStayAreTheOriginalObjects() {
        List<Text> in = pet();
        List<Text> lines = shortened(in);
        int[] kept = {0, -1, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 26, 27, 30}; // -1: the name line, rebuilt
        for (int i = 0; i < kept.length; i++) {
            if (kept[i] >= 0) {
                assertSame(in.get(kept[i]), lines.get(i), "line " + i);
            }
        }
    }

    @Test
    void rarityMarkMovesToTheNameKeepingItsFont() {
        List<Text> lines = shortened(pet());
        boolean[] found = {false};
        lines.get(1).visit((style, s) -> {
            if (s.contains("") && new StyleSpriteSource.Font(Identifier.of("foecollab", "tags")).equals(style.getFont())) {
                found[0] = true;
            }
            return Optional.empty();
        }, Style.EMPTY);
        assertTrue(found[0]);
        assertTrue(lines.get(1).getString().startsWith(" 퀁"));
    }

    @Test
    void everyLineKeepsItsWindowPrefix() {
        List<Text> lines = shortened(pet());
        for (Text t : lines.subList(1, lines.size())) {
            assertTrue(t.getString().startsWith(" "), t.getString());
        }
    }

    @Test
    void noRuleIsLeftBeforeTheEndOfTheWindow() {
        List<Text> lines = shortened(pet());
        String last = lines.get(lines.size() - 2).getString();
        assertTrue(last.contains("Date: "), last); // the line before the end of the window is the date, not the rule that was above the hint
    }

    @Test
    void skinThatSaysEmptyGoesToo() {
        List<Text> in = pet();
        in.set(18, equipment("Equipped Skin: ", "Empty"));
        assertEquals(18, shortened(in).size());
    }

    @Test
    void equippedLinesStayAndTheSeparatorsAroundThemBecomeOne() {
        List<Text> in = pet();
        in.set(18, equipment("Equipped Skin: ", "Moonlight"));
        in.set(21, equipment("Equipped Item: ", "Golden Rod"));
        List<Text> lines = shortened(in);
        List<String> p = plain(in);
        assertEquals(List.of(p.get(16), "", p.get(18), "", p.get(21), "", p.get(26), p.get(27), ""),
                plain(lines).subList(lines.size() - 9, lines.size()));
    }

    @Test
    void priceLineAndLinesAfterTheWindowStay() {
        List<Text> in = pet();
        Text price = body(3, seg("Price: ", LABEL), seg("$100", Style.EMPTY.withColor(Formatting.GREEN)));
        in.add(29, price); // the auction house puts a price line before the end, behind the last separator
        in.add(Text.literal("minecraft:player_head"));
        in.add(Text.literal("30 component(s)"));
        List<Text> lines = shortened(in);
        assertTrue(lines.contains(price));
        assertEquals("price: $100", plain(lines).get(lines.indexOf(price)));
        assertTrue(plain(lines).get(lines.indexOf(price) - 1).isEmpty()); // the rule above it stays
        assertSame(in.get(in.size() - 2), lines.get(lines.size() - 2));
        assertSame(in.get(in.size() - 1), lines.get(lines.size() - 1));
    }

    @Test
    void separatorsStayWhenNothingWasRemoved() {
        List<Text> in = List.of(pet().get(0), pet().get(1), pet().get(5), separator(), separator(), pet().get(30));
        List<Text> lines = new ArrayList<>(in);
        PetTooltip.shorten(lines);
        assertEquals(in, lines);
    }

    @Test
    void runningTwiceChangesNothingMore() {
        List<Text> once = shortened(pet());
        List<Text> twice = shortened(once);
        assertEquals(once.size(), twice.size());
        for (int i = 0; i < once.size(); i++) {
            assertSame(once.get(i), twice.get(i));
        }
    }

    @Test
    void aTooltipWithoutTheServersWindowIsLeftAlone() {
        List<Text> in = List.of(Text.literal("Some Pet"), Text.literal("a plain lore line"), Text.literal("Equipped Skin: Default"));
        List<Text> lines = new ArrayList<>(in);
        PetTooltip.shorten(lines);
        assertEquals(in, lines);
    }

    @Test
    void onlyAStackWithTypePetCounts() {
        NbtCompound pet = new NbtCompound();
        pet.putString("type", "pet");
        assertTrue(PetTooltip.isPet(pet));
        pet.putString("type", "fish");
        assertFalse(PetTooltip.isPet(pet));
        assertFalse(PetTooltip.isPet(new NbtCompound()));
    }
}
