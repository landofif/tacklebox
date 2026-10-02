package dev.landofif.dofisaddons.trigger;

import dev.landofif.dofisaddons.display.SoundQueue;
import dev.landofif.dofisaddons.display.TitleOverlay;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.text.Text;

import java.util.HashMap;
import java.util.Map;

/**
 * Runs every incoming chat line through the enabled triggers. Purely reactive: it shows titles
 * and plays sounds, and never sends anything to the server.
 */
public final class TriggerEngine {
    private static final Map<String, Matcher> MATCHERS = new HashMap<>();
    private static final Map<String, Long> LAST_FIRED = new HashMap<>();
    private static final Map<String, Integer> FIRE_COUNTS = new HashMap<>();

    private TriggerEngine() {}

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
        long now = System.currentTimeMillis();

        for (Trigger trigger : TriggerStore.all()) {
            if (!trigger.enabled || (trigger.fishOnMcOnly && !onFishOnMc)) {
                continue;
            }
            Matcher.Result result = matcher(trigger).match(raw);
            if (result == null) {
                continue;
            }
            long last = LAST_FIRED.getOrDefault(trigger.id, 0L);
            if (now - last < (long) (trigger.cooldownSeconds * 1000)) {
                continue;
            }
            LAST_FIRED.put(trigger.id, now);
            int count = FIRE_COUNTS.merge(trigger.id, 1, Integer::sum);
            fire(trigger, result.values(), count);
            if (trigger.hideMessage) {
                show = false;
            }
        }
        return show;
    }

    /** Shows the title and plays the sound (also used by the editor's Preview button). */
    public static void fire(Trigger trigger, Map<String, String> values, int count) {
        Map<String, String> all = new HashMap<>(values);
        MinecraftClient client = MinecraftClient.getInstance();
        all.putIfAbsent("player", client.getSession() != null ? client.getSession().getUsername() : "");
        all.putIfAbsent("count", String.valueOf(count));
        if (trigger.title.enabled) {
            TitleOverlay.show(trigger.title, fill(trigger.title.title, all), fill(trigger.title.subtitle, all));
        }
        if (trigger.sound.enabled) {
            SoundQueue.play(trigger.sound);
        }
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
                                value = e.getValue();
                                break;
                            }
                        }
                    }
                    if (value != null) {
                        out.append(lower ? value.toLowerCase(java.util.Locale.ROOT) : value);
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

    public static boolean isOnFishOnMc() {
        ServerInfo info = MinecraftClient.getInstance().getCurrentServerEntry();
        return info != null && info.address != null && info.address.toLowerCase().contains("fishonmc");
    }
}
