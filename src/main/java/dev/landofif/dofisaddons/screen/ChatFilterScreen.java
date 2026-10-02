package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.chat.ChatFilter;
import dev.landofif.dofisaddons.chat.ChatTweaks;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.trigger.Trigger;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.ClickableWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.List;

/**
 * Chat: rules that hide lines from chat, a field to test a line against them, and the way to move the
 * chat box and turn right click copy on or off.
 */
public class ChatFilterScreen extends DofiScreen {
    private static final int MODE_W = 74;
    private static final int SCOPE_W = 74;

    private final Screen parent;
    private final DofiSettings s = StatsTracker.settings();
    private ChatFilter focusOnOpen;
    /** Focus for a new rule's field, applied next tick (a click focuses the button it hit afterwards). */
    private RowListWidget.Row focusRow;
    private TextFieldWidget focusField;
    private RowListWidget list;
    private TextFieldWidget test;
    private String testValue = "";

    public ChatFilterScreen(Screen parent) {
        this(parent, null);
    }

    /** Opens with {@code newRule} added (shift + right click on a chat line). */
    public ChatFilterScreen(Screen parent, ChatFilter newRule) {
        super(Mono.text("dofis addons · chat"));
        this.parent = parent;
        if (newRule != null) {
            s.chatFilters.add(newRule);
            StatsTracker.saveSettings();
            focusOnOpen = newRule;
        }
    }

