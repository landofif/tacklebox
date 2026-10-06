package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.chat.ChatCopy;
import dev.landofif.dofisaddons.mixin.ChatHudAccessor;
import dev.landofif.dofisaddons.ui.DofiScreen;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.gui.hud.ChatHudLine;
import net.minecraft.util.Language;
import net.minecraft.text.Text;

import java.util.List;
import java.util.Locale;

/** ctrl + f in chat: search every line the chat still remembers. Click a line to copy it. Display only. */
public class ChatSearchScreen extends DofiScreen {
    private static final int ROW = 11;

    private final Screen parent;
    private TextFieldWidget field;
    private List<Text> hits = List.of();
    private int scroll;

    public ChatSearchScreen(Screen parent) {
        super(Foe.text("dofis addons · search chat"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Math.min(560, width - 20);
        field = new TextFieldWidget(textRenderer, (width - w) / 2, 24, w, 18, Foe.text("search"));
        field.setMaxLength(100);
        field.setPlaceholder(Foe.text("type to search").formatted(net.minecraft.util.Formatting.DARK_GRAY));
        field.setChangedListener(v -> search());
        addDrawableChild(field);
        setInitialFocus(field);
        search();
    }

    private void search() {
        String q = field.getText().toLowerCase(Locale.ROOT).trim();
        List<ChatHudLine> all = ((ChatHudAccessor) client.inGameHud.getChatHud()).dofisaddons$messages();
        hits = all.stream().map(ChatHudLine::content)
                .filter(t -> q.isEmpty() || t.getString().toLowerCase(Locale.ROOT).contains(q)).toList();
        scroll = 0;
    }

    private int rows() {
        return (height - 50 - 12) / ROW;
    }

    @Override
    public boolean mouseScrolled(double x, double y, double h, double v) {
        scroll = Math.max(0, Math.min(Math.max(0, hits.size() - rows()), scroll - (int) Math.signum(v) * 3));
        return true;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        int w = Math.min(560, width - 20), x = (width - w) / 2;
        int i = scroll + ((int) click.y() - 48) / ROW;
        if (click.y() >= 48 && click.x() >= x && click.x() < x + w && i < hits.size() && i - scroll < rows()) {
            client.keyboard.setClipboard(ChatCopy.plain(hits.get(i)));
            return true;
        }
        return super.mouseClicked(click, doubled);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int w = Math.min(560, width - 20), x = (width - w) / 2;
        for (int r = 0; r < rows() && scroll + r < hits.size(); r++) {
            var line = Language.getInstance().reorder(textRenderer.trimToWidth(hits.get(scroll + r), w));
            ctx.drawText(textRenderer, line, x, 48 + r * ROW, Foe.TEXT, false);
        }
        ctx.drawText(textRenderer, Foe.text(hits.size() + " lines · newest first · click copies").withColor(Foe.DIM & 0xFFFFFF),
                x, height - 14, 0xFFFFFFFF, false);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
