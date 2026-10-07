package dev.landofif.dofisaddons.chat;

import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.DrawnTextConsumer;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.render.state.TextGuiElementRenderState;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.hud.MessageIndicator;
import net.minecraft.network.message.ChatVisibility;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import org.joml.Matrix3x2f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Separate chat boxes (direct messages, crew chat, anything with a rule): routing lines into them,
 * drawing each as a foecollab box with its name in the top edge, and scrolling, hover text, clicks
 * and copying while chat is open. Only what you see changes; nothing is sent.
 */
public final class ChatBoxes {
    /** Logged like vanilla chat, so lines that skip the chat box still show up in latest.log. */
    private static final Logger CHAT_LOG = LoggerFactory.getLogger(ChatHud.class);
    private static final int KEEP = 200;
    private static final int PAD_X = 8;
    private static final int PAD_Y = 7;
    private static final int LINE_H = 10;

    private record Line(Text text, long at) {}

    /** What a box holds this session, its wrapped lines (for the current width) and scroll position. */
    private static final class State {
        final Deque<Line> lines = new ArrayDeque<>();
        List<OrderedText> wrapped = List.of();
        /** The message each wrapped line came from. */
        List<Text> wrappedFrom = List.of();
        int wrappedWidth = -1;
        int version;
        int wrappedVersion = -1;
        int scroll;
        long lastAt;
    }

    private static final Map<ChatBox, State> STATE = new IdentityHashMap<>();

    private ChatBoxes() {}

    /** The boxes everyone starts with: direct messages and crew chat. */
    public static List<ChatBox> defaults() {
        List<ChatBox> out = new ArrayList<>();
        out.add(new ChatBox("dms", 60, 8, "^✉ ?(from|to) "));
        out.add(new ChatBox("crew", 60, 42, "^\\[crew chat\\] "));
        return out;
    }

    private static List<ChatBox> boxes() {
        DofiSettings s = DofiSettings.get();
        return s == null || s.chatBoxes == null ? List.of() : s.chatBoxes;
    }

    private static State state(ChatBox box) {
        return STATE.computeIfAbsent(box, b -> new State());
    }

    /** The box whose rules take this line, or null. */
    public static ChatBox boxFor(String line, boolean onFishOnMc) {
        for (ChatBox box : boxes()) {
            if (box == null || !box.enabled || box.rules == null) {
                continue;
            }
            for (ChatFilter f : box.rules) {
                if (f != null && f.enabled && (!f.fishOnMcOnly || onFishOnMc) && ChatTweaks.matcher(f).match(line) != null) {
                    return box;
                }
            }
        }
        return null;
    }

    /**
     * A line on its way into the chat box. Returns true when it went to a separate box instead, in
     * which case it is logged here, the way vanilla logs chat.
     */
    public static boolean route(Text message, MessageIndicator indicator) {
        ChatBox box = boxFor(message.getString(), TriggerEngine.isOnFishOnMc());
        if (box == null) {
            return false;
        }
        State st = state(box);
        long now = System.currentTimeMillis();
        st.lines.addLast(new Line(message, now));
        while (st.lines.size() > KEEP) {
            st.lines.removeFirst();
        }
        st.version++;
        st.lastAt = now;
        if (st.scroll > 0) {
            st.scroll++; // keep the lines you scrolled to where they are
        }
        if (box.alsoInChat) {
            return false;
        }
        String logged = message.getString().replace("\r", "\\r").replace("\n", "\\n");
        String kind = indicator == null ? null : indicator.loggedName();
        if (kind != null) {
            CHAT_LOG.info("[{}] [CHAT] {}", kind, logged);
        } else {
            CHAT_LOG.info("[CHAT] {}", logged);
        }
        return true;
    }

    // ------------------------------------------------------------------ layout

    /** Where a box is on screen: x, y, width, height in gui units (title included). */
    public static int[] rect(ChatBox box, int screenW, int screenH) {
        return rect(box, screenW, screenH, chatOpen());
    }

    /** Lines a box shows: more while chat is open. */
    public static int lines(ChatBox box, boolean open) {
        return Math.max(1, open ? Math.max(box.lines, box.linesOpen) : box.lines);
    }

    /** The height of a box that shows {@code lines} lines, and back (the move screen resizes by height). */
    public static int heightOf(int lines) {
        return PAD_Y * 2 + lines * LINE_H - (LINE_H - 9) + 4 + 9; // + title row
    }

    public static int linesOf(int height) {
        return Math.round((height - heightOf(0)) / (float) LINE_H);
    }

    public static int[] rect(ChatBox box, int screenW, int screenH, boolean open) {
        int w = Math.max(80, Math.min(screenW, box.width));
        int h = heightOf(lines(box, open));
        int x = (int) Math.round(box.x / 100.0 * screenW);
        int y = (int) Math.round(box.y / 100.0 * screenH);
        x = Math.max(0, Math.min(screenW - w, x));
        y = Math.max(0, Math.min(screenH - h, y));
        return new int[]{x, y, w, h};
    }

    private static List<OrderedText> wrapped(ChatBox box, State st, TextRenderer tr, int textW) {
        if (st.wrappedWidth != textW || st.wrappedVersion != st.version) {
            List<OrderedText> out = new ArrayList<>();
            List<Text> from = new ArrayList<>();
            for (Line line : st.lines) {
                for (OrderedText part : tr.wrapLines(line.text(), textW)) {
                    out.add(part);
                    from.add(line.text());
                }
            }
            st.wrapped = out;
            st.wrappedFrom = from;
            st.wrappedWidth = textW;
            st.wrappedVersion = st.version;
            st.scroll = Math.max(0, Math.min(st.scroll, Math.max(0, out.size() - lines(box, chatOpen()))));
        }
        return st.wrapped;
    }

