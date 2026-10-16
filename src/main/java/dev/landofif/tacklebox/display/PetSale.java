package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.WeakHashMap;
import java.util.function.Supplier;

/**
 * What the pet store would pay for a pet in the auction house, next to what it costs (setting petProfitMarks,
 * docs/display/pet-profit.md). Display only: it reads the listing's item data and changes the tooltip lines
 * and a tint the client draws, never the item.
 *
 * <p>The payout was worked out from luna's pasted {@code SELL} lines (2026-10-16): it only depends on the pet's
 * rating (the average of its four {@code percent_max} stats, the same number the lore shows as "rating") and
 * its rarity, and it doubles with every 10 points of rating.
 */
public final class PetSale {
    /** What the store pays for a 50% pet. Rarities not in here have no sell samples yet, so they get no mark. */
    private static final Map<String, Double> AT_FIFTY = Map.of("common", 1250.0, "rare", 2500.0);

    private static final int LABEL = 0xCBCED3;
    private static final int VALUE = 0xE7FFF5;

    private PetSale() {}

    /** One listing: the pet's rating in percent, what the store pays, and what the listing costs. */
    public record Sale(double rating, double sells, double price) {
        public double profit() {
            return sells - price;
        }

        /** At least a cent, so a pet priced exactly at its value is not marked. */
        public boolean profitable() {
            return profit() >= 0.01;
        }
    }

    /** The store's payout, or empty for a rarity there are no samples of. */
    public static OptionalDouble payout(String rarity, double rating) {
        Double base = AT_FIFTY.get(rarity);
        return base == null ? OptionalDouble.empty() : OptionalDouble.of(base * Math.pow(2, (rating - 50) / 10));
    }

    /** The rating in percent (0 to 100): luck and scale of the climate and of the location, averaged. */
    public static OptionalDouble rating(NbtCompound pet) {
        double sum = 0;
        for (String list : new String[] {"cbase", "lbase"}) {
            NbtList stats = pet.getListOrEmpty(list);
            for (int i = 0; i < 2; i++) {
                Optional<Float> percent = stats.getCompoundOrEmpty(i).getFloat("percent_max");
                if (percent.isEmpty()) {
                    return OptionalDouble.empty();
                }
                sum += percent.get();
            }
        }
        return OptionalDouble.of(Math.round(sum * 25 * 100) / 100.0); // cut the float noise: 0.52f + ... is not exact
    }

    /** A pet listed with a price (the auction keeps it in {@code renderInfo[0].money}) of a rarity the store is known for. */
    public static Optional<Sale> of(NbtCompound pet) {
        if (!"pet".equals(pet.getString("type", ""))) {
            return Optional.empty();
        }
        Optional<Float> money = pet.getListOrEmpty("renderInfo").getCompoundOrEmpty(0).getFloat("money");
        OptionalDouble rating = rating(pet);
        if (money.isEmpty() || money.get() <= 0 || rating.isEmpty()) {
            return Optional.empty();
        }
        OptionalDouble sells = payout(pet.getString("rarity", ""), rating.getAsDouble());
        return sells.isEmpty() ? Optional.empty() : Optional.of(new Sale(rating.getAsDouble(), sells.getAsDouble(), money.get()));
    }

    /**
     * Slots are asked every frame and the item data is big, so each stack is read once (stacks are replaced when the
     * page changes). Synchronized: a bare WeakHashMap here froze the client in an endless get() on 2026-10-16.
     */
    private static final Map<Object, Optional<Sale>> CACHE = Collections.synchronizedMap(new WeakHashMap<>());

    public static Optional<Sale> of(ItemStack stack) {
        if (stack.isEmpty()) {
            return Optional.empty();
        }
        return cached(stack, () -> {
            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            return data == null ? Optional.empty() : of(data.copyNbt());
        });
    }

    /** The cache itself, apart from ItemStack so a test can use plain keys (stacks need the game's registries). */
    static Optional<Sale> cached(Object key, Supplier<Optional<Sale>> compute) {
        return CACHE.computeIfAbsent(key, k -> compute.get());
    }

    /** 2176.4 as "$2.18K", 824.69 as "$824.69", 8500 as "$8.5K": like the server writes money. */
    public static String money(double v) {
        if (v >= 1_000_000) {
            return "$" + trim(v / 1_000_000) + "M";
        }
        return v >= 1000 ? "$" + trim(v / 1000) + "K" : "$" + trim(v);
    }

    private static String trim(double v) {
        String s = String.format(Locale.ROOT, "%.2f", v);
        return s.contains(".") ? s.replaceAll("0+$", "").replaceAll("\\.$", "") : s;
    }

    /** The segment that carries the server's window art on a lore line, or null for a line without it. */
    private static Text prefix(Text line) {
        Text[] first = new Text[1];
        line.visit((style, s) -> {
            if (s.isEmpty()) {
                return Optional.empty();
            }
            first[0] = Text.literal(s).setStyle(style);
            return Optional.of(Boolean.TRUE); // the first segment is all that is wanted
        }, Style.EMPTY);
        return first[0] != null && first[0].getString().codePoints().anyMatch(Glyphs::isArt) ? first[0] : null;
    }

    /**
     * Adds "sells for: $2.18K (+$176.38)" under the Price line, with a copy of that line's window prefix so the
     * server's window art goes on around it. A tooltip with no Price line or no window, or one that already has
     * the line, is left alone.
     */
    public static void addLine(List<Text> lines, Sale sale) {
        String label = Glyphs.plain(Foe.text("sells for:"));
        int price = -1;
        for (int i = 0; i < lines.size(); i++) {
            String plain = Glyphs.plain(lines.get(i));
            if (plain.startsWith(label) && prefix(lines.get(i)) != null) {
                return;
            }
            if (price < 0 && plain.startsWith("price:") && prefix(lines.get(i)) != null) {
                price = i;
            }
        }
        if (price < 0) {
            return;
        }
        double profit = sale.profit();
        MutableText line = Text.empty().append(prefix(lines.get(price)));
        line.append(Foe.text("sells for: ").setStyle(Style.EMPTY.withColor(LABEL)));
        line.append(Text.literal(money(sale.sells())).setStyle(Style.EMPTY.withColor(VALUE)));
        line.append(Text.literal(" (" + (profit >= 0 ? "+" : "-") + money(Math.abs(profit)) + ")")
                .setStyle(Style.EMPTY.withColor(sale.profitable() ? Formatting.GREEN : Formatting.RED)));
        List<Text> out = new ArrayList<>(lines);
        out.add(price + 1, line);
        lines.clear();
        lines.addAll(out);
    }
}
