package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.hud.CustomHud;
import dev.landofif.dofisaddons.hud.HudRenderer;
import dev.landofif.dofisaddons.hud.HudStore;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/** Drag HUD boxes into place and scroll on one to resize it. Shows the game behind them. */
public class HudMoveScreen extends DofiScreen {
    private final Screen parent;
    /** null = every HUD in the store (saved on Done); otherwise just these drafts. */
    private final List<CustomHud> only;
    private final Map<CustomHud, int[]> boxes = new IdentityHashMap<>();
    private CustomHud dragging;
    private double grabX;
    private double grabY;

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
        context.drawCenteredTextWithShadow(textRenderer, Mono.text("scroll on a box to resize · esc when done").formatted(Formatting.GRAY), this.width / 2, 19, 0xFFFFFFFF);
        super.render(context, mouseX, mouseY, delta);
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
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        CustomHud h = hit(mouseX, mouseY);
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
        }
        client.setScreen(parent);
    }
}
