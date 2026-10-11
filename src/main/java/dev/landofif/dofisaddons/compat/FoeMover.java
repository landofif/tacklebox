package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.chat.ChatBar;
import dev.landofif.dofisaddons.chat.ChatBox;
import dev.landofif.dofisaddons.chat.ChatBoxes;
import dev.landofif.dofisaddons.hud.HudPos;
import dev.landofif.dofisaddons.hud.XpEstimate;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.input.MouseInput;
import net.minecraft.client.option.GameOptions;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The mods huds in foecollabs hud mover (its {@code MoveHudScreen}, see MoveHudScreenMixin): the quests
 * left box, the chat and every chat box can be dragged like foecollabs own. Right click puts one back;
 * scroll a chat box for more or fewer lines, shift + scroll for its width, or drag its edges (the chat's own chat window sizes, closed and open). While the mover is open the
 * mods huds draw here instead of in the game, so they show once.
 */
public final class FoeMover {
    private static final String MOVE = "io.github.foecollab.screens.movehud.MoveHudScreen";

    private FoeMover() {}

    public static boolean isMover(Screen screen) {
        return screen != null && screen.getClass().getName().equals(MOVE);
    }

    /** A drag box for each of the mods huds that is on. */
    public static List<ClickableWidget> boxes() {
        DofiSettings s = DofiSettings.get();
        List<ClickableWidget> add = new ArrayList<>();
        Box chatBox = new Box(ctx -> chat(ctx), (x, y, r) -> {
            s.chatPos.moveTo(sw(), sh(), x, 0, r[2], 0); // sideways only, up and down it is an offset from the bottom
            s.chatY += Math.max(0, Math.min(sh() - r[3], y)) - r[1];
        }, null, () -> {
            s.chatPos = new HudPos(0, 0);
            s.chatY = 0;
        });
        chatBox.edges(true, FoeMover::chatOpen, FoeMover::dragChat);
        add.add(chatBox);
        add.add(new Box(ctx -> {
            int w = 182 * s.hotbarScale / 100, h = 22 * s.hotbarScale / 100;
            return new int[]{sw() / 2 - w / 2 + s.hotbarX, sh() - h + s.hotbarY, w, h};
        }, (x, y, r) -> {
            s.hotbarX = Math.max(-sw() / 2 + r[2] / 2, Math.min(sw() / 2 - r[2] / 2, x - (sw() / 2 - r[2] / 2)));
            s.hotbarY = Math.max(-sh() + r[3], Math.min(0, y - (sh() - r[3])));
        }, (step, shift) -> s.hotbarScale = Math.max(50, Math.min(200, s.hotbarScale + step * 5)), () -> {
            s.hotbarX = 0;
            s.hotbarY = 0;
            s.hotbarScale = 100;
        }));
        add.add(new Box(ctx -> {
            int w = 131 * s.equipScale / 100, h = 20 * s.equipScale / 100; // foecollabs default place: the row above the hotbar
            int[] off = s.equipOffset();
            return new int[]{sw() / 2 - w / 2 + s.hotbarX + s.equipX + off[0], sh() - 28 - h + s.hotbarY + s.equipY + off[1], w, h};
        }, (x, y, r) -> {
            int[] off = s.equipOffset();
            s.equipX = x - (sw() / 2 - r[2] / 2 + s.hotbarX + off[0]);
            s.equipY = y - (sh() - 28 - r[3] + s.hotbarY + off[1]);
        }, (step, shift) -> s.equipScale = Math.max(50, Math.min(200, s.equipScale + step * 5)), () -> {
            s.equipX = 0;
            s.equipY = 0;
            s.equipScale = 100;
        }));
        if (s.questsHud) {
            add.add(new Box(ctx -> XpEstimate.draw(ctx, true), (x, y, r) -> s.questsPos.moveTo(sw(), sh(), x, y, r[2], r[3]), (step, shift) -> s.questsScale = Math.max(50, Math.min(300, s.questsScale + step * 5)), () -> {
                s.questsScale = 100;
                s.questsPos = new HudPos(4, 70);
            }));
        }
        if (s.levelBars) {
            add.add(new Box(ctx -> dev.landofif.dofisaddons.hud.LevelBars.draw(ctx, true), (x, y, r) -> s.levelBarsPos.moveTo(sw(), sh(), x, y, r[2], r[3]), (step, shift) -> s.levelBarsScale = Math.max(50, Math.min(300, s.levelBarsScale + step * 5)), () -> {
                s.levelBarsScale = 100;
                s.levelBarsPos = new HudPos(4, 40);
            }));
        }
        if (s.boostsHud) {
            add.add(new Box(ctx -> dev.landofif.dofisaddons.hud.BoostsHud.draw(ctx, true), (x, y, r) -> s.boostsPos.moveTo(sw(), sh(), x, y, r[2], r[3]), (step, shift) -> s.boostsScale = Math.max(50, Math.min(300, s.boostsScale + step * 5)), () -> {
                s.boostsScale = 100;
                s.boostsPos = new HudPos(182, 70);
            }));
        }
        if (s.nowPlaying) {
            add.add(new Box(ctx -> dev.landofif.dofisaddons.hud.NowPlaying.draw(ctx, true), (x, y, r) -> s.nowPlayingPos.moveTo(sw(), sh(), x, y, r[2], r[3]), (step, shift) -> s.nowPlayingScale = Math.max(50, Math.min(300, s.nowPlayingScale + step * 5)), () -> {
                s.nowPlayingScale = 100;
                s.nowPlayingPos = new HudPos(4, 4);
            }));
        }
        if (s.serverHud) {
            add.add(new Box(ctx -> dev.landofif.dofisaddons.hud.ServerHud.draw(ctx, true), (x, y, r) -> s.serverHudPos.moveTo(sw(), sh(), x, y, r[2], r[3]), (step, shift) -> s.serverHudScale = Math.max(50, Math.min(200, s.serverHudScale + step * 5)), () -> {
                s.serverHudPos = new HudPos(4, 120);
                s.serverHudScale = 100;
            }));
        }
        if (s.timerMode == DofiSettings.TimerMode.HUD || s.timerMode == DofiSettings.TimerMode.BOTH) {
            add.add(new Box(ctx -> dev.landofif.dofisaddons.display.BobberTimer.drawTimer(ctx, true), (x, y, r) -> {
                s.timerX = Math.max(0, Math.min(100, (x + r[2] / 2.0) * 100 / Math.max(1, sw())));
                s.timerY = Math.max(0, Math.min(100, (y + r[3] / 2.0) * 100 / Math.max(1, sh())));
            }, (step, shift) -> s.timerSize = Math.max(50, Math.min(500, s.timerSize + step * 10)), () -> {
                s.timerX = 50;
                s.timerY = 58;
                s.timerSize = 100;
            }));
        }
        if (s.biteOn) {
            add.add(new Box(ctx -> dev.landofif.dofisaddons.display.BobberTimer.drawBite(ctx, true), (x, y, r) -> {
                s.biteX = Math.max(0, Math.min(100, (x + r[2] / 2.0) * 100 / Math.max(1, sw())));
                s.biteY = Math.max(0, Math.min(100, (y + r[3] / 2.0) * 100 / Math.max(1, sh())));
            }, (step, shift) -> s.biteSize = Math.max(50, Math.min(600, s.biteSize + step * 20)), () -> {
                s.biteX = 50;
                s.biteY = 40;
                s.biteSize = 250;
            }));
        }
        for (ChatBox box : s.chatBoxes) {
            if (!box.enabled) {
                continue;
            }
            Box b = new Box(ctx -> ChatBoxes.draw(ctx, box, false), (x, y, r) -> box.pos.moveTo(sw(), sh(), x, y, r[2], r[3]), (step, shift) -> {
                if (shift) {
                    box.width = Math.max(80, Math.min(600, box.width + step * 10));
                } else {
                    box.lines = Math.max(1, Math.min(30, box.lines + step));
                    box.linesOpen = Math.max(box.lines, box.linesOpen);
                }
            }, null);
            b.edges(false, r -> ChatBoxes.rect(box, sw(), sh(), true), (mode, mx, my, r) -> dragBox(box, mode, mx, my, r));
            add.add(b);
        }
        return add;
    }

    private static int sw() {
        return MinecraftClient.getInstance().getWindow().getScaledWidth();
    }

    private static int sh() {
        return MinecraftClient.getInstance().getWindow().getScaledHeight();
    }

    /** Where the default chat is now, drawn as a plain box (mirrors ChatHud's own layout): x, y, width, height. */
    private static int[] chat(DrawContext ctx) {
        GameOptions o = MinecraftClient.getInstance().options;
        DofiSettings s = DofiSettings.get();
        double scale = o.getChatScale().getValue();
        int lineH = (int) (9 * (o.getChatLineSpacing().getValue() + 1.0));
        int lines = Math.max(1, ChatHud.getHeight(o.getChatHeightUnfocused().getValue()) / lineH);
        int w = ChatBar.width();
        int h = (int) Math.ceil(lines * lineH * scale);
        int bottom = (int) (Math.floor((ctx.getScaledWindowHeight() - 40) / scale) * scale);
        s.chatPos.measure(sw(), sh(), w, 0);
        int[] r = {s.chatPos.left(sw()), bottom - h + s.chatY, w, h};
        ctx.fill(r[0], r[1], r[0] + w, r[1] + h, 0x50000000);
        ctx.drawTextWithShadow(MinecraftClient.getInstance().textRenderer, Foe.text("chat"), r[0] + 4, r[1] + 4, Foe.DIM);
        return r;
    }

    // ------------------------------------------------------------------ resizing from the edges

    /** The default chat as it is while chat is open (the focused height), from the closed rectangle. */
    private static int[] chatOpen(int[] closed) {
        GameOptions o = MinecraftClient.getInstance().options;
        double scale = o.getChatScale().getValue();
        int lineH = (int) (9 * (o.getChatLineSpacing().getValue() + 1.0));
        int lines = Math.max(1, ChatHud.getHeight(o.getChatHeightFocused().getValue()) / lineH);
        int h = (int) Math.ceil(lines * lineH * scale);
        int bottom = closed[1] + closed[3];
        return new int[]{closed[0], bottom - h, closed[2], h};
    }

    /** Edge drag on the default chat. It grows upward, so the bottom stays; width and heights are the game's own chat options. */
    private static void dragChat(int mode, int mx, int my, int[] r) {
        MinecraftClient mc = MinecraftClient.getInstance();
        GameOptions o = mc.options;
        double scale = o.getChatScale().getValue();
        int right = r[0] + r[2];
        int bottom = r[1] + r[3];
        if (mode == 1 || mode == 2) {
            int px = (mode == 2 ? mx - r[0] : right - mx) - (int) Math.round(12 * scale);
            double v = Math.max(0, Math.min(1, (px - 40) / 280.0));
            o.getChatWidth().setValue(v);
            int w = ChatBar.width(); // pinned again from where the left edge is now, or the right edge stays where it was dragged from
            DofiSettings.get().chatPos.moveTo(sw(), sh(), mode == 1 ? Math.max(0, right - w) : r[0], 0, w, 0);
        } else {
            double v = Math.max(0, Math.min(1, ((bottom - my) / scale - 20) / 160.0));
            (mode == 3 ? o.getChatHeightUnfocused() : o.getChatHeightFocused()).setValue(v);
        }
    }

    /** Edge drag on a chat box. The closed height is its bottom edge; the open height grows upward from there. */
    private static void dragBox(ChatBox box, int mode, int mx, int my, int[] r) {
        int right = r[0] + r[2];
        int left = r[0];
        int h = r[3];
        if (mode == 2) {
            box.width = Math.max(80, Math.min(600, mx - r[0]));
        } else if (mode == 1) {
            box.width = Math.max(80, Math.min(600, right - mx));
            left = right - box.width;
        } else if (mode == 3) {
            box.lines = Math.max(1, Math.min(30, ChatBoxes.linesOf(my - r[1])));
            box.linesOpen = Math.max(box.lines, box.linesOpen);
            h = ChatBoxes.heightOf(box.lines);
        } else {
            box.linesOpen = Math.max(box.lines, Math.min(40, ChatBoxes.linesOf(r[1] + r[3] - my))); // from the top edge of the open outline down to the box's bottom
            return;
        }
        box.pos.moveTo(sw(), sh(), left, r[1], mode == 3 ? r[2] : box.width, h); // the size changed: stick it to the nearest sides again so the edge you drag stays under the mouse
    }

    private interface EdgeDrag {
        void drag(int mode, int mx, int my, int[] now);
    }

    private interface MoveTo {
        void to(int x, int y, int[] now);
    }

    private interface Resize {
        void by(int step, boolean shift);
    }

    /** One draggable hud: it draws itself, reports where it is, and is outlined while hovered or held. */
    private static final class Box extends ClickableWidget {
        private final Function<DrawContext, int[]> draw;
        private final MoveTo move;
        private final Resize resize;
        private final Runnable reset;
        private int[] now;
        private int grabX;
        private int grabY;
        /** Resizing from the edges: the rectangle while chat is open, which way it grows, and what a drag does. 0 = moving. */
        private Function<int[], int[]> openOf;
        private boolean growUp;
        private EdgeDrag edgeDrag;
        private int mode;
        /** How far from an edge a click still grabs it, inside or outside. */
        private static final int GRAB = 5;

        Box(Function<DrawContext, int[]> draw, MoveTo move, Resize resize, Runnable reset) {
            super(0, 0, 0, 0, Text.empty());
            this.draw = draw;
            this.move = move;
            this.resize = resize;
            this.reset = reset;
        }

        void edges(boolean growUp, Function<int[], int[]> openOf, EdgeDrag edgeDrag) {
            this.growUp = growUp;
            this.openOf = openOf;
            this.edgeDrag = edgeDrag;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int[] r = draw.apply(ctx);
            if (r == null) {
                setDimensions(0, 0);
                return;
            }
            now = r;
            int[] o = openOf == null ? r : openOf.apply(r);
            int top = Math.min(r[1], o[1]);
            int bottom = Math.max(r[1] + r[3], o[1] + o[3]);
            int m = edgeDrag == null ? 0 : GRAB; // the widget covers both sizes and a margin, so edges can be grabbed from outside too
            setX(r[0] - m);
            setY(top - m);
            setDimensions(r[2] + 2 * m, bottom - top + 2 * m);
            boolean hot = isHovered() || isFocused();
            if (o != r) { // the size while chat is open, fainter, with the closed size on top
                ctx.drawStrokedRectangle(o[0], o[1], o[2], o[3], hot ? Foe.DIM : Foe.FAINT);
            }
            ctx.drawStrokedRectangle(r[0], r[1], r[2], r[3], hot ? Foe.ACCENT : Foe.FAINT);
            if (edgeDrag != null) {
                handles(ctx, r, o, mode != 0 ? mode : hot ? edgeAt(mouseX, mouseY) : 0, hot);
            }
        }

        /** Little squares on the edges that can be dragged, the one under the mouse lit up and named. */
        private void handles(DrawContext ctx, int[] r, int[] o, int picked, boolean hot) {
            int closedEdge = growUp ? r[1] : r[1] + r[3];
            int cx = r[0] + r[2] / 2;
            int cy = r[1] + r[3] / 2;
            int[][] spots = {{r[0], cy}, {r[0] + r[2], cy}, {cx, closedEdge}, {cx, o[1]}};
            String[] names = {"width", "width", "height", "open height"};
            for (int i = 0; i < 4; i++) {
                if (i == 3 && o[1] == closedEdge) {
                    continue; // same edge as the closed height
                }
                boolean lit = picked == i + 1;
                int c = lit ? Foe.ACCENT : hot ? Foe.TEXT : Foe.FAINT;
                ctx.fill(spots[i][0] - 3, spots[i][1] - 3, spots[i][0] + 3, spots[i][1] + 3, c);
                if (lit) {
                    var tr = MinecraftClient.getInstance().textRenderer;
                    ctx.drawTextWithShadow(tr, Foe.text("drag: " + names[i]), spots[i][0] + 6, spots[i][1] - 4, Foe.ACCENT);
                }
            }
        }

        /** Which edge of the rectangles the click is on: 1 left, 2 right, 3 closed height, 4 open height, 0 none (move). */
        private int edgeAt(double mx, double my) {
            if (edgeDrag == null || now == null) {
                return 0;
            }
            int tol = GRAB;
            int[] r = now;
            int[] o = openOf.apply(r);
            int closedEdge = growUp ? r[1] : r[1] + r[3];
            int openEdge = o[1]; // the open size always grows upward
            boolean inX = mx >= r[0] - tol && mx <= r[0] + r[2] + tol;
            if (inX && Math.abs(my - closedEdge) <= tol) {
                return 3;
            }
            if (inX && openEdge != closedEdge && Math.abs(my - openEdge) <= tol) {
                return 4;
            }
            if (my >= r[1] - tol && my <= r[1] + r[3] + tol) {
                if (Math.abs(mx - r[0]) <= tol) {
                    return 1;
                }
                if (Math.abs(mx - (r[0] + r[2])) <= tol) {
                    return 2;
                }
            }
            return 0;
        }

        @Override
        protected boolean isValidClickButton(MouseInput input) {
            return input.button() == 0 || input.button() == 1;
        }

        @Override
        public void onClick(Click click, boolean doubled) {
            if (click.button() == 1 && reset != null) {
                reset.run();
                return;
            }
            mode = edgeAt(click.x(), click.y());
            grabX = (int) click.x() - (now == null ? getX() : now[0]);
            grabY = (int) click.y() - (now == null ? getY() : now[1]);
        }

        @Override
        protected void onDrag(Click click, double dx, double dy) {
            if (click.button() == 0 && now != null && mode != 0) {
                edgeDrag.drag(mode, (int) click.x(), (int) click.y(), now);
            } else if (click.button() == 0 && now != null) {
                move.to((int) click.x() - grabX, (int) click.y() - grabY, now);
            }
        }

        @Override
        public void onRelease(Click click) {
            if (mode != 0) { // the default chat's size is the game's own option: keep it
                MinecraftClient.getInstance().options.write();
            }
            mode = 0;
        }

        @Override
        public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
            if (resize == null || verticalAmount == 0 || !isMouseOver(mouseX, mouseY)) {
                return false;
            }
            resize.by(verticalAmount > 0 ? 1 : -1, MinecraftClient.getInstance().isShiftPressed());
            return true;
        }

        @Override
        protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        }
    }
}
