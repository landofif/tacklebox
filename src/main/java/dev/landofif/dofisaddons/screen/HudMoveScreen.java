package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.chat.ChatBox;
import dev.landofif.dofisaddons.chat.ChatBoxes;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.hud.CustomHud;
import dev.landofif.dofisaddons.hud.HudRenderer;
import dev.landofif.dofisaddons.hud.HudStore;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.option.GameOptions;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Drag HUD boxes into place and scroll on one to resize it. When moving every HUD, the chat box and the
 * separate chat boxes can be dragged too (right click the chat box to put it back; scroll a chat box for
 * more or fewer lines, shift + scroll for its width). Shows the game behind them.
 */
public class HudMoveScreen extends DofiScreen {
    private final Screen parent;
    /** null = every HUD in the store (saved on Done); otherwise just these drafts. */
    private final List<CustomHud> only;
    private final Map<CustomHud, int[]> boxes = new IdentityHashMap<>();
    private CustomHud dragging;
    private double grabX;
    private double grabY;
    private boolean draggingChat;
    private final Map<ChatBox, int[]> chatBoxes = new IdentityHashMap<>();
    private ChatBox draggingBox;

    public HudMoveScreen(Screen parent) {
        this(parent, null);
    }

    public HudMoveScreen(Screen parent, List<CustomHud> only) {
        super(Mono.text("move huds"));
        this.parent = parent;
        this.only = only;
    }

    private List<CustomHud> targets() {
        if (only != null) {
            return only;
        }
        List<CustomHud> out = new ArrayList<>();
        for (CustomHud h : HudStore.all()) {
            if (h.enabled) out.add(h);
        }
        return out;
    }

