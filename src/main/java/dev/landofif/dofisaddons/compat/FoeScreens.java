package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.DofisAddons;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.TranslatableTextContent;

/** foecollab's own screens, made by reflection so the mod never compiles against foecollab. */
public final class FoeScreens {
    private static final String MAIN = "io.github.foecollab.screens.main.MainScreen";
    private static final String MOVE = "io.github.foecollab.screens.movehud.MoveHudScreen";

    private FoeScreens() {}

    /** foecollab's menu (custom huds, pet calculator, its config). */
    public static Screen main(Screen parent) {
        return make(MAIN, parent);
    }

    /** foecollab's hud mover; the mods own huds are in it too (see FoeMover). */
    public static Screen move(Screen parent) {
        return make(MOVE, parent);
    }

    /** Opens the hud mover from {@code from}, saving the mods settings first. */
    public static void openMover(Screen from) {
        DofiSettings.save();
        Screen mover = move(from);
        if (mover != null) {
            MinecraftClient.getInstance().setScreen(mover);
        }
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

    /** foecollab's cloth config screen, which the mod swaps for its own foecollab settings tab. */
    public static boolean isClothConfig(Screen screen) {
        return screen != null && screen.getClass().getName().startsWith("me.shedaniel.clothconfig2.")
                && screen.getTitle().getContent() instanceof TranslatableTextContent t
                && t.getKey().equals("text.autoconfig.foecollab.title");
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
