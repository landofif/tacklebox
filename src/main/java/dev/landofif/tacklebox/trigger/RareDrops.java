package dev.landofif.tacklebox.trigger;

import dev.landofif.tacklebox.TackleboxSettings;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Your own rare drops (pet drop, rare catch lines) as two built-in triggers that are not in /triggers. Which part of each drop
 * shows (title, totem pop, sound) is a setting per kind of drop ({@code dropTitleOff}, {@code dropPopOff}, {@code dropSoundOff}),
 * so the engine fires a copy of the built-in with those parts switched off.
 */
public final class RareDrops {
    /** A kind of drop with its own switches in the settings. */
    public record Row(String key, String label, String desc) {}

    public static final String PET = "pet", SHARD = "shard", BOTTLE = "bottle", CAPSULE = "capsule",
            AMULET = "amulet", BIGFOOT = "bigfoot", OTHER = "other";

    public static final List<Row> ROWS = List.of(
            new Row(PET, "pet drop", "a pet drop."),
            new Row(SHARD, "shards", "a shard of any climate."),
            new Row(BOTTLE, "lightning in a bottle", "a lightning in a bottle."),
            new Row(CAPSULE, "infusion capsule", "an infusion capsule."),
            new Row(AMULET, "prospecting amulet", "a prospecting amulet."),
            new Row(BIGFOOT, "bigfoot items", "bigfoot fur, bigfoot tooth and the other bigfoot gifts."),
            new Row(OTHER, "other rare catch", "any other rare catch line."));

    private static final Trigger PET_DROP = builtIn(TriggerPresets.petDrop(), "builtin:pet-drop");
    private static final Trigger RARE_CATCH = builtIn(TriggerPresets.rareCatch(), "builtin:rare-catch");

    private RareDrops() {}

    private static Trigger builtIn(Trigger t, String id) {
        t.id = id;
        return t;
    }

    /** The built-in triggers while "rare drops" is on. */
    public static List<Trigger> triggers() {
        return TackleboxSettings.get().rareDrops ? List.of(PET_DROP, RARE_CATCH) : List.of();
    }

    public static boolean isBuiltIn(Trigger t) {
        return t == PET_DROP || t == RARE_CATCH;
    }

    /** The kind of drop a line was: the pet drop trigger is a pet, a rare catch is told apart by its item. */
    public static String row(Trigger builtIn, Matcher.Result r) {
        if (builtIn == PET_DROP) {
            return PET;
        }
        String item = r.values().getOrDefault("item", "").toLowerCase(Locale.ROOT);
        if (item.contains("shard")) return SHARD;
        if (item.contains("lightning in a bottle")) return BOTTLE;
        if (item.contains("infusion capsule")) return CAPSULE;
        if (item.contains("prospecting amulet")) return AMULET;
        if (item.contains("bigfoot")) return BIGFOOT;
        return OTHER;
    }

    /** A copy of the built-in with this drop's switched off parts off and the count in the title when more than one, or null when the drop shows nothing at all. */
    public static Trigger apply(Trigger builtIn, Matcher.Result r) {
        TackleboxSettings s = TackleboxSettings.get();
        String row = row(builtIn, r);
        boolean title = !s.dropTitleOff.contains(row);
        boolean pop = !s.dropPopOff.contains(row);
        boolean sound = !s.dropSoundOff.contains(row);
        if (!title && !pop && !sound) {
            return null;
        }
        Trigger t = builtIn.copyKeepId();
        t.title.enabled = title;
        t.totem.enabled = pop;
        t.sound.enabled = sound;
        String n = r.values().get("n");
        if (n != null && !n.equals("1")) {
            t.title.title = "&l+" + n + " {item.lower}"; // "+3 arctic shard"; one is just "+ arctic shard"
        }
        return t;
    }

    /**
     * The old pet drop and rare catch triggers made from the presets would fire a second time next to the built-ins, so they are
     * switched off once (not deleted). Triggers whose pattern was edited are left alone.
     *
     * @return true when this was the one time, so the caller saves the triggers and the settings
     */
    public static boolean migrate() {
        TackleboxSettings s = TackleboxSettings.get();
        if (s.dropsMigrated) {
            return false;
        }
        s.dropsMigrated = true;
        List<String> off = new ArrayList<>();
        for (Trigger t : TriggerStore.all()) {
            if (t.enabled && t.mode == Trigger.MatchMode.REGEX
                    && (TriggerPresets.PET_DROP.equals(t.pattern) || TriggerPresets.RARE_CATCH.equals(t.pattern))) {
                t.enabled = false;
                off.add(t.name);
            }
        }
        if (!off.isEmpty()) {
            dev.landofif.tacklebox.Tacklebox.LOGGER.info("Switched off {} trigger(s) now built in under settings > rare drops", off);
        }
        return true;
    }
}
