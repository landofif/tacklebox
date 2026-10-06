package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.ui.Foe;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.stats.StatBook;
import dev.landofif.dofisaddons.stats.StatNames;
import dev.landofif.dofisaddons.stats.StatsTracker;
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
        ALL_TIME("all-time"), SESSION("this session");

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
        super(Foe.text("dofis addons · stats"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.contentW = Math.min(470, this.width - 20);
        this.left = (this.width - contentW) / 2;

        int tabW = (contentW - 4) / 2;
        int i = 0;
        for (Tab t : Tab.values()) {
            ButtonWidget b = ButtonWidget.builder(Foe.text(t.label), btn -> {
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
            addDrawableChild(ButtonWidget.builder(Foe.text("reset session"), b -> confirm(
                            "start a new session?", "this sessions numbers go back to zero. all-time stats are kept.",
                            StatsTracker::resetSession))
                    .dimensions(left, by, bw, 20).build());
        } else if (tab == Tab.ALL_TIME) {
            addDrawableChild(ButtonWidget.builder(Foe.text("reset all-time").formatted(Formatting.RED), b -> confirm(
                            "erase all-time stats?", "every number dofis addons has collected for this account is deleted. this cant be undone.",
                            StatsTracker::resetAllTime))
                    .dimensions(left, by, bw, 20).build());
        }
        addDrawableChild(ButtonWidget.builder(Foe.text("custom huds…"), b -> {
                    net.minecraft.client.gui.screen.Screen huds = dev.landofif.dofisaddons.compat.FoeScreens.customHuds(this);
                    if (huds != null) client.setScreen(huds);
                })
                .dimensions(left + bw + 4, by, bw, 20)
                .tooltip(Tooltip.of(Foe.text("put any of these numbers on your screen"))).build());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close())
                .dimensions(left + (bw + 4) * 2, by, bw, 20).build());
    }

    private void confirm(String title, String body, Runnable action) {
        client.setScreen(new ConfirmScreen(yes -> {
            if (yes) {
                action.run();
            }
            client.setScreen(this);
        }, Foe.text(title), Foe.text(body)));
    }

    // ------------------------------------------------------------------ stat sections

    private List<Section> sections(StatBook b, boolean allTime) {
        List<Section> out = new ArrayList<>();

        List<Text> overview = new ArrayList<>();
        overview.add(row("fish caught", StatNames.number(b.fish)));
        overview.add(row("per hour", b.perHour() > 0 ? StatNames.compact(b.perHour()) : "-"));
        overview.add(row("time fishing", StatNames.duration(b.activeMillis)));
        overview.add(row("fish value", "$" + StatNames.compact(b.value)));
        overview.add(row("xp", StatNames.compact(b.xp)));
        overview.add(row("sold for", "$" + StatNames.compact(b.moneyEarned)));
        overview.add(row("quests done", StatNames.number(b.questsCompleted)));
        out.add(new Section("overview", overview));

        List<Text> drops = new ArrayList<>();
        drops.add(dropRow(b, "pets", b.pets, "pet", allTime));
        drops.add(dropRow(b, "shards", b.shards, "shard", allTime));
        drops.add(dropRow(b, "lightning", b.lightningBottles, "lightning", allTime));
        drops.add(dropRow(b, "capsules", b.infusionCapsules, "capsule", allTime));
        out.add(new Section(allTime ? "rare drops  (fish since last)" : "rare drops", drops));

        out.add(new Section(allTime ? "rarity  (fish since last)" : "rarity", kindRows(b, b.rarity, StatNames.RARITIES, "rarity:", allTime)));
        out.add(new Section("size", kindRows(b, b.size, StatNames.SIZES, "size:", allTime)));
        out.add(new Section("variants", kindRows(b, b.variant, StatNames.VARIANTS, "variant:", allTime)));

        List<Text> top = new ArrayList<>();
        b.species.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> top.add(row(StatNames.pretty(e.getKey()), StatNames.number(e.getValue()))));
        if (top.isEmpty()) top.add(Foe.text("nothing yet").formatted(Formatting.DARK_GRAY));
        out.add(new Section("most caught", top));

        List<Text> heavy = new ArrayList<>();
        b.heaviest.entrySet().stream()
                .sorted(Comparator.comparingDouble((Map.Entry<String, StatBook.Best> e) -> e.getValue().weight).reversed())
                .limit(5)
                .forEach(e -> heavy.add(row(StatNames.pretty(e.getKey()), String.format(java.util.Locale.ROOT, "%.1flb", e.getValue().weight))));
        if (heavy.isEmpty()) heavy.add(Foe.text("nothing yet").formatted(Formatting.DARK_GRAY));
        out.add(new Section("heaviest", heavy));

        List<Text> places = new ArrayList<>();
        b.location.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(5)
                .forEach(e -> places.add(row(StatNames.pretty(e.getKey()), StatNames.number(e.getValue()))));
        if (places.isEmpty()) places.add(Foe.text("nothing yet").formatted(Formatting.DARK_GRAY));
        out.add(new Section("locations", places));
        return out;
    }

    private List<Text> kindRows(StatBook b, Map<String, Long> map, List<StatNames.Kind> kinds, String prefix, boolean since) {
        List<Text> rows = new ArrayList<>();
        for (StatNames.Kind k : kinds) {
            long n = b.count(map, k.id());
            MutableText t = Foe.text(k.name() + " ").withColor(k.color())
                    .append(Foe.text(StatNames.number(n)).formatted(Formatting.WHITE));
            if (b.fish > 0 && n > 0) {
                t.append(Foe.text(String.format(java.util.Locale.ROOT, " %.1f%%", 100.0 * n / b.fish)).formatted(Formatting.DARK_GRAY));
            }
            if (since) {
                long s = b.since(prefix + k.id());
                if (s >= 0) t.append(Foe.text(" (" + StatNames.compact(s) + ")").formatted(Formatting.GRAY));
            }
            rows.add(t);
        }
        return rows;
    }

    private Text dropRow(StatBook b, String label, long count, String key, boolean since) {
        MutableText t = Foe.text(label + " ").formatted(Formatting.GRAY).append(Foe.text(StatNames.number(count)).formatted(Formatting.WHITE));
        if (since) {
            long s = b.since(key);
            if (s >= 0) t.append(Foe.text(" (" + StatNames.compact(s) + ")").formatted(Formatting.GRAY));
        }
        return t;
    }

    private static Text row(String label, String value) {
        return Foe.text(label + " ").formatted(Formatting.GRAY).append(Foe.text(value).formatted(Formatting.WHITE));
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
        context.drawCenteredTextWithShadow(textRenderer, Foe.text(note).formatted(Formatting.DARK_GRAY),
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
            context.drawTextWithShadow(textRenderer, Foe.bold(sec.title()).withColor(Foe.ACCENT & 0xFFFFFF), x, y, 0xFFFFFFFF);
            y += LINE_H + 1;
            for (Text line : sec.lines()) {
                Text shown = textRenderer.getWidth(line) > colW - 6
                        ? Foe.text(textRenderer.trimToWidth(line, colW - 12).getString() + "…")
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
