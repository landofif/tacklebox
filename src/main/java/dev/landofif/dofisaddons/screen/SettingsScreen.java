package dev.landofif.dofisaddons.screen;

import dev.isxander.yacl3.api.ConfigCategory;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.api.OptionDescription;
import dev.isxander.yacl3.api.OptionGroup;
import dev.isxander.yacl3.api.YetAnotherConfigLib;
import dev.isxander.yacl3.api.controller.BooleanControllerBuilder;
import dev.isxander.yacl3.api.controller.DoubleSliderControllerBuilder;
import dev.isxander.yacl3.api.controller.EnumControllerBuilder;
import dev.isxander.yacl3.api.controller.IntegerSliderControllerBuilder;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.screen.Screen;

import java.util.Locale;

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
                .save(StatsTracker::saveSettings)
                .build()
                .generateScreen(parent);
    }
}
