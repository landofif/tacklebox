package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.display.SoundQueue;
import dev.landofif.dofisaddons.display.TitleOverlay;
import dev.landofif.dofisaddons.trigger.Matcher;
import dev.landofif.dofisaddons.trigger.Trigger;
import dev.landofif.dofisaddons.trigger.TriggerEngine;
import dev.landofif.dofisaddons.trigger.TriggerStore;
import dev.landofif.dofisaddons.util.Mono;
import dev.landofif.dofisaddons.util.RichText;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Edits one trigger in three steps, shown as tabs:
 * <ol>
 *   <li><b>When</b>: which chat message to react to, with a live "does it match?" tester.</li>
 *   <li><b>Title</b>: what to show on screen and how it animates.</li>
 *   <li><b>Sound</b>: what to play.</li>
 * </ol>
 * Changes go into a draft; nothing is saved until "Save". "Preview" fires the draft right here.
 */
public class TriggerEditScreen extends DofiScreen {
    private enum Tab {
        WHEN("1. when"), TITLE("2. title"), SOUND("3. sound");
        final String label;
        Tab(String label) { this.label = label; }
    }

    private record SoundChoice(String label, String id) {}

    private static final List<SoundChoice> SOUNDS = List.of(
            new SoundChoice("challenge complete", "minecraft:ui.toast.challenge_complete"),
            new SoundChoice("level up", "minecraft:entity.player.levelup"),
            new SoundChoice("xp pickup", "minecraft:entity.experience_orb.pickup"),
            new SoundChoice("bell", "minecraft:block.bell.use"),
            new SoundChoice("pling", "minecraft:block.note_block.pling"),
            new SoundChoice("chime", "minecraft:block.note_block.chime"),
            new SoundChoice("amethyst", "minecraft:block.amethyst_block.chime"),
            new SoundChoice("beacon", "minecraft:block.beacon.activate"),
            new SoundChoice("totem", "minecraft:item.totem.use"),
            new SoundChoice("firework", "minecraft:entity.firework_rocket.twinkle"),
            new SoundChoice("thunder", "minecraft:entity.lightning_bolt.thunder"),
            new SoundChoice("anvil", "minecraft:block.anvil.land"),
            new SoundChoice("villager yes", "minecraft:entity.villager.yes")
    );

    private static final int ROW = 22;
    private static final int FIRST_ROW = 46;
    private static final int LABEL_W = 78;

    private final TriggerListScreen parent;
    private final Trigger draft;
    private final boolean isNew;
    private Tab tab = Tab.WHEN;
    private String testLine;

    private int left;
    private int width;
    private int fieldX;
    private int fieldW;

    private String status = "";
    private long statusUntil;
    private long previewUntil;

    // cached live-test result for the When tab
    private String testKey = "";
    private Matcher testMatcher;
    private Matcher.Result testResult;

    public TriggerEditScreen(TriggerListScreen parent, Trigger draft, boolean isNew) {
        super(Mono.text(isNew ? "new trigger" : "edit trigger"));
        this.parent = parent;
        this.draft = draft;
        this.isNew = isNew;
        this.testLine = sampleLine(draft);
    }

