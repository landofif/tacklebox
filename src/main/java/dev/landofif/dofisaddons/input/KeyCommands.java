package dev.landofif.dofisaddons.input;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.stats.StatsTracker;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.InputUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Keys that run a command. The command is sent once per key press, only while no screen is open, so it is
 * the players own press and never anything the mod decides (see the display-only rule in CLAUDE.md).
 */
public final class KeyCommands {
    /** One key and the command it runs (without the slash). */
    public static class Entry {
        public int key = -1;
        public String command = "";
    }

    private static final Set<Integer> held = new HashSet<>();

    private KeyCommands() {}

    public static void tick(MinecraftClient client) {
        DofiSettings s = StatsTracker.settings();
        if (s == null || s.keyCommands.isEmpty()) {
            return;
        }
        boolean canRun = client.currentScreen == null && client.player != null && client.getOverlay() == null;
        Set<Integer> now = new HashSet<>();
        for (Entry e : s.keyCommands) {
            if (e.key < 0 || e.command.isBlank() || !InputUtil.isKeyPressed(client.getWindow(), e.key)) {
                continue;
            }
            now.add(e.key);
            if (canRun && !held.contains(e.key)) {
                client.player.networkHandler.sendChatCommand(e.command.strip().replaceFirst("^/+", ""));
            }
        }
        held.clear();
        held.addAll(now);
    }
}
