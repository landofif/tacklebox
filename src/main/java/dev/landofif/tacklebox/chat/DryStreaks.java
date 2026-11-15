package dev.landofif.tacklebox.chat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Dry streak chat messages (luna, 2026-11-15): when a rare drop finally lands, a chat line says how many catches it took,
 * the catch that brought it included. Pure, so the tests run it: {@link DryStreakChat} feeds it the catches and the chat.
 * The idea comes from FOE+ (itshaxr/FOE-Plus), which builds on FOE-R (DannyPX) and FOECollab (Kimberlyow); no code was copied.
 * <p>
 * Pets, shards, infusion capsules and lightning bottles are announced by the server's own {@code PET DROP!} and
 * {@code RARE CATCH!} lines; variants and gigantic fish are read from the caught fish. Capsules only count catches in a
 * blood moon and bottles only catches in a storm, since that is when they drop.
 */
public final class DryStreaks {
    /** A kind of drop with its own switch and text. */
    public record Kind(String key, String label, String desc, String template) {}

    public static final String PET = "pet", VARIANT = "variant", GIGANTIC = "gigantic", SHARD = "shard", CAPSULE = "capsule", BOTTLE = "bottle";

    public static final List<Kind> KINDS = List.of(
            new Kind(PET, "pet", "a pet you fished up", "&atacklebox &8» &d%item% &7after &f%streak% &7catches"),
            new Kind(VARIANT, "variant", "an albino, melanistic, trophy or fabled fish", "&atacklebox &8» &e%variant% &f%item% &7after &f%streak% &7catches"),
            new Kind(GIGANTIC, "gigantic", "a gigantic fish", "&atacklebox &8» &6gigantic &f%item% &7after &f%streak% &7catches"),
            new Kind(SHARD, "shard", "a shard of any climate", "&atacklebox &8» &b%count%x %item% &7after &f%streak% &7catches"),
            new Kind(CAPSULE, "capsule", "an infusion capsule", "&atacklebox &8» &c%item% &7after &f%streak% &7blood moon catches"),
            new Kind(BOTTLE, "bottle", "a lightning in a bottle", "&atacklebox &8» &e%item% &7after &f%streak% &7storm catches"));

    /** The variants with a streak of their own, counted under "variant:albino" and so on. */
    public static final List<String> VARIANTS = List.of("albino", "melanistic", "trophy", "fabled");
    /** The kinds the server announces in chat; the others are seen on the caught fish. */
    private static final List<String> LINE_KINDS = List.of(PET, SHARD, CAPSULE, BOTTLE);

    /** A drop line and the catch it belongs to come this close together, in either order. */
    static final long NEAR = 3000;

    // own lines only: a player quoting one has "name >" in front
    private static final Pattern PET_LINE = Pattern.compile(
            "^pet drop! you (?:pulled|caught) (?:an? )?(?:\\[?(common|rare|epic|legendary|mythic|mythical)]? )?(.+? pet)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern RARE_LINE = Pattern.compile(
            "^rare catch! you (?:pulled|caught) (?:an? )?(?:(\\d+)x )?(.+?)!?(?: \\(x\\d+\\))?$", Pattern.CASE_INSENSITIVE);

    private static long lastCatchAt;
    /** Per kind: the next catch before this time brought a drop that was already counted. */
    private static final Map<String, Long> skipUntil = new HashMap<>();

    private DryStreaks() {}

    /** A drop the server announced: its kind, the item as the line names it, how many, and a pet's rarity ("" otherwise). */
    public record Drop(String kind, String item, int count, String rarity) {}

    /** The drop a normalized chat line announces, or null (other lines, other players' drops, rare catches without a streak). */
    public static Drop parse(String line) {
        Matcher m = PET_LINE.matcher(line);
        if (m.find()) {
            return new Drop(PET, m.group(2), 1, m.group(1) == null ? "" : m.group(1).toLowerCase(Locale.ROOT));
        }
        m = RARE_LINE.matcher(line);
        if (!m.find()) {
            return null;
        }
        String item = m.group(2), l = item.toLowerCase(Locale.ROOT);
        String kind = l.contains("shard") ? SHARD : l.contains("infusion capsule") ? CAPSULE : l.contains("lightning in a bottle") ? BOTTLE : null;
        return kind == null ? null : new Drop(kind, item, m.group(1) == null ? 1 : Integer.parseInt(m.group(1)), "");
    }

    /**
     * An own catch: counts it for every kind and returns the chat lines for a variant or gigantic fish (kinds in {@code on} only).
     *
     * @param dry  the saved counts, changed here
     * @param text the edited texts by kind; a kind without one uses its default
     * @param name the fish's name for the line
     */
    public static List<String> onCatch(Map<String, Integer> dry, Set<String> on, Map<String, String> text, String name, String variant,
                                       String size, boolean bloodMoon, boolean storm, long now) {
        for (String kind : LINE_KINDS) {
            Long skip = skipUntil.remove(kind);
            if (skip != null && now <= skip) {
                continue; // its drop line came first and counted this catch already
            }
            if (kind.equals(CAPSULE) && !bloodMoon || kind.equals(BOTTLE) && !storm) {
                continue;
            }
            dry.merge(kind, 1, Integer::sum);
        }
        lastCatchAt = now;
        List<String> out = new ArrayList<>();
        String v = variant == null ? "" : variant.toLowerCase(Locale.ROOT);
        for (String each : VARIANTS) {
            int n = dry.merge(VARIANT + ":" + each, 1, Integer::sum);
            if (each.equals(v)) {
                dry.put(VARIANT + ":" + each, 0);
                if (on.contains(VARIANT)) {
                    out.add(fill(template(text, VARIANT), name, n, 1, each, ""));
                }
            }
        }
        int n = dry.merge(GIGANTIC, 1, Integer::sum);
        if ("gigantic".equalsIgnoreCase(size)) {
            dry.put(GIGANTIC, 0);
            if (on.contains(GIGANTIC)) {
                out.add(fill(template(text, GIGANTIC), name, n, 1, "", ""));
            }
        }
        return out;
    }

    /** A drop line: ends its streak and returns the chat line, or null when that kind is switched off. */
    public static String onDrop(Map<String, Integer> dry, Set<String> on, Map<String, String> text, Drop d, long now) {
        int n = dry.getOrDefault(d.kind(), 0);
        if (now - lastCatchAt > NEAR) { // the catch that brought it is not counted yet: count it here and not again when it comes
            n++;
            skipUntil.put(d.kind(), now + NEAR);
        }
        dry.put(d.kind(), 0);
        return on.contains(d.kind()) ? fill(template(text, d.kind()), d.item(), Math.max(1, n), d.count(), "", d.rarity()) : null;
    }

    /** The text of a kind: the edited one, or the default when there is none or it is blank. */
    public static String template(Map<String, String> text, String kind) {
        String t = text.get(kind);
        if (t != null && !t.isBlank()) {
            return t;
        }
        for (Kind k : KINDS) {
            if (k.key().equals(kind)) {
                return k.template();
            }
        }
        return "";
    }

    /** The text with its placeholders filled: %item%, %streak% (1,234), %count%, %variant%, %rarity%. */
    public static String fill(String template, String item, int streak, int count, String variant, String rarity) {
        return template.replace("%item%", item).replace("%streak%", String.format(Locale.ROOT, "%,d", streak))
                .replace("%count%", Integer.toString(count)).replace("%variant%", variant).replace("%rarity%", rarity);
    }

    /** For tests and on leaving the server: forget the last catch and drop times. */
    public static void reset() {
        lastCatchAt = 0;
        skipUntil.clear();
    }
}
