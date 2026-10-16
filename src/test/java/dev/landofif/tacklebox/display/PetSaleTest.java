package dev.landofif.tacklebox.display;

import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The pet store payouts luna pasted on 2026-10-16 (rating, rarity, money earned) and the first listings of her
 * auction house pet page dump of the same day.
 */
class PetSaleTest {
    private static NbtCompound snbt(String s) {
        try {
            return StringNbtReader.readCompound(s);
        } catch (CommandSyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    /** A listing as the auction house gives it: the stats at the top, the price in renderInfo[0].money. */
    private static NbtCompound listing(String rarity, int level, String stats, float money) {
        return snbt("{" + stats + ",level:" + level + ",pet:\"x\",rarity:\"" + rarity + "\",type:\"pet\","
                + "renderInfo:[{category:\"pet\",money:" + money + "f,player:\"seller\"}]}");
    }

    private static final String FOX = "cbase:[{cur:0,cur_max:13,percent_max:0.52f},{cur:0,cur_max:9,percent_max:0.36f}],"
            + "lbase:[{cur:0,cur_max:21,percent_max:0.84f},{cur:0,cur_max:22,percent_max:0.88f}]";
    private static final String BULLFROG = "cbase:[{percent_max:0.32f},{percent_max:0.92f}],lbase:[{percent_max:0.8f},{percent_max:0.28f}]";
    private static final String DUCK = "cbase:[{percent_max:0.2f},{percent_max:0.2f}],lbase:[{percent_max:0.52f},{percent_max:0.68f}]";
    private static final String KANGAROO = "cbase:[{percent_max:0.36f},{percent_max:0.64f}],lbase:[{percent_max:0.64f},{percent_max:0.84f}]";

    // --- the pet store formula -------------------------------------------------------------------------

    @Test
    void payoutsMatchEveryPastedSellLine() {
        Object[][] sells = {
                {44, "common", "$824.69"}, {36, "common", "$473.66"}, {48, "common", "$1.09K"},
                {41, "common", "$669.86"}, {25, "common", "$220.97"}, {26, "common", "$236.83"},
                {24, "common", "$206.17"}, {39, "common", "$583.15"}, {54, "rare", "$3.3K"},
                {71, "rare", "$10.72K"}, {38, "rare", "$1.09K"},
        };
        for (Object[] s : sells) {
            double pay = PetSale.payout((String) s[1], (int) s[0]).orElseThrow();
            assertEquals(s[2], PetSale.money(pay), s[0] + "% " + s[1]);
        }
    }

    @Test
    void ratingGoesUpTenPointsAndTheValueDoubles() {
        double at40 = PetSale.payout("common", 40).orElseThrow();
        assertEquals(625.0, at40, 1e-9);
        assertEquals(1250.0, PetSale.payout("common", 50).orElseThrow(), 1e-9);
        assertEquals(2 * at40, PetSale.payout("common", 50).orElseThrow(), 1e-9);
        assertEquals(PetSale.payout("common", 58).orElseThrow(), PetSale.payout("rare", 48).orElseThrow(), 1e-9);
    }

    @Test
    void rarityWithoutSamplesHasNoPayout() {
        assertTrue(PetSale.payout("epic", 62).isEmpty());
        assertTrue(PetSale.payout("uncommon", 62).isEmpty());
        assertTrue(PetSale.payout("", 62).isEmpty());
    }

    // --- reading a pet ---------------------------------------------------------------------------------

    @Test
    void ratingIsTheAverageOfTheFourStats() {
        assertEquals(65.0, PetSale.rating(listing("common", 0, FOX, 8500)).orElseThrow(), 1e-9);
        assertEquals(58.0, PetSale.rating(listing("common", 0, BULLFROG, 2000)).orElseThrow(), 1e-9);
        assertEquals(40.0, PetSale.rating(listing("common", 0, DUCK, 2000)).orElseThrow(), 1e-9);
        assertEquals(62.0, PetSale.rating(listing("epic", 27, KANGAROO, 250000)).orElseThrow(), 1e-9);
    }

    @Test
    void foxAtEightAndAHalfThousandLosesMoney() {
        PetSale.Sale sale = PetSale.of(listing("common", 0, FOX, 8500)).orElseThrow();
        assertEquals(3535.53, sale.sells(), 0.005);
        assertEquals(8500.0, sale.price(), 1e-9);
        assertEquals(-4964.47, sale.profit(), 0.005);
        assertFalse(sale.profitable());
    }

    @Test
    void bullfrogAtTwoThousandMakesMoney() {
        PetSale.Sale sale = PetSale.of(listing("common", 0, BULLFROG, 2000)).orElseThrow();
        assertEquals(2176.38, sale.sells(), 0.005);
        assertEquals(176.38, sale.profit(), 0.005);
        assertTrue(sale.profitable());
    }

    @Test
    void duckAtTwoThousandLosesMoney() {
        PetSale.Sale sale = PetSale.of(listing("common", 0, DUCK, 2000)).orElseThrow();
        assertEquals(625.0, sale.sells(), 1e-9);
        assertFalse(sale.profitable());
    }

    @Test
    void epicKangarooIsLeftAloneBecauseEpicPaysAreNotKnown() {
        assertTrue(PetSale.of(listing("epic", 27, KANGAROO, 250000)).isEmpty());
    }

    @Test
    void breakEvenIsNotAProfit() {
        double exact = PetSale.payout("common", 58).orElseThrow();
        assertFalse(PetSale.of(listing("common", 0, BULLFROG, (float) exact)).orElseThrow().profitable());
    }

    @Test
    void thingsThatAreNotListedPetsGiveNothing() {
        assertTrue(PetSale.of(snbt("{fish:\"cod\",rarity:\"common\"}")).isEmpty());
        // your own pet in the pet menu: no price
        assertTrue(PetSale.of(snbt("{" + BULLFROG + ",pet:\"bullfrog\",rarity:\"common\",type:\"pet\"}")).isEmpty());
        // a price but no stats
        assertTrue(PetSale.of(snbt("{pet:\"x\",rarity:\"common\",type:\"pet\",renderInfo:[{money:2000.0f}]}")).isEmpty());
        // a price of nothing
        assertTrue(PetSale.of(listing("common", 0, BULLFROG, 0)).isEmpty());
        // only one stat in a list
        assertTrue(PetSale.of(snbt("{cbase:[{percent_max:0.5f}],lbase:[{percent_max:0.5f},{percent_max:0.5f}],rarity:\"common\","
                + "type:\"pet\",renderInfo:[{money:2000.0f}]}")).isEmpty());
    }

    @Test
    void moneyIsWrittenLikeTheServerDoesIt() {
        assertEquals("$2K", PetSale.money(2000));
        assertEquals("$8.5K", PetSale.money(8500));
        assertEquals("$250K", PetSale.money(250000));
        assertEquals("$824.69", PetSale.money(824.69));
        assertEquals("$176.4", PetSale.money(176.4));
        assertEquals("$1.5M", PetSale.money(1_500_000));
        assertEquals("$0", PetSale.money(0));
    }

    // --- the tooltip line ------------------------------------------------------------------------------

    private static final String PREFIX = " 퀃    ";
    private static final Style WHITE = Style.EMPTY.withColor(Formatting.WHITE);
    private static final Style LABEL = Style.EMPTY.withColor(0xCBCED3);

    private static Text auctionLine(String prefix, String label, String value) {
        MutableText t = Text.empty().setStyle(Style.EMPTY.withColor(Formatting.DARK_PURPLE).withItalic(false));
        t.append(Text.literal(prefix).setStyle(WHITE));
        t.append(Text.literal(label).setStyle(LABEL));
        t.append(Text.literal(value).setStyle(Style.EMPTY.withColor(Formatting.GOLD)));
        return t;
    }

    /** The end of the lore of a listing: seller, price, time, a gap, the purchase hint. */
    private static List<Text> listingTooltip() {
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("Bullfrog Pet"));
        lines.add(auctionLine(PREFIX, "ʀɪɢʜᴛ ᴄʟɪᴄᴋ ᴛᴏ ᴏᴘᴇɴ ᴘᴇᴛ ᴍᴇɴᴜ", ""));
        lines.add(auctionLine(" 퀁    ", "Seller: ", "x_Soymilk_x"));
        lines.add(auctionLine(PREFIX, "Price: ", "$2K"));
        lines.add(auctionLine(PREFIX, "Time: ", "23h 29m 26s"));
        lines.add(auctionLine(PREFIX, "ᴄʟɪᴄᴋ ᴛᴏ ᴄᴏɴꜰɪʀᴍ ᴘᴜʀᴄʜᴀsᴇ", ""));
        return lines;
    }

    private static PetSale.Sale bullfrog() {
        return PetSale.of(listing("common", 0, BULLFROG, 2000)).orElseThrow();
    }

    @Test
    void lineGoesRightUnderThePriceAndKeepsTheWindowPrefix() {
        List<Text> lines = listingTooltip();
        PetSale.addLine(lines, bullfrog());
        assertEquals(7, lines.size());
        assertTrue(Glyphs.plain(lines.get(3)).startsWith("price:"));
        Text added = lines.get(4);
        assertTrue(Glyphs.plain(added).startsWith("sells for:"), Glyphs.plain(added));
        assertTrue(Glyphs.plain(added).contains("$2.18k"), Glyphs.plain(added));
        assertTrue(Glyphs.plain(added).contains("+$176.38"), Glyphs.plain(added));
        assertTrue(added.getString().startsWith(PREFIX));
        assertTrue(Glyphs.plain(lines.get(5)).startsWith("time:"));
    }

    @Test
    void aLossShowsRedWithAMinusAndAProfitGreenWithAPlus() {
        List<Text> loss = listingTooltip();
        PetSale.addLine(loss, PetSale.of(listing("common", 0, DUCK, 2000)).orElseThrow());
        assertTrue(Glyphs.plain(loss.get(4)).contains("-$1.38k"), Glyphs.plain(loss.get(4)));
        assertEquals(Optional.of(Formatting.RED.getColorValue()), lastColor(loss.get(4)));

        List<Text> gain = listingTooltip();
        PetSale.addLine(gain, bullfrog());
        assertEquals(Optional.of(Formatting.GREEN.getColorValue()), lastColor(gain.get(4)));
    }

    private static Optional<Integer> lastColor(Text line) {
        List<Integer> colors = new ArrayList<>();
        line.visit((style, s) -> {
            if (!s.isBlank() && style.getColor() != null) {
                colors.add(style.getColor().getRgb());
            }
            return Optional.empty();
        }, Style.EMPTY);
        return colors.isEmpty() ? Optional.empty() : Optional.of(colors.get(colors.size() - 1));
    }

    @Test
    void addingTwiceStillLeavesOneLine() {
        List<Text> lines = listingTooltip();
        PetSale.addLine(lines, bullfrog());
        PetSale.addLine(lines, bullfrog());
        assertEquals(7, lines.size());
    }

    @Test
    void tooltipWithoutAPriceLineOrWindowIsLeftAlone() {
        List<Text> noPrice = listingTooltip();
        noPrice.remove(3);
        List<Text> before = new ArrayList<>(noPrice);
        PetSale.addLine(noPrice, bullfrog());
        assertEquals(before, noPrice);

        List<Text> noWindow = new ArrayList<>(List.of(Text.literal("Bullfrog Pet"), Text.literal("Price: $2K")));
        PetSale.addLine(noWindow, bullfrog());
        assertEquals(2, noWindow.size());
    }
}
