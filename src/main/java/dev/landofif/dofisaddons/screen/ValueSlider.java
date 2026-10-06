package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;

import java.util.function.DoubleConsumer;
import java.util.function.DoubleFunction;

/** A slider over [min, max] that snaps to {@code step} and shows "Label: value". */
public class ValueSlider extends SliderWidget {
    private final String label;
    private final double min;
    private final double max;
    private final double step;
    private final String unit;
    private final DoubleConsumer onChange;
    private DoubleFunction<String> format;

    public ValueSlider(int x, int y, int width, int height, String label, double min, double max, double step,
                       String unit, double initial, DoubleConsumer onChange) {
        super(x, y, width, height, Text.empty(), (clamp(initial, min, max) - min) / (max - min));
        this.label = label;
        this.min = min;
        this.max = max;
        this.step = step;
        this.unit = unit;
        this.onChange = onChange;
        updateMessage();
    }

    /** Shows the value as {@code format(value)} instead of number + unit (e.g. "never" for 0). */
    public ValueSlider format(DoubleFunction<String> format) {
        this.format = format;
        updateMessage();
        return this;
    }

    public double getValue() {
        double raw = min + this.value * (max - min);
        return clamp(Math.round(raw / step) * step, min, max);
    }

    @Override
    protected void updateMessage() {
        double v = getValue();
        String prefix = label.isEmpty() ? "" : label + ": ";
        if (format != null) {
            setMessage(Foe.text(prefix + format.apply(v)));
            return;
        }
        String shown = step >= 1 ? String.valueOf((long) v) : String.format(java.util.Locale.ROOT, step >= 0.1 ? "%.1f" : "%.2f", v);
        setMessage(Foe.text(prefix + shown + unit));
    }

    @Override
    protected void applyValue() {
        onChange.accept(getValue());
    }

    private static double clamp(double v, double min, double max) {
        return Math.max(min, Math.min(max, v));
    }
}
