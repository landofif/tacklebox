package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofisAddons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableTextContent;

/** foecollab's own screens, made by reflection so the mod never compiles against foecollab. */
public final class FoeScreens {
    public static final String TITLE = "io.github.foecollab.screens.main.FoETitleScreen";
    private static final String MAIN = "io.github.foecollab.screens.main.MainScreen";
    private static final String MOVE = "io.github.foecollab.screens.movehud.MoveHudScreen";

    private FoeScreens() {}

    /** foecollab's menu (custom huds, pet calculator, its config). */
    public static Screen main(Screen parent) {
        return make(MAIN, parent);
    }

    /** foecollab's hud mover. */
    public static Screen move(Screen parent) {
        return make(MOVE, parent);
    }

    /** foecollab's custom hud editor (the list of huds and their lines). */
    public static Screen customHuds(Screen parent) {
        try {
            return (Screen) Class.forName("io.github.foecollab.screens.CustomHudMakerScreen").getConstructor(Screen.class).newInstance(parent);
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            DofisAddons.LOGGER.warn("Could not open foecollabs custom hud editor", e);
            return null;
        }
    }

    public static Screen title() {
        try {
            return (Screen) Class.forName(TITLE).getConstructor().newInstance();
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            DofisAddons.LOGGER.warn("Could not open the foecollab title screen", e);
            return null;
        }
    }

    /** foecollab's cloth config screen, which the mod swaps for its own foecollab settings tab. */
    public static boolean isClothConfig(Screen screen) {
        return screen != null && screen.getClass().getName().startsWith("me.shedaniel.clothconfig2.")
                && screen.getTitle().getContent() instanceof TranslatableTextContent t
                && t.getKey().equals("text.autoconfig.foecollab.title");
    }

    public static boolean isTitle(Screen screen) {
        return screen != null && screen.getClass().getName().equals(TITLE);
    }

    private static Screen make(String name, Screen parent) {
        try {
            return (Screen) Class.forName(name).getConstructor(MinecraftClient.class, Screen.class)
                    .newInstance(MinecraftClient.getInstance(), parent);
        } catch (ReflectiveOperationException | LinkageError | ClassCastException e) {
            DofisAddons.LOGGER.warn("Could not open {}", name, e);
            return null;
        }
    }
}
