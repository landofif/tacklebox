package dev.landofif.dofisaddons;

import dev.landofif.dofisaddons.chat.ChatTweaks;
import dev.landofif.dofisaddons.display.SoundQueue;
import dev.landofif.dofisaddons.display.TitleOverlay;
import dev.landofif.dofisaddons.hud.ServerInfo;
import dev.landofif.dofisaddons.hud.XpEstimate;
import dev.landofif.dofisaddons.screen.ChatFilterScreen;
import dev.landofif.dofisaddons.screen.TriggerEditScreen;
import dev.landofif.dofisaddons.screen.TriggerListScreen;
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

    /** Dumps the open screen (title, slots, lore) to a file and the clipboard, see display/DebugDump.gui. F8 by default. */
    private static KeyBinding dumpKey;

    @Override
    public void onInitializeClient() {
        // a pack of our own for foecollabs old title logo: mod resources load in id order, so foecollabs own would win over ours
        net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(c ->
                net.fabricmc.fabric.api.resource.v1.ResourceLoader.registerBuiltinPack(net.minecraft.util.Identifier.of(MOD_ID, "foelogo"), c,
                        net.minecraft.text.Text.literal("foecollab old logo"), net.fabricmc.fabric.api.resource.v1.pack.PackActivationType.ALWAYS_ENABLED));
        dev.landofif.dofisaddons.menu.FishMenu.register();
        dev.landofif.dofisaddons.menu.Vaults.register();
        TriggerStore.load();
        DofiSettings.load();
        dev.landofif.dofisaddons.hud.NowPlaying.start();
        dev.landofif.dofisaddons.display.FlatLeaves.register();

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) {
                XpEstimate.onChat(message);
                dev.landofif.dofisaddons.hud.ServerHud.onChat(message);
                dev.landofif.dofisaddons.hud.BoostsHud.onChat(message);
            }
            boolean show = TriggerEngine.onGameMessage(message, overlay);
            return show && (overlay || ChatTweaks.allow(message));
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) -> ChatTweaks.allow(message));

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dofisaddons.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        dumpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.dofisaddons.dump_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, category));
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
                net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen).register((s, input) -> {
                    if (dumpKey.matchesKey(input)) {
                        dev.landofif.dofisaddons.display.DebugDump.gui(s);
                    }
                }));

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            DofiSettings.save(); // what the quests left hud learned
            dev.landofif.dofisaddons.hud.ServerHud.reset();
            dev.landofif.dofisaddons.hud.BoostsHud.reset();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> DofiSettings.save());

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "custom_huds"), (context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // These screens draw HUD boxes themselves.
            if (client.options.hudHidden || dev.landofif.dofisaddons.compat.FoeMover.isMover(client.currentScreen)) {
                return;
            }
            XpEstimate.renderHud(context);
            dev.landofif.dofisaddons.hud.ServerHud.renderHud(context);
            dev.landofif.dofisaddons.hud.BoostsHud.renderHud(context);
            dev.landofif.dofisaddons.hud.NowPlaying.renderHud(context);
            dev.landofif.dofisaddons.display.BobberTimer.renderHud(context);
        });
        HudElementRegistry.attachElementAfter(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "chat_boxes"), (context, tickCounter) -> {
                    if (!dev.landofif.dofisaddons.compat.FoeMover.isMover(MinecraftClient.getInstance().currentScreen)) { // it draws the boxes itself
                        dev.landofif.dofisaddons.chat.ChatBoxes.render(context, tickCounter);
                    }
                });
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
            ServerInfo.tick(client);
            XpEstimate.tick(client);
            dev.landofif.dofisaddons.display.BobberTimer.tick(client);
            dev.landofif.dofisaddons.display.PetHeads.tick(client);
            dev.landofif.dofisaddons.input.KeyCommands.tick(client);
            TriggerEngine.tick();
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(dev.landofif.dofisaddons.compat.FoeScreens.config(null));
                }
            }
            if (pendingScreen != null) {
                client.setScreen(pendingScreen);
                pendingScreen = null;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(ClientCommandManager.literal("dofi")
                    .executes(ctx -> open(() -> dev.landofif.dofisaddons.compat.FoeScreens.config(null)))
                    .then(ClientCommandManager.literal("triggers").executes(ctx -> open(() -> new TriggerListScreen(null))))
                    .then(ClientCommandManager.literal("huds").executes(ctx -> open(() -> dev.landofif.dofisaddons.compat.FoeScreens.customHuds(null))))
                    .then(ClientCommandManager.literal("words").executes(ctx -> open(() -> new dev.landofif.dofisaddons.screen.WordSwapScreen(null))))
                    .then(ClientCommandManager.literal("keys").executes(ctx -> open(() -> new dev.landofif.dofisaddons.screen.KeyCommandScreen(null))))
                    .then(ClientCommandManager.literal("viewmodel").executes(ctx -> open(() -> new dev.landofif.dofisaddons.screen.ViewmodelScreen(null))))
                    .then(ClientCommandManager.literal("bobber").executes(ctx -> open(() -> new dev.landofif.dofisaddons.screen.BobberScreen(null))))
                    .then(ClientCommandManager.literal("serverhud").executes(ctx -> open(() -> new dev.landofif.dofisaddons.screen.ServerHudScreen(null))))
                    .then(ClientCommandManager.literal("bait").executes(ctx -> open(() -> new dev.landofif.dofisaddons.screen.BaitScreen(null))))
                    .then(ClientCommandManager.literal("power").executes(ctx -> open(() -> new dev.landofif.dofisaddons.screen.PowerScreen(null))))
                    .then(ClientCommandManager.literal("chat").executes(ctx -> open(() -> new ChatFilterScreen(null))))
                    .then(ClientCommandManager.literal("debug")
                            .then(ClientCommandManager.literal("item").executes(ctx -> dev.landofif.dofisaddons.display.DebugDump.heldItem())))
                    .then(ClientCommandManager.literal("move").executes(ctx -> open(() -> dev.landofif.dofisaddons.compat.FoeScreens.move(null)))));
            dispatcher.register(ClientCommandManager.literal("triggers").executes(ctx -> open(() -> new TriggerListScreen(null))));
        });
    }

    private static int open(Supplier<Screen> screen) {
        pendingScreen = screen.get();
        return 1;
    }
}