    /** The lines shown right now, oldest first, with the y of the first one inside the panel. */
    private static List<OrderedText> visible(ChatBox box, State st, TextRenderer tr) {
        List<OrderedText> all = wrapped(box, st, tr, box.width - PAD_X * 2);
        int end = Math.max(0, all.size() - st.scroll);
        int start = Math.max(0, end - lines(box, chatOpen()));
        return all.subList(start, end);
    }

    // ------------------------------------------------------------------ drawing

    private static boolean chatOpen() {
        return MinecraftClient.getInstance().currentScreen instanceof ChatScreen;
    }

    /** Hud element, drawn just after the chat. */
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN) {
            return;
        }
        boolean open = chatOpen();
        long now = System.currentTimeMillis();
        for (ChatBox box : boxes()) {
            if (box == null || !box.enabled) {
                continue;
            }
            State st = state(box);
            if (!open && (st.lines.isEmpty() || (box.fadeSeconds > 0 && now - st.lastAt > box.fadeSeconds * 1000L))) {
                continue;
            }
            draw(ctx, box, false);
        }
    }

    /** Draws one box and returns its rectangle. {@code highlight} outlines it (the move screen). */
    public static int[] draw(DrawContext ctx, ChatBox box, boolean highlight) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int[] r = rect(box, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        int x = r[0], y = r[1], w = r[2], h = r[3];
        State st = state(box);

        Text title = Foe.text((box.name.isBlank() ? "chat" : box.name) + (st.scroll > 0 ? " ↓" + st.scroll : "")).withColor(Foe.TITLE & 0xFFFFFF);
        if (highlight) { // the move screen outlines the box so you can find it
            ctx.drawStrokedRectangle(x, y, w, h, Foe.ACCENT);
        }
        ctx.drawTextWithShadow(tr, title, x + PAD_X, y + 6, 0xFFFFFFFF);

        List<OrderedText> lines = visible(box, st, tr);
        if (lines.isEmpty()) {
            ctx.drawTextWithShadow(tr, Foe.text("nothing here yet").withColor(Foe.DIM & 0xFFFFFF), x + PAD_X, y + h - PAD_Y - 9, 0xFFFFFFFF);
        }
        int ty = y + h - PAD_Y - 9 - (lines.size() - 1) * LINE_H;
        for (OrderedText line : lines) {
            ctx.drawTextWithShadow(tr, line, x + PAD_X, ty, 0xFFFFFFFF);
            ty += LINE_H;
        }
        return r;
    }

    // ------------------------------------------------------------------ chat open: mouse

    /** The enabled, shown box under the mouse, or null. */
    public static ChatBox at(double mx, double my) {
        MinecraftClient client = MinecraftClient.getInstance();
        int sw = client.getWindow().getScaledWidth();
        int sh = client.getWindow().getScaledHeight();
        List<ChatBox> list = boxes();
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatBox box = list.get(i);
            if (box == null || !box.enabled) continue;
            int[] r = rect(box, sw, sh);
            if (mx >= r[0] && mx < r[0] + r[2] && my >= r[1] && my < r[1] + r[3]) {
                return box;
            }
        }
        return null;
    }

    public static boolean scroll(double mx, double my, double amount) {
        ChatBox box = at(mx, my);
        if (box == null || amount == 0) {
            return false;
        }
        State st = state(box);
        int total = wrapped(box, st, MinecraftClient.getInstance().textRenderer, box.width - PAD_X * 2).size();
        st.scroll = Math.max(0, Math.min(Math.max(0, total - lines(box, chatOpen())), st.scroll + (amount > 0 ? 1 : -1)));
        return true;
    }

    /**
     * The wrapped line under the mouse: its index in the box's wrapped lines, with the box in
     * {@code box[0]} and the line's x and y in {@code at}. -1 if there is none.
     */
    private static int lineAt(double mx, double my, ChatBox[] box, int[] at) {
        box[0] = at(mx, my);
        if (box[0] == null) {
            return -1;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int[] r = rect(box[0], client.getWindow().getScaledWidth(), client.getWindow().getScaledHeight());
        int y = r[1], h = r[3];
        State st = state(box[0]);
        List<OrderedText> lines = visible(box[0], st, tr);
        int first = Math.max(0, st.wrapped.size() - st.scroll) - lines.size();
        int ty = y + h - PAD_Y - 9 - (lines.size() - 1) * LINE_H;
        for (int i = 0; i < lines.size(); i++) {
            if (my >= ty - 1 && my < ty + LINE_H - 1) {
                at[0] = r[0] + PAD_X;
                at[1] = ty;
                return first + i;
            }
            ty += LINE_H;
        }
        return -1;
    }

    /** The style (click / hover / insertion) of the text under the mouse in a box, or null. */
    public static Style styleAt(double mx, double my) {
        ChatBox[] box = new ChatBox[1];
        int[] at = new int[2];
        int i = lineAt(mx, my, box, at);
        if (i < 0) {
            return null;
        }
        OrderedText line = state(box[0]).wrapped.get(i);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        Style[] found = new Style[1];
        TextGuiElementRenderState render = new TextGuiElementRenderState(tr, line, new Matrix3x2f(), at[0], at[1], 0xFFFFFFFF, 0, false, true, null);
        DrawnTextConsumer.handleHover(render, (float) mx, (float) my, s -> found[0] = s);
        return found[0];
    }

    /** The whole message under the mouse in a box (for copying), or null. */
    public static Text messageAt(double mx, double my) {
        ChatBox[] box = new ChatBox[1];
        int i = lineAt(mx, my, box, new int[2]);
        return i < 0 ? null : state(box[0]).wrappedFrom.get(i);
    }
}
