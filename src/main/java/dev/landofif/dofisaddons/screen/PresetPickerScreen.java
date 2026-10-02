package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.trigger.TriggerPresets;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.util.Formatting;

import java.util.List;

/** "What should the new trigger start from?" Blank, or one of the FishOnMC presets. */
public class PresetPickerScreen extends DofiScreen {
    private final TriggerListScreen parent;

    public PresetPickerScreen(TriggerListScreen parent) {
        super(Mono.text("New trigger"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int listWidth = Math.min(320, this.width - 20);
        int x = (this.width - listWidth) / 2;
        RowListWidget list = new RowListWidget(this.client, x, 36, listWidth, this.height - 36 - 34, 24);
        addDrawableChild(list);
        for (TriggerPresets.Preset preset : TriggerPresets.all()) {
            ButtonWidget b = ButtonWidget.builder(Mono.text(preset.name()),
                            btn -> client.setScreen(new TriggerEditScreen(parent, preset.create().get(), true)))
                    .dimensions(0, 0, list.getRowWidth(), 20)
                    .tooltip(Tooltip.of(Mono.text(preset.description())))
                    .build();
            list.addRow(new RowListWidget.Row(List.of(b)));
        }
        addDrawableChild(ButtonWidget.builder(Mono.text("Back"), b -> close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawCenteredTextWithShadow(textRenderer,
                Mono.text("pick a starting point. you can change everything after.").formatted(Formatting.GRAY),
                this.width / 2, 22, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
