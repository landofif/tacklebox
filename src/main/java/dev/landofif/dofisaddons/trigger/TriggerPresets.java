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
                new Preset("rare catch", "rare catch! you pulled ... (shards, bottles, capsules)", TriggerPresets::rareCatch),
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
        t.title.title = "&a&lquest {n} complete";
        t.title.subtitle = "type /quests to claim it";
        t.title.titleColor = "#55ff55";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:ui.toast.challenge_complete";
        return t;
    }

    public static Trigger petDrop() {
        Trigger t = new Trigger();
        t.name = "pet drop";
        t.pattern = "pet drop! you pulled {pet}";
        t.title.title = "&#fd95f6&lpet drop!";
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
        t.pattern = "rare catch! you pulled {item}";
        t.title.title = "&6&lrare catch!";
        t.title.subtitle = "{item.lower}";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:block.amethyst_block.chime";
        t.sound.plays = 3;
        t.sound.secondsBetween = 0.15;
        return t;
    }

    public static Trigger fabled() {
        Trigger t = new Trigger();
        t.name = "fabled event";
        t.pattern = "fabled > a fabled event is active at {place}";
        t.title.title = "&#f7453e&lfabled event";
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
        t.title.title = "&e&l⚡ {kind.lower}";
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
        t.title.title = "&#60c2e9&l{moon.lower} moon";
        t.title.subtitle = "has risen";
        t.title.animation = Trigger.Animation.SLIDE;
        t.sound.soundId = "minecraft:block.beacon.activate";
        return t;
    }

    public static Trigger sold() {
        Trigger t = new Trigger();
        t.name = "fish sold";
        t.pattern = "sell > you just earned {amount} from selling your fish!";
        t.title.title = "&a&l+{amount}";
        t.title.subtitle = "fish sold";
        t.title.scale = 2.5;
        t.title.staySeconds = 1.5;
        t.title.animation = Trigger.Animation.SLIDE;
        t.sound.soundId = "minecraft:entity.experience_orb.pickup";
        return t;
    }
}