    @Override
    protected void init() {
        int w = Math.min(440, width - 20);
        int left = (width - w) / 2;
        int top = 40;
        int bottom = height - 80;
        list = new RowListWidget(client, left, top, w, bottom - top, 24);
        addDrawableChild(list);
        rebuild();

        test = new TextFieldWidget(textRenderer, left + 2, height - 72, w - 16, 18, Mono.text("test with"));
        test.setMaxLength(512);
        test.setPlaceholder(Mono.text("paste a chat line to see if a rule hides it").formatted(Formatting.DARK_GRAY));
        test.setText(testValue);
        test.setChangedListener(v -> testValue = v);
        addDrawableChild(test);

        int bw = (w - 12) / 4;
        int y = height - 28;
        addDrawableChild(ButtonWidget.builder(Mono.text("+ hide rule"), b -> {
            ChatFilter f = new ChatFilter();
            s.chatFilters.add(f);
            focusOnOpen = f;
            rebuild();
            list.setScrollY(list.getMaxScrollY());
        }).dimensions(left, y, bw, 20).tooltip(Tooltip.of(Mono.text("add a rule. use * for anything, like: tackle shop > *restocked*"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("move chat"), b -> {
            StatsTracker.saveSettings();
            client.setScreen(new HudMoveScreen(this));
        }).dimensions(left + (bw + 4), y, bw, 20).tooltip(Tooltip.of(Mono.text("drag the chat box anywhere, right click it to put it back"))).build());
        addDrawableChild(ButtonWidget.builder(copyLabel(), b -> {
            s.chatCopy = !s.chatCopy;
            b.setMessage(copyLabel());
        }).dimensions(left + (bw + 4) * 2, y, bw, 20)
                .tooltip(Tooltip.of(Mono.text("with chat open, right click a line to copy it. shift + right click adds a hide rule for it."))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("done"), b -> close()).dimensions(left + (bw + 4) * 3, y, bw, 20).build());
    }

    private Text copyLabel() {
        return Mono.text("copy: ").append(Mono.text(s.chatCopy ? "on" : "off").formatted(s.chatCopy ? Formatting.GREEN : Formatting.RED));
    }

    private void rebuild() {
        double scroll = list.getScrollY();
        list.clearRows();
        int rowW = list.getRowWidth();
        int fieldW = Math.max(60, rowW - 20 - MODE_W - SCOPE_W - 20 - 2 * 4);
        for (ChatFilter f : s.chatFilters) {
            ButtonWidget toggle = ButtonWidget.builder(dot(f), b -> {
                f.enabled = !f.enabled;
                b.setMessage(dot(f));
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Mono.text("switch this rule on or off"))).build();

            TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, fieldW, 20, Mono.text("pattern"));
            field.setMaxLength(512);
            field.setText(f.pattern);
            field.setPlaceholder(Mono.text("chat text to hide").formatted(Formatting.DARK_GRAY));
            field.setChangedListener(v -> {
                f.pattern = v;
                field.setTooltip(Tooltip.of(fieldTip(f)));
            });
            field.setTooltip(Tooltip.of(fieldTip(f)));

            ButtonWidget mode = ButtonWidget.builder(Mono.text(modeLabel(f.mode)), b -> {
                Trigger.MatchMode[] modes = Trigger.MatchMode.values();
                f.mode = modes[((f.mode == null ? 0 : f.mode.ordinal()) + 1) % modes.length];
                b.setMessage(Mono.text(modeLabel(f.mode)));
                field.setTooltip(Tooltip.of(fieldTip(f)));
            }).dimensions(0, 0, MODE_W, 20).tooltip(Tooltip.of(Mono.text(
                    "contains: the line has this text somewhere (* = anything)\nwhole line: the line is exactly this\nregex: a java regular expression"))).build();

            ButtonWidget scope = ButtonWidget.builder(Mono.text(f.fishOnMcOnly ? "fishonmc" : "everywhere"), b -> {
                f.fishOnMcOnly = !f.fishOnMcOnly;
                b.setMessage(Mono.text(f.fishOnMcOnly ? "fishonmc" : "everywhere"));
            }).dimensions(0, 0, SCOPE_W, 20).tooltip(Tooltip.of(Mono.text("hide only on fishonmc, or on every server"))).build();

            ButtonWidget delete = ButtonWidget.builder(Mono.text("✕").formatted(Formatting.RED), b -> {
                s.chatFilters.remove(f);
                rebuild();
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Mono.text("delete this rule"))).build();

            RowListWidget.Row row = new RowListWidget.Row(List.<ClickableWidget>of(toggle, field, mode, scope, delete));
            list.addRow(row);
            if (f == focusOnOpen) {
                focusRow = row;
                focusField = field;
                focusOnOpen = null;
            }
        }
        list.setScrollY(scroll);
    }

    @Override
    public void tick() {
        if (focusRow != null) {
            // type straight into the new rule
            setFocused(list);
            list.setFocused(focusRow);
            focusRow.setFocused(focusField);
            focusRow = null;
            focusField = null;
        }
    }

    private static Text dot(ChatFilter f) {
        return Mono.text(f.enabled ? "●" : "○").formatted(f.enabled ? Formatting.GREEN : Formatting.DARK_GRAY);
    }

    private static String modeLabel(Trigger.MatchMode mode) {
        return switch (mode == null ? Trigger.MatchMode.SIMPLE : mode) {
            case SIMPLE -> "contains";
            case EXACT -> "whole line";
            case REGEX -> "regex";
        };
    }

    private static Text fieldTip(ChatFilter f) {
        String error = ChatTweaks.matcher(f).error();
        if (error != null && !f.pattern.isBlank()) {
            return Mono.text(error).formatted(Formatting.RED);
        }
        return Mono.text("hid " + ChatTweaks.hidden(f) + " lines this session");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.drawCenteredTextWithShadow(textRenderer, Mono.text("hide chat lines you dont want to see. * matches anything").withColor(S24.TEXT_3 & 0xFFFFFF),
                width / 2, 23, 0xFFFFFFFF);
        if (s.chatFilters.isEmpty()) {
            ctx.drawCenteredTextWithShadow(textRenderer, Mono.text("no rules yet. click + hide rule, or shift + right click a line in chat.")
                    .withColor(S24.TEXT_4 & 0xFFFFFF), width / 2, 70, 0xFFFFFFFF);
        }
        if (!testValue.isBlank()) {
            ChatFilter hit = ChatTweaks.hiddenBy(testValue, true, false);
            Text result = hit == null
                    ? Mono.text("shown: no rule matches this line").formatted(Formatting.GRAY)
                    : Mono.text("hidden by: " + hit.pattern).formatted(Formatting.GREEN);
            ctx.drawText(textRenderer, result, test.getX(), test.getY() + 22, 0xFFFFFFFF, false);
        }
    }

    @Override
    public void close() {
        s.chatFilters.removeIf(f -> f.pattern == null || f.pattern.isBlank());
        StatsTracker.saveSettings();
        client.setScreen(parent);
    }
}
