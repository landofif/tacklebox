package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.compat.FoeScreens;
import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.chat.ChatBox;
import dev.landofif.dofisaddons.chat.ChatFilter;
import dev.landofif.dofisaddons.chat.ChatTweaks;
import dev.landofif.dofisaddons.trigger.Trigger;
import dev.landofif.dofisaddons.ui.DofiScreen;
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
 * chat box and turn right click copy on or off. The same screen edits a chat box's rules (which lines
 * go to that box instead of chat) when opened for a box.
 */
public class ChatFilterScreen extends DofiScreen {
    private static final int MODE_W = 74;
    private static final int SCOPE_W = 74;

    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();
    /** null: the hide rules. Otherwise the box whose rules this edits. */
    private final ChatBox box;
    private final List<ChatFilter> rules;
    private ChatFilter focusOnOpen;
    /** Focus for a new rule's field, applied next tick (a click focuses the button it hit afterwards). */
    private RowListWidget.Row focusRow;
    private TextFieldWidget focusField;
    private RowListWidget list;
    private TextFieldWidget test;
    private String testValue = "";

    public ChatFilterScreen(Screen parent) {
        this(parent, (ChatFilter) null);
    }

    /** Opens with {@code newRule} added (shift + right click on a chat line). */
    public ChatFilterScreen(Screen parent, ChatFilter newRule) {
        super(Foe.text("dofis addons · chat"));
        this.parent = parent;
        this.box = null;
        this.rules = s.chatFilters;
        if (newRule != null) {
            s.chatFilters.add(newRule);
            DofiSettings.save();
            focusOnOpen = newRule;
        }
    }

