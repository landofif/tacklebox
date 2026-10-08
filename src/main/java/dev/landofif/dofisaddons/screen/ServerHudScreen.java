package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.hud.ServerHud;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/**
 * Which lines the server hud shows: one chip per line (green is on), the picture's opacity, and the hud itself on the
 * right as it looks with the choices, so there is nothing to guess.
 */
public class ServerHudScreen extends DofiScreen {
    private static final int LEFT = 14, TOP = 34, CHIP_W = 118, CHIP_H = 20, GAP = 6, COLUMNS = 3;

    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();

    public ServerHudScreen(Screen parent) {
        super(Foe.text("dofis addons · server hud lines"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int i = 0;
        for (String[] line : ServerHud.LINES) {
            String key = line[0];
            ButtonWidget chip = ButtonWidget.builder(Text.empty(), b -> {
                if (!s.serverHudHide.remove(key)) {
                    s.serverHudHide.add(key);
                }
                label(b, key, line[1]);
            }).dimensions(LEFT + i % COLUMNS * (CHIP_W + GAP), TOP + i / COLUMNS * (CHIP_H + GAP), CHIP_W, CHIP_H).build();
            label(chip, key, line[1]);
            addDrawableChild(chip);
            i++;
        }
        int y = TOP + (i + COLUMNS - 1) / COLUMNS * (CHIP_H + GAP) + 10;
        int full = COLUMNS * CHIP_W + (COLUMNS - 1) * GAP;
        addDrawableChild(new ValueSlider(LEFT, y, full, 20, "picture opacity", 0, 100, 5, "%", s.serverHudImage, v -> s.serverHudImage = (int) v));
        addDrawableChild(ButtonWidget.builder(Foe.text("open picture folder"), b -> {
            try {
                java.nio.file.Files.createDirectories(ServerHud.IMAGE.getParent());
            } catch (java.io.IOException ignored) {
            }
            net.minecraft.util.Util.getOperatingSystem().open(ServerHud.IMAGE.getParent());
        }).dimensions(LEFT, y + 26, full, 20).build());
    }

    private void label(ButtonWidget b, String key, String name) {
        boolean on = !s.serverHudHide.contains(key);
        b.setMessage(Text.literal(Foe.smallCaps(name)).formatted(on ? Formatting.GREEN : Formatting.DARK_GRAY));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int x = LEFT + COLUMNS * (CHIP_W + GAP) + 24;
        ctx.drawTextWithShadow(textRenderer, Foe.text("preview"), x, TOP - 12, Foe.DIM);
        ServerHud.draw(ctx, x, TOP);
        ctx.drawTextWithShadow(textRenderer, Foe.sentence("Prospect has two chips: the number and the 1/x chance. Luck, scale, speeds and prospect fill in when you open /menu."),
                LEFT, height - 16, Foe.FAINT);
    }

    @Override
    public void close() {
        DofiSettings.save();
        client.setScreen(parent);
    }
}
