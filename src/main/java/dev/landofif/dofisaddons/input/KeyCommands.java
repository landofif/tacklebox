package dev.landofif.dofisaddons.input;

import dev.landofif.dofisaddons.DofiSettings;
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

    /** Mouse buttons are stored as {@code MOUSE + glfw button} (3 is "mouse 4", 4 is "mouse 5"), keys as their glfw code. */
    public static final int MOUSE = 1000;

    private static final Set<Integer> held = new HashSet<>();

    private KeyCommands() {}

    private static boolean pressed(MinecraftClient client, int key) {
        return key >= MOUSE ? org.lwjgl.glfw.GLFW.glfwGetMouseButton(client.getWindow().getHandle(), key - MOUSE) == org.lwjgl.glfw.GLFW.GLFW_PRESS
                : InputUtil.isKeyPressed(client.getWindow(), key);
    }

    /** What the key is called in the editor. */
    public static String label(int key) {
        return key >= MOUSE ? "mouse " + (key - MOUSE + 1) : InputUtil.Type.KEYSYM.createFromCode(key).getLocalizedText().getString().toLowerCase();
    }

    public static void tick(MinecraftClient client) {
        DofiSettings s = DofiSettings.get();
        if (s == null || s.keyCommands.isEmpty()) {
            return;
        }
        boolean canRun = client.currentScreen == null && client.player != null && client.getOverlay() == null;
        Set<Integer> now = new HashSet<>();
        for (Entry e : s.keyCommands) {
            if (e.key < 0 || e.command.isBlank() || !pressed(client, e.key)) {
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
