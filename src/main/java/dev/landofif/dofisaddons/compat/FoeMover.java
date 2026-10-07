package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.chat.ChatBox;
import dev.landofif.dofisaddons.chat.ChatBoxes;
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
            s.chatX = Math.max(0, Math.min(sw() - r[2], x));
            s.chatY += Math.max(0, Math.min(sh() - r[3], y)) - r[1];
        }, null, () -> {
            s.chatX = 0;
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
            add.add(new Box(ctx -> XpEstimate.draw(ctx, true), (x, y, r) -> {
                s.questsX = Math.max(0, Math.min(sw() - 40, x));
                s.questsY = Math.max(0, Math.min(sh() - 20, y));
            }, null, () -> {
                s.questsX = 4;
                s.questsY = 70;
            }));
        }
        for (ChatBox box : s.chatBoxes) {
            if (!box.enabled) {
                continue;
            }
            Box b = new Box(ctx -> ChatBoxes.draw(ctx, box, false), (x, y, r) -> {
                box.x = Math.max(0, Math.min(100, x * 100.0 / Math.max(1, sw())));
                box.y = Math.max(0, Math.min(100, y * 100.0 / Math.max(1, sh())));
            }, (step, shift) -> {
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
        int w = (int) Math.ceil((Math.ceil(ChatHud.getWidth(o.getChatWidth().getValue()) / scale) + 12) * scale);
        int h = (int) Math.ceil(lines * lineH * scale);
        int bottom = (int) (Math.floor((ctx.getScaledWindowHeight() - 40) / scale) * scale);
        int[] r = {s.chatX, bottom - h + s.chatY, w, h};
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
            double v = Math.max(0, Math.min(1, (px / scale - 40) / 280.0));
            o.getChatWidth().setValue(v);
            if (mode == 1) {
                int w = (int) Math.ceil((Math.ceil(ChatHud.getWidth(v) / scale) + 12) * scale);
                DofiSettings.get().chatX = Math.max(0, right - w);
            }
        } else {
            double v = Math.max(0, Math.min(1, ((bottom - my) / scale - 20) / 160.0));
            (mode == 3 ? o.getChatHeightUnfocused() : o.getChatHeightFocused()).setValue(v);
        }
    }

    /** Edge drag on a chat box. It grows downward from its top left corner. */
    private static void dragBox(ChatBox box, int mode, int mx, int my, int[] r) {
        int right = r[0] + r[2];
        if (mode == 2) {
            box.width = Math.max(80, Math.min(600, mx - r[0]));
        } else if (mode == 1) {
            box.width = Math.max(80, Math.min(600, right - mx));
            box.x = Math.max(0, Math.min(100, (right - box.width) * 100.0 / Math.max(1, sw())));
        } else if (mode == 3) {
            box.lines = Math.max(1, Math.min(30, ChatBoxes.linesOf(my - r[1])));
            box.linesOpen = Math.max(box.lines, box.linesOpen);
        } else {
            box.linesOpen = Math.max(box.lines, Math.min(40, ChatBoxes.linesOf(my - r[1])));
        }
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
            setX(r[0]); // the widget covers both sizes, so the open size's edge can be grabbed too
            setY(top);
            setDimensions(r[2], bottom - top);
            boolean hot = isHovered() || isFocused();
            if (o != r) { // the size while chat is open, fainter, with the closed size on top
                ctx.drawStrokedRectangle(o[0], o[1], o[2], o[3], hot ? Foe.DIM : Foe.FAINT);
            }
            ctx.drawStrokedRectangle(r[0], r[1], r[2], r[3], hot ? Foe.ACCENT : Foe.FAINT);
        }

        /** Which edge of the rectangles the click is on: 1 left, 2 right, 3 closed height, 4 open height, 0 none (move). */
        private int edgeAt(double mx, double my) {
            if (edgeDrag == null || now == null) {
                return 0;
            }
            int tol = 3;
            int[] r = now;
            int[] o = openOf.apply(r);
            int closedEdge = growUp ? r[1] : r[1] + r[3];
            int openEdge = growUp ? o[1] : o[1] + o[3];
            boolean inX = mx >= r[0] - tol && mx <= r[0] + r[2] + tol;
            if (inX && Math.abs(my - closedEdge) <= tol) {
                return 3;
            }
            if (inX && openEdge != closedEdge && Math.abs(my - openEdge) <= tol) {
                return 4;
            }
            if (my >= r[1] && my <= r[1] + r[3]) {
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
