package dev.landofif.dofisaddons.screen;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.ColorControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.screen.Screen;

import java.awt.Color;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** General settings, built with YACL. The bigger editors (triggers, HUDs) have their own screens. */
public final class SettingsScreen {
    private SettingsScreen() {}

    public static Screen create(Screen parent) {
        DofiSettings s = StatsTracker.settings();
        DofiSettings defaults = new DofiSettings();

        Option<DofiSettings.BobberMode> bobberMode = Option.<DofiSettings.BobberMode>createBuilder()
                .name(Mono.text("Bobber timer"))
                .description(OptionDescription.of(Mono.text(
                        "shows how long your line has been in the water. turns yellow, then red, as it gets long, and says bite! when something bites. display only.")))
                .binding(defaults.bobberTimer, () -> s.bobberTimer, v -> s.bobberTimer = v)
                .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(DofiSettings.BobberMode.class)
                        .formatValue(v -> Mono.text(v.label)))
                .build();

        Option<Double> slow = Option.<Double>createBuilder()
                .name(Mono.text("Turn red after"))
                .description(OptionDescription.of(Mono.text("the timer turns red after this many seconds. 0 = always white.")))
                .binding(defaults.bobberSlowSeconds, () -> s.bobberSlowSeconds, v -> s.bobberSlowSeconds = v)
                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                        .range(0.0, 60.0).step(1.0)
                        .formatValue(v -> Mono.text(v == 0 ? "never" : String.format(Locale.ROOT, "%.0fs", v))))
                .build();

        Option<Integer> pause = Option.<Integer>createBuilder()
                .name(Mono.text("Pause fishing clock after"))
                .description(OptionDescription.of(Mono.text(
                        "the time fishing clock (and fish per hour) stops when you havent caught anything for this long, so afk time doesnt count.")))
                .binding(defaults.pauseAfterSeconds, () -> s.pauseAfterSeconds, v -> s.pauseAfterSeconds = v)
                .controller(opt -> IntegerSliderControllerBuilder.create(opt)
                        .range(10, 600).step(10)
                        .formatValue(v -> Mono.text(v + "s")))
                .build();

        Option<Boolean> hideXp = Option.<Boolean>createBuilder()
                .name(Mono.text("Hide vanilla XP bar"))
                .description(OptionDescription.of(Mono.text(
                        "on fishonmc the xp bar shows your fishing level. this hides it and its number so the level hud can take its place. add the level hud from custom huds.")))
                .binding(defaults.hideVanillaXp, () -> s.hideVanillaXp, v -> s.hideVanillaXp = v)
                .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                .build();

        Option<DofiSettings.BobberSkin> skin = Option.<DofiSettings.BobberSkin>createBuilder()
                .name(Mono.text("Bobber skin"))
                .description(OptionDescription.of(Mono.text(
                        "swaps the look of your bobber. only you see it, other players still see the normal one.")))
                .binding(defaults.bobberSkin, () -> s.bobberSkin, v -> s.bobberSkin = v)
                .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(DofiSettings.BobberSkin.class)
                        .formatValue(v -> Mono.text(v.label)))
                .build();

        Option<Boolean> everyone = Option.<Boolean>createBuilder()
                .name(Mono.text("Skin every bobber"))
                .description(OptionDescription.of(Mono.text(
                        "also uses your skin for other players bobbers, on your screen only.")))
                .binding(defaults.skinEveryone, () -> s.skinEveryone, v -> s.skinEveryone = v)
                .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                .build();

        Option<Boolean> lineOn = Option.<Boolean>createBuilder()
                .name(Mono.text("Custom line colour"))
                .description(OptionDescription.of(Mono.text(
                        "colours your own fishing line. other lines stay normal.")))
                .binding(defaults.customLineColor, () -> s.customLineColor, v -> s.customLineColor = v)
                .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                .build();

        Option<Color> lineColor = Option.<Color>createBuilder()
                .name(Mono.text("Line colour"))
                .description(OptionDescription.of(Mono.text("the colour of your fishing line.")))
                .binding(new Color(defaults.lineColor), () -> new Color(s.lineColor), v -> s.lineColor = v.getRGB() & 0xFFFFFF)
                .controller(ColorControllerBuilder::create)
                .build();

        Option<Boolean> vmOn = Option.<Boolean>createBuilder()
                .name(Mono.text("Custom viewmodel"))
                .description(OptionDescription.of(Mono.text(
                        "changes how big your held item is and where it sits on screen. turn off to go back to normal.")))
                .binding(defaults.viewmodel, () -> s.viewmodel, v -> s.viewmodel = v)
                .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                .build();

        Option<Double> vmScale = slider("Size", "how big the held item is. 1 = normal.",
                defaults.vmScale, () -> s.vmScale, v -> s.vmScale = v, 0.1, 3.0, 0.05, "x");
        Option<Double> vmX = slider("Left / right", "moves the item sideways. mirrored for your off hand.",
                defaults.vmX, () -> s.vmX, v -> s.vmX = v, -1.0, 1.0, 0.05, "");
        Option<Double> vmY = slider("Up / down", "moves the item up or down.",
                defaults.vmY, () -> s.vmY, v -> s.vmY = v, -1.0, 1.0, 0.05, "");
        Option<Double> vmZ = slider("Forward / back", "moves the item closer or further away. negative is further.",
                defaults.vmZ, () -> s.vmZ, v -> s.vmZ = v, -1.0, 1.0, 0.05, "");
        Option<Double> rotX = slider("Tilt", "tilts the item forward or back, in degrees.",
                defaults.vmRotX, () -> s.vmRotX, v -> s.vmRotX = v, -180.0, 180.0, 1.0, "°");
        Option<Double> rotY = slider("Turn", "turns the item left or right, in degrees.",
                defaults.vmRotY, () -> s.vmRotY, v -> s.vmRotY = v, -180.0, 180.0, 1.0, "°");
        Option<Double> rotZ = slider("Roll", "rolls the item sideways, in degrees.",
                defaults.vmRotZ, () -> s.vmRotZ, v -> s.vmRotZ = v, -180.0, 180.0, 1.0, "°");

        Option<Boolean> noDip = Option.<Boolean>createBuilder()
                .name(Mono.text("No switch dip"))
                .description(OptionDescription.of(Mono.text(
                        "stops the item from dipping down when you switch slots or use it.")))
                .binding(defaults.vmNoEquipDip, () -> s.vmNoEquipDip, v -> s.vmNoEquipDip = v)
                .controller(opt -> BooleanControllerBuilder.create(opt).coloured(true))
                .build();

        Option<DofiSettings.SwingStyle> swing = Option.<DofiSettings.SwingStyle>createBuilder()
                .name(Mono.text("Swing style"))
                .description(OptionDescription.of(Mono.text(
                        "how your hand swings. smooth eases in and out, snappy moves fast at the start, no swing keeps the item still. only you see it.")))
                .binding(defaults.swingStyle, () -> s.swingStyle, v -> s.swingStyle = v)
                .controller(opt -> EnumControllerBuilder.create(opt)
                        .enumClass(DofiSettings.SwingStyle.class)
                        .formatValue(v -> Mono.text(v.label)))
                .build();

        Option<Double> swingSpeed = slider("Swing length", "how long one swing takes. 1 = normal, 2 = twice as slow, 0.5 = twice as fast. only changes how it looks.",
                defaults.swingSlowness, () -> s.swingSlowness, v -> s.swingSlowness = v, 0.25, 4.0, 0.05, "x");

        return YetAnotherConfigLib.createBuilder()
                .title(Mono.text("Dofi's Addons · Settings"))
                .category(ConfigCategory.createBuilder()
                        .name(Mono.text("General"))
                        .group(OptionGroup.createBuilder()
                                .name(Mono.text("Level"))
                                .option(hideXp)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Mono.text("Bobber timer"))
                                .option(bobberMode)
                                .option(slow)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Mono.text("Stats"))
                                .option(pause)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Mono.text("Bobber"))
                        .group(OptionGroup.createBuilder()
                                .name(Mono.text("Skin"))
                                .option(skin)
                                .option(everyone)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Mono.text("Line"))
                                .option(lineOn)
                                .option(lineColor)
                                .build())
                        .build())
                .category(ConfigCategory.createBuilder()
                        .name(Mono.text("Viewmodel"))
                        .group(OptionGroup.createBuilder()
                                .name(Mono.text("Held item"))
                                .option(vmOn)
                                .option(vmScale)
                                .option(vmX)
                                .option(vmY)
                                .option(vmZ)
                                .option(rotX)
                                .option(rotY)
                                .option(rotZ)
                                .option(noDip)
                                .build())
                        .group(OptionGroup.createBuilder()
                                .name(Mono.text("Swing"))
                                .option(swing)
                                .option(swingSpeed)
                                .build())
                        .build())
                .save(StatsTracker::saveSettings)
                .build()
                .generateScreen(parent);
    }

    private static Option<Double> slider(String name, String desc, double def, Supplier<Double> get, Consumer<Double> set,
                                         double min, double max, double step, String unit) {
        return Option.<Double>createBuilder()
                .name(Mono.text(name))
                .description(OptionDescription.of(Mono.text(desc)))
                .binding(def, get, set)
                .controller(opt -> DoubleSliderControllerBuilder.create(opt)
                        .range(min, max).step(step)
                        .formatValue(v -> Mono.text(String.format(Locale.ROOT, step >= 1 ? "%.0f%s" : "%.2f%s", v, unit))))
                .build();
    }
}
