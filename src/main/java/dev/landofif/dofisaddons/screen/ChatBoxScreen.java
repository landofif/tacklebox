package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.compat.FoeScreens;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.chat.ChatBox;
import dev.landofif.dofisaddons.ui.DofiScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Chat boxes: separate panels for dms, crew chat or anything with a rule. Each row turns a box on or off,
 * names it, opens its rules, sets when it fades, or deletes it. Move them in the move screen.
 */
public class ChatBoxScreen extends DofiScreen {
    private static final int RULES_W = 84;
    private static final int FADE_W = 84;
    private static final int[] FADES = {-1, 10, 30, 60, 300};

    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();
    private RowListWidget list;

    public ChatBoxScreen(Screen parent) {
        super(Foe.text("dofis addons · chat boxes"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(440, width - 20);
        int left = (width - w) / 2;
        list = new RowListWidget(client, left, 40, w, height - 40 - 36, 24);
        addDrawableChild(list);
        rebuild();

        int bw = (w - 8) / 3;
        int y = height - 28;
        addDrawableChild(ButtonWidget.builder(Foe.text("+ box"), b -> {
            ChatBox box = new ChatBox();
            box.name = "box " + (s.chatBoxes.size() + 1);
            s.chatBoxes.add(box);
            client.setScreen(new ChatFilterScreen(this, box));
        }).dimensions(left, y, bw, 20).tooltip(Tooltip.of(Foe.sentence("a new box. you pick which lines go in it next"))).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("move boxes"), b -> FoeScreens.openMover(this)).dimensions(left + bw + 4, y, bw, 20)
                .tooltip(Tooltip.of(Foe.sentence("drag the boxes anywhere. scroll on one for more or fewer lines, shift + scroll for its width"))).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(left + (bw + 4) * 2, y, bw, 20).build());
    }

    private void rebuild() {
        double scroll = list.getScrollY();
        list.clearRows();
        int rowW = list.getRowWidth();
        int nameW = Math.max(60, rowW - 20 - RULES_W - FADE_W - 20 - 4 * 4);
        for (ChatBox box : s.chatBoxes) {
            ButtonWidget toggle = ButtonWidget.builder(dot(box), b -> {
                box.enabled = !box.enabled;
                b.setMessage(dot(box));
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.sentence("show this box or not. while off its lines stay in chat"))).build();

            TextFieldWidget name = new TextFieldWidget(textRenderer, 0, 0, nameW, 20, Foe.text("name"));
            name.setMaxLength(24);
            name.setText(box.name);
            name.setPlaceholder(Foe.text("name").formatted(Formatting.DARK_GRAY));
            name.setChangedListener(v -> box.name = v);
            name.setTooltip(Tooltip.of(Foe.sentence("the name in the top border of the box")));

            int count = box.rules.size();
            ButtonWidget rules = ButtonWidget.builder(Foe.text(count + (count == 1 ? " rule" : " rules") + " →"),
                    b -> client.setScreen(new ChatFilterScreen(this, box))).dimensions(0, 0, RULES_W, 20)
                    .tooltip(Tooltip.of(Foe.sentence("which chat lines go to this box"))).build();

            ButtonWidget fade = ButtonWidget.builder(fadeLabel(box), b -> {
                int i = 0;
                while (i < FADES.length && FADES[i] != box.fadeSeconds) i++;
                box.fadeSeconds = FADES[(i + 1) % FADES.length];
                b.setMessage(fadeLabel(box));
            }).dimensions(0, 0, FADE_W, 20).tooltip(Tooltip.of(Foe.sentence("hide the box this long after its last line while chat is closed"))).build();

            ButtonWidget delete = ButtonWidget.builder(Foe.text("✕").formatted(Formatting.RED), b -> {
                s.chatBoxes.remove(box);
                rebuild();
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.sentence("delete this box. its lines go back to chat"))).build();

            list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(toggle, name, rules, fade, delete)));
        }
        list.setScrollY(scroll);
    }

    private static Text dot(ChatBox box) {
        return Foe.text(box.enabled ? "●" : "○").formatted(box.enabled ? Formatting.GREEN : Formatting.DARK_GRAY);
    }

    private static Text fadeLabel(ChatBox box) {
        return Foe.text(box.fadeSeconds < 0 ? "always shown" : "fade: " + box.fadeSeconds + "s");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("lines that match a box go there instead of chat")
                .withColor(Foe.DIM & 0xFFFFFF), width / 2, 23, 0xFFFFFFFF);
        if (s.chatBoxes.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("no boxes. click + box to make one.")
                    .withColor(Foe.DIM & 0xFFFFFF), width / 2, 70, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        DofiSettings.save();
        client.setScreen(parent);
    }
}
