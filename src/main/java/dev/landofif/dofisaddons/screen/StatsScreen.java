package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.S24;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.stats.StatBook;
import dev.landofif.dofisaddons.stats.StatNames;
import dev.landofif.dofisaddons.stats.StatsTracker;
import dev.landofif.dofisaddons.util.Mono;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/** Session and all-time numbers in readable sections. */
public class StatsScreen extends DofiScreen {
    private enum Tab {
        ALL_TIME("All-time"), SESSION("This session");

        final String label;

        Tab(String label) {
            this.label = label;
        }
    }

    private record Section(String title, List<Text> lines) {}

    private static final int COL_W = 150;
    private static final int LINE_H = 11;
    private static final int TOP = 48;

    private final Screen parent;
    private Tab tab = Tab.ALL_TIME;
    private int left;
    private int contentW;
    private double scroll;
    private int contentHeight;

    public StatsScreen(Screen parent) {
        super(Mono.text("Dofi's Addons · Stats"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.contentW = Math.min(470, this.width - 20);
        this.left = (this.width - contentW) / 2;

        int tabW = (contentW - 4) / 2;
        int i = 0;
        for (Tab t : Tab.values()) {
            ButtonWidget b = ButtonWidget.builder(Mono.text(t.label), btn -> {
                tab = t;
                scroll = 0;
                clearAndInit();
            }).dimensions(left + i * (tabW + 4), 22, tabW, 18).build();
            b.active = t != tab;
            addDrawableChild(b);
            i++;
        }

        int by = this.height - 26;
        int bw = (contentW - 8) / 3;
        if (tab == Tab.SESSION) {
            addDrawableChild(ButtonWidget.builder(Mono.text("Reset session"), b -> confirm(
                            "start a new session?", "this sessions numbers go back to zero. all-time stats are kept.",
                            StatsTracker::resetSession))
                    .dimensions(left, by, bw, 20).build());
        } else if (tab == Tab.ALL_TIME) {
            addDrawableChild(ButtonWidget.builder(Mono.text("Reset all-time").formatted(Formatting.RED), b -> confirm(
                            "erase all-time stats?", "every number dofis addons has collected for this account is deleted. this cant be undone.",
                            StatsTracker::resetAllTime))
                    .dimensions(left, by, bw, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Mono.text("Custom HUDs…"), b -> client.setScreen(new HudListScreen(this)))
                .dimensions(left + bw + 4, by, bw, 20)
                .tooltip(Tooltip.of(Mono.text("put any of these numbers on your screen"))).build());
        addDrawableChild(ButtonWidget.builder(Mono.text("Done"), b -> close())
                .dimensions(left + (bw + 4) * 2, by, bw, 20).build());
    }

    private void confirm(String title, String body, Runnable action) {
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                action.run();
            }
            client.setScreen(this);
        }, Mono.text(title), Mono.text(body)));
    }

    // ------------------------------------------------------------------ stat sections

    private List<Section> sections(StatBook b, boolean allTime) {
        List<Section> out = new ArrayList<>();

        List<Text> overview = new ArrayList<>();
        overview.add(row("Fish caught", StatNames.number(b.fish)));
        overview.add(row("Per hour", b.perHour() > 0 ? StatNames.compact(b.perHour()) : "-"));
        overview.add(row("Time fishing", StatNames.duration(b.activeMillis)));
        overview.add(row("Fish value", "$" + StatNames.compact(b.value)));
        overview.add(row("XP", StatNames.compact(b.xp)));
        overview.add(row("Sold for", "$" + StatNames.compact(b.moneyEarned)));
        overview.add(row("Quests done", StatNames.number(b.questsCompleted)));
        out.add(new Section("Overview", overview));

        List<Text> drops = new ArrayList<>();
        drops.add(dropRow(b, "Pets", b.pets, "pet", allTime));
        drops.add(dropRow(b, "Shards", b.shards, "shard", allTime));
        drops.add(dropRow(b, "Lightning", b.lightningBottles, "lightning", allTime));
        drops.add(dropRow(b, "Capsules", b.infusionCapsules, "capsule", allTime));
        out.add(new Section(allTime ? "Rare drops  (fish since last)" : "Rare drops", drops));

        out.add(new Section(allTime ? "Rarity  (fish since last)" : "Rarity", kindRows(b, b.rarity, StatNames.RARITIES, "rarity:", allTime)));
        out.add(new Section("Size", kindRows(b, b.size, StatNames.SIZES, "size:", allTime)));
        out.add(new Section("Variants", kindRows(b, b.variant, StatNames.VARIANTS, "variant:", allTime)));

        List<Text> top = new ArrayList<>();
        b.species.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> top.add(row(StatNames.pretty(e.getKey()), StatNames.number(e.getValue()))));
        if (top.isEmpty()) top.add(Mono.text("nothing yet").formatted(Formatting.DARK_GRAY));
        out.add(new Section("Most caught", top));

        List<Text> heavy = new ArrayList<>();
        b.heaviest.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, StatBook.Best> e) -> e.getValue().weight).reversed())
                .limit(5)
                .forEach(e -> heavy.add(row(StatNames.pretty(e.getKey()), String.format(java.util.Locale.ROOT, "%.1flb", e.getValue().weight))));
        if (heavy.isEmpty()) heavy.add(Mono.text("nothing yet").formatted(Formatting.DARK_GRAY));
        out.add(new Section("Heaviest", heavy));

        List<Text> places = new ArrayList<>();
        b.location.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> places.add(row(StatNames.pretty(e.getKey()), StatNames.number(e.getValue()))));
        if (places.isEmpty()) places.add(Mono.text("nothing yet").formatted(Formatting.DARK_GRAY));
        out.add(new Section("Locations", places));
        return out;
    }

    private List<Text> kindRows(StatBook b, Map<String, Long> map, List<StatNames.Kind> kinds, String prefix, boolean since) {
        List<Text> rows = new ArrayList<>();
        for (StatNames.Kind k : kinds) {
            long n = b.count(map, k.id());
            MutableText t = Mono.text(k.name() + " ").withColor(k.color())
                    .append(Mono.text(StatNames.number(n)).formatted(Formatting.WHITE));
            if (b.fish > 0 && n > 0) {
                t.append(Mono.text(String.format(java.util.Locale.ROOT, " %.1f%%", 100.0 * n / b.fish)).formatted(Formatting.DARK_GRAY));
            }
            if (since) {
                long s = b.since(prefix + k.id());
                if (s >= 0) t.append(Mono.text(" (" + StatNames.compact(s) + ")").formatted(Formatting.GRAY));
            }
            rows.add(t);
        }
        return rows;
    }

    private Text dropRow(StatBook b, String label, long count, String key, boolean since) {
        MutableText t = Mono.text(label + " ").formatted(Formatting.GRAY).append(Mono.text(StatNames.number(count)).formatted(Formatting.WHITE));
        if (since) {
            long s = b.since(key);
            if (s >= 0) t.append(Mono.text(" (" + StatNames.compact(s) + ")").formatted(Formatting.GRAY));
        }
        return t;
    }

    private static Text row(String label, String value) {
        return Mono.text(label + " ").formatted(Formatting.GRAY).append(Mono.text(value).formatted(Formatting.WHITE));
    }

    // ------------------------------------------------------------------ drawing

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        boolean allTime = tab == Tab.ALL_TIME;
        StatBook book = allTime ? StatsTracker.allTime() : StatsTracker.session();
        String note = allTime
                ? "everything this mod has seen on this account since you installed it"
                : "since you started the game. only counts while on fishonmc";
        context.drawCenteredTextWithShadow(textRenderer, Mono.text(note).formatted(Formatting.DARK_GRAY),
                this.width / 2, TOP - 6, 0xFFFFFFFF);

        int top = TOP + 8;
        int bottom = this.height - 32;
        int cols = Math.max(1, Math.min(3, contentW / COL_W));
        int colW = contentW / cols;
        List<Section> sections = sections(book, allTime);

        // Greedy: put each section in the currently shortest column.
        int[] colY = new int[cols];
        context.enableScissor(left, top, left + contentW, bottom);
        int off = (int) scroll;
        for (Section sec : sections) {
            int c = 0;
            for (int i = 1; i < cols; i++) {
                if (colY[i] < colY[c]) c = i;
            }
            int x = left + c * colW;
            int y = top + colY[c] - off;
            context.drawTextWithShadow(textRenderer, Mono.bold(sec.title()).withColor(S24.ACCENT & 0xFFFFFF), x, y, 0xFFFFFFFF);
            y += LINE_H + 1;
            for (Text line : sec.lines()) {
                Text shown = textRenderer.getWidth(line) > colW - 6
                        ? Mono.text(textRenderer.trimToWidth(line, colW - 12).getString() + "…")
                        : line;
                context.drawTextWithShadow(textRenderer, shown, x, y, 0xFFFFFFFF);
                y += LINE_H;
            }
            colY[c] += (sec.lines().size() + 1) * LINE_H + 1 + 8;
        }
        context.disableScissor();

        contentHeight = 0;
        for (int h : colY) contentHeight = Math.max(contentHeight, h);
        int visible = bottom - top;
        if (contentHeight > visible) {
            int barH = Math.max(12, visible * visible / contentHeight);
            int barY = top + (int) ((visible - barH) * (scroll / (contentHeight - visible)));
            context.fill(left + contentW + 3, barY, left + contentW + 5, barY + barH, 0x80FFFFFF);
        }
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        int visible = (this.height - 32) - (TOP + 8);
        scroll = Math.max(0, Math.min(Math.max(0, contentHeight - visible), scroll - verticalAmount * 20));
        return true;
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
