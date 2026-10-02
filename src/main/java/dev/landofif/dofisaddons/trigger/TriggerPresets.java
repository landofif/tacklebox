package dev.landofif.dofisaddons.trigger;

import java.util.List;
import java.util.function.Supplier;

/** Ready-made triggers for common FishOnMC chat lines, offered when creating a new trigger. */
public final class TriggerPresets {
    public record Preset(String name, String description, Supplier<Trigger> create) {}

    private TriggerPresets() {}

    public static List<Preset> all() {
        return List.of(
                new Preset("Blank trigger", "Start from scratch", TriggerPresets::blank),
                new Preset("Quest completed", "QUESTS > Quest #1-8 has been completed!", TriggerPresets::questComplete),
                new Preset("Pet drop", "PET DROP! You pulled ...", TriggerPresets::petDrop),
                new Preset("Rare catch", "RARE CATCH! You pulled ... (shards, bottles, capsules)", TriggerPresets::rareCatch),
                new Preset("Fabled event", "FABLED > A Fabled Event is active at ...", TriggerPresets::fabled),
                new Preset("Storm", "STORM > A thunderstorm / supercell has formed!", TriggerPresets::storm),
                new Preset("Moon rising", "FULL / BLUE / SUPER / BLOOD MOON has risen", TriggerPresets::moon),
                new Preset("Fish sold", "SELL > You just earned $... from selling your fish!", TriggerPresets::sold)
        );
    }

    public static Trigger blank() {
        Trigger t = new Trigger();
        t.name = "My trigger";
        t.pattern = "";
        t.title.title = "&lHello!";
        t.title.subtitle = "";
        return t;
    }

    public static Trigger questComplete() {
        Trigger t = new Trigger();
        t.name = "Quest completed";
        t.pattern = "QUESTS > Quest #{n} has been completed!";
        t.title.title = "&a&lQUEST {n} COMPLETE";
        t.title.subtitle = "Type /quests to claim it";
        t.title.titleColor = "#55FF55";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:ui.toast.challenge_complete";
        return t;
    }

    public static Trigger petDrop() {
        Trigger t = new Trigger();
        t.name = "Pet drop";
        t.pattern = "PET DROP! You pulled {pet}";
        t.title.title = "&#FD95F6&lPET DROP!";
        t.title.subtitle = "{pet}";
        t.title.animation = Trigger.Animation.SHAKE;
        t.sound.soundId = "minecraft:entity.player.levelup";
        t.sound.plays = 2;
        t.sound.secondsBetween = 0.3;
        return t;
    }

    public static Trigger rareCatch() {
        Trigger t = new Trigger();
        t.name = "Rare catch";
        t.pattern = "RARE CATCH! You pulled {item}";
        t.title.title = "&6&lRARE CATCH!";
        t.title.subtitle = "{item}";
        t.title.animation = Trigger.Animation.POP;
        t.sound.soundId = "minecraft:block.amethyst_block.chime";
        t.sound.plays = 3;
        t.sound.secondsBetween = 0.15;
        return t;
    }

    public static Trigger fabled() {
        Trigger t = new Trigger();
        t.name = "Fabled event";
        t.pattern = "FABLED > A Fabled Event is active at {place}";
        t.title.title = "&#F7453E&lFABLED EVENT";
        t.title.subtitle = "at {place}";
        t.title.animation = Trigger.Animation.TYPEWRITER;
        t.title.staySeconds = 4;
        t.sound.soundId = "minecraft:block.bell.use";
        return t;
    }

    public static Trigger storm() {
        Trigger t = new Trigger();
        t.name = "Storm";
        t.pattern = "STORM > A {kind} has formed!";
        t.title.title = "&e&l⚡ {kind}";
        t.title.subtitle = "Bite speed is up";
        t.title.animation = Trigger.Animation.SHAKE;
        t.sound.soundId = "minecraft:entity.lightning_bolt.thunder";
        t.sound.volume = 0.6;
        return t;
    }

    public static Trigger moon() {
        Trigger t = new Trigger();
        t.name = "Moon rising";
        t.mode = Trigger.MatchMode.REGEX;
        t.pattern = "(?<moon>FULL|BLUE|SUPER|BLOOD) MOON > A .* has risen";
        t.title.title = "&#60C2E9&l{moon} MOON";
        t.title.subtitle = "has risen";
        t.title.animation = Trigger.Animation.SLIDE;
        t.sound.soundId = "minecraft:block.beacon.activate";
        return t;
    }

    public static Trigger sold() {
        Trigger t = new Trigger();
        t.name = "Fish sold";
        t.pattern = "SELL > You just earned {amount} from selling your fish!";
        t.title.title = "&a&l+{amount}";
        t.title.subtitle = "fish sold";
        t.title.scale = 2.5;
        t.title.staySeconds = 1.5;
        t.title.animation = Trigger.Animation.SLIDE;
        t.sound.soundId = "minecraft:entity.experience_orb.pickup";
        return t;
    }
}
