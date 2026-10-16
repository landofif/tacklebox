package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Shorter tooltips for caught fish (setting shortFishTooltips, docs/display/item-tooltips.md). Display only:
 * the item and its lore are never touched, only the list of lines the client is about to draw.
 *
 * <p>The server starts every lore line with a window prefix (art glyphs in the rarity colour), so a line that
 * is removed takes its slice of the window with it and the rest stays whole. Lines are found by their label
 * or their shape, never by index, because foecollab and F3+H also change the list. A line that is not
 * recognised is left alone.
 */
public final class FishTooltip {
    /** How the window prefix on the first segment of every lore line starts. */
    private static final String WINDOW = "";

    private FishTooltip() {}

    private record Seg(String text, Style style) {}

    /** One tooltip line. The segments are read when first needed, the text is rebuilt only for edited lines. */
    private static final class Line {
        private Text text;
        private List<Seg> segs;

        Line(Text text) {
            this.text = text;
        }

        List<Seg> segs() {
            if (segs == null) {
                List<Seg> out = new ArrayList<>();
                text.visit((style, s) -> {
                    if (!s.isEmpty()) {
                        out.add(new Seg(s, style));
                    }
                    return Optional.empty();
                }, Style.EMPTY);
                segs = out;
            }
            return segs;
        }

        void set(List<Seg> next) {
            MutableText out = Text.empty();
            for (Seg s : next) {
                out.append(Text.literal(s.text()).setStyle(s.style()));
            }
            segs = next;
            text = out;
        }

        boolean isWindow() {
            return !segs().isEmpty() && segs().get(0).text().startsWith(WINDOW);
        }

        /** Everything after the window prefix. */
        List<Seg> content() {
            return isWindow() ? segs().subList(1, segs().size()) : List.of();
        }

        String plain() {
            return Glyphs.plain(text);
        }

        /** The rarity mark line: only spaces and glyphs after the prefix. */
        boolean isIcons() {
            boolean glyph = false;
            for (Seg s : content()) {
                for (int i = 0; i < s.text().length(); ) {
                    int cp = s.text().codePointAt(i);
                    i += Character.charCount(cp);
                    if (Glyphs.isArt(cp)) {
                        glyph = true;
                    } else if (!Character.isWhitespace(cp)) {
                        return false;
                    }
                }
            }
            return glyph;
        }

        /** A rule across the window: blank, struck through. */
        boolean isSeparator() {
            List<Seg> content = content();
            return !content.isEmpty() && content.stream().allMatch(s -> s.text().isBlank() && s.style().isStrikethrough());
        }

        /** The grey scientific name under the rarity mark: text without a label. */
        boolean isLatinName() {
            return isWindow() && !isSeparator() && !plain().isEmpty() && !plain().contains(":");
        }

        boolean hasLabel(String label) {
            return isWindow() && plain().startsWith(label);
        }
    }

    /** A caught fish: its custom_data has the fish key and it is not a display item of the shop. */
    public static boolean isFish(NbtCompound data) {
        return data.contains("fish") && !data.getBoolean("shopitem", false);
    }

    /**
     * Shortens the tooltip lines of a caught fish in place: the rarity mark moves up beside the name, and the
     * scientific name, the separator under it, the value line and the ecosystem line go. Weight and length
     * share one line without the kg and cm.
     */
    public static void shorten(List<Text> lines) {
        List<Line> work = new ArrayList<>();
        for (Text t : lines) {
            work.add(new Line(t));
        }
        int header = -1; // the name with the window's top; line 0 is the plain item name and stays as it is
        for (int i = 1; i < work.size() && header < 0; i++) {
            if (work.get(i).isWindow()) {
                header = i;
            }
        }
        if (header < 0) {
            return;
        }
        int below = header + 1;
        if (below < work.size() && work.get(below).isIcons()) {
            List<Seg> name = new ArrayList<>(work.get(header).segs());
            name.add(new Seg(" ", Style.EMPTY));
            name.addAll(trimmed(work.get(below).content()));
            work.get(header).set(name);
            work.remove(below);
            if (below < work.size() && work.get(below).isLatinName()) {
                work.remove(below);
            }
            if (below < work.size() && work.get(below).isSeparator()) {
                work.remove(below);
            }
        }
        work.removeIf(l -> l.hasLabel("value:") || l.hasLabel("ecosystem:"));
        weightAndLength(work);

        List<Text> out = new ArrayList<>();
        for (Line l : work) {
            out.add(l.text);
        }
        if (!out.equals(lines)) {
            lines.clear();
            lines.addAll(out);
        }
    }

    /** Weight and length as `0.17lb/7.11in` on the weight line; alone, each just loses its bracketed kg or cm. */
    private static void weightAndLength(List<Line> work) {
        Line weight = null;
        Line length = null;
        for (Line l : work) {
            if (weight == null && l.hasLabel("weight:")) {
                weight = l;
            } else if (length == null && l.hasLabel("length:")) {
                length = l;
            }
        }
        List<Seg> wv = weight == null ? List.of() : value(weight.segs());
        List<Seg> lv = length == null ? List.of() : value(length.segs());
        if (!wv.isEmpty() && !lv.isEmpty()) {
            List<Seg> merged = new ArrayList<>();
            merged.add(weight.segs().get(0)); // the window prefix
            merged.add(new Seg(" ", Style.EMPTY));
            merged.addAll(wv);
            merged.add(new Seg("/", wv.get(wv.size() - 1).style()));
            merged.addAll(lv);
            weight.set(merged);
            work.remove(length);
            return;
        }
        for (Line single : new Line[] {weight, length}) {
            List<Seg> v = single == null ? List.of() : value(single.segs());
            if (!v.isEmpty()) {
                int colon = colon(single.segs());
                List<Seg> out = new ArrayList<>(single.segs().subList(0, colon + 1));
                out.addAll(v);
                single.set(out);
            }
        }
    }

    /** The part after the label's colon without a bracketed metric value, or empty when there is no colon. */
    private static List<Seg> value(List<Seg> segs) {
        int colon = colon(segs);
        if (colon < 0) {
            return List.of();
        }
        List<Seg> out = new ArrayList<>();
        for (Seg s : segs.subList(colon + 1, segs.size())) {
            if (!s.text().strip().startsWith("(")) {
                out.add(s);
            }
        }
        return trimmed(out);
    }

    private static int colon(List<Seg> segs) {
        for (int i = 1; i < segs.size(); i++) {
            if (segs.get(i).text().contains(":")) {
                return i;
            }
        }
        return -1;
    }

    /** Without blank segments and trailing spaces at the end. */
    private static List<Seg> trimmed(List<Seg> segs) {
        List<Seg> out = new ArrayList<>(segs);
        while (!out.isEmpty() && out.get(out.size() - 1).text().isBlank()) {
            out.remove(out.size() - 1);
        }
        if (!out.isEmpty()) {
            Seg last = out.remove(out.size() - 1);
            out.add(new Seg(last.text().stripTrailing(), last.style()));
        }
        return out;
    }
}
