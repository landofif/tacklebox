package dev.landofif.tacklebox.trigger;

import java.util.List;
import java.util.function.Supplier;

/** Ready-made triggers for common FishOnMC chat lines, offered when creating a new trigger. */
public final class TriggerPresets {
    public record Preset(String name, String description, Supplier<Trigger> create) {}

    private TriggerPresets() {}

    public static List<Preset> all() {
        return List.of(
                new Preset("blank trigger", "start from scratch", TriggerPresets::blank),
                new Preset("quest completed", "quests > quest #1-8 has been completed!", TriggerPresets::questComplete),
                new Preset("pet drop", "pet drop! you pulled ... with a totem pop of the pets head", TriggerPresets::petDrop),
                new Preset("pet max level", "pets > your ... pet is now level ... → 100 with a totem pop of the pets head", TriggerPresets::petMaxLevel),
                new Preset("rare catch", "rare catch! you pulled ... (shards, bottles, capsules, amulets) with a totem pop of the item", TriggerPresets::rareCatch),
                new Preset("variant catch", "albino! / melanistic! / trophy! ... you caught a ...", TriggerPresets::variant),
                new Preset("new in compendium", "a catch of yours that was missing in the compendium (needs foecollab and one read of that species page)", TriggerPresets::compendium),
                new Preset("fabled event", "fabled > a fabled event is active at ...", TriggerPresets::fabled),
                new Preset("storm", "storm > a thunderstorm / supercell has formed!", TriggerPresets::storm),
                new Preset("moon rising", "full / blue / super / blood moon has risen", TriggerPresets::moon),
                new Preset("fish sold", "sell > you just earned $... from selling your fish!", TriggerPresets::sold),
                new Preset("rod not cast", "your rod in hand and no bobber out for a while (time in settings > bobber). it never casts for you", TriggerPresets::rodIdle)
        );
    }

    /** A new trigger for a preset; captured words keep their chat colours. */
    private static Trigger preset(String name) {
        Trigger t = new Trigger();
        t.name = name;
        t.title.chatColors = true;
        return t;
    }

    public static Trigger blank() {
        Trigger t = preset("my trigger");
        t.pattern = "";
        t.title.title = "&lhello!";
        t.title.subtitle = "";
        return t;
    }

    public static Trigger questComplete() {
        Trigger t = preset("quest completed");
        t.pattern = "quests > quest #{n} has been completed!";
        t.title.title = "&lquest{s} {n} complete";
        t.title.subtitle = "type /quests to claim it";
        t.title.titleColor = "#55ff55";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:ui.toast.challenge_complete";
        return t;
    }

    /** Anchored: other players' drops are broadcast as "! [366] name » PET DROP! ..." and must not fire. The rarity is a tag glyph (removed before matching), a word or a word in brackets ("[COMMON]", as the game log shows it), so it is optional here. */
    private static final String PET_DROP = "^pet drop! you (?:pulled|caught) (?:an? )?(?:\\[?(?<rarity>common|rare|epic|legendary|mythic|mythical)]? )?(?<pet>.+?) pet\\b";

    public static Trigger petDrop() {
        Trigger t = preset("pet drop");
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = PET_DROP;
        t.title.title = "&lpet drop!";
        t.title.titleColor = "#fd95f6";
        t.title.subtitle = "{pet.lower}";
        t.title.animation = Trigger.Animation.SHAKE;
        t.totem.enabled = true;
        t.totem.item = "pet:{pet}";
        t.sound.soundId = "minecraft:entity.player.levelup";
        t.sound.plays = 2;
        t.sound.secondsBetween = 0.3;
        return t;
    }

    /** "PETS » Your Bullfrog Pet is now level 99 → 100 (+1)", also jumps like "97 → 100 (+3)". 100 is the top: no log line goes past it. */
    private static final String PET_MAX = "^pets > your (?<pet>.+?) pet is now level \\d+ → 100\\b";

    public static Trigger petMaxLevel() {
        Trigger t = preset("pet max level");
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = PET_MAX;
        t.title.title = "&l{pet.lower} max level!";
        t.title.titleColor = "#55ff55";
        t.title.subtitle = "level 100";
        t.title.animation = Trigger.Animation.POP;
        t.totem.enabled = true;
        t.totem.item = "pet:{pet}";
        t.sound.soundId = "minecraft:block.bell.use";
        t.sound.pitch = 2.0;
        return t;
    }

