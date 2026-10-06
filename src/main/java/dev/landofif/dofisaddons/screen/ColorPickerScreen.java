package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.ui.DofiScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.Locale;
import java.util.function.IntConsumer;

/**
 * Pick a colour by dragging (see {@link ColorPickerWidget}), typing a hex code, or clicking one of the
 * chat colours. Done hands the colour back; Esc or Cancel leaves it unchanged.
 */
public class ColorPickerScreen extends DofiScreen {
    private static final int PICKER_W = 200;
    private static final int PICKER_H = 120;
    /** The 16 chat colours, in &0..&f order, so a click gives exactly the colour a code would. */
    private static final int[] PALETTE = {
            0x000000, 0x0000AA, 0x00AA00, 0x00AAAA, 0xAA0000, 0xAA00AA, 0xFFAA00, 0xAAAAAA,
            0x555555, 0x5555FF, 0x55FF55, 0x55FFFF, 0xFF5555, 0xFF55FF, 0xFFFF55, 0xFFFFFF};

    private final Screen parent;
    private final int original;
    private final IntConsumer onDone;
    private int rgb;
    private ColorPickerWidget picker;
    private TextFieldWidget hex;
    private boolean syncing;

    public ColorPickerScreen(Screen parent, String title, int rgb, IntConsumer onDone) {
        super(Foe.text(title));
        this.parent = parent;
        this.original = rgb & 0xFFFFFF;
        this.rgb = this.original;
        this.onDone = onDone;
    }

    @Override
    protected void init() {
        int left = (width - PICKER_W) / 2;
        int top = Math.max(30, (height - PICKER_H - 90) / 2);

        picker = new ColorPickerWidget(left, top, PICKER_W, PICKER_H, rgb, c -> {
            rgb = c;
            syncHex();
        });
        addDrawableChild(picker);

        int y = top + PICKER_H + 8;
        hex = new TextFieldWidget(textRenderer, left, y, 80, 20, Text.empty());
        hex.setMaxLength(7);
        hex.setChangedListener(s -> {
            if (syncing) {
                return;
            }
            String h = s.startsWith("#") ? s.substring(1) : s;
            if (h.length() == 6 && h.chars().allMatch(ch -> Character.digit(ch, 16) >= 0)) {
                rgb = Integer.parseInt(h, 16);
                picker.setRgb(rgb);
            }
        });
        addDrawableChild(hex);
        syncHex();

        y += 26;
        int sw = (PICKER_W + 2) / PALETTE.length;
        for (int i = 0; i < PALETTE.length; i++) {
            int c = PALETTE[i];
            addDrawableChild(new SwatchButton(left + i * sw, y, sw - 2, 12, () -> c, () -> {
                rgb = c;
                picker.setRgb(c);
                syncHex();
            }));
        }

        y += 20;
        int bw = (PICKER_W - 4) / 2;
        addDrawableChild(ButtonWidget.builder(Foe.text("cancel"), b -> close()).dimensions(left, y, bw, 20).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done").formatted(Formatting.GREEN), b -> {
            onDone.accept(rgb);
            close();
        }).dimensions(left + bw + 4, y, bw, 20).build());
    }

    private void syncHex() {
        syncing = true;
        hex.setText(String.format(Locale.ROOT, "#%06x", rgb));
        syncing = false;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        // Before / after, next to the hex field.
        int x = hex.getX() + hex.getWidth() + 8;
        int y = hex.getY();
        int w = (picker.getX() + PICKER_W - x) / 2;
        context.fill(x, y, x + w * 2, y + 20, 0xFF000000);
        context.fill(x + 1, y + 1, x + w, y + 19, 0xFF000000 | original);
        context.fill(x + w, y + 1, x + w * 2 - 1, y + 19, 0xFF000000 | rgb);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
