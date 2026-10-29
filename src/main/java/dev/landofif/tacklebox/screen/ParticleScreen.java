package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.display.ParticleSwitches;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Particle switches: one chip per particle type seen this session (plus rain and the ones already off), green is
 * shown and grey is switched off, with how many were made so far. Click a chip to switch it.
 */
public class ParticleScreen extends TackleboxScreen {
    private static final int LEFT = 14, TOP = 34, CHIP_W = 112, CHIP_H = 16, GAP = 4;

    private final Screen parent;
    private final TackleboxSettings s = TackleboxSettings.get();

    public ParticleScreen(Screen parent) {
        super(Foe.text("tacklebox · particles"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int columns = Math.max(1, (width - 2 * LEFT + GAP) / (CHIP_W + GAP));
        List<String> ids = ParticleSwitches.list(ParticleSwitches.seenIds(), s.hiddenParticles);
        for (int i = 0; i < ids.size(); i++) {
            String id = ids.get(i);
            ButtonWidget chip = ButtonWidget.builder(Text.empty(), b -> {
                ParticleSwitches.toggle(s.hiddenParticles, id);
                label(b, id);
            }).dimensions(LEFT + i % columns * (CHIP_W + GAP), TOP + i / columns * (CHIP_H + GAP), CHIP_W, CHIP_H).build();
            label(chip, id);
            addDrawableChild(chip);
        }
    }

    private void label(ButtonWidget b, String id) {
        boolean on = !s.hiddenParticles.contains(id);
        int count = ParticleSwitches.count(id);
        String name = id.startsWith("minecraft:") ? id.substring("minecraft:".length()) : id;
        String text = Foe.smallCaps(name.replace('_', ' ')) + (count > 0 ? " " + count(count) : "");
        b.setMessage(Text.literal(text).formatted(on ? Formatting.GREEN : Formatting.DARK_GRAY));
    }

    private static String count(int n) {
        return n < 1000 ? String.valueOf(n) : n < 1_000_000 ? n / 1000 + "k" : n / 1_000_000 + "m";
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        List<OrderedText> hint = textRenderer.wrapLines(Foe.sentence("Click a particle to switch it off or back on. The list has every particle seen since the game started, with how many, so open it after the effect you want gone. A switched off particle is hidden for everyone, your own too."), width - 2 * LEFT);
        for (int i = 0; i < hint.size(); i++) {
            ctx.drawTextWithShadow(textRenderer, hint.get(i), LEFT, height - 6 - (hint.size() - i) * 10, Foe.FAINT);
        }
    }

    @Override
    public void close() {
        TackleboxSettings.save();
        client.setScreen(parent);
    }
}
