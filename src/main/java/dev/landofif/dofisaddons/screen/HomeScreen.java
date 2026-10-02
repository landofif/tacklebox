package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.hud.HudStore;
import dev.landofif.dofisaddons.stats.StatNames;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.trigger.TriggerStore;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** /dofi: one place to reach every part of the mod. */
public class HomeScreen extends DofiScreen {
    private final Screen parent;

    public HomeScreen(Screen parent) {
        super(Mono.text("dofis addons"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 220;
        int x = (this.width - w) / 2;
        int y = this.height / 2 - 52;
        addDrawableChild(ButtonWidget.builder(entry("chat triggers", TriggerStore.all().size() + " set up"),
                b -> client.setScreen(new TriggerListScreen(this))).dimensions(x, y, w, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(entry("custom huds", HudStore.all().stream().filter(h -> h.enabled).count() + " shown"),
                b -> client.setScreen(new HudListScreen(this))).dimensions(x, y, w, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(entry("stats", StatNames.number(StatsTracker.session().fish) + " fish this session"),
                b -> client.setScreen(new StatsScreen(this))).dimensions(x, y, w, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(entry("settings", "level, bobber timer, clock"),
                b -> client.setScreen(new SettingsScreen(this))).dimensions(x, y, w, 20).build());
        y += 32;
        addDrawableChild(ButtonWidget.builder(Mono.text("done"), b -> close()).dimensions(x + w / 4, y, w / 2, 20).build());
    }

    private static Text entry(String name, String detail) {
        return Mono.text(name).append(Mono.text("  " + detail).formatted(Formatting.DARK_GRAY));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        int y = this.height / 2 - 84;
        context.drawCenteredTextWithShadow(textRenderer, Mono.bold(this.title.getString()).withColor(S24.ACCENT & 0xFFFFFF), this.width / 2, y, 0xFFFFFFFF);
        context.drawCenteredTextWithShadow(textRenderer, Mono.text("fishonmc addons · display only, no automation").formatted(Formatting.GRAY),
                this.width / 2, y + 12, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
