package dev.landofif.tacklebox;

import dev.landofif.tacklebox.chat.ChatTweaks;
import dev.landofif.tacklebox.chat.ChatPeek;
import dev.landofif.tacklebox.display.FishTooltip;
import dev.landofif.tacklebox.display.PetSale;
import dev.landofif.tacklebox.display.PetTooltip;
import dev.landofif.tacklebox.display.SoundQueue;
import dev.landofif.tacklebox.display.TitleOverlay;
import dev.landofif.tacklebox.hud.ServerInfo;
import dev.landofif.tacklebox.hud.XpEstimate;
import dev.landofif.tacklebox.screen.ChatFilterScreen;
import dev.landofif.tacklebox.screen.TriggerEditScreen;
import dev.landofif.tacklebox.screen.TriggerListScreen;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import dev.landofif.tacklebox.trigger.TriggerStore;
import dev.landofif.tacklebox.util.ConfigMigration;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.nbt.NbtCompound;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.util.Identifier;
import org.slf4j.Logger;
import org.lwjgl.glfw.GLFW;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Supplier;

public final class Tacklebox implements ClientModInitializer {
    public static final String MOD_ID = "tacklebox";
    public static final Logger LOGGER = LoggerFactory.getLogger("tacklebox");
    /** When to send /communityevent after joining (0 = not pending); sent once per game start. */
    private static long communityAskAt;
    private static boolean communityAsked;
    /** When to send /active (0 = not pending), 2 seconds after /communityevent so the boosts hud knows the running boosters. */
    private static long activeAskAt;

    private static void saveCommunity() {
        TackleboxSettings s = TackleboxSettings.get();
        s.communityCurrent = dev.landofif.tacklebox.hud.CommunityGoal.current();
        s.communityGoal = dev.landofif.tacklebox.hud.CommunityGoal.goal();
        s.communityMine = dev.landofif.tacklebox.hud.CommunityGoal.mine();
    }

    /** Screen to open on the next tick (a chat command can't open one directly: chat closes after it). */
    private static Screen pendingScreen;

    /** Opens /tacklebox. Right shift by default; change it in options → controls → tacklebox. */
    private static KeyBinding openKey;

    /** Dumps the open screen (title, slots, lore) to a file and the clipboard, see display/DebugDump.gui. F8 by default. */
    private static KeyBinding dumpKey;

