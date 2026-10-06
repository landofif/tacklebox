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
 * scroll a chat box for more or fewer lines, shift + scroll for its width. While the mover is open the
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
        add.add(new Box(ctx -> chat(ctx), (x, y, r) -> {
            s.chatX = Math.max(0, Math.min(sw() - r[2], x));
            s.chatY += Math.max(0, Math.min(sh() - r[3], y)) - r[1];
        }, null, () -> {
            s.chatX = 0;
            s.chatY = 0;
        }));
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
        add.add(new Box(ctx -> new int[]{sw() / 2 - 65 + s.hotbarX + s.equipX, sh() - 48 + s.hotbarY + s.equipY, 131, 20}, (x, y, r) -> {
            s.equipX = x - (sw() / 2 - 65 + s.hotbarX); // foecollabs default place: the row above the hotbar
            s.equipY = y - (sh() - 48 + s.hotbarY);
        }, null, () -> {
            s.equipX = 0;
            s.equipY = 0;
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
            add.add(new Box(ctx -> ChatBoxes.draw(ctx, box, false), (x, y, r) -> {
                box.x = Math.max(0, Math.min(100, x * 100.0 / Math.max(1, sw())));
                box.y = Math.max(0, Math.min(100, y * 100.0 / Math.max(1, sh())));
            }, (step, shift) -> {
                if (shift) {
                    box.width = Math.max(80, Math.min(600, box.width + step * 10));
                } else {
                    box.lines = Math.max(1, Math.min(30, box.lines + step));
                }
            }, null));
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

        Box(Function<DrawContext, int[]> draw, MoveTo move, Resize resize, Runnable reset) {
            super(0, 0, 0, 0, Text.empty());
            this.draw = draw;
            this.move = move;
            this.resize = resize;
            this.reset = reset;
        }

        @Override
        protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
            int[] r = draw.apply(ctx);
            if (r == null) {
                setDimensions(0, 0);
                return;
            }
            now = r;
            setX(r[0]);
            setY(r[1]);
            setDimensions(r[2], r[3]);
            ctx.drawStrokedRectangle(r[0], r[1], r[2], r[3], isHovered() || isFocused() ? Foe.ACCENT : Foe.FAINT);
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
            grabX = (int) click.x() - getX();
            grabY = (int) click.y() - getY();
        }

        @Override
        protected void onDrag(Click click, double dx, double dy) {
            if (click.button() == 0 && now != null) {
                move.to((int) click.x() - grabX, (int) click.y() - grabY, now);
            }
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
