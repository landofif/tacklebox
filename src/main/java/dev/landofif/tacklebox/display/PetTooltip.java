package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.display.FishTooltip.Line;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.Text;

import java.util.List;

/**
 * Shorter tooltips for pets (setting shortPetTooltips, docs/display/item-tooltips.md). Display only, found and
 * written back the way {@link FishTooltip} does it: by label or shape, a line that is not recognised stays.
 */
public final class PetTooltip {
    private PetTooltip() {}

    /** A pet: its custom_data type is "pet". */
    public static boolean isPet(NbtCompound data) {
        return "pet".equals(data.getString("type", ""));
    }

    /**
     * Shortens the tooltip lines of a pet in place: the rarity mark moves up beside the name and the kind line
     * under it goes, as do equipped skin, item and trail lines with nothing equipped and the hint to right click.
     * The separators that end up next to each other become one.
     */
    public static void shorten(List<Text> lines) {
        List<Line> work = FishTooltip.lines(lines);
        if (!FishTooltip.moveMark(work)) {
            return;
        }
        if (work.removeIf(PetTooltip::isClutter)) {
            for (int i = work.size() - 1; i > 0; i--) {
                if (work.get(i).isSeparator() && work.get(i - 1).isSeparator()) {
                    work.remove(i);
                }
            }
            dropTrailingSeparators(work);
        }
        FishTooltip.apply(work, lines);
    }

    private static boolean isClutter(Line l) {
        if (!l.isWindow()) {
            return false;
        }
        String plain = l.plain();
        return (l.hasLabel("equipped skin:") && (plain.endsWith("default") || plain.endsWith("empty")))
                || (l.hasLabel("equipped item:") && plain.endsWith("empty"))
                || (l.hasLabel("trail item:") && plain.endsWith("empty"))
                || plain.equals("right click to open pet menu");
    }

    /** A separator with only blank window lines after it would draw a rule onto nothing. */
    private static void dropTrailingSeparators(List<Line> work) {
        for (int i = work.size() - 1; i >= 0; i--) {
            Line l = work.get(i);
            if (!l.isWindow()) {
                continue; // F3+H lines after the window
            }
            if (l.isSeparator()) {
                work.remove(i);
            } else if (!l.content().isEmpty()) {
                return;
            }
        }
    }
}
