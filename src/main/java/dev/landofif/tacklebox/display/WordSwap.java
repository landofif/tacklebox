package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;

import java.util.ArrayList;
import java.util.List;

/**
 * Word swaps (skyhannis /shwords): every piece of text the game draws or measures has each enabled phrase
 * replaced by its word, keeping the style of the text it replaces. Only what you see changes.
 */
public final class WordSwap {
    public static class Entry {
        public String from = "";
        public String to = "";
        public boolean enabled = true;
        public boolean caseSensitive = false;
        /** RGB the replacement is drawn in, or -1 to keep the colour of the text it replaces. */
        public int color = -1;
    }

    private record Ch(int cp, Style style) {}

    /** What {@link #swap(OrderedText)} returns, so text that was already swapped is left alone. */
    private record Swapped(List<Ch> chars) implements OrderedText {
        @Override
        public boolean accept(net.minecraft.text.CharacterVisitor visitor) {
            int i = 0;
            for (Ch c : chars) {
                if (!visitor.accept(i++, c.style, c.cp)) {
                    return false;
                }
            }
            return true;
        }
    }

    private WordSwap() {}

    private static List<Entry> active() {
        TackleboxSettings s = TackleboxSettings.get();
        // the editor shows what you type, not its own swapped text
        return s.wordSwap && !(net.minecraft.client.MinecraftClient.getInstance().currentScreen instanceof dev.landofif.tacklebox.screen.WordSwapScreen) ? s.words : List.of();
    }

    public static String swap(String text) {
        for (Entry e : active()) {
            if (e.enabled && !e.from.isEmpty()) {
                text = e.caseSensitive ? text.replace(e.from, e.to)
                        : java.util.regex.Pattern.compile(java.util.regex.Pattern.quote(e.from), java.util.regex.Pattern.CASE_INSENSITIVE)
                        .matcher(text).replaceAll(java.util.regex.Matcher.quoteReplacement(e.to));
            }
        }
        return text;
    }

    public static OrderedText swap(OrderedText text) {
        List<Entry> entries = active();
        if (entries.isEmpty() || text == null || text instanceof Swapped) {
            return text;
        }
        List<Ch> scan = new ArrayList<>();
        StringBuilder plain = new StringBuilder();
        text.accept((i, style, cp) -> {
            scan.add(new Ch(cp, style));
            plain.appendCodePoint(cp);
            return true;
        });
        List<Ch> chars = scan;
        String flat = plain.toString().toLowerCase();
        boolean any = false;
        for (Entry e : entries) {
            if (e.enabled && !e.from.isEmpty() && flat.contains(e.from.toLowerCase())) {
                any = true;
                break;
            }
        }
        if (!any) {
            return text; // the usual case: nothing to do, no allocation beyond the scan
        }
        for (Entry e : entries) {
            if (e.enabled && !e.from.isEmpty()) {
                chars = replace(chars, e);
            }
        }
        return new Swapped(chars);
    }

    private static List<Ch> replace(List<Ch> in, Entry e) {
        int[] from = e.from.codePoints().toArray();
        int[] to = e.to.codePoints().toArray();
        List<Ch> out = new ArrayList<>(in.size());
        for (int i = 0; i < in.size(); ) {
            if (matches(in, i, from, e.caseSensitive)) {
                for (int cp : to) {
                    Style st = in.get(i).style;
                    out.add(new Ch(cp, e.color < 0 ? st : st.withColor(e.color & 0xFFFFFF)));
                }
                i += from.length;
            } else {
                out.add(in.get(i++));
            }
        }
        return out;
    }

    private static boolean matches(List<Ch> in, int at, int[] from, boolean caseSensitive) {
        if (at + from.length > in.size()) {
            return false;
        }
        for (int k = 0; k < from.length; k++) {
            int a = in.get(at + k).cp;
            int b = from[k];
            if (a != b && (caseSensitive || Character.toLowerCase(a) != Character.toLowerCase(b))) {
                return false;
            }
        }
        return true;
    }
}
