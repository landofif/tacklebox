package dev.landofif.tacklebox.screen;

import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.narration.NarrationMessageBuilder;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.text.Text;

import java.util.function.IntSupplier;

/** A clickable square of colour (outlined white when hovered). */
public class SwatchButton extends ClickableWidget {
    private final IntSupplier rgb;
    private final Runnable onPress;

    public SwatchButton(int x, int y, int width, int height, IntSupplier rgb, Runnable onPress) {
        super(x, y, width, height, Text.empty());
        this.rgb = rgb;
        this.onPress = onPress;
    }

    @Override
    protected void renderWidget(DrawContext ctx, int mouseX, int mouseY, float delta) {
        int x = getX();
        int y = getY();
        ctx.fill(x, y, x + width, y + height, isSelected() ? 0xFFFFFFFF : 0xFF000000);
        ctx.fill(x + 1, y + 1, x + width - 1, y + height - 1, 0xFF000000 | rgb.getAsInt());
    }

    @Override
    public void onClick(Click click, boolean doubled) {
        onPress.run();
    }

    @Override
    protected void appendClickableNarrations(NarrationMessageBuilder builder) {
        appendDefaultNarrations(builder);
    }
}
