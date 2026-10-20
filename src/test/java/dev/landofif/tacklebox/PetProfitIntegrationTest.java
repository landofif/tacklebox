package dev.landofif.tacklebox;

import com.google.gson.Gson;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import dev.landofif.tacklebox.display.PetSale;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.nbt.StringNbtReader;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The switch loads off and survives a save, and with it on a listing's item data ends as a marked tooltip. */
class PetProfitIntegrationTest {
    private static TackleboxSettings load(String json) {
        TackleboxSettings s = new Gson().fromJson(json, TackleboxSettings.class);
        TackleboxSettings.importLegacy(com.google.gson.JsonParser.parseString(json).getAsJsonObject()); // the options are no longer in the gson file
        s.sanitize();
        return s;
    }

    @Test
    void oldSettingsKeepPetMarksOff() {
        assertFalse(after(load("{\"swingSlowness\": 2.0}"), TackleboxSettings.Menus.petProfitMarks));
    }

    @Test
    void savedSwitchRoundTrips() {
        TackleboxSettings s = load("{\"petProfitMarks\": true}");
        assertTrue(TackleboxSettings.Menus.petProfitMarks);
        assertFalse(after(load("{}"), TackleboxSettings.Menus.petProfitMarks));
    }

    /** The Bullfrog Pet of the 2026-10-16 auction dump (slot 2, $2K) from its item data to the line under its price. */
    @Test
    void bullfrogListingEndsAsAGreenLineUnderThePrice() throws CommandSyntaxException {
        var data = StringNbtReader.readCompound("{cbase:[{cur:0,cur_max:8,id:\"subtropical_luck\",max:25,min:5,percent_max:0.32f},"
                + "{cur:0,cur_max:23,id:\"subtropical_scale\",max:25,min:5,percent_max:0.92f}],climate:\"subtropical\","
                + "lbase:[{cur:0,cur_max:20,id:\"spawn_luck\",max:25,min:5,percent_max:0.8f},"
                + "{cur:0,cur_max:7,id:\"spawn_scale\",max:25,min:5,percent_max:0.28f}],level:0,location:\"spawn\","
                + "pet:\"bullfrog\",rarity:\"common\",renderInfo:[{category:\"pet\",money:2000.0f,player:\"x_Soymilk_x\"}],"
                + "type:\"pet\",xp_cur:0.0f,xp_need:1000.0f}");
        PetSale.Sale sale = PetSale.of(data).orElseThrow();
        assertEquals(58.0, sale.rating(), 1e-9);
        assertTrue(sale.profitable());

        String prefix = " 퀃    ";
        List<Text> lines = new ArrayList<>();
        lines.add(Text.literal("Bullfrog Pet"));
        lines.add(Text.literal(prefix).setStyle(Style.EMPTY.withColor(Formatting.WHITE)).append(Text.literal("Price: $2K")));
        lines.add(Text.literal(prefix).setStyle(Style.EMPTY.withColor(Formatting.WHITE)).append(Text.literal("Time: 23h 29m 26s")));
        PetSale.addLine(lines, sale);
        assertEquals(4, lines.size());
        assertEquals("sells for: $2.18k (+$176.38)", Glyphs.plain(lines.get(2)));
        assertEquals("time: 23h 29m 26s", Glyphs.plain(lines.get(3)));
    }

    /** Evaluates {@code first} (the load) before reading the static option. */
    private static <T> T after(Object first, T value) {
        return value;
    }
}