    @Override
    public void onInitializeClient() {
        // first, before anything reads the config folder
        try {
            if (ConfigMigration.run(FabricLoader.getInstance().getConfigDir(), MOD_ID)) {
                LOGGER.info("moved config/dofisaddons to config/{}", MOD_ID);
            }
        } catch (IOException e) {
            LOGGER.error("Could not move config/dofisaddons to config/{}; starting with fresh settings", MOD_ID, e);
        }
        try {
            java.nio.file.Path zip = dev.landofif.tacklebox.util.ConfigBackups.run(TackleboxSettings.dir(), java.time.LocalDate.now(),
                    dev.landofif.tacklebox.util.ConfigBackups.KEEP);
            if (zip != null) {
                LOGGER.info("backed up config/{} to {}", MOD_ID, zip.getFileName());
            }
        } catch (IOException e) {
            LOGGER.error("Could not back up config/{}", MOD_ID, e);
        }
        // a pack of our own for foecollabs old title logo (a built-in pack doesn't depend on how the mod ids sort)
        net.fabricmc.loader.api.FabricLoader.getInstance().getModContainer(MOD_ID).ifPresent(c ->
                net.fabricmc.fabric.api.resource.v1.ResourceLoader.registerBuiltinPack(net.minecraft.util.Identifier.of(MOD_ID, "foelogo"), c,
                        net.minecraft.text.Text.literal("foecollab old logo"), net.fabricmc.fabric.api.resource.v1.pack.PackActivationType.ALWAYS_ENABLED));
        dev.landofif.tacklebox.menu.FishMenu.register();
        dev.landofif.tacklebox.display.TooltipScroll.register(); // before the page wheel: a tall tooltip takes the wheel first
        dev.landofif.tacklebox.menu.MenuPages.register();
        dev.landofif.tacklebox.menu.Vaults.register();
        TriggerStore.load();
        TackleboxSettings.load();
        TackleboxSettings cs = TackleboxSettings.get();
        dev.landofif.tacklebox.hud.CommunityGoal.restore(cs.communityCurrent, cs.communityGoal, cs.communityMine);
        dev.landofif.tacklebox.hud.NowPlaying.start();
        dev.landofif.tacklebox.display.FlatLeaves.register();
        dev.landofif.tacklebox.display.ChummerRing.register();
        dev.landofif.tacklebox.display.ScreenshotPreview.register();

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (!overlay) {
                XpEstimate.onChat(message);
                dev.landofif.tacklebox.hud.ServerHud.onChat(message);
                dev.landofif.tacklebox.hud.BoostsHud.onChat(message);
                dev.landofif.tacklebox.menu.QuestRedo.onChat(message);
                dev.landofif.tacklebox.hud.CommunityGoal.onChat(dev.landofif.tacklebox.trigger.Matcher.normalize(message.getString()));
                if (dev.landofif.tacklebox.hud.EventHud.onChat(dev.landofif.tacklebox.trigger.Matcher.normalize(message.getString()), System.currentTimeMillis())) {
                    TackleboxSettings.save();
                }
            }
            boolean show = TriggerEngine.onGameMessage(message, overlay);
            return show && (overlay || ChatTweaks.allow(message));
        });
        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signed, sender, params, time) -> ChatTweaks.allow(message));

        // after foecollabs own tooltip handlers (default phase), which have swapped their tag fonts by then
        Identifier shortTooltips = Identifier.of(MOD_ID, "short_tooltips");
        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, shortTooltips);
        ItemTooltipCallback.EVENT.register(shortTooltips, (stack, context, type, lines) -> {
            TackleboxSettings settings = TackleboxSettings.get();
            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (data == null || !(settings.shortFishTooltips || settings.shortPetTooltips)) {
                return;
            }
            NbtCompound nbt = data.copyNbt();
            if (settings.shortFishTooltips && FishTooltip.isFish(nbt)) {
                FishTooltip.shorten(lines);
            } else if (settings.shortPetTooltips && PetTooltip.isPet(nbt)) {
                PetTooltip.shorten(lines);
            }
        });

        Identifier petProfit = Identifier.of(MOD_ID, "pet_profit");
        ItemTooltipCallback.EVENT.addPhaseOrdering(Event.DEFAULT_PHASE, petProfit);
        ItemTooltipCallback.EVENT.register(petProfit, (stack, context, type, lines) -> {
            if (TackleboxSettings.get().petProfitMarks) {
                PetSale.of(stack).ifPresent(sale -> PetSale.addLine(lines, sale));
            }
        });

        KeyBinding.Category category = KeyBinding.Category.create(Identifier.of(MOD_ID, "main"));
        openKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tacklebox.open", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT, category));

        ChatPeek.key = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tacklebox.chat_peek", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, category));
        dumpKey = KeyBindingHelper.registerKeyBinding(new KeyBinding("key.tacklebox.dump_gui", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_F8, category));
        net.fabricmc.fabric.api.client.screen.v1.ScreenEvents.AFTER_INIT.register((client, screen, w, h) ->
                net.fabricmc.fabric.api.client.screen.v1.ScreenKeyboardEvents.afterKeyPress(screen).register((s, input) -> {
                    if (dumpKey.matchesKey(input)) {
                        dev.landofif.tacklebox.display.DebugDump.gui(s);
                    }
                }));

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            dev.landofif.tacklebox.chat.ChatRestore.join(handler.getRegistryManager());
            if (!communityAsked) {
                communityAskAt = System.currentTimeMillis() + 5000; // luna 2026-10-18: ask the server once per game start
            }
        });
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            communityAskAt = 0;
            activeAskAt = 0;
            saveCommunity();
            TackleboxSettings.save(); // what the quests left hud learned
            dev.landofif.tacklebox.hud.ServerHud.reset();
            dev.landofif.tacklebox.hud.BoostsHud.reset();
            dev.landofif.tacklebox.display.ChummerRing.reset();
        });
        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
            dev.landofif.tacklebox.chat.ChatRestore.stopping(client);
            saveCommunity();
            TackleboxSettings.save();
        });

        HudElementRegistry.addLast(Identifier.of(MOD_ID, "custom_huds"), (context, tickCounter) -> {
            MinecraftClient client = MinecraftClient.getInstance();
            // These screens draw HUD boxes themselves.
            if (client.options.hudHidden || dev.landofif.tacklebox.hud.HudMover.isMover(client.currentScreen)) {
                return;
            }
            XpEstimate.renderHud(context);
            dev.landofif.tacklebox.hud.LevelBars.renderHud(context);
            dev.landofif.tacklebox.hud.EquipmentRow.renderHud(context);
            dev.landofif.tacklebox.hud.ServerHud.renderHud(context);
            dev.landofif.tacklebox.hud.BoostsHud.renderHud(context);
            dev.landofif.tacklebox.hud.EventHud.renderHud(context);
            dev.landofif.tacklebox.hud.NowPlaying.renderHud(context);
            dev.landofif.tacklebox.display.BobberTimer.renderHud(context);
        });
        HudElementRegistry.attachElementAfter(net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements.CHAT,
                Identifier.of(MOD_ID, "chat_boxes"), (context, tickCounter) -> {
                    if (!dev.landofif.tacklebox.hud.HudMover.isMover(MinecraftClient.getInstance().currentScreen)) { // it draws the boxes itself
                        dev.landofif.tacklebox.chat.ChatBoxes.render(context, tickCounter);
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

        dev.landofif.tacklebox.input.CommandAliases.register();
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            SoundQueue.tick();
            if (communityAskAt > 0 && System.currentTimeMillis() >= communityAskAt && client.player != null) {
                communityAskAt = 0;
                if (TriggerEngine.isOnFishOnMc()) {
                    communityAsked = true;
                    client.player.networkHandler.sendChatCommand("communityevent");
                    activeAskAt = System.currentTimeMillis() + 2000; // luna 2026-10-29
                }
            }
            if (activeAskAt > 0 && System.currentTimeMillis() >= activeAskAt && client.player != null) {
                activeAskAt = 0;
                client.player.networkHandler.sendChatCommand("active");
            }
            ServerInfo.tick(client);
            XpEstimate.tick(client);
            dev.landofif.tacklebox.display.BobberTimer.tick(client);
            dev.landofif.tacklebox.display.RodIdle.tick(client);
            dev.landofif.tacklebox.hud.EquipmentRow.tick(client);
            dev.landofif.tacklebox.display.PetHeads.tick(client);
            dev.landofif.tacklebox.display.TotemPop.tick(client);
            dev.landofif.tacklebox.display.ChummerRing.tick(client);
            dev.landofif.tacklebox.input.KeyCommands.tick(client);
            ChatPeek.tick(client);
            TriggerEngine.tick();
            while (openKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new dev.landofif.tacklebox.screen.SettingsScreen(null));
                }
            }
            if (pendingScreen != null) {
                client.setScreen(pendingScreen);
                pendingScreen = null;
            }
        });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> {
            dispatcher.register(settingsCommand("tacklebox"));
            dispatcher.register(settingsCommand("dofi")); // the name before the rename still works
            dispatcher.register(ClientCommandManager.literal("triggers").executes(ctx -> open(() -> new TriggerListScreen(null))));
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> settingsCommand(String name) {
        LiteralArgumentBuilder<FabricClientCommandSource> command = ClientCommandManager.literal(name);
        if (dev.landofif.tacklebox.compat.FoeCollab.loaded()) { // foecollab's custom hud editor
            command.then(ClientCommandManager.literal("huds").executes(ctx -> open(() -> dev.landofif.tacklebox.compat.FoeScreens.customHuds(null))));
        }
        return command
                .executes(ctx -> open(() -> new dev.landofif.tacklebox.screen.SettingsScreen(null)))
                .then(ClientCommandManager.literal("triggers").executes(ctx -> open(() -> new TriggerListScreen(null))))
                .then(ClientCommandManager.literal("words").executes(ctx -> open(() -> new dev.landofif.tacklebox.screen.WordSwapScreen(null))))
                .then(ClientCommandManager.literal("keys").executes(ctx -> open(() -> new dev.landofif.tacklebox.screen.KeyCommandScreen(null))))
                .then(ClientCommandManager.literal("viewmodel").executes(ctx -> open(() -> new dev.landofif.tacklebox.screen.ViewmodelScreen(null))))
                .then(ClientCommandManager.literal("bobber").executes(ctx -> open(() -> new dev.landofif.tacklebox.screen.BobberScreen(null))))
                .then(ClientCommandManager.literal("serverhud").executes(ctx -> open(() -> new dev.landofif.tacklebox.screen.ServerHudScreen(null))))
                .then(ClientCommandManager.literal("bait").executes(ctx -> open(() -> new dev.landofif.tacklebox.screen.BaitScreen(null))))
                .then(ClientCommandManager.literal("power").executes(ctx -> open(() -> new dev.landofif.tacklebox.screen.PowerScreen(null))))
                .then(ClientCommandManager.literal("chat").executes(ctx -> open(() -> new ChatFilterScreen(null))))
                .then(ClientCommandManager.literal("debug")
                        .then(ClientCommandManager.literal("displays").executes(ctx -> dev.landofif.tacklebox.display.DebugDump.toggleDisplays()))
                        .then(ClientCommandManager.literal("count").executes(ctx -> dev.landofif.tacklebox.display.DebugDump.count()))
                        .then(ClientCommandManager.literal("item").executes(ctx -> dev.landofif.tacklebox.display.DebugDump.heldItem()))
                        .then(ClientCommandManager.literal("entities").executes(ctx -> dev.landofif.tacklebox.display.DebugDump.entities(24))
                                .then(ClientCommandManager.argument("radius", com.mojang.brigadier.arguments.IntegerArgumentType.integer(1, 64))
                                        .executes(ctx -> dev.landofif.tacklebox.display.DebugDump.entities(
                                                com.mojang.brigadier.arguments.IntegerArgumentType.getInteger(ctx, "radius"))))))
                .then(ClientCommandManager.literal("move").executes(ctx -> open(() -> new dev.landofif.tacklebox.hud.HudMover(null))));
    }

    private static int open(Supplier<Screen> screen) {
        pendingScreen = screen.get();
        return 1;
    }
}
