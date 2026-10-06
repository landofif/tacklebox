package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.input.KeyCommands;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.client.util.InputUtil;
import org.lwjgl.glfw.GLFW;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/** Key commands: pick a key and the command it runs when you press it in game. */
public class KeyCommandScreen extends DofiScreen {
    private static final int KEY_W = 90;

    private final Screen parent;
    private final DofiSettings s = StatsTracker.settings();
    private RowListWidget list;
    private KeyCommands.Entry listening;

    public KeyCommandScreen(Screen parent) {
        super(Mono.text("dofis addons · key commands"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(440, width - 20);
        int left = (width - w) / 2;
        list = new RowListWidget(client, left, 40, w, height - 40 - 36, 24);
        addDrawableChild(list);
        rebuild();
        int bw = (w - 4) / 2;
        int y = height - 28;
        addDrawableChild(ButtonWidget.builder(Mono.text("+ key"), b -> {
            s.keyCommands.add(new KeyCommands.Entry());
            rebuild();
        }).dimensions(left, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("done"), b -> close()).dimensions(left + bw + 4, y, bw, 20).build());
    }

    private void rebuild() {
        double scroll = list.getScrollY();
        list.clearRows();
        int cmdW = Math.max(60, list.getRowWidth() - KEY_W - 20 - 2 * 4);
        for (KeyCommands.Entry e : s.keyCommands) {
            ButtonWidget key = ButtonWidget.builder(keyLabel(e), b -> {
                listening = e;
                rebuild();
            }).dimensions(0, 0, KEY_W, 20).tooltip(Tooltip.of(Mono.text("click, then press the key. esc cancels, delete unbinds"))).build();

            TextFieldWidget command = new TextFieldWidget(textRenderer, 0, 0, cmdW, 20, Mono.text("command"));
            command.setMaxLength(256);
            command.setText(e.command);
            command.setPlaceholder(Mono.text("command, like /menu").formatted(Formatting.DARK_GRAY));
            command.setChangedListener(v -> e.command = v);

            ButtonWidget delete = ButtonWidget.builder(Mono.text("✕").formatted(Formatting.RED), b -> {
                s.keyCommands.remove(e);
                rebuild();
            }).dimensions(0, 0, 20, 20).build();
            list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(key, command, delete)));
        }
        list.setScrollY(scroll);
    }

    private Text keyLabel(KeyCommands.Entry e) {
        if (e == listening) {
            return Mono.text("press a key…").formatted(Formatting.YELLOW);
        }
        return e.key < 0 ? Mono.text("not set").formatted(Formatting.DARK_GRAY)
                : Mono.text(InputUtil.Type.KEYSYM.createFromCode(e.key).getLocalizedText().getString().toLowerCase());
    }

    @Override
    public boolean keyPressed(KeyInput input) {
        if (listening != null) {
            if (input.key() == GLFW.GLFW_KEY_DELETE) {
                listening.key = -1;
            } else if (input.key() != GLFW.GLFW_KEY_ESCAPE) {
                listening.key = input.key();
            }
            listening = null;
            rebuild();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Mono.text("pressing the key in game runs the command, once per press")
                .withColor(S24.TEXT_3 & 0xFFFFFF), width / 2, 23, 0xFFFFFFFF);
        if (s.keyCommands.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Mono.text("no keys yet. click + key to add one.")
                    .withColor(S24.TEXT_4 & 0xFFFFFF), width / 2, 70, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        StatsTracker.saveSettings();
        client.setScreen(parent);
    }
}
