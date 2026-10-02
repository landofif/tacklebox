package dev.landofif.dofisaddons;

import dev.landofif.dofisaddons.display.SoundQueue;
import dev.landofif.dofisaddons.display.TitleOverlay;
import dev.landofif.dofisaddons.screen.TriggerListScreen;
import dev.landofif.dofisaddons.screen.TriggerEditScreen;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.trigger.TriggerStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class DofisAddons implements ClientModInitializer {
    public static final String MOD_ID = "dofisaddons";
    public static final Logger LOGGER = LoggerFactory.getLogger("Dofi's Addons");

    /** Screen to open on the next tick (a chat command can't open one directly: chat closes after it). */
    private static Screen pendingScreen;

    @Override
    public void onInitializeClient() {
        TriggerStore.load();

        ClientReceiveMessageEvents.ALLOW_GAME.register(TriggerEngine::onGameMessage);

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "trigger_titles"), (context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // The editor draws its own live preview on top of its UI.
            if (client.options.hudHidden || client.currentScreen instanceof TriggerEditScreen) {
                return;
            }
            TitleOverlay.render(context);
        });

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SoundQueue.tick();
            if (pendingScreen != null) {
                client.setScreen(pendingScreen);
                pendingScreen = null;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            for (String name : new String[]{"dofi", "triggers"}) {
                dispatcher.register(ClientCommandManager.literal(name).executes(ctx -> {
                    pendingScreen = new TriggerListScreen(null);
                    return 1;
                }));
            }
        });
    }
}
