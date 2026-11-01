package dev.landofif.tacklebox.trigger;

import dev.landofif.tacklebox.display.SoundQueue;
import dev.landofif.tacklebox.display.TitleOverlay;
import dev.landofif.tacklebox.display.TotemPop;
import dev.landofif.tacklebox.util.RichText;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * Runs every incoming chat line through the enabled triggers. Purely reactive: it shows titles
 * and plays sounds, and never sends anything to the server.
 */
public final class TriggerEngine {
    private static final Map<String, Matcher> MATCHERS = new HashMap<>();
    private static final Map<String, Long> LAST_FIRED = new HashMap<>();
    private static final Map<String, Integer> FIRE_COUNTS = new HashMap<>();

    /** More lines of a trigger this soon after the one that fired it (two quests finished by one catch) join its title. */
    private static final long MERGE_MS = 250;
    private static final Map<String, Merge> MERGING = new HashMap<>();
    static LongSupplier clock = System::currentTimeMillis;
    /** Plays a trigger's sound; tests count the plays. */
    static Consumer<Trigger.SoundSettings> playSound = SoundQueue::play;

    /** A trigger that just fired: the values of its lines so far, their chat colours, and the title they made. */
    private static final class Merge {
        final Trigger trigger;
        final long until;
        final int count;
        final Map<String, List<String>> values = new LinkedHashMap<>();
        final Map<String, List<int[]>> colors = new HashMap<>();
        int lines;
        TitleOverlay.Shown title;

        Merge(Trigger trigger, long until, int count) {
            this.trigger = trigger;
            this.until = until;
            this.count = count;
        }

        /** {@code lineColors}: the colour of each character of the normalized line, or null. */
        void add(Matcher.Result r, int[] lineColors) {
            lines++;
            r.values().forEach((k, v) -> {
                List<String> l = values.computeIfAbsent(k, y -> new ArrayList<>());
                if (!l.contains(v)) {
                    l.add(v);
                    int[] span = r.spans().get(k);
                    colors.computeIfAbsent(k, y -> new ArrayList<>())
                            .add(lineColors == null || span == null ? null : Arrays.copyOfRange(lineColors, span[0], span[1]));
                }
            });
        }

        Map<String, String> joined() {
            Map<String, String> joined = new HashMap<>();
            values.forEach((k, l) -> joined.put(k, join(l)));
            joined.put("matches", String.valueOf(lines));
            joined.put("s", lines > 1 ? "s" : "");
            return withDefaults(joined, count);
        }

        Map<String, int[]> joinedColors() {
            Map<String, int[]> joined = new HashMap<>();
            values.forEach((k, l) -> joined.put(k, joinColors(l, colors.get(k))));
            return joined;
        }

        /** A later line of the same trigger: the title on screen shows the joined values, its timer goes on. */
        void update() {
            if (title != null) {
                Map<String, String> v = joined();
                Map<String, int[]> c = joinedColors();
                TitleOverlay.update(title, fill(trigger.title.title, v, c), fill(trigger.title.subtitle, v, c));
            }
        }
    }

    private TriggerEngine() {}

    /** Ends the merge windows that have passed. Called every client tick. */
    public static void tick() {
        if (!MERGING.isEmpty()) {
            long now = clock.getAsLong();
            MERGING.values().removeIf(m -> now >= m.until);
        }
    }

    /** "1", "1 & 2", "1, 2 & 3". */
    private static String join(List<String> l) {
        return l.size() < 2 ? String.join("", l)
                : String.join(", ", l.subList(0, l.size() - 1)) + " & " + l.get(l.size() - 1);
    }

    /** The colours of {@link #join}'s text: each value's own, none (-1) for the commas and "&". */
    private static int[] joinColors(List<String> l, List<int[]> colors) {
        int[] out = new int[join(l).length()];
        Arrays.fill(out, -1);
        int at = 0;
        for (int i = 0; i < l.size(); i++) {
            if (i > 0) {
                at += i == l.size() - 1 ? " & ".length() : ", ".length();
            }
            int[] c = colors.get(i);
            if (c != null) {
                System.arraycopy(c, 0, out, at, c.length);
            }
            at += l.get(i).length();
        }
        return out;
    }

    /** The chat colour (0xRRGGBB, -1 for none) of each character of the line as {@link Matcher#normalize} gives it. */
    static int[] lineColors(Text line) {
        StringBuilder plain = new StringBuilder();
        List<TextColor> colors = new ArrayList<>();
        line.visit((style, s) -> {
            plain.append(s);
            for (int i = 0; i < s.length(); i++) {
                colors.add(style.getColor());
            }
            return Optional.empty();
        }, Style.EMPTY);
        int[] from = new int[plain.length()];
        String normalized = Matcher.normalize(plain.toString(), from);
        int[] out = new int[normalized.length()];
        for (int i = 0; i < out.length; i++) {
            TextColor c = colors.get(from[i]);
            out[i] = c == null ? -1 : c.getRgb();
        }
        return out;
    }

    /** Tests: forget cooldowns and lines being merged, so a test's own clock is not blocked by another test's fires. */
    static void forgetFires() {
        LAST_FIRED.clear();
        MERGING.clear();
    }

    /** Patterns changed: recompile lazily on the next message. */
    public static void invalidate() {
        MATCHERS.clear();
    }

