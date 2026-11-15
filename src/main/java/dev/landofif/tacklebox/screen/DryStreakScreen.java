package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.chat.DryStreaks;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;

import java.util.List;

/** The dry streak chat messages (chat/DryStreaks): per kind a switch, its text and a button back to the default text. */
public class DryStreakScreen extends TackleboxScreen {
    private final Screen parent;
    private final TackleboxSettings s = TackleboxSettings.get();

    public DryStreakScreen(Screen parent) {
        super(Foe.text("tacklebox · dry streak messages"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(520, width - 20);
        int left = (width - w) / 2;
        RowListWidget list = new RowListWidget(client, left, 52, w, height - 52 - 36, 24);
        addDrawableChild(list);
        int fieldW = Math.max(60, list.getRowWidth() - 90 - 50 - 2 * 4);
        for (DryStreaks.Kind k : DryStreaks.KINDS) {
            ButtonWidget on = ButtonWidget.builder(label(k), b -> {
                if (!s.dryOn.remove(k.key())) {
                    s.dryOn.add(k.key());
                }
                b.setMessage(label(k));
            }).dimensions(0, 0, 90, 20).build();
            TextFieldWidget text = new TextFieldWidget(textRenderer, 0, 0, fieldW, 20, Foe.text(k.label()));
            text.setMaxLength(256);
            text.setText(DryStreaks.template(s.dryText, k.key()));
            text.setChangedListener(v -> {
                if (v.isBlank() || v.equals(k.template())) {
                    s.dryText.remove(k.key());
                } else {
                    s.dryText.put(k.key(), v);
                }
            });
            ButtonWidget reset = ButtonWidget.builder(Foe.text("default"), b -> text.setText(k.template())).dimensions(0, 0, 50, 20).build();
            list.addRow(new RowListWidget.Row(List.<ClickableWidget>of(on, text, reset)));
        }
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(left, height - 28, w, 20).build());
    }

    private net.minecraft.text.Text label(DryStreaks.Kind k) {
        return Foe.text(k.label() + ": " + (s.dryOn.contains(k.key()) ? "on" : "off"));
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.sentence("a chat line when the drop lands. %item% %streak% %count% %variant% %rarity%")
                .withColor(Foe.DIM & 0xFFFFFF), width / 2, 23, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.sentence("colours: &a to &f, &0 to &9, &#rrggbb, &l bold. an empty text gives the default back")
                .withColor(Foe.DIM & 0xFFFFFF), width / 2, 35, 0xFFFFFFFF);
    }

    @Override
    public void close() {
        TackleboxSettings.save();
        client.setScreen(parent);
    }
}