    @Override
    protected void init() {
        this.width = Math.min(380, super.width - 20);
        this.left = (super.width - width) / 2;
        this.fieldX = left + LABEL_W + 4;
        this.fieldW = width - LABEL_W - 4;

        // Tabs
        int tabW = (width - 8) / 3;
        int i = 0;
        for (Tab t : Tab.values()) {
            ButtonWidget b = ButtonWidget.builder(Mono.text(t.label), btn -> switchTab(t))
                    .dimensions(left + i * (tabW + 4), 22, tabW, 18).build();
            b.active = t != tab;
            addDrawableChild(b);
            i++;
        }

        switch (tab) {
            case WHEN -> initWhen();
            case TITLE -> initTitle();
            case SOUND -> initSound();
        }

        // Bottom bar
        int by = this.height - 26;
        int bw = (width - 8) / 3;
        addDrawableChild(ButtonWidget.builder(Mono.text("cancel"), b -> close())
                .dimensions(left, by, bw, 20)
                .tooltip(Tooltip.of(Mono.text("leave without saving"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("▶ preview"), b -> preview())
                .dimensions(left + bw + 4, by, bw, 20)
                .tooltip(Tooltip.of(Mono.text("show the title and play the sound now, using the test message"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("save").formatted(Formatting.GREEN), b -> save())
                .dimensions(left + (bw + 4) * 2, by, bw, 20).build());
    }

    // ------------------------------------------------------------------ tabs

    private void initWhen() {
        int y = FIRST_ROW;
        textField(y, draft.name, 64, s -> draft.name = s, "e.g. quest completed");
        y += ROW;

        addDrawableChild(ButtonWidget.builder(modeLabel(), b -> {
                    draft.mode = Trigger.MatchMode.values()[(draft.mode.ordinal() + 1) % Trigger.MatchMode.values().length];
                    b.setMessage(modeLabel());
                    b.setTooltip(Tooltip.of(Mono.text(modeHelp())));
                }).dimensions(fieldX, y, fieldW, 20)
                .tooltip(Tooltip.of(Mono.text(modeHelp()))).build());
        y += ROW;

        textField(y, draft.pattern, 512, s -> draft.pattern = s, "e.g. quests > quest #{n} has been completed!");
        y += ROW + 14; // room for the hint line

        int thirdW = (width - 8) / 3;
        addDrawableChild(toggle(left, y, thirdW, "ignore caps", () -> draft.ignoreCase, v -> draft.ignoreCase = v,
                "on: \"quest\" also matches \"quest\""));
        addDrawableChild(toggle(left + thirdW + 4, y, thirdW, "hide chat line", () -> draft.hideMessage, v -> draft.hideMessage = v,
                "on: the original message is removed from chat"));
        addDrawableChild(toggle(left + (thirdW + 4) * 2, y, thirdW, "fishonmc only", () -> draft.fishOnMcOnly, v -> draft.fishOnMcOnly = v,
                "on: only works while youre on fishonmc"));
        y += ROW;

        addDrawableChild(new ValueSlider(left, y, width, 20, "wait before firing again", 0, 30, 0.5, "s",
                draft.cooldownSeconds, v -> draft.cooldownSeconds = v));
        y += ROW;

        TextFieldWidget test = textField(y, testLine, 512, s -> testLine = s, "paste a chat message to test");
        test.setTooltip(Tooltip.of(Mono.text("type or paste a real chat message. the line below says if it would fire.")));
    }

    private void initTitle() {
        Trigger.TitleSettings t = draft.title;
        int y = FIRST_ROW;
        int half = (width - 4) / 2;
        addDrawableChild(toggle(left, y, half, "show title", () -> t.enabled, v -> t.enabled = v, "turn the on-screen title on or off"));
        addDrawableChild(ButtonWidget.builder(animationLabel(), b -> {
                    t.animation = Trigger.Animation.values()[(t.animation.ordinal() + 1) % Trigger.Animation.values().length];
                    b.setMessage(animationLabel());
                }).dimensions(left + half + 4, y, half, 20)
                .tooltip(Tooltip.of(Mono.text("how the title appears: fade, pop, slide, typewriter or shake"))).build());
        y += ROW;

        TextFieldWidget titleField = textField(y, t.title, 256, s -> t.title = s, "e.g. &lquest {n} complete");
        y += ROW;
        TextFieldWidget subtitleField = textField(y, t.subtitle, 256, s -> t.subtitle = s, "optional second line");
        y += ROW;

        // [title colour][swatch] [subtitle colour][swatch]; a swatch opens the colour picker. Picking a colour
        // drops a colour code at the start of that line, since the code would win and hide the pick.
        int colorW = (fieldW - 56) / 2;
        TextFieldWidget tc = new TextFieldWidget(textRenderer, fieldX, y, colorW, 20, Text.empty());
        tc.setText(t.titleColor);
        tc.setChangedListener(s -> {
            t.titleColor = s;
            if (RichText.parseColor(s, -1) != -1) {
                dropLeadColor(titleField);
            }
        });
        tc.setTooltip(Tooltip.of(Mono.text("title colour: #ff8800, &a or a name like gold")));
        addDrawableChild(tc);
        addDrawableChild(colorSwatch(fieldX + colorW + 3, y, "title colour",
                () -> RichText.parseColor(t.titleColor, 0xFFFFFF), c -> {
                    t.titleColor = hex(c);
                    dropLeadColor(titleField);
                }));
        TextFieldWidget sc = new TextFieldWidget(textRenderer, fieldX + colorW + 28, y, colorW, 20, Text.empty());
        sc.setText(t.subtitleColor);
        sc.setChangedListener(s -> {
            t.subtitleColor = s;
            if (RichText.parseColor(s, -1) != -1) {
                dropLeadColor(subtitleField);
            }
        });
        sc.setTooltip(Tooltip.of(Mono.text("subtitle colour: #ff8800, &a or a name like gray")));
        addDrawableChild(sc);
        addDrawableChild(colorSwatch(fieldX + colorW * 2 + 31, y, "subtitle colour",
                () -> RichText.parseColor(t.subtitleColor, 0xBBBBBB), c -> {
                    t.subtitleColor = hex(c);
                    dropLeadColor(subtitleField);
                }));
        y += ROW;

        addDrawableChild(new ValueSlider(left, y, half, 20, "size", 0.5, 8, 0.1, "x", t.scale, v -> t.scale = v));
        addDrawableChild(new ValueSlider(left + half + 4, y, half, 20, "height", 0, 100, 1, "%", t.heightPercent, v -> t.heightPercent = v));
        y += ROW;

        int third = (width - 8) / 3;
        addDrawableChild(new ValueSlider(left, y, third, 20, "in", 0, 3, 0.1, "s", t.fadeInSeconds, v -> t.fadeInSeconds = v));
        addDrawableChild(new ValueSlider(left + third + 4, y, third, 20, "stay", 0, 15, 0.1, "s", t.staySeconds, v -> t.staySeconds = v));
        addDrawableChild(new ValueSlider(left + (third + 4) * 2, y, third, 20, "out", 0, 3, 0.1, "s", t.fadeOutSeconds, v -> t.fadeOutSeconds = v));
        y += ROW;

        addDrawableChild(toggle(left, y, half, "text shadow", () -> t.shadow, v -> t.shadow = v, "dark shadow behind the letters"));
    }

    private void initSound() {
        Trigger.SoundSettings s = draft.sound;
        int y = FIRST_ROW;
        int half = (width - 4) / 2;
        addDrawableChild(toggle(left, y, half, "play sound", () -> s.enabled, v -> s.enabled = v, "turn the sound on or off"));
        addDrawableChild(ButtonWidget.builder(Mono.text("▶ play sound"), b -> SoundQueue.play(s))
                .dimensions(left + half + 4, y, half, 20).build());
        y += ROW;

        TextFieldWidget[] idField = new TextFieldWidget[1];
        ButtonWidget pick = ButtonWidget.builder(soundLabel(), b -> {
                    int current = soundIndex(s.soundId);
                    SoundChoice next = SOUNDS.get((current + 1) % SOUNDS.size());
                    s.soundId = next.id;
                    idField[0].setText(next.id);
                    b.setMessage(soundLabel());
                    SoundQueue.play(s);
                }).dimensions(fieldX, y, fieldW, 20)
                .tooltip(Tooltip.of(Mono.text("click to go through popular sounds (plays each one)"))).build();
        addDrawableChild(pick);
        y += ROW;

        idField[0] = textField(y, s.soundId, 256, v -> {
            s.soundId = v;
            pick.setMessage(soundLabel());
        }, "e.g. minecraft:block.note_block.pling");
        idField[0].setTooltip(Tooltip.of(Mono.text("any sound id, including sounds from the fishonmc server pack")));
        y += ROW;

        addDrawableChild(new ValueSlider(left, y, half, 20, "volume", 0, 1, 0.05, "", s.volume, v -> s.volume = v));
        addDrawableChild(new ValueSlider(left + half + 4, y, half, 20, "pitch", 0.5, 2, 0.05, "", s.pitch, v -> s.pitch = v));
        y += ROW;

        addDrawableChild(new ValueSlider(left, y, half, 20, "times", 1, 10, 1, "x", s.plays, v -> s.plays = (int) v));
        addDrawableChild(new ValueSlider(left + half + 4, y, half, 20, "gap", 0.05, 2, 0.05, "s", s.secondsBetween, v -> s.secondsBetween = v));
    }

    // ------------------------------------------------------------------ actions

    private void switchTab(Tab next) {
        this.tab = next;
        clearAndInit();
        replayIntro();
    }

    private void preview() {
        Map<String, String> values = previewValues();
        if (draft.title.enabled) {
            TitleOverlay.preview(draft.title, TriggerEngine.fill(draft.title.title, values), TriggerEngine.fill(draft.title.subtitle, values));
            previewUntil = System.currentTimeMillis()
                    + (long) ((draft.title.fadeInSeconds + draft.title.staySeconds + draft.title.fadeOutSeconds) * 1000);
        }
        if (draft.sound.enabled) {
            SoundQueue.play(draft.sound);
        }
    }

    private void save() {
        draft.sanitize();
        Matcher m = Matcher.of(draft);
        if (m.error() != null) {
            tab = Tab.WHEN;
            clearAndInit();
            setStatus(m.error(), Formatting.RED);
            return;
        }
        if (draft.name.isBlank()) {
            draft.name = RichText.strip(draft.title.title).isBlank() ? draft.pattern : RichText.strip(draft.title.title);
        }
        TriggerStore.put(draft);
        client.setScreen(parent);
        parent.setStatus((isNew ? "created \"" : "saved \"") + draft.name + "\"", Formatting.GREEN);
    }

    @Override
    public void close() {
        TitleOverlay.clear();
        client.setScreen(parent);
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        switch (tab) {
            case WHEN -> renderWhen(context);
            case TITLE -> renderTitle(context);
            case SOUND -> renderSound(context);
        }

        if (!status.isEmpty() && System.currentTimeMillis() < statusUntil) {
            context.drawCenteredTextWithShadow(textRenderer, Mono.text(status), super.width / 2, this.height - 38, 0xFFFFFFFF);
        }

        // Live preview on top of everything, over a dimmed backdrop so it reads like in-game.
        if (System.currentTimeMillis() < previewUntil) {
            context.fill(0, 0, super.width, this.height, 0x99000000);
            TitleOverlay.render(context);
        }
    }

    private void renderWhen(DrawContext context) {
        int y = FIRST_ROW;
        label(context, "name", y);
        y += ROW;
        label(context, "match type", y);
        y += ROW;
        label(context, "chat says", y);
        hint(context, patternHint(), y + 23);
        y += ROW + 14;
        y += ROW; // toggles
        y += ROW; // cooldown
        label(context, "test with", y);

        // live result
        String key = draft.mode + "|" + draft.ignoreCase + "|" + draft.pattern + "|" + testLine;
        if (!key.equals(testKey)) {
            testKey = key;
            testMatcher = Matcher.of(draft);
            testResult = testMatcher.error() == null ? testMatcher.match(testLine) : null;
        }
        Text result;
        if (testMatcher.error() != null) {
            result = Mono.text("⚠ " + testMatcher.error()).formatted(Formatting.YELLOW);
        } else if (testResult == null) {
            result = Mono.text("✘ this message would not fire the trigger").formatted(Formatting.RED);
        } else {
            StringBuilder sb = new StringBuilder("✔ fires!");
            for (Map.Entry<String, String> e : testResult.values().entrySet()) {
                if (!e.getKey().equals("message") && !e.getKey().chars().allMatch(Character::isDigit)) {
                    sb.append("  {").append(e.getKey()).append("} = ").append(e.getValue());
                }
            }
            result = Mono.text(textRenderer.trimToWidth(sb.toString(), width)).formatted(Formatting.GREEN);
        }
        context.drawTextWithShadow(textRenderer, result, left, y + 24, 0xFFFFFFFF);
    }

    private void renderTitle(DrawContext context) {
        int y = FIRST_ROW + ROW;
        label(context, "title", y);
        y += ROW;
        label(context, "subtitle", y);
        y += ROW;
        label(context, "colours", y);
        y += ROW * 4;
        hint(context, "use {n}-style values from step 1, {player}, {count}. colours: &a &l &o &#ff8800", y + 2);
    }

    private void renderSound(DrawContext context) {
        int y = FIRST_ROW + ROW;
        label(context, "sound", y);
        y += ROW;
        label(context, "sound id", y);
        y += ROW * 3;
        hint(context, "tip: press play sound to hear it. volume is on the master slider.", y + 2);
    }

    private void label(DrawContext context, String text, int y) {
        context.drawTextWithShadow(textRenderer, Mono.text(text).formatted(Formatting.GRAY), left, y + 6, 0xFFFFFFFF);
    }

    private void hint(DrawContext context, String text, int y) {
        context.drawTextWithShadow(textRenderer, Mono.text(textRenderer.trimToWidth(text, width)).formatted(Formatting.DARK_GRAY), left, y, 0xFFFFFFFF);
    }

    private SwatchButton colorSwatch(int x, int y, String name, java.util.function.IntSupplier get,
                                     java.util.function.IntConsumer set) {
        SwatchButton b = new SwatchButton(x, y, 22, 20, get,
                () -> client.setScreen(new ColorPickerScreen(this, name, get.getAsInt(), set)));
        b.setTooltip(Tooltip.of(Mono.text("click to pick the " + name.toLowerCase(java.util.Locale.ROOT))));
        return b;
    }

    /** Removes a colour code from the start of the line in {@code field} (its listener updates the draft). */
    private static void dropLeadColor(TextFieldWidget field) {
        RichText.Lead lead = RichText.liftLeadColor(field.getText());
        if (lead != null) {
            field.setText(lead.rest());
        }
    }

    private static String hex(int rgb) {
        return String.format(java.util.Locale.ROOT, "#%06x", rgb & 0xFFFFFF);
    }

    // ------------------------------------------------------------------ helpers

    private TextFieldWidget textField(int y, String value, int maxLength, Consumer<String> onChange, String placeholder) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, fieldX, y, fieldW, 20, Text.empty());
        f.setMaxLength(maxLength);
        f.setText(value == null ? "" : value);
        f.setChangedListener(onChange);
        f.setPlaceholder(Mono.text(placeholder).formatted(Formatting.DARK_GRAY));
        addDrawableChild(f);
        return f;
    }

    private interface BoolGetter { boolean get(); }
    private interface BoolSetter { void set(boolean v); }

    private ButtonWidget toggle(int x, int y, int w, String name, BoolGetter get, BoolSetter set, String help) {
        return ButtonWidget.builder(toggleText(name, get.get()), b -> {
                    set.set(!get.get());
                    b.setMessage(toggleText(name, get.get()));
                }).dimensions(x, y, w, 20)
                .tooltip(Tooltip.of(Mono.text(help))).build();
    }

    private static Text toggleText(String name, boolean on) {
        return Mono.text(name + ": ").append(Mono.text(on ? "on" : "off").formatted(on ? Formatting.GREEN : Formatting.RED));
    }

    private Text modeLabel() {
        return Mono.text(switch (draft.mode) {
            case SIMPLE -> "contains (simple)";
            case EXACT -> "whole message";
            case REGEX -> "regex (advanced)";
        });
    }

    private String modeHelp() {
        return switch (draft.mode) {
            case SIMPLE -> "fires when the message contains your text. * = anything, {name} = remember that part";
            case EXACT -> "fires only when the whole message is exactly your text ({name} and * still work)";
            case REGEX -> "java regular expression. named groups (?<name>...) become {name}";
        };
    }

    private String patternHint() {
        return switch (draft.mode) {
            case SIMPLE, EXACT -> "* matches anything · {n} grabs a part to show in the title · > also matches »";
            case REGEX -> "example: quests > quest #(?<n>\\d+) has been completed!";
        };
    }

    private Text animationLabel() {
        String name = draft.title.animation.name();
        return Mono.text("animation: " + name.toLowerCase(java.util.Locale.ROOT));
    }

    private Text soundLabel() {
        int i = soundIndex(draft.sound.soundId);
        return Mono.text("♪ " + (i >= 0 ? SOUNDS.get(i).label : "custom sound") + "  (click for next)");
    }

    private static int soundIndex(String id) {
        if (id == null) {
            return -1;
        }
        String normalized = id.contains(":") ? id : "minecraft:" + id;
        for (int i = 0; i < SOUNDS.size(); i++) {
            if (SOUNDS.get(i).id.equalsIgnoreCase(normalized)) {
                return i;
            }
        }
        return -1;
    }

    /** Values for Preview: captured from the test message if it matches, else example values. */
    private Map<String, String> previewValues() {
        Map<String, String> values = new LinkedHashMap<>();
        Matcher m = Matcher.of(draft);
        Matcher.Result r = m.error() == null ? m.match(testLine) : null;
        if (r != null) {
            values.putAll(r.values());
        } else {
            values.put("message", testLine);
        }
        values.put("count", String.valueOf(TriggerEngine.count(draft) + 1));
        values.putIfAbsent("player", client.getSession().getUsername());
        return values;
    }

    /** A plausible chat line for the tester, made from the pattern ({n} -> 6, * -> text). */
    private static String sampleLine(Trigger t) {
        if (t.mode == Trigger.MatchMode.REGEX || t.pattern == null) {
            return "";
        }
        return t.pattern.replaceAll("\\{[^}]*}", "6").replace("*", "something");
    }

    private void setStatus(String message, Formatting color) {
        this.status = "§" + color.getCode() + message;
        this.statusUntil = System.currentTimeMillis() + 4000;
    }
}
