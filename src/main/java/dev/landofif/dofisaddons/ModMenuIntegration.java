package dev.landofif.dofisaddons;

import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import dev.landofif.dofisaddons.screen.HomeScreen;

/** Mods -> Dofi's Addons -> Configure opens the home screen. Only loaded when Mod Menu is installed. */
public final class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return HomeScreen::new;
    }
}
