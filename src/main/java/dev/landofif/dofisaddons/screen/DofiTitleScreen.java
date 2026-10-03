package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.util.Mono;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.SharedConstants;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerWarningScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.option.ServerList;
import net.minecraft.network.NetworkingBackend;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;

/**
 * Replaces the minecraft title screen (see MinecraftClientMixin): the system24 frame, the mod name, a big
 * join fishonmc button with the servers player count and ping, and the usual menu buttons under it.
 * Joining is the same as picking fishonmc in the server list; it only happens when you click.
 */
public class DofiTitleScreen extends DofiScreen {
    private static final String ADDRESS = "play.fishonmc.net";

    private final MultiplayerServerListPinger pinger = new MultiplayerServerListPinger();
    private ServerInfo server;
    private ButtonWidget join;
    private boolean pinged;

    public DofiTitleScreen() {
        super(Mono.text("dofis addons"));
    }

    @Override
    protected void init() {
        if (server == null) {
            server = findServer();
        }
        int w = 220;
        int x = (width - w) / 2;
        int half = (w - 4) / 2;
        int y = height / 2 - 14;
        boolean online = client.isMultiplayerEnabled();

        join = addDrawableChild(ButtonWidget.builder(Mono.text("join fishonmc"), b -> join())
                .dimensions(x, y, w, 24).build());
        join.active = online;
        y += 32;
        addDrawableChild(ButtonWidget.builder(Mono.text("singleplayer"), b -> client.setScreen(new SelectWorldScreen(this)))
                .dimensions(x, y, half, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("multiplayer"), b -> client.setScreen(
                client.options.skipMultiplayerWarning ? new MultiplayerScreen(this) : new MultiplayerWarningScreen(this)))
                .dimensions(x + w - half, y, half, 20).build()).active = online;
        y += 24;
        addDrawableChild(ButtonWidget.builder(Mono.text("options"), b -> client.setScreen(new OptionsScreen(this, client.options)))
                .dimensions(x, y, half, 20).build());
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            addDrawableChild(ButtonWidget.builder(Mono.text("mods"), b -> client.setScreen(ModMenuLink.mods(this)))
                    .dimensions(x + w - half, y, half, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Mono.text("dofis addons"), b -> client.setScreen(new HomeScreen(this)))
                    .dimensions(x + w - half, y, half, 20).build());
        }
        y += 24;
        if (FabricLoader.getInstance().isModLoaded("modmenu")) {
            addDrawableChild(ButtonWidget.builder(Mono.text("dofis addons"), b -> client.setScreen(new HomeScreen(this)))
                    .dimensions(x, y, half, 20).build());
            addDrawableChild(ButtonWidget.builder(Mono.text("quit"), b -> client.scheduleStop())
                    .dimensions(x + w - half, y, half, 20).build());
        } else {
            addDrawableChild(ButtonWidget.builder(Mono.text("quit"), b -> client.scheduleStop())
                    .dimensions(x, y, w, 20).build());
        }