    /**
     * Own rare catches only, anchored like {@link #PET_DROP}: "RARE CATCH! You pulled a 1x Subtropical Shard", "... pulled 1x Lightning in a Bottle".
     * The pop shows the item itself ({@code {item}} is its name, see display/TotemPop).
     */
    private static final String RARE_CATCH = "^rare catch! you (?:pulled|caught) (?:an? )?(?<n>\\d+)x (?<item>.+?)!?$";

    public static Trigger rareCatch() {
        Trigger t = preset("rare catch");
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = RARE_CATCH;
        t.title.title = "&lrare catch!";
        t.title.titleColor = "#ffaa00";
        t.title.subtitle = "{item.lower}";
        t.title.animation = Trigger.Animation.POP;
        t.totem.enabled = true;
        t.totem.item = "{item}";
        t.sound.soundId = "minecraft:block.amethyst_block.chime";
        t.sound.plays = 3;
        t.sound.secondsBetween = 0.15;
        return t;
    }

    /** Your own variant catches: "MELANISTIC! You caught a Grass Carp" (others read "name caught ..."). */
    public static Trigger variant() {
        Trigger t = preset("variant catch");
        t.mode = Trigger.MatchMode.REGEX;
        t.ignoreCase = false;
        t.pattern = "^(?<variant>[A-Z]{4,})! You caught a (?<fish>.+)$";
        t.title.title = "&l{variant.lower}!";
        t.title.titleColor = "#55ff55";
        t.title.subtitle = "{fish.lower}";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:entity.player.levelup";
        t.sound.pitch = 1.3;
        return t;
    }

    /** The line is made by the mod (menu/Compendium) when a catch fills a missing compendium entry, it is not a server line. */
    public static Trigger compendium() {
        Trigger t = preset("new in compendium");
        t.pattern = "compendium > new {variant} fish: {fish} at {place}";
        t.title.title = "&lnew in compendium!";
        t.title.titleColor = "#55ff55";
        t.title.subtitle = "{fish} ({variant})";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:entity.player.levelup";
        t.sound.pitch = 1.3;
        return t;
    }

    public static Trigger fabled() {
        Trigger t = preset("fabled event");
        t.pattern = "fabled > a fabled event is active at {place}";
        t.title.title = "&lfabled event";
        t.title.titleColor = "#f7453e";
        t.title.subtitle = "at {place.lower}";
        t.title.animation = Trigger.Animation.TYPEWRITER;
        t.title.staySeconds = 4;
        t.sound.soundId = "minecraft:block.bell.use";
        return t;
    }

    public static Trigger storm() {
        Trigger t = preset("storm");
        t.pattern = "storm > a {kind} has formed!";
        t.title.title = "&l⚡ {kind.lower}";
        t.title.titleColor = "#ffff55";
        t.title.subtitle = "bite speed is up";
        t.title.animation = Trigger.Animation.SHAKE;
        t.sound.soundId = "minecraft:entity.lightning_bolt.thunder";
        t.sound.volume = 0.6;
        return t;
    }

    public static Trigger moon() {
        Trigger t = preset("moon rising");
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = "(?<moon>full|blue|super|blood) moon > a .* has risen";
        t.title.title = "&l{moon.lower} moon";
        t.title.titleColor = "#60c2e9";
        t.title.subtitle = "has risen";
        t.title.animation = Trigger.Animation.SLIDE;
        t.sound.soundId = "minecraft:block.beacon.activate";
        return t;
    }

    public static Trigger sold() {
        Trigger t = preset("fish sold");
        t.pattern = "sell > you just earned {amount} from selling your fish!";
        t.title.title = "&l+{amount}";
        t.title.titleColor = "#55ff55";
        t.title.subtitle = "fish sold";
        t.title.scale = 2.5;
        t.title.staySeconds = 1.5;
        t.title.animation = Trigger.Animation.SLIDE;
        t.sound.soundId = "minecraft:entity.experience_orb.pickup";
        return t;
    }

    /** The line is made by the mod ({@code display/RodIdle}), not the server: the rod has been in hand with no bobber out for {@code rodIdleSeconds}. */
    public static Trigger rodIdle() {
        Trigger t = preset("rod not cast");
        t.pattern = "tacklebox > rod not cast for {seconds}s";
        t.title.title = "&lcast your rod";
        t.title.titleColor = "#ffaa00";
        t.title.subtitle = "not cast for {seconds}s";
        t.title.animation = Trigger.Animation.SHAKE;
        t.sound.soundId = "minecraft:block.note_block.bell";
        t.sound.plays = 2;
        t.sound.secondsBetween = 0.2;
        return t;
    }
}
