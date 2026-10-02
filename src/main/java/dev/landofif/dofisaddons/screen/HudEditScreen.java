package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.hud.CustomHud;
import dev.landofif.dofisaddons.hud.HudRenderer;
import dev.landofif.dofisaddons.hud.HudStore;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.EditBoxWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Edit one HUD: name, look, and its lines. The right side shows a live preview with real values,
 * so you see exactly what you'll get. Cancel throws the changes away.
 */
public class HudEditScreen extends DofiScreen {
    private static final int LABEL_W = 60;

    private final HudListScreen parent;
    private final CustomHud draft;
    private final int index;

    private EditBoxWidget linesBox;
    private int left;
    private int editW;
    private int previewX;
    private int previewW;

    private HudRenderer.Resolved preview = HudRenderer.Resolved.EMPTY;
    private long previewAt;
    private String status = "";
    private long statusUntil;

    public HudEditScreen(HudListScreen parent, CustomHud draft, int index) {
        super(Mono.text(index < 0 ? "new hud" : "edit hud"));
        this.parent = parent;
        this.draft = draft;
        this.index = index;
    }

    @Override
    protected void init() {
        boolean wide = this.width >= 480;
        editW = wide ? Math.min(300, this.width / 2) : this.width - 20;
        left = wide ? Math.max(10, this.width / 2 - editW - 10) : 10;
        previewX = wide ? left + editW + 16 : left;
        previewW = wide ? Math.min(260, this.width - previewX - 10) : editW;

        int fieldX = left + LABEL_W;
        int fieldW = editW - LABEL_W;
        int half = (editW - 4) / 2;

        int y = 24;
        TextFieldWidget name = new TextFieldWidget(textRenderer, fieldX, y, fieldW, 20, Text.empty());
        name.setMaxLength(64);
        name.setText(draft.name);
        name.setChangedListener(s -> draft.name = s);
        name.setPlaceholder(Mono.text("e.g. session stats").formatted(Formatting.DARK_GRAY));
        addDrawableChild(name);
        y += 24;

        addDrawableChild(ButtonWidget.builder(shownText(), b -> {
                    draft.enabled = !draft.enabled;
                    b.setMessage(shownText());
                }).dimensions(left, y, half, 20)
                .tooltip(Tooltip.of(Mono.text("show or hide this box in game"))).build());
        addDrawableChild(ButtonWidget.builder(alignText(), b -> {
                    draft.align = draft.align.next();
                    b.setMessage(alignText());
                }).dimensions(left + half + 4, y, half, 20)
                .tooltip(Tooltip.of(Mono.text("which way the lines line up"))).build());
        y += 24;

        addDrawableChild(new ValueSlider(left, y, half, 20, "size", 0.5, 3, 0.1, "x", draft.scale, v -> draft.scale = v));
        addDrawableChild(new ValueSlider(left + half + 4, y, half, 20, "backdrop", 0, 100, 5, "%", draft.background, v -> draft.background = (int) v));
        y += 24 + 12;

        int boxBottom = this.height - 56;
        linesBox = EditBoxWidget.builder()
                .x(left).y(y)
                .placeholder(Mono.text("one line per row. try: &7fish &f%session.fish%").formatted(Formatting.DARK_GRAY))
                .build(textRenderer, editW, Math.max(40, boxBottom - y), Mono.text("lines"));
        linesBox.setMaxLength(8192);
        linesBox.setText(String.join("\n", draft.lines));
        addDrawableChild(linesBox);

        int by = this.height - 50;
        addDrawableChild(ButtonWidget.builder(Mono.text("+ insert value…"), b -> {
                    commitLines();
                    client.setScreen(new VariablePickerScreen(this, this::insert));
                }).dimensions(left, by, half, 20)
                .tooltip(Tooltip.of(Mono.text("pick a stat or server value to add to your lines"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("move on screen…"), b -> {
                    commitLines();
                    client.setScreen(new HudMoveScreen(this, List.of(draft)));
                }).dimensions(left + half + 4, by, half, 20)
                .tooltip(Tooltip.of(Mono.text("drag this box where you want it"))).build());

        int bottom = this.height - 26;
        addDrawableChild(ButtonWidget.builder(Mono.text("cancel"), b -> close())
                .dimensions(left, bottom, half, 20)
                .tooltip(Tooltip.of(Mono.text("leave without saving"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("save").formatted(Formatting.GREEN), b -> save())
                .dimensions(left + half + 4, bottom, half, 20).build());
    }

    private Text shownText() {
        return Mono.text("shown: ").append(Mono.text(draft.enabled ? "on" : "off").formatted(draft.enabled ? Formatting.GREEN : Formatting.RED));
    }

    private Text alignText() {
        return Mono.text("align: " + switch (draft.align) {
            case LEFT -> "left";
            case CENTER -> "center";
            case RIGHT -> "right";
        });
    }

    private void commitLines() {
        List<String> lines = new ArrayList<>(Arrays.asList(linesBox.getText().split("\n", -1)));
        while (!lines.isEmpty() && lines.get(lines.size() - 1).isBlank()) {
            lines.remove(lines.size() - 1);
        }
        draft.lines = lines;
    }

    /** Adds a picked value at the end of the text and puts it on the clipboard too. */
    private void insert(String token) {
        commitLines();
        if (draft.lines.isEmpty()) {
            draft.lines.add(token);
        } else {
            int last = draft.lines.size() - 1;
            String line = draft.lines.get(last);
            draft.lines.set(last, line.isEmpty() ? token : line + " " + token);
        }
        client.keyboard.setClipboard(token);
        status = "§aadded " + token + " to the last line (also copied: ctrl+v to paste it elsewhere)";
        statusUntil = System.currentTimeMillis() + 5000;
    }

    private void save() {
        commitLines();
        if (draft.name == null || draft.name.isBlank()) {
            draft.name = "hud " + (HudStore.all().size() + 1);
        }
        draft.sanitize();
        if (index >= 0 && index < HudStore.all().size()) {
            HudStore.all().set(index, draft);
        } else {
            HudStore.all().add(draft);
        }
        HudStore.save();
        parent.setStatus("saved \"" + draft.name + "\"", Formatting.GREEN);
        client.setScreen(parent);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);
        context.drawTextWithShadow(textRenderer, Mono.text("name").formatted(Formatting.GRAY), left, 30, 0xFFFFFFFF);
        context.drawTextWithShadow(textRenderer, Mono.text("lines").formatted(Formatting.GRAY), left, 24 * 4 - 10, 0xFFFFFFFF);

        // Live preview with real values (refreshed a few times a second).
        long now = System.currentTimeMillis();
        if (now - previewAt > 200) {
            CustomHud tmp = draft.copy();
            tmp.lines = new ArrayList<>(Arrays.asList(linesBox.getText().split("\n", -1)));
            preview = HudRenderer.resolve(tmp);
            previewAt = now;
        }
        boolean wide = previewX != left;
        if (wide) {
            int py = 24;
            context.drawTextWithShadow(textRenderer, Mono.text("preview").formatted(Formatting.GRAY), previewX, py - 10 + 6, 0xFFFFFFFF);
            CustomHud tmp = draft.copy();
            tmp.align = CustomHud.Align.LEFT;
            tmp.x = previewX * 100.0 / this.width;
            tmp.y = (py + 10) * 100.0 / this.height;
            if (preview.isEmpty()) {
                context.drawTextWithShadow(textRenderer, Mono.text("(nothing to show right now)").formatted(Formatting.DARK_GRAY),
                        previewX, py + 12, 0xFFFFFFFF);
            } else {
                HudRenderer.draw(context, tmp, preview, false);
            }
            int hy = this.height - 140;
            for (String help : List.of(
                    "how lines work:",
                    "&a &b &c ... colour, &l bold",
                    "&#ff8800 any hex colour",
                    "%session.fish% a live value",
                    "label%tab%value: value on the right",
                    "bold 1st line = title in border",
                    "a line whose value isnt",
                    "available right now hides.",
                    "use + insert value to browse.")) {
                context.drawTextWithShadow(textRenderer, Mono.text(help).formatted(Formatting.DARK_GRAY), previewX, hy, 0xFFFFFFFF);
                hy += 10;
            }
        }
        if (!status.isEmpty() && now < statusUntil) {
            context.drawCenteredTextWithShadow(textRenderer, Mono.text(status), this.width / 2, this.height - 62, 0xFFFFFFFF);
        }
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
