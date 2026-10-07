package dev.landofif.dofisaddons.compat;

import me.shedaniel.clothconfig2.gui.entries.TooltipListEntry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.Element;
import net.minecraft.client.gui.Selectable;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import java.util.function.IntSupplier;

/**
 * A cloth config row with a label and one button: either a colour (a swatch, and a click opens the mods draggable
 * {@code ColorPickerScreen}) or a plain action (opens one of the mods screens). Cloth has neither built in.
 */
public class ButtonEntry extends TooltipListEntry<Integer> {
    private final ButtonWidget button;
    private final boolean colour;
    private final int original;
    private int value;

    private ButtonEntry(Text name, int value, boolean colour, Text buttonText, Consumer<ButtonEntry> press, Consumer<Integer> save) {
        super(name, () -> Optional.empty());
        this.value = value;
        this.original = value;
        this.colour = colour;
        this.saveCallback = save;
        this.button = ButtonWidget.builder(buttonText, b -> press.accept(this)).dimensions(0, 0, colour ? 130 : 150, 20).build();
        if (colour) {
            button.setMessage(hex());
        }
    }

    /** A colour row; {@code set} gets the RGB when the cloth screen is saved. */
    public static ButtonEntry color(Text name, IntSupplier get, IntConsumer set) {
        return new ButtonEntry(name, get.getAsInt() & 0xFFFFFF, true, Text.empty(), e -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.setScreen(new dev.landofif.dofisaddons.screen.ColorPickerScreen(mc.currentScreen, name.getString(), e.value, rgb -> {
                e.value = rgb & 0xFFFFFF;
                e.button.setMessage(e.hex());
            }));
        }, v -> set.accept(v & 0xFFFFFF));
    }

    /** A row whose button runs {@code press}. */
    public static ButtonEntry action(Text name, Text buttonText, Runnable press) {
        return new ButtonEntry(name, 0, false, buttonText, e -> press.run(), null);
    }

    private Text hex() {
        return Text.literal(String.format(java.util.Locale.ROOT, "#%06X", value));
    }

    @Override
    public void render(DrawContext ctx, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float delta) {
        super.render(ctx, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, delta);
        MinecraftClient mc = MinecraftClient.getInstance();
        ctx.drawTextWithShadow(mc.textRenderer, getDisplayedFieldName(), x, y + 6, getPreferredTextColor());
        int bx = x + entryWidth - button.getWidth();
        button.setX(bx);
        button.setY(y);
        button.render(ctx, mouseX, mouseY, delta);
        if (colour) {
            ctx.fill(bx - 22, y + 1, bx - 2, y + 19, 0xFFFFFFFF);
            ctx.fill(bx - 21, y + 2, bx - 3, y + 18, 0xFF000000 | value);
        }
    }

    @Override
    public List<? extends Element> children() {
        return List.of(button);
    }

    @Override
    public List<? extends Selectable> narratables() {
        return List.of(button);
    }

    @Override
    public Integer getValue() {
        return value;
    }

    @Override
    public Optional<Integer> getDefaultValue() {
        return Optional.empty();
    }

    @Override
    public boolean isEdited() {
        return colour && value != original;
    }
}
