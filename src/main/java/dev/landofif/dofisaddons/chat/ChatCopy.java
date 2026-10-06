package dev.landofif.dofisaddons.chat;

import dev.landofif.dofisaddons.mixin.ChatHudAccessor;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

/**
 * Finds the chat message under the mouse with the chat open. It runs the chat's own layout pass with a
 * backend that only records which line the mouse is on, so it lines up with wherever the chat is drawn
 * (moved, scaled, scrolled).
 */
public final class ChatCopy {
    private ChatCopy() {}

    /** The whole message (all its wrapped lines) under the mouse, or null. */
    public static @Nullable Text messageAt(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHud hud = client.inGameHud.getChatHud();
        ChatHudAccessor chat = (ChatHudAccessor) hud;
        Finder finder = new Finder(mouseX, mouseY, lineHeight(client));
        chat.dofisaddons$render(finder, client.getWindow().getScaledHeight(), client.inGameHud.getTicks(), true);
        if (finder.found == null) {
            return null;
        }
        List<ChatHudLine.Visible> visible = chat.dofisaddons$visibleMessages();
        List<ChatHudLine> messages = chat.dofisaddons$messages();
        // visible lines are newest first, and each message ends (index-wise starts) with an endOfEntry line
        int message = -1;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).endOfEntry()) {
                message++;
            }
            if (visible.get(i).content() == finder.found) {
                return message >= 0 && message < messages.size() ? messages.get(message).content() : null;
            }
        }
        return null;
    }

    /** Screen rectangle {x1, y1, x2, y2} of the whole message under the mouse (its lines that are on screen), or null. */
    public static float @Nullable [] boundsAt(double mouseX, double mouseY) {
        MinecraftClient client = MinecraftClient.getInstance();
        ChatHudAccessor chat = (ChatHudAccessor) client.inGameHud.getChatHud();
        Finder finder = new Finder(mouseX, mouseY, lineHeight(client));
        chat.dofisaddons$render(finder, client.getWindow().getScaledHeight(), client.inGameHud.getTicks(), true);
        if (finder.found == null) {
            return null;
        }
        List<ChatHudLine.Visible> visible = chat.dofisaddons$visibleMessages();
        int at = -1;
        for (int i = 0; i < visible.size(); i++) {
            if (visible.get(i).content() == finder.found) {
                at = i;
            }
        }
        if (at < 0) {
            return null;
        }
        int start = at; // newest first: a message starts at its endOfEntry line and runs to the next one
        while (start > 0 && !visible.get(start).endOfEntry()) {
            start--;
        }
        int end = at + 1;
        while (end < visible.size() && !visible.get(end).endOfEntry()) {
            end++;
        }
        float[] out = null;
        for (int i = start; i < end; i++) {
            float[] r = finder.rects.get(visible.get(i).content());
            if (r == null) {
                continue;
            }
            out = out == null ? r.clone() : new float[]{Math.min(out[0], r[0]), Math.min(out[1], r[1]), Math.max(out[2], r[2]), Math.max(out[3], r[3])};
        }
        return out;
    }

    /** Plain text for the clipboard: server icon glyphs dropped, spaces tidied. */
    public static String plain(Text message) {
        StringBuilder out = new StringBuilder();
        message.getString().codePoints().forEach(cp -> {
            if (!((cp >= 0xE000 && cp <= 0xF8FF) || cp >= 0xF0000)) { // private-use icons only, keep real text
                out.appendCodePoint(cp);
            }
        });
        return out.toString().replaceAll("\\s+", " ").trim();
    }

    private static int lineHeight(MinecraftClient client) {
        return (int) (9 * (client.options.getChatLineSpacing().getValue() + 1.0));
    }

    private static final class Finder implements ChatHud.Backend {
        private final double mouseX;
        private final double mouseY;
        private final int lineHeight;
        private final Matrix3x2f pose = new Matrix3x2f();
        private final Vector2f local = new Vector2f();
        private OrderedText found;
        /** Where each drawn line is on screen: x1, y1, x2, y2. */
        final java.util.Map<OrderedText, float[]> rects = new java.util.IdentityHashMap<>();

        Finder(double mouseX, double mouseY, int lineHeight) {
            this.mouseX = mouseX;
            this.mouseY = mouseY;
            this.lineHeight = lineHeight;
            local.set((float) mouseX, (float) mouseY);
        }

        @Override
        public void updatePose(Consumer<Matrix3x2f> transformer) {
            transformer.accept(pose);
            new Matrix3x2f(pose).invert().transformPosition((float) mouseX, (float) mouseY, local);
        }

        @Override
        public void fill(int x1, int y1, int x2, int y2, int color) {
        }

        @Override
        public boolean text(int y, float opacity, OrderedText text) {
            int w = MinecraftClient.getInstance().textRenderer.getWidth(text);
            Vector2f a = pose.transformPosition(-4, y - 1, new Vector2f());
            Vector2f b = pose.transformPosition(w + 4, y - 1 + lineHeight, new Vector2f());
            rects.put(text, new float[]{a.x, a.y, b.x, b.y});
            // a line's box runs from just above its text to the next line; x covers the chat width
            if (local.y >= y - 1 && local.y < y - 1 + lineHeight && local.x >= -4) {
                int width = MinecraftClient.getInstance().textRenderer.getWidth(text);
                if (local.x <= Math.max(width, 40) + 8) {
                    found = text;
                }
            }
            return false;
        }

        @Override
        public void indicator(int x1, int y1, int x2, int y2, float opacity, MessageIndicator indicator) {
        }

        @Override
        public void indicatorIcon(int left, int bottom, boolean forceDraw, MessageIndicator indicator, MessageIndicator.Icon icon) {
        }
    }
}