        if (!pinged && online) {
            pinged = true;
            ping();
        }
    }

    /** The fishonmc entry from the server list (keeps your resource pack choice), or a hidden new one. */
    private ServerInfo findServer() {
        ServerList list = new ServerList(client);
        list.loadFile();
        for (int i = 0; i < list.size(); i++) {
            ServerInfo info = list.get(i);
            if (info.address.toLowerCase(Locale.ROOT).contains("fishonmc")) {
                return info;
            }
        }
        ServerInfo info = list.get(ADDRESS);
        if (info == null) {
            // hidden, like a direct connect: remembers the pack prompt answer without adding a list entry
            info = new ServerInfo("fishonmc", ADDRESS, ServerInfo.ServerType.OTHER);
            list.add(info, true);
            list.saveFile();
        }
        return info;
    }

    private void join() {
        ConnectScreen.connect(this, client, ServerAddress.parse(server.address), server, false, null);
    }

    private void ping() {
        ServerInfo info = server;
        info.setStatus(ServerInfo.Status.PINGING);
        Thread thread = new Thread(() -> {
            try {
                pinger.add(info, () -> {}, () -> info.setStatus(
                        info.protocolVersion == SharedConstants.getGameVersion().protocolVersion()
                                ? ServerInfo.Status.SUCCESSFUL : ServerInfo.Status.INCOMPATIBLE),
                        NetworkingBackend.remote(client.options.shouldUseNativeTransport()));
            } catch (Exception e) {
                info.setStatus(ServerInfo.Status.UNREACHABLE);
                DofisAddons.LOGGER.debug("Could not ping {}", info.address, e);
            }
        }, "dofisaddons title ping");
        thread.setDaemon(true);
        thread.start();
    }

    private Text status() {
        if (!client.isMultiplayerEnabled()) {
            return Mono.text("multiplayer is disabled for this account").formatted(Formatting.RED);
        }
        return switch (server.getStatus()) {
            case SUCCESSFUL -> {
                String players = server.players == null ? "" : server.players.online() + " fishing · ";
                yield Mono.text(players + server.ping + "ms").withColor(S24.TEXT_3 & 0xFFFFFF);
            }
            case INCOMPATIBLE -> Mono.text("online · server wants a different version").formatted(Formatting.GOLD);
            case UNREACHABLE -> Mono.text("cant reach the server right now").formatted(Formatting.RED);
            default -> Mono.text("pinging…").withColor(S24.TEXT_4 & 0xFFFFFF);
        };
    }

    @Override
    public void tick() {
        pinger.tick();
    }

    @Override
    public void removed() {
        pinger.cancel();
        if (server.getStatus() == ServerInfo.Status.PINGING) {
            pinged = false; // ping again when we come back
        }
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int cx = width / 2;
        int top = height / 2 - 72;

        // the name at twice the size, in the medium face, pointed at the matching font variant
        Text name = Mono.prepare(Mono.bold("dofis addons").withColor(S24.ACCENT & 0xFFFFFF), 2f, false);
        ctx.getMatrices().pushMatrix();
        ctx.getMatrices().translate(cx, top);
        ctx.getMatrices().scale(2f, 2f);
        ctx.drawText(textRenderer, name, -textRenderer.getWidth(name) / 2, 0, 0xFFFFFFFF, false);
        ctx.getMatrices().popMatrix();
        ctx.drawCenteredTextWithShadow(textRenderer, Mono.text("fishonmc addons · display only, no automation").withColor(S24.TEXT_4 & 0xFFFFFF),
                cx, top + 24, 0xFFFFFFFF);

        // status in the joins top border, like a panel label
        Text status = status();
        int sw = textRenderer.getWidth(status);
        Text address = Mono.text(ADDRESS).withColor(S24.TEXT_4 & 0xFFFFFF);
        if (textRenderer.getWidth(address) + 8 + sw <= join.getWidth()) { // a long status gets the whole line
            ctx.drawText(textRenderer, address, join.getX(), join.getY() - 12, 0xFFFFFFFF, false);
        }
        ctx.drawText(textRenderer, status, join.getRight() - sw, join.getY() - 12, 0xFFFFFFFF, false);
        if (join.active && !join.isSelected()) {
            S24.outline(ctx, join.getX(), join.getY(), join.getWidth(), join.getHeight(), S24.ACCENT_DIM);
        }

        int bottom = height - 18;
        ctx.drawText(textRenderer, Mono.text(client.getSession().getUsername()).withColor(S24.TEXT_4 & 0xFFFFFF), 12, bottom, 0xFFFFFFFF, false);
        Text version = Mono.text("minecraft " + SharedConstants.getGameVersion().name() + " · fabric").withColor(S24.TEXT_5 & 0xFFFFFF);
        ctx.drawText(textRenderer, version, width - 12 - textRenderer.getWidth(version), bottom, 0xFFFFFFFF, false);
    }

    /** Only loaded when mod menu is installed. */
    private static final class ModMenuLink {
        static Screen mods(Screen parent) {
            return com.terraformersmc.modmenu.api.ModMenuApi.createModsScreen(parent);
        }
    }
}