    /** @return false to hide the message from chat */
    public static boolean onGameMessage(Text message, boolean overlay) {
        if (overlay) {
            return true; // action bar: FishOnMC uses it for the minigame, not for announcements
        }
        String raw = message.getString();
        boolean onFishOnMc = isOnFishOnMc();
        boolean show = true;
        long now = clock.getAsLong();
        int[] colors = null;

        List<Trigger> all = new ArrayList<>(RareDrops.triggers());
        all.addAll(TriggerStore.all());
        for (Trigger trigger : all) {
            if (!trigger.enabled || (trigger.fishOnMcOnly && !onFishOnMc)) {
                continue;
            }
            Matcher.Result result = matcher(trigger).match(raw);
            if (result == null) {
                continue;
            }
            if (RareDrops.isBuiltIn(trigger)) {
                trigger = RareDrops.apply(trigger, result);
                if (trigger == null) {
                    continue; // every part of this drop is switched off
                }
            }
            if (trigger.title.chatColors && colors == null) {
                colors = lineColors(message);
            }
            Merge merge = MERGING.get(trigger.id);
            if (merge != null && now < merge.until) {
                merge.add(result, trigger.title.chatColors ? colors : null);
                merge.update();
            } else {
                long last = LAST_FIRED.getOrDefault(trigger.id, 0L);
                if (now - last < (long) (trigger.cooldownSeconds * 1000)) {
                    continue;
                }
                LAST_FIRED.put(trigger.id, now);
                merge = new Merge(trigger, now + MERGE_MS, FIRE_COUNTS.merge(trigger.id, 1, Integer::sum));
                merge.add(result, trigger.title.chatColors ? colors : null);
                MERGING.put(trigger.id, merge);
                merge.title = fire(trigger, merge.joined(), merge.joinedColors());
            }
            if (trigger.hideMessage) {
                show = false;
            }
        }
        return show;
    }

    /** Shows the title, pops the totem and plays the sound, all at once. @return the title on screen, or null */
    private static TitleOverlay.Shown fire(Trigger trigger, Map<String, String> values, Map<String, int[]> colors) {
        TitleOverlay.Shown title = null;
        if (trigger.title.enabled) {
            title = TitleOverlay.show(trigger.id, trigger.title, fill(trigger.title.title, values, colors), fill(trigger.title.subtitle, values, colors));
        }
        if (trigger.totem.enabled) {
            TotemPop.show(fill(trigger.totem.item, values), trigger.totem.scale);
        }
        if (trigger.sound.enabled) {
            playSound.accept(trigger.sound);
        }
        return title;
    }

    private static Map<String, String> withDefaults(Map<String, String> values, int count) {
        MinecraftClient client = MinecraftClient.getInstance();
        values.putIfAbsent("player", client != null && client.getSession() != null ? client.getSession().getUsername() : "");
        values.putIfAbsent("count", String.valueOf(count));
        values.putIfAbsent("s", "");
        values.putIfAbsent("matches", "1");
        return values;
    }

    public static int count(Trigger trigger) {
        return FIRE_COUNTS.getOrDefault(trigger.id, 0);
    }

    public static Matcher matcher(Trigger trigger) {
        return MATCHERS.computeIfAbsent(trigger.id + '\u0000' + trigger.mode + '\u0000' + trigger.ignoreCase + '\u0000' + trigger.pattern,
                k -> Matcher.of(trigger));
    }

    /** Replaces {name} with captured values; unknown placeholders are left as typed. */
    public static String fill(String template, Map<String, String> values) {
        return fill(template, values, null);
    }

    /**
     * {@link #fill(String, Map)}, with a value that has {@code colors} (one per character, -1 for none) written in those
     * colours and the colour and formatting from before it (title colour, {@code &l}) back after it.
     */
    static String fill(String template, Map<String, String> values, Map<String, int[]> colors) {
        if (template == null || template.indexOf('{') < 0) {
            return template == null ? "" : template;
        }
        StringBuilder out = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char c = template.charAt(i);
            if (c == '{') {
                int close = template.indexOf('}', i);
                if (close > i) {
                    String key = template.substring(i + 1, close);
                    // {name.lower}: the captured text in lowercase (server lines are often in caps)
                    boolean lower = key.toLowerCase(java.util.Locale.ROOT).endsWith(".lower");
                    if (lower) {
                        key = key.substring(0, key.length() - ".lower".length());
                    }
                    String value = values.get(key);
                    if (value == null) {
                        // case-insensitive fallback, so {N} works for {n}
                        for (Map.Entry<String, String> e : values.entrySet()) {
                            if (e.getKey().equalsIgnoreCase(key)) {
                                key = e.getKey();
                                value = e.getValue();
                                break;
                            }
                        }
                    }
                    if (value != null) {
                        value = lower ? value.toLowerCase(java.util.Locale.ROOT) : value;
                        int[] valueColors = colors == null ? null : colors.get(key);
                        if (valueColors != null && valueColors.length == value.length()) {
                            appendColored(out, value, valueColors);
                        } else {
                            out.append(value);
                        }
                        i = close + 1;
                        continue;
                    }
                }
            }
            out.append(c);
            i++;
        }
        return out.toString();
    }

    private static void appendColored(StringBuilder out, String value, int[] colors) {
        RichText.Codes before = RichText.codesAtEnd(out.toString());
        int current = -1; // -1: the style from before the value
        for (int i = 0; i < value.length(); i++) {
            if (colors[i] != current) {
                current = colors[i];
                out.append(current < 0 ? before.color() : String.format(java.util.Locale.ROOT, "&#%06x", current)).append(before.formats());
            }
            char ch = value.charAt(i);
            out.append(ch == '&' ? "&&" : String.valueOf(ch));
        }
        if (current >= 0) {
            out.append(before.color()).append(before.formats());
        }
    }

    /** Tests have no server to be on: this says they are on FishOnMC. */
    public static boolean assumeFishOnMc;

    public static boolean isOnFishOnMc() {
        if (assumeFishOnMc) {
            return true;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return false; // tests
        }
        ServerInfo info = client.getCurrentServerEntry();
        return info != null && info.address != null && info.address.toLowerCase().contains("fishonmc");
    }
}
