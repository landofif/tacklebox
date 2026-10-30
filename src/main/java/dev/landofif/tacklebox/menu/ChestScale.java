package dev.landofif.tacklebox.menu;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.Generic3x3ContainerScreen;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.screen.ingame.HopperScreen;
import net.minecraft.client.gui.screen.ingame.ShulkerBoxScreen;
import org.jspecify.annotations.Nullable;

/**
 * Server chest menus at their own gui scale (setting chestScale, 0 = the games): while one is open the window uses that
 * scale, the hud behind it included, and the games own scale comes back when it closes. Whole steps only, like the
 * games option. The mods own menu, presets and vault screens keep the games scale and their own size settings.
 */
public final class ChestScale {
    private ChestScale() {}

    public static boolean isChest(@Nullable Screen screen) {
        return screen instanceof GenericContainerScreen || screen instanceof Generic3x3ContainerScreen
                || screen instanceof HopperScreen || screen instanceof ShulkerBoxScreen;
    }

    /** The gui scale option to use: the chest menu one while a chest is open and it is set, else the games. */
    public static int guiScale(boolean chest, int chestScale, int game) {
        return chest && chestScale > 0 ? chestScale : game;
    }

    /** Puts the window on the scale the open screen wants; with {@code resize}, the open screen is laid out again for it. */
    public static void apply(MinecraftClient client, boolean resize) {
        var window = client.getWindow();
        int want = window.calculateScaleFactor(guiScale(isChest(client.currentScreen), TackleboxSettings.get().chestScale,
                client.options.getGuiScale().getValue()), client.forcesUnicodeFont());
        if (want == window.getScaleFactor()) {
            return;
        }
        window.setScaleFactor(want);
        client.mouse.onResolutionChanged();
        if (resize && client.currentScreen != null) {
            client.currentScreen.resize(window.getScaledWidth(), window.getScaledHeight());
        }
    }
}
