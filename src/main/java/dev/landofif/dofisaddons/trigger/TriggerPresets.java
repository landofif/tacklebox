package dev.landofif.dofisaddons.trigger;

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
                new Preset("pet drop", "pet drop! you pulled ...", TriggerPresets::petDrop),
                new Preset("pet totem", "pet drop! ... as a totem pop showing the pets head", TriggerPresets::petTotem),
                new Preset("rare catch", "rare catch! you pulled ... (shards, bottles, capsules)", TriggerPresets::rareCatch),
                new Preset("climate shard", "rare catch! you pulled a 1x subarctic shard (one per climate)", TriggerPresets::climateShard),
                new Preset("shard totem", "rare catch! ... climate shard as a totem pop showing the shard", TriggerPresets::shardTotem),
                new Preset("variant catch", "albino! / melanistic! / trophy! ... you caught a ...", TriggerPresets::variant),
                new Preset("fabled event", "fabled > a fabled event is active at ...", TriggerPresets::fabled),
                new Preset("storm", "storm > a thunderstorm / supercell has formed!", TriggerPresets::storm),
                new Preset("moon rising", "full / blue / super / blood moon has risen", TriggerPresets::moon),
                new Preset("fish sold", "sell > you just earned $... from selling your fish!", TriggerPresets::sold)
        );
    }

    public static Trigger blank() {
        Trigger t = new Trigger();
        t.name = "my trigger";
        t.pattern = "";
        t.title.title = "&lhello!";
        t.title.subtitle = "";
        return t;
    }

    public static Trigger questComplete() {
        Trigger t = new Trigger();
        t.name = "quest completed";
        t.pattern = "quests > quest #{n} has been completed!";
        t.title.title = "&lquest{s} {n} complete";
        t.title.subtitle = "type /quests to claim it";
        t.title.titleColor = "#55ff55";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:ui.toast.challenge_complete";
        return t;
    }

    /** Anchored: other players' drops are broadcast as "! [366] name » PET DROP! ..." and must not fire. The rarity is a tag glyph (removed before matching) or a word, so it is optional here. */
    private static final String PET_DROP = "^pet drop! you (?:pulled|caught) (?:an? )?(?:(?<rarity>common|rare|epic|legendary|mythic|mythical) )?(?<pet>.+?) pet\\b";

    public static Trigger petTotem() {
        Trigger t = new Trigger();
        t.name = "pet totem";
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = PET_DROP;
        t.title.enabled = false;
        t.totem.enabled = true;
        t.totem.item = "pet:{pet}";
        t.sound.soundId = "minecraft:item.totem.use";
        return t;
    }

    public static Trigger petDrop() {
        Trigger t = new Trigger();
        t.name = "pet drop";
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = PET_DROP;
        t.title.title = "&lpet drop!";
        t.title.titleColor = "#fd95f6";
        t.title.subtitle = "{pet.lower}";
        t.title.animation = Trigger.Animation.SHAKE;
        t.sound.soundId = "minecraft:entity.player.levelup";
        t.sound.plays = 2;
        t.sound.secondsBetween = 0.3;
        return t;
    }

    public static Trigger rareCatch() {
        Trigger t = new Trigger();
        t.name = "rare catch";
        t.pattern = "rare catch! you * {item}";
        t.title.title = "&lrare catch!";
        t.title.titleColor = "#ffaa00";
        t.title.subtitle = "{item.lower}";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:block.amethyst_block.chime";
        t.sound.plays = 3;
        t.sound.secondsBetween = 0.15;
        return t;
    }

    /** One shard per climate: "RARE CATCH! You caught a 1x Subarctic Shard!". */
    /** Anchored like {@link #PET_DROP}: other players' catches come with a "! [n] name »" prefix. */
    private static final String SHARD = "^rare catch! you (?:pulled|caught) an? (?<n>\\d+)x (?<climate>[a-z -]+?) shard";

    public static Trigger shardTotem() {
        Trigger t = new Trigger();
        t.name = "shard totem";
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = SHARD;
        t.title.enabled = false;
        t.totem.enabled = true;
        t.totem.item = "shard:{climate}";
        t.sound.soundId = "minecraft:item.totem.use";
        return t;
    }

    public static Trigger climateShard() {
        Trigger t = new Trigger();
        t.name = "climate shard";
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = SHARD;
        t.title.title = "&l{climate.lower} shard!";
        t.title.titleColor = "#ffaa00";
        t.title.subtitle = "{n}x";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:block.amethyst_block.chime";
        t.sound.plays = 3;
        t.sound.secondsBetween = 0.15;
        return t;
    }

    /** Your own variant catches: "MELANISTIC! You caught a Grass Carp" (others read "name caught ..."). */
    public static Trigger variant() {
        Trigger t = new Trigger();
        t.name = "variant catch";
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

    public static Trigger fabled() {
        Trigger t = new Trigger();
        t.name = "fabled event";
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
        Trigger t = new Trigger();
        t.name = "storm";
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
        Trigger t = new Trigger();
        t.name = "moon rising";
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
        Trigger t = new Trigger();
        t.name = "fish sold";
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
}
