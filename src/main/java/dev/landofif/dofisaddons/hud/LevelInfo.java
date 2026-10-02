package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * FishOnMC uses the vanilla XP bar for your fishing level: the number is your level and the bar is
 * the progress to the next one. This turns that into HUD values and colours it by level band.
 */
public final class LevelInfo {
    /** Level bands and their colours, as FishOnMC colours levels. */
    private static final int[][] BANDS = {
            {0, 0x8A897E}, {5, 0x868A6E}, {20, 0xBBCF53}, {35, 0xE8D41E}, {50, 0xE8A81E},
            {65, 0x7FD223}, {80, 0x23F133}, {95, 0x1EE8B2}, {110, 0x1EB6E8}, {125, 0x1958E9},
            {140, 0x411DE8}, {155, 0x9B1EE8}, {170, 0xBE12D2}, {185, 0xE81ED4}, {200, 0xE81E61},
            {210, 0xAC1648}, {220, 0x83232B}, {230, 0x71316B}, {240, 0xA00950}, {250, 0xD03EC2},
            {260, 0xE675DB}, {270, 0xDE9DD8}, {280, 0xE4C1E1}, {290, 0xE1C9DF}, {300, 0xFFFEFA}};

    private LevelInfo() {}

    public static int color(int level) {
        int c = BANDS[0][1];
        for (int[] band : BANDS) {
            if (level >= band[0]) c = band[1];
        }
        return c;
    }

    /** True when the vanilla XP bar and number should be hidden. */
    public static boolean hideVanilla() {
        return StatsTracker.settings().hideVanillaXp && TriggerEngine.isOnFishOnMc();
    }

    /** The level number in its band colour. */
    public static Text coloredLevel() {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        int level = p == null ? 0 : p.experienceLevel;
        return Mono.text(String.valueOf(level)).withColor(color(level));
    }

    /** The bar font: U+E100 is a solid 2px column, U+E101 a -1px space that closes the glyph gap. */
    private static final StyleSpriteSource BAR_FONT = new StyleSpriteSource.Font(Identifier.of("dofisaddons", "bar"));
    private static final int UNITS_PER_BLOCK = 3; // 3 columns x 2px = 6px, one character cell

    /** One continuous progress bar, {@code blocks} character cells long, filled to the XP progress. */
    public static Text bar(int blocks) {
        ClientPlayerEntity p = MinecraftClient.getInstance().player;
        if (p == null) {
            return Text.empty();
        }
        int units = blocks * UNITS_PER_BLOCK;
        int filled = Math.round(Math.max(0, Math.min(1, p.experienceProgress)) * units);
        MutableText t = Text.empty().setStyle(Style.EMPTY.withFont(BAR_FONT).withBold(false).withoutShadow());
        t.append(Text.literal("\ue100\ue101".repeat(filled)).withColor(color(p.experienceLevel)));
        t.append(Text.literal("\ue100\ue101".repeat(units - filled)).withColor(0x333333));
        return t;
    }
}
