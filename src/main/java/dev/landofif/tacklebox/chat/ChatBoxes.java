package dev.landofif.tacklebox.chat;

import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.hud.HudPos;
import dev.landofif.tacklebox.trigger.Trigger;
import dev.landofif.tacklebox.trigger.TriggerEngine;
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
        /** When each wrapped line's message arrived. */
        List<Long> wrappedAt = List.of();
        int wrappedWidth = -1;
        int version;
        int wrappedVersion = -1;
        int scroll;
        /** The scroll position being drawn: eases toward {@link #scroll}. */
        float shown;
        long shownAt;
        /** The scroll bar this frame: x, top, bottom, thumb height, total lines, lines shown; null when there is none. */
        float[] bar;
        long lastAt;
    }

    private static final Map<ChatBox, State> STATE = new IdentityHashMap<>();

    /** Server lines that start with the crews tag: joins, leaves, missions, teleports. */
    public static final String CREWS_RULE = "^CREWS > "; // chat lines are normalized first: » reads as >

    private ChatBoxes() {}

    /** The boxes everyone starts with: direct messages and crew chat. */
    public static List<ChatBox> defaults() {
        List<ChatBox> out = new ArrayList<>();
        out.add(new ChatBox("dms", 60, 8, "^✉ ?(from|to) "));
        ChatBox crew = new ChatBox("crew", 60, 42, "^\\[crew chat\\] ");
        crew.rules.add(new ChatFilter(CREWS_RULE, Trigger.MatchMode.REGEX));
        out.add(crew);
        return out;
    }

    /**
     * A saved crew box that still has only the default crew chat rule gets the crews tag rule too.
     * One that luna edited (other rules, or the crews rule already there) is left alone.
     */
    public static void addCrewsRule(List<ChatBox> list) {
        for (ChatBox b : list) {
            if ("crew".equals(b.name) && b.rules.size() == 1 && "^\\[crew chat\\] ".equals(b.rules.get(0).pattern)) {
                b.rules.add(new ChatFilter(CREWS_RULE, Trigger.MatchMode.REGEX));
            }
        }
    }

    private static List<ChatBox> boxes() {
        TackleboxSettings s = TackleboxSettings.get();
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
        st.lines.addLast(new Line(stack(st.lines, message), now));
        while (st.lines.size() > KEEP) {
            st.lines.removeFirst();
        }
        st.version++;
        st.lastAt = now;
        if (st.scroll > 0) {
            st.scroll++; // keep the lines you scrolled to where they are
            st.shown++;
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

    /** Where a box is on screen: x, y, width, height in gui units. */
    public static int[] rect(ChatBox box, int screenW, int screenH) {
        return rect(box, screenW, screenH, chatOpen());
    }

    /** Lines a box shows: more while chat is open. */
    public static int lines(ChatBox box, boolean open) {
        return Math.max(1, open ? Math.max(box.lines, box.linesOpen) : box.lines);
    }

    /** The height of a box that shows {@code lines} lines, and back (the move screen resizes by height). */
    public static int heightOf(int lines) {
        return PAD_Y * 2 + lines * LINE_H - (LINE_H - 9) + 4;
    }

    public static int linesOf(int height) {
        return Math.round((height - heightOf(0)) / (float) LINE_H);
    }

    /** Where a box is. Open chat makes it taller upward: the bottom edge stays, so the newest line does not move. */
    public static int[] rect(ChatBox box, int screenW, int screenH, boolean open) {
        int w = Math.max(80, Math.min(screenW, box.width));
        int h = heightOf(lines(box, false));
        if (box.pos == null) {
            box.pos = new HudPos((int) Math.round(box.x / 100.0 * screenW), (int) Math.round(box.y / 100.0 * screenH));
        }
        box.pos.measure(screenW, screenH, w, h);
        int x = box.pos.left(screenW);
        int y = box.pos.top(screenH);
        if (open) {
            int bottom = y + h;
            h = heightOf(lines(box, true));
            y = Math.max(0, bottom - h);
        }
        return new int[]{x, y, w, h};
    }

    /**
     * The part of a box its lines take up. Lines sit at the bottom, so the empty top of a box that is not full
     * is left out (width and height 0 when it has no line to show).
     */
    public static int[] contentRect(ChatBox box, int screenW, int screenH) {
        int[] r = rect(box, screenW, screenH);
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        State st = state(box);
        int n = Math.min(lines(box, chatOpen()), Math.max(0, wrapped(box, st, tr, box.width - PAD_X * 2).size() - st.scroll));
        return fitToLines(r, n);
    }

    /** The bottom {@code n} lines of a box rectangle (the whole rectangle when they fill it). */
    static int[] fitToLines(int[] r, int n) {
        if (n <= 0) {
            return new int[]{r[0], r[1], 0, 0};
        }
        int h = Math.min(r[3], heightOf(n));
        return new int[]{r[0], r[1] + r[3] - h, r[2], h};
    }

    private static List<OrderedText> wrapped(ChatBox box, State st, TextRenderer tr, int textW) {
        if (st.wrappedWidth != textW || st.wrappedVersion != st.version) {
            List<OrderedText> out = new ArrayList<>();
            List<Text> from = new ArrayList<>();
            List<Long> at = new ArrayList<>();
            for (Line line : st.lines) {
                if (ChatSearch.hides(line.text())) {
                    continue;
                }
                for (OrderedText part : tr.wrapLines(line.text(), textW)) {
                    out.add(part);
                    from.add(line.text());
                    at.add(line.at());
                }
            }
            st.wrapped = out;
            st.wrappedFrom = from;
            st.wrappedAt = at;
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

    /** Chat is open, or the peek key is held (chat/ChatPeek). */
    public static boolean chatOpen() {
        return MinecraftClient.getInstance().currentScreen instanceof ChatScreen || ChatPeek.active();
    }

    /** Hud element, drawn just after the chat. */
    public static void render(DrawContext ctx, RenderTickCounter tick) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.options.hudHidden || client.options.getChatVisibility().getValue() == ChatVisibility.HIDDEN) {
            return;
        }
        for (ChatBox box : boxes()) {
            if (box == null || !box.enabled) {
                continue;
            }
            if (!shows(box)) {
                continue;
            }
            draw(ctx, box, false);
        }
    }

    /** True when the box draws something right now: it has lines that have not faded, or chat is open. */
    public static boolean shows(ChatBox box) {
        State st = state(box);
        return chatOpen() || !(st.lines.isEmpty() || (box.fadeSeconds > 0 && System.currentTimeMillis() - st.lastAt > box.fadeSeconds * 1000L));
    }

    /** Draws one box and returns its rectangle. {@code highlight} outlines it (the move screen). */
    public static int[] draw(DrawContext ctx, ChatBox box, boolean highlight) {
        MinecraftClient client = MinecraftClient.getInstance();
        TextRenderer tr = client.textRenderer;
        int[] r = rect(box, ctx.getScaledWindowWidth(), ctx.getScaledWindowHeight());
        int x = r[0], y = r[1], w = r[2], h = r[3];
        State st = state(box);

        if (highlight) { // the move screen outlines the box and names it, so you can find it
            ctx.drawStrokedRectangle(x, y, w, h, Foe.ACCENT);
            Text title = Foe.text(box.name.isBlank() ? "chat" : box.name).withColor(Foe.TITLE & 0xFFFFFF);
            ctx.drawTextWithShadow(tr, title, x, y >= 11 ? y - 11 : y + h + 2, 0xFFFFFFFF);
        }

        boolean open = chatOpen();
        List<OrderedText> all = wrapped(box, st, tr, box.width - PAD_X * 2);
        int shownLines = lines(box, open);
        // ease toward the scroll position; one more line is drawn while it is between two, clipped to the lines area
        long now = System.nanoTime();
        double dt = Math.min(0.25, (now - st.shownAt) / 1e9);
        st.shownAt = now;
        st.shown += (float) ((st.scroll - st.shown) * (1 - Math.exp(-dt * 18)));
        if (dragBox == box || Math.abs(st.scroll - st.shown) < 0.01f) {
            st.shown = st.scroll;
        }
        st.shown = Math.max(0, Math.min(st.shown, Math.max(0, all.size() - shownLines)));
        int whole = (int) st.shown;
        float frac = st.shown - whole;
        int end = Math.max(0, all.size() - whole);
        int start = Math.max(0, end - shownLines - (frac > 0.001f ? 1 : 0));
        List<OrderedText> lines = all.subList(start, end);
        int areaTop = y + h - PAD_Y - 9 - (shownLines - 1) * LINE_H - 1;
        int ty = y + h - PAD_Y - 9 - (lines.size() - 1) * LINE_H + Math.round(frac * LINE_H);
        // the same dark strip vanilla puts behind chat lines, at the "text background opacity" accessibility setting
        int back = (int) (client.options.getTextBackgroundOpacity().getValue() * 255) << 24;
        ctx.enableScissor(x, areaTop, x + w, y + h);
        for (OrderedText line : lines) {
            if (back != 0) {
                ctx.fill(x, ty - 1, x + w, ty + LINE_H - 1, back);
            }
            ctx.drawTextWithShadow(tr, line, x + PAD_X, ty, 0xFFFFFFFF);
            ty += LINE_H;
        }
        ctx.disableScissor();
        st.bar = null;
        if (open && all.size() > shownLines) {
            float top = areaTop, bottom = y + h - PAD_Y + 1, track = bottom - top;
            float thumb = Math.min(track, Math.max(12, track * shownLines / all.size()));
            float fr = Math.max(0, Math.min(1, st.shown / (all.size() - shownLines)));
            float thumbTop = top + (1 - fr) * (track - thumb);
            int bx = x + w - 7;
            st.bar = new float[]{bx, top, bottom, thumb, all.size(), shownLines};
            ctx.fill(bx, (int) top, bx + 5, (int) bottom, 0x50000000);
            ctx.fill(bx, Math.round(thumbTop), bx + 5, Math.round(thumbTop + thumb), dragBox == box ? Foe.ACCENT : 0xB0FFFFFF);
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

    private static ChatBox dragBox;
    private static float dragGrab;

    /** A left click on a box's scroll bar starts dragging it. */
    public static boolean pressBar(double mx, double my) {
        List<ChatBox> list = boxes();
        for (int i = list.size() - 1; i >= 0; i--) {
            ChatBox box = list.get(i);
            float[] b = box == null || !box.enabled ? null : state(box).bar;
            if (b != null && mx >= b[0] - 3 && mx <= b[0] + 8 && my >= b[1] && my <= b[2]) {
                float thumbTop = b[1] + (1 - Math.max(0, Math.min(1, state(box).shown / (b[4] - b[5])))) * (b[2] - b[1] - b[3]);
                dragBox = box;
                dragGrab = my >= thumbTop && my <= thumbTop + b[3] ? (float) my - thumbTop : b[3] / 2;
                dragBar((float) my, true);
                return true;
            }
        }
        return false;
    }

    /** Moves the scroll with the mouse while the bar is held. */
    public static void dragBar(double my, boolean held) {
        if (dragBox == null) {
            return;
        }
        State st = state(dragBox);
        float[] b = st.bar;
        if (!held || b == null) {
            dragBox = null;
            return;
        }
        float range = Math.max(1, b[2] - b[1] - b[3]);
        float frac = Math.max(0, Math.min(1, 1 - ((float) my - dragGrab - b[1]) / range));
        st.scroll = Math.round(frac * (b[4] - b[5]));
        st.shown = st.scroll;
    }

    /** The search text changed: every box wraps again with only the lines that match. */
    public static void refilter() {
        for (State st : STATE.values()) {
            st.version++;
            st.scroll = 0;
        }
    }

    /** Stacking in a box (chat/ChatStack): the repeated line leaves, the new one gets the count. */
    private static Text stack(Deque<Line> lines, Text message) {
        TackleboxSettings s = TackleboxSettings.get();
        if (s == null || !s.chatStack) {
            return message;
        }
        java.util.function.Function<String, String> bare = t -> s.chatTimeMode > 0 ? ChatTimes.bare(t) : t;
        List<Line> newestFirst = new ArrayList<>(lines).reversed();
        int i = ChatStack.find(newestFirst, l -> bare.apply(l.text().getString()), bare.apply(message.getString()));
        if (i < 0) {
            return message;
        }
        Line old = newestFirst.get(i);
        lines.removeLastOccurrence(old);
        int count = ChatStack.count(bare.apply(old.text().getString())) + 1;
        return message.copy().append(Text.literal(" (x" + count + ")").formatted(net.minecraft.util.Formatting.GRAY));
    }

    /** Closing the chat or sending a message brings every box back to its newest line. */
    public static void scrollToBottom() {
        for (State st : STATE.values()) {
            st.scroll = 0;
        }
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

    /** ctrl + f search: leaves it and scrolls the box so the message under the mouse is in the middle. False when there is none. */
    public static boolean jumpTo(double mx, double my) {
        ChatBox[] box = new ChatBox[1];
        int i = lineAt(mx, my, box, new int[2]);
        if (i < 0) {
            return false;
        }
        Text message = state(box[0]).wrappedFrom.get(i);
        ChatSearch.leave(); // every box wraps all its lines again, at the bottom
        State st = state(box[0]);
        List<OrderedText> all = wrapped(box[0], st, MinecraftClient.getInstance().textRenderer, box[0].width - PAD_X * 2);
        int first = -1, last = -1;
        for (int j = 0; j < st.wrappedFrom.size(); j++) {
            if (st.wrappedFrom.get(j) == message) {
                first = first < 0 ? j : first;
                last = j;
            }
        }
        if (first >= 0) { // wrapped lines are oldest first, scroll counts up from the newest
            st.scroll = ChatSearch.scrollToShow(all.size() - 1 - last, last - first + 1, lines(box[0], chatOpen()), all.size());
            st.shown = st.scroll;
        }
        return true;
    }

    /** The arrival time of the message under the mouse in a box ("HH:mm:ss"), or null. */
    public static String timeAt(double mx, double my) {
        ChatBox[] box = new ChatBox[1];
        int i = lineAt(mx, my, box, new int[2]);
        return i < 0 ? null : ChatTimes.format(state(box[0]).wrappedAt.get(i));
    }
}
