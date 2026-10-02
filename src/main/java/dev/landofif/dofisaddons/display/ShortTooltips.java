package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.util.Mono;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Trims fishonmc item tooltips (ported from fishutils). Each line carries the servers box border glyphs
 * next to the visible text, so lines are matched on their folded form ({@link Holograms#fold}) and edits
 * cut spans out run by run to keep the colours.
 *
 * <ul>
 *   <li>Caught fish (has both "fish group" and "ecosystem"): drops the latin name, the value line, the
 *   ecosystem line and the metric part of weight and length ({@code 0.03lb (0.01kg)} → {@code 0.03lb}).
 *   Spacer lines stay so the sections keep apart.</li>
 *   <li>Pets (has "pet level"): {@code +175/+175} → {@code +175} for maxed stats, and empty or default
 *   skin / item / trail slots are dropped, with the dividers they leave collapsed.</li>
 * </ul>
 */
public final class ShortTooltips {
    private static final Pattern LATIN_NAME = Pattern.compile("[a-z][a-z ]*");
    private static final Pattern PAREN_GROUP = Pattern.compile("\\s*[(（][^()（）]*[)）]");
    private static final Pattern STAT_PAIR = Pattern.compile("([+\\-]?\\d[\\d.,]*)\\s*/\\s*([+\\-]?\\d[\\d.,]*)");

    /** Set by /dofi debug tooltip: the next tooltip is printed line by line once. */
    public static volatile boolean dumpNext;

    private ShortTooltips() {}

    public static void register() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            boolean dumping = dumpNext;
            if (dumping) {
                dumpNext = false;
                dump(lines, "original");
            }
            DofiSettings s = StatsTracker.settings();
            if (s.shortFishTooltips) {
                shortenFish(lines);
            }
            if (s.shortPetTooltips) {
                shortenPet(lines);
            }
            if (dumping) {
                dump(lines, "shortened");
            }
        });
    }

    // ------------------------------------------------------------------ fish

    static boolean shortenFish(List<Text> lines) {
        if (indexOfLabel(lines, "fish group") < 0 || indexOfLabel(lines, "ecosystem") < 0) {
            return false;
        }
        String nameFolded = lines.isEmpty() ? "" : Holograms.fold(lines.get(0).getString()).trim();
        lines.remove(indexOfLabel(lines, "ecosystem"));

        int idx = indexOfLabel(lines, "value");
        if (idx >= 0) {
            lines.remove(idx);
            // between the item name and the value line only the bordered name copy and the latin name fold
            // to pure letters; skipping the copy keeps the line that carries the top border glyph
            for (int i = 1; i < idx; i++) {
                String folded = Holograms.fold(lines.get(i).getString()).trim();
                if (!folded.isEmpty() && !folded.equals(nameFolded) && LATIN_NAME.matcher(folded).matches()) {
                    lines.remove(i);
                    break;
                }
            }
        }
        for (int i = 0; i < lines.size(); i++) {
            String folded = Holograms.fold(lines.get(i).getString()).trim();
            if (folded.startsWith("weight") || folded.startsWith("length")) {
                lines.set(i, stripMetricParen(lines.get(i)));
            }
        }
        return true;
    }

    private static Text stripMetricParen(Text line) {
        Matcher m = PAREN_GROUP.matcher(flatten(line));
        while (m.find()) {
            String inside = Holograms.fold(m.group()).trim();
            if (inside.endsWith("kg") || inside.endsWith("cm")) {
                return cutSpan(line, m.start(), m.end());
            }
        }
        return line;
    }

    // ------------------------------------------------------------------ pets

    static boolean shortenPet(List<Text> lines) {
        if (indexOfLabel(lines, "pet level") < 0) {
            return false;
        }
        for (int i = 0; i < lines.size(); i++) {
            lines.set(i, collapseMaxedPair(lines.get(i)));
        }
        lines.removeIf(ShortTooltips::isEmptySlotLine);
        for (int i = lines.size() - 1; i > 0; i--) {
            if (isBlank(lines.get(i)) && isBlank(lines.get(i - 1))) {
                lines.remove(i);
            }
        }
        return true;
    }

    private static Text collapseMaxedPair(Text line) {
        Matcher m = STAT_PAIR.matcher(flatten(line));
        while (m.find()) {
            if (m.group(1).equals(m.group(2))) {
                return cutSpan(line, m.end(1), m.end(2));
            }
        }
        return line;
    }

    private static boolean isEmptySlotLine(Text line) {
        String folded = Holograms.fold(line.getString()).trim();
        boolean slot = folded.startsWith("equipped skin") || folded.startsWith("equipped item") || folded.startsWith("trail item");
        return slot && (folded.endsWith("empty") || folded.endsWith("default"));
    }

    // ------------------------------------------------------------------ helpers

    /** A line with no visible text (spacers still carry border glyphs, which fold to spaces). */
    private static boolean isBlank(Text line) {
        return Holograms.fold(line.getString()).isBlank();
    }

    private static int indexOfLabel(List<Text> lines, String label) {
        for (int i = 0; i < lines.size(); i++) {
            if (Holograms.fold(lines.get(i).getString()).trim().startsWith(label)) {
                return i;
            }
        }
        return -1;
    }

    private static String flatten(Text line) {
        StringBuilder sb = new StringBuilder();
        line.visit((style, part) -> {
            sb.append(part);
            return Optional.empty();
        }, Style.EMPTY);
        return sb.toString();
    }

    /** The line without the characters in [start, end), every other run keeping its style. */
    private static Text cutSpan(Text line, int start, int end) {
        List<String> runs = new ArrayList<>();
        List<Style> styles = new ArrayList<>();
        line.visit((style, part) -> {
            runs.add(part);
            styles.add(style);
            return Optional.empty();
        }, Style.EMPTY);
        MutableText out = Text.empty();
        int offset = 0;
        for (int i = 0; i < runs.size(); i++) {
            String run = runs.get(i);
            StringBuilder kept = new StringBuilder(run.length());
            for (int j = 0; j < run.length(); j++) {
                int pos = offset + j;
                if (pos < start || pos >= end) {
                    kept.append(run.charAt(j));
                }
            }
            offset += run.length();
            if (!kept.isEmpty()) {
                out.append(Text.literal(kept.toString()).setStyle(styles.get(i)));
            }
        }
        return out;
    }

    private static void dump(List<Text> lines, String label) {
        MinecraftClient client = MinecraftClient.getInstance();
        for (int i = 0; i < lines.size(); i++) {
            String raw = lines.get(i).getString();
            String line = String.format(Locale.ROOT, "[tooltip %s] %02d text=\"%s\" folded=\"%s\"", label, i, escape(raw), Holograms.fold(raw));
            DofisAddons.LOGGER.info(line);
            if (client.player != null) {
                client.player.sendMessage(Mono.text(line).formatted(Formatting.GRAY), false);
            }
        }
    }

    /** Non-ascii characters as \\uXXXX so pack glyphs can be told apart. */
    public static String escape(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            sb.append(c < 0x20 || c > 0x7E ? String.format(Locale.ROOT, "\\u%04X", (int) c) : String.valueOf(c));
        }
        return sb.toString();
    }
}