    @Override
    protected void init() {
        addDrawableChild(ButtonWidget.builder(Mono.text("done").formatted(Formatting.GREEN), b -> close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    @Override
    protected boolean framed() {
        return false;
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // No blur or dim: you see the HUDs exactly as in game.
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        boxes.clear();
        chatBoxes.clear();
        if (only == null) {
            renderChatBox(context, mouseX, mouseY); // under the hud boxes, which also win clicks
            for (ChatBox box : StatsTracker.settings().chatBoxes) {
                if (!box.enabled) continue;
                if (box == draggingBox) {
                    box.x = Math.max(0, Math.min(100, (mouseX - grabX) * 100.0 / Math.max(1, this.width)));
                    box.y = Math.max(0, Math.min(100, (mouseY - grabY + 5) * 100.0 / Math.max(1, this.height)));
                }
                int[] r = ChatBoxes.rect(box, this.width, this.height);
                chatBoxes.put(box, ChatBoxes.draw(context, box, box == draggingBox || over(r, mouseX, mouseY)));
            }
        }
        for (CustomHud hud : targets()) {
            if (hud == dragging) {
                moveTo(hud, mouseX - grabX, mouseY - grabY);
            }
            HudRenderer.Resolved lines = HudRenderer.resolve(hud);
            if (lines.isEmpty()) {
                lines = HudRenderer.Resolved.of(Mono.text(hud.name + " (nothing to show now)").formatted(Formatting.GRAY));
            }
            boxes.put(hud, HudRenderer.draw(context, hud, lines, true));
        }
        context.fill(0, 4, this.width, 30, 0x90000000);
        context.drawCenteredTextWithShadow(textRenderer, Mono.text("drag a box to move it").formatted(Formatting.WHITE), this.width / 2, 8, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Mono.text(only == null
                ? "scroll: size · shift + scroll: box width · right click chat: reset"
                : "scroll on a box to resize · esc when done").formatted(Formatting.GRAY), this.width / 2, 19, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }

    /** Where the chat box is now: x, y, width, height in gui units (mirrors ChatHud's own layout). */
    private int[] chatBox() {
        GameOptions o = client.options;
        DofiSettings s = StatsTracker.settings();
        double scale = o.getChatScale().getValue();
        int lineH = (int) (9 * (o.getChatLineSpacing().getValue() + 1.0));
        int lines = Math.max(1, ChatHud.getHeight(o.getChatHeightUnfocused().getValue()) / lineH);
        int w = (int) Math.ceil((Math.ceil(ChatHud.getWidth(o.getChatWidth().getValue()) / scale) + 12) * scale);
        int h = (int) Math.ceil(lines * lineH * scale);
        int bottom = (int) (Math.floor((this.height - 40) / scale) * scale);
        return new int[]{s.chatX, bottom - h + s.chatY, w, h};
    }

    private void renderChatBox(DrawContext ctx, int mouseX, int mouseY) {
        if (draggingChat) {
            int[] at = chatBox();
            DofiSettings s = StatsTracker.settings();
            int nx = (int) Math.round(mouseX - grabX);
            int ny = (int) Math.round(mouseY - grabY);
            s.chatX = Math.max(0, Math.min(this.width - at[2], nx));
            s.chatY += Math.max(0, Math.min(this.height - at[3], ny)) - at[1];
        }
        int[] b = chatBox();
        boolean hot = draggingChat || over(b, mouseX, mouseY);
        ctx.fill(b[0], b[1], b[0] + b[2], b[1] + b[3], S24.alpha(0x50, S24.BG_4));
        S24.outline(ctx, b[0], b[1], b[2], b[3], hot ? S24.ACCENT : S24.TEXT_5);
        Text label = Mono.text("chat");
        ctx.fill(b[0] + 6, b[1], b[0] + 12 + textRenderer.getWidth(label), b[1] + 1, S24.alpha(0x50, S24.BG_4));
        ctx.drawText(textRenderer, label, b[0] + 9, b[1] - 4, hot ? S24.ACCENT : S24.TEXT_3, false);
    }

    /** Places the box's top-left corner at (left, top), storing it as the HUD's anchor in %. */
    private void moveTo(CustomHud hud, double left, double top) {
        int[] box = boxes.getOrDefault(hud, new int[]{0, 0, 0, 0});
        double anchorX = switch (hud.align) {
            case LEFT -> left;
            case CENTER -> left + box[2] / 2.0;
            case RIGHT -> left + box[2];
        };
        hud.x = Math.max(0, Math.min(100, anchorX * 100.0 / Math.max(1, this.width)));
        hud.y = Math.max(0, Math.min(100, top * 100.0 / Math.max(1, this.height)));
    }

    private static boolean over(int[] b, double x, double y) {
        return b != null && x >= b[0] && x < b[0] + b[2] && y >= b[1] && y < b[1] + b[3];
    }

    private ChatBox boxAt(double x, double y) {
        for (Map.Entry<ChatBox, int[]> e : chatBoxes.entrySet()) {
            if (over(e.getValue(), x, y)) return e.getKey();
        }
        return null;
    }

    private CustomHud hit(double x, double y) {
        List<CustomHud> list = targets();
        for (int i = list.size() - 1; i >= 0; i--) {
            if (over(boxes.get(list.get(i)), x, y)) return list.get(i);
        }
        return null;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        CustomHud h = hit(click.x(), click.y());
        ChatBox cb = h == null ? boxAt(click.x(), click.y()) : null;
        if (cb != null) {
            int[] b = chatBoxes.get(cb);
            draggingBox = cb;
            grabX = click.x() - b[0];
            grabY = click.y() - b[1];
            return true;
        }
        if (h == null && only == null && over(chatBox(), click.x(), click.y())) {
            if (click.button() == 1) {
                StatsTracker.settings().chatX = 0;
                StatsTracker.settings().chatY = 0;
            } else {
                int[] b = chatBox();
                draggingChat = true;
                grabX = click.x() - b[0];
                grabY = click.y() - b[1];
            }
            return true;
        }
        if (h != null) {
            int[] b = boxes.get(h);
            dragging = h;
            grabX = click.x() - b[0];
            grabY = click.y() - b[1];
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = null;
        draggingChat = false;
        draggingBox = null;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        CustomHud h = hit(mouseX, mouseY);
        ChatBox cb = h == null ? boxAt(mouseX, mouseY) : null;
        if (cb != null && verticalAmount != 0) {
            int step = verticalAmount > 0 ? 1 : -1;
            if (client.isShiftPressed()) {
                cb.width = Math.max(80, Math.min(600, cb.width + step * 10));
            } else {
                cb.lines = Math.max(1, Math.min(30, cb.lines + step));
            }
            return true;
        }
        if (h != null && verticalAmount != 0) {
            h.scale = Math.max(0.5, Math.min(3, Math.round((h.scale + Math.signum(verticalAmount) * 0.1) * 10) / 10.0));
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
    }

    @Override
    public void close() {
        if (only == null) {
            HudStore.save();
            StatsTracker.saveSettings();
        }
        client.setScreen(parent);
    }
}