    /** Edits which lines go to {@code box}. */
    public ChatFilterScreen(Screen parent, ChatBox box) {
        super(Foe.text("dofis addons · " + (box.name.isBlank() ? "chat" : box.name) + " box"));
        this.parent = parent;
        this.box = box;
        this.rules = box.rules;
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

        test = new TextFieldWidget(textRenderer, left + 2, height - 72, w - 16, 18, Foe.text("test with"));
        test.setMaxLength(512);
        test.setPlaceholder(Foe.text(box == null ? "paste a chat line to see if a rule hides it"
                : "paste a chat line to see if it goes to this box").formatted(Formatting.DARK_GRAY));
        test.setText(testValue);
        test.setChangedListener(v -> testValue = v);
        addDrawableChild(test);

        int y = height - 28;
        if (box != null) {
            int bw = (w - 8) / 3;
            addDrawableChild(ButtonWidget.builder(Foe.text("+ rule"), b -> addRule()).dimensions(left, y, bw, 20)
                    .tooltip(Tooltip.of(Foe.text("add a rule. use * for anything, like: [crew chat] *"))).build());
            addDrawableChild(ButtonWidget.builder(alsoLabel(), b -> {
                box.alsoInChat = !box.alsoInChat;
                b.setMessage(alsoLabel());
            }).dimensions(left + bw + 4, y, bw, 20).tooltip(Tooltip.of(Foe.text("keep these lines in the main chat as well"))).build());
            addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(left + (bw + 4) * 2, y, bw, 20).build());
            return;
        }
        int bw = (w - 20) / 6;
        addDrawableChild(ButtonWidget.builder(Foe.text("+ hide rule"), b -> addRule()).dimensions(left, y, bw, 20)
                .tooltip(Tooltip.of(Foe.text("add a rule. use * for anything, like: tackle shop > *restocked*"))).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("chat boxes"), b -> {
            DofiSettings.save();
            client.setScreen(new ChatBoxScreen(this));
        }).dimensions(left + (bw + 4), y, bw, 20).tooltip(Tooltip.of(Foe.text("separate chat boxes for dms, crew chat or anything else"))).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("move chat"), b -> FoeScreens.openMover(this)).dimensions(left + (bw + 4) * 2, y, bw, 20).tooltip(Tooltip.of(Foe.text("drag the chat box and the chat boxes anywhere, right click chat to put it back"))).build());
        addDrawableChild(ButtonWidget.builder(copyLabel(), b -> {
            s.chatCopy = !s.chatCopy;
            b.setMessage(copyLabel());
        }).dimensions(left + (bw + 4) * 3, y, bw, 20)
                .tooltip(Tooltip.of(Foe.text("with chat open, right click a line to copy it. shift + right click adds a hide rule for it."))).build());
        addDrawableChild(ButtonWidget.builder(stackLabel(), b -> {
            s.chatStack = !s.chatStack;
            b.setMessage(stackLabel());
        }).dimensions(left + (bw + 4) * 4, y, bw, 20)
                .tooltip(Tooltip.of(Foe.text("the same line again and again becomes one line with a count, like (x3)"))).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(left + (bw + 4) * 5, y, bw, 20).build());
    }

    private Text stackLabel() {
        return Foe.text("stack: ").append(Foe.text(s.chatStack ? "on" : "off").formatted(s.chatStack ? Formatting.GREEN : Formatting.RED));
    }

    private void addRule() {
        ChatFilter f = new ChatFilter();
        rules.add(f);
        focusOnOpen = f;
        rebuild();
        list.setScrollY(list.getMaxScrollY());
    }

    private Text alsoLabel() {
        return Foe.text("also in chat: ").append(Foe.text(box.alsoInChat ? "on" : "off").formatted(box.alsoInChat ? Formatting.GREEN : Formatting.RED));
    }

    private Text copyLabel() {
        return Foe.text("copy: ").append(Foe.text(s.chatCopy ? "on" : "off").formatted(s.chatCopy ? Formatting.GREEN : Formatting.RED));
    }

    private void rebuild() {
        double scroll = list.getScrollY();
        list.clearRows();
        int rowW = list.getRowWidth();
        int fieldW = Math.max(60, rowW - 20 - MODE_W - SCOPE_W - 20 - 2 * 4);
        for (ChatFilter f : rules) {
            ButtonWidget toggle = ButtonWidget.builder(dot(f), b -> {
                f.enabled = !f.enabled;
                b.setMessage(dot(f));
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.text("switch this rule on or off"))).build();

            TextFieldWidget field = new TextFieldWidget(textRenderer, 0, 0, fieldW, 20, Foe.text("pattern"));
            field.setMaxLength(512);
            field.setText(f.pattern);
            field.setPlaceholder(Foe.text(box == null ? "chat text to hide" : "chat text for this box").formatted(Formatting.DARK_GRAY));
            field.setChangedListener(v -> {
                f.pattern = v;
                field.setTooltip(Tooltip.of(fieldTip(f)));
            });
            field.setTooltip(Tooltip.of(fieldTip(f)));

            ButtonWidget mode = ButtonWidget.builder(Foe.text(modeLabel(f.mode)), b -> {
                Trigger.MatchMode[] modes = Trigger.MatchMode.values();
                f.mode = modes[((f.mode == null ? 0 : f.mode.ordinal()) + 1) % modes.length];
                b.setMessage(Foe.text(modeLabel(f.mode)));
                field.setTooltip(Tooltip.of(fieldTip(f)));
            }).dimensions(0, 0, MODE_W, 20).tooltip(Tooltip.of(Foe.text(
                    "contains: the line has this text somewhere (* = anything)\nwhole line: the line is exactly this\nregex: a java regular expression"))).build();

            ButtonWidget scope = ButtonWidget.builder(Foe.text(f.fishOnMcOnly ? "fishonmc" : "everywhere"), b -> {
                f.fishOnMcOnly = !f.fishOnMcOnly;
                b.setMessage(Foe.text(f.fishOnMcOnly ? "fishonmc" : "everywhere"));
            }).dimensions(0, 0, SCOPE_W, 20).tooltip(Tooltip.of(Foe.text("hide only on fishonmc, or on every server"))).build();

            ButtonWidget delete = ButtonWidget.builder(Foe.text("✕").formatted(Formatting.RED), b -> {
                rules.remove(f);
                rebuild();
            }).dimensions(0, 0, 20, 20).tooltip(Tooltip.of(Foe.text("delete this rule"))).build();

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
        return Foe.text(f.enabled ? "●" : "○").formatted(f.enabled ? Formatting.GREEN : Formatting.DARK_GRAY);
    }

    private static String modeLabel(Trigger.MatchMode mode) {
        return switch (mode == null ? Trigger.MatchMode.SIMPLE : mode) {
            case SIMPLE -> "contains";
            case EXACT -> "whole line";
            case REGEX -> "regex";
        };
    }

    private Text fieldTip(ChatFilter f) {
        String error = ChatTweaks.matcher(f).error();
        if (error != null && !f.pattern.isBlank()) {
            return Foe.text(error).formatted(Formatting.RED);
        }
        return Foe.text(box == null ? "hid " + ChatTweaks.hidden(f) + " lines this session" : "lines that match go to this box");
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        String intro = box == null ? "hide chat lines you dont want to see. * matches anything"
                : "lines that match go to the " + box.name + " box. * matches anything";
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.text(intro).withColor(Foe.DIM & 0xFFFFFF), width / 2, 23, 0xFFFFFFFF);
        if (rules.isEmpty()) {
            String empty = box == null ? "no rules yet. click + hide rule, or shift + right click a line in chat."
                    : "no rules yet. click + rule and paste a line from chat.";
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text(empty).withColor(Foe.DIM & 0xFFFFFF), width / 2, 70, 0xFFFFFFFF);
        }
        if (!testValue.isBlank()) {
            Text result;
            if (box == null) {
                ChatFilter hit = ChatTweaks.hiddenBy(testValue, true, false);
                result = hit == null
                        ? Foe.text("shown: no rule matches this line").formatted(Formatting.GRAY)
                        : Foe.text("hidden by: " + hit.pattern).formatted(Formatting.GREEN);
            } else {
                boolean hit = rules.stream().anyMatch(f -> f.enabled && ChatTweaks.matcher(f).match(testValue) != null);
                result = hit
                        ? Foe.text("goes to the " + box.name + " box").formatted(Formatting.GREEN)
                        : Foe.text("stays in chat: no rule matches this line").formatted(Formatting.GRAY);
            }
            ctx.drawText(textRenderer, result, test.getX(), test.getY() + 22, 0xFFFFFFFF, false);
        }
    }

    @Override
    public void close() {
        rules.removeIf(f -> f.pattern == null || f.pattern.isBlank());
        DofiSettings.save();
        client.setScreen(parent);
    }
}
