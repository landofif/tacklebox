package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.compat.FoeScreens;
import dev.landofif.dofisaddons.stats.StatNames;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.trigger.TriggerStore;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

/** /dofi: one place to reach every part of the mod. */
public class HomeScreen extends DofiScreen {
    private final Screen parent;

    public HomeScreen(Screen parent) {
        super(Foe.text("dofis addons"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = 220;
        int x = (this.width - w) / 2;
        int y = this.height / 2 - 90;
        addDrawableChild(ButtonWidget.builder(entry("chat triggers", TriggerStore.all().size() + " set up"),
                b -> client.setScreen(new TriggerListScreen(this))).dimensions(x, y, w, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(entry("custom huds", "foecollabs editor"),
                b -> {
                    Screen huds = FoeScreens.customHuds(this);
                    if (huds != null) client.setScreen(huds);
                }).dimensions(x, y, w, 20).build());
        y += 24;
        long rules = StatsTracker.settings().chatFilters.stream().filter(f -> f.enabled).count();
        long boxes = StatsTracker.settings().chatBoxes.stream().filter(b -> b.enabled).count();
        addDrawableChild(ButtonWidget.builder(entry("chat", rules + (rules == 1 ? " hide rule, " : " hide rules, ") + boxes + (boxes == 1 ? " box" : " boxes")),
                b -> client.setScreen(new ChatFilterScreen(this))).dimensions(x, y, w, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(entry("key commands", StatsTracker.settings().keyCommands.size() + " set up"),
                b -> client.setScreen(new KeyCommandScreen(this))).dimensions(x, y, w, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(entry("stats", StatNames.number(StatsTracker.session().fish) + " fish this session"),
                b -> client.setScreen(new StatsScreen(this))).dimensions(x, y, w, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(entry("settings", "foecollab and dofis addons"),
                b -> client.setScreen(SettingsScreen.dofis(this))).dimensions(x, y, w, 20).build());
        y += 24;
        addDrawableChild(ButtonWidget.builder(entry("move huds", "foecollabs mover"), b -> FoeScreens.openMover(this)).dimensions(x, y, w, 20).build());
        y += 32;
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(x + w / 4, y, w / 2, 20).build());
    }

    private static Text entry(String name, String detail) {
        return Foe.text(name).append(Foe.text("  " + detail).formatted(Formatting.DARK_GRAY));
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer, Foe.text("fishonmc addons for foecollab · display only, no automation").formatted(Formatting.GRAY),
                this.width / 2, this.height / 2 - 106, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
