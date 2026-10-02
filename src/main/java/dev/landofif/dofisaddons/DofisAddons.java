package dev.landofif.dofisaddons;

import dev.landofif.dofisaddons.chat.ChatTweaks;
import dev.landofif.dofisaddons.display.BobberTimer;
import dev.landofif.dofisaddons.display.SoundQueue;
import dev.landofif.dofisaddons.display.TitleOverlay;
import dev.landofif.dofisaddons.hud.HudRenderer;
import dev.landofif.dofisaddons.hud.HudStore;
import dev.landofif.dofisaddons.hud.ServerInfo;
import dev.landofif.dofisaddons.screen.ChatFilterScreen;
import dev.landofif.dofisaddons.screen.HomeScreen;
import dev.landofif.dofisaddons.screen.HudEditScreen;
import dev.landofif.dofisaddons.screen.HudListScreen;
import dev.landofif.dofisaddons.screen.HudMoveScreen;
import dev.landofif.dofisaddons.screen.StatsScreen;
import dev.landofif.dofisaddons.screen.TriggerEditScreen;
import dev.landofif.dofisaddons.screen.TriggerListScreen;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.trigger.TriggerStore;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

public final class DofisAddons implements ClientModInitializer {
    public static final String MOD_ID = "dofisaddons";
    public static final Logger LOGGER = LoggerFactory.getLogger("Dofi's Addons");

    /** Screen to open on the next tick (a chat command can't open one directly: chat closes after it). */
    private static Screen pendingScreen;

    /** Opens /dofi. Right shift by default; change it in options → controls → dofis addons. */
    private static KeyBinding openKey;

    @Override
    public void onInitializeClient() {
        TriggerStore.load();
        HudStore.load();
        StatsTracker.loadSettings();

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) {
                StatsTracker.onChat(message);
            }
            boolean show = TriggerEngine.onGameMessage(message, overlay);
            return show && (overlay || ChatTweaks.allow(message));
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) -> ChatTweaks.allow(message));

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dofisaddons.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> StatsTracker.onJoin());
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> StatsTracker.onLeave());
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> StatsTracker.saveAllTime());

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "custom_huds"), (context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // These screens draw HUD boxes themselves.
            if (client.options.hudHidden || client.currentScreen instanceof HudMoveScreen || client.currentScreen instanceof HudEditScreen) {
                return;
            }
            HudRenderer.renderAll(context);
            BobberTimer.renderHud(context);
        });
        HudElementRegistry.addLast(Identifier.of(MOD_ID, "trigger_titles"), (context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // The editor draws its own live preview on top of its UI.
            if (client.options.hudHidden || client.currentScreen instanceof TriggerEditScreen) {
                return;
            }
            TitleOverlay.render(context);
        });
        WorldRenderEvents.AFTER_ENTITIES.register(BobberTimer::renderWorld);

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SoundQueue.tick();
            ServerInfo.tick(client);
            StatsTracker.tick(client);
            BobberTimer.tick(client);
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new HomeScreen(null));
                }
            }
            if (pendingScreen != null) {
                client.setScreen(pendingScreen);
                pendingScreen = null;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("dofi")
                    .executes(ctx -> open(() -> new HomeScreen(null)))
                    .then(ClientCommandManager.literal("triggers").executes(ctx -> open(() -> new TriggerListScreen(null))))
                    .then(ClientCommandManager.literal("huds").executes(ctx -> open(() -> new HudListScreen(null))))
                    .then(ClientCommandManager.literal("stats").executes(ctx -> open(() -> new StatsScreen(null))))
                    .then(ClientCommandManager.literal("chat").executes(ctx -> open(() -> new ChatFilterScreen(null))))
                    .then(ClientCommandManager.literal("move").executes(ctx -> open(() -> new HudMoveScreen(null)))));
            dispatcher.register(ClientCommandManager.literal("triggers").executes(ctx -> open(() -> new TriggerListScreen(null))));
        });
    }

    private static int open(Supplier<Screen> screen) {
        pendingScreen = screen.get();
        return 1;
    }
}
