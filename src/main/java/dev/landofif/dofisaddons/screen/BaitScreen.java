package dev.landofif.dofisaddons.screen;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.hud.ServerInfo;
import dev.landofif.dofisaddons.menu.BaitPicker;
import dev.landofif.dofisaddons.menu.BaitPicker.Row;
import dev.landofif.dofisaddons.menu.Locations;
import dev.landofif.dofisaddons.ui.DofiScreen;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Which quest finishes fastest at a location and which bait to fish it with (the model is menu/BaitPicker). Luck and
 * scale come from the armour you wear or one of the presets seen on /presets, the level from the compendium.
 */
public class BaitScreen extends DofiScreen {
    private static final int LEFT = 10, LW = 150, TOP = 24, ROW = 36, CARD = 48;
    /** Quest index of the rarity quests: rare, epic, legendary roll rarity 1, 2, 3. */
    private static final int RARE = 2;

    private final Screen parent;
    private final DofiSettings s = DofiSettings.get();
    private Locations.Location place;
    private int quest = 3;
    private int scroll;

    private Map<String, Double> stats = Map.of();
    private int armorLuck, armorScale, luck, scale, level;
    private boolean levelKnown;
    private double[] odds, vals;
    private double[] casts;
    private int[] need;
    private int top;
    private List<Row> shown = List.of();
    private final List<List<Row>> best = new ArrayList<>();

    private ButtonWidget source, luresBtn, biggerBtn;
    private final ButtonWidget[] tierBtn = new ButtonWidget[5];

    public BaitScreen(Screen parent) {
        super(Foe.text("dofis addons · bait picker"));
        this.parent = parent;
        place = Locations.byName(ServerInfo.locationName);
        if (place == null) {
            place = Locations.ALL.stream().filter(l -> l.id().equals(s.baitLoc)).findFirst().orElse(Locations.ALL.get(0));
        }
    }

    @Override
    protected void init() {
        int y = TOP;
        int at = Locations.ALL.indexOf(place);
        addDrawableChild(new ValueSlider(LEFT, y, LW, 20, "", 0, Locations.ALL.size() - 1, 1, "", at, v -> {
            place = Locations.ALL.get((int) v);
            s.baitLoc = place.id();
            refresh();
        }).format(v -> Locations.ALL.get((int) v).name()));
        source = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            List<String> all = sources();
            s.baitSource = all.get((all.indexOf(s.baitSource) + 1) % all.size());
            refresh();
        }).dimensions(LEFT, y += 24, LW, 20).tooltip(Tooltip.of(Foe.sentence("Where luck and scale come from: the armour you wear, or a preset you have opened on /presets."))).build());
        addDrawableChild(new ValueSlider(LEFT, y += 24, LW, 20, "extra luck", 0, 2000, 25, "", s.baitLuck, v -> {
            s.baitLuck = (int) v;
            refresh();
        }));
        addDrawableChild(new ValueSlider(LEFT, y += 24, LW, 20, "extra scale", 0, 3000, 25, "", s.baitScale, v -> {
            s.baitScale = (int) v;
            refresh();
        }));
        int tw = LW / 5;
        for (int r = 0; r < 5; r++) {
            int bit = 1 << r;
            ButtonWidget b = ButtonWidget.builder(Text.empty(), x -> {
                s.baitTiers ^= bit;
                refresh();
            }).dimensions(LEFT + r * tw, y += r == 0 ? 24 : 0, tw, 20)
                    .tooltip(Tooltip.of(Foe.sentence(BaitPicker.RARITY[r] + " baits and lures"))).build();
            tierBtn[r] = addDrawableChild(b);
        }
        luresBtn = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            s.baitLures = !s.baitLures;
            refresh();
        }).dimensions(LEFT, y += 24, LW, 20).build());
        biggerBtn = addDrawableChild(ButtonWidget.builder(Text.empty(), b -> {
            s.baitBigger = !s.baitBigger;
            refresh();
        }).dimensions(LEFT, y += 24, LW, 20).tooltip(Tooltip.of(Foe.sentence("Adult and large quests also count the bigger sizes."))).build());
        refresh();
    }

    private List<String> sources() {
        List<String> all = new ArrayList<>(List.of("worn"));
        s.presetStats.keySet().stream().sorted(java.util.Comparator.comparingInt(k -> Integer.parseInt(k.substring(k.indexOf(':') + 1)))).forEach(all::add);
        return all;
    }

    /** Re-reads the armour and the level, recomputes every quest and relabels the buttons. */
    private void refresh() {
        List<String> all = sources();
        if (!all.contains(s.baitSource)) {
            s.baitSource = "worn";
        }
        if (s.baitSource.equals("worn")) {
            List<ItemStack> worn = new ArrayList<>();
            if (client != null && client.player != null) {
                for (EquipmentSlot slot : List.of(EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET)) {
                    worn.add(client.player.getEquippedStack(slot));
                }
            }
            stats = BaitPicker.armorStats(worn);
        } else {
            stats = s.presetStats.getOrDefault(s.baitSource, Map.of());
        }
        armorLuck = BaitPicker.stat(stats, place, "luck");
        armorScale = BaitPicker.stat(stats, place, "scale");
        luck = armorLuck + s.baitLuck;
        scale = armorScale + s.baitScale;
        String saved = s.locationLevels.get(place.id());
        levelKnown = saved != null;
        level = levelKnown ? Integer.parseInt(saved.substring(0, saved.indexOf('|'))) : 1;

        BaitPicker.Loc loc = BaitPicker.loc(place);
        boolean[] tiers = new boolean[5];
        for (int r = 0; r < 5; r++) {
            tiers[r] = (s.baitTiers >> r & 1) != 0;
        }
        odds = BaitPicker.odds(luck);
        double[] size = BaitPicker.sizeOdds(scale);
        vals = new double[5];
        vals[0] = s.baitBigger ? size[2] + size[3] + size[4] : size[2];
        vals[1] = s.baitBigger ? size[3] + size[4] : size[3];
        best.clear();
        for (int q = 0; q < 5; q++) {
            List<Row> rows = q < RARE ? List.of() : BaitPicker.rows(loc, luck, q - 1, tiers, s.baitLures);
            best.add(rows);
            if (q >= RARE) {
                double base = odds[q - 1];
                vals[q] = !rows.isEmpty() && rows.get(0).chance() > base + .0005 ? rows.get(0).chance() : base;
            }
        }
        need = new int[5];
        casts = new double[5];
        top = 0;
        for (int q = 0; q < 5; q++) {
            need[q] = BaitPicker.catches(q, level);
            casts[q] = vals[q] > 0 ? need[q] / vals[q] : Double.MAX_VALUE;
            if (casts[q] < casts[top]) {
                top = q;
            }
        }
        pickList();

        source.setMessage(Foe.text("armor: " + (s.baitSource.equals("worn") ? "worn" : "preset " + s.baitSource.substring(s.baitSource.indexOf(':') + 1))));
        for (int r = 0; r < 5; r++) {
            tierBtn[r].setMessage(Text.literal(Foe.smallCaps(String.valueOf(BaitPicker.RL.charAt(r)))).formatted(tiers[r] ? Formatting.GREEN : Formatting.DARK_GRAY));
        }
        luresBtn.setMessage(Foe.text("lures: " + (s.baitLures ? "on" : "off")));
        biggerBtn.setMessage(Foe.text("bigger sizes: " + (s.baitBigger ? "on" : "off")));
    }

    private void pickList() {
        scroll = 0;
        if (quest < RARE) {
            shown = List.of();
            return;
        }
        double base = odds[quest - 1];
        shown = best.get(quest).stream().filter(r -> r.chance() - base >= .03).toList();
    }

    private int listX() {
        return LEFT + LW + 12;
    }

    private int cardW() {
        return (width - listX() - 10 - 4 * 3) / 5;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        for (int q = 0; q < 5; q++) {
            int x = listX() + q * (cardW() + 3);
            if (click.x() >= x && click.x() < x + cardW() && click.y() >= TOP && click.y() < TOP + CARD) {
                quest = q;
                pickList();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mx, double my, double horizontal, double vertical) {
        if (mx >= listX()) {
            scroll = Math.max(0, Math.min(maxScroll(), scroll - (int) (vertical * ROW)));
            return true;
        }
        return super.mouseScrolled(mx, my, horizontal, vertical);
    }

    private int listTop() {
        return TOP + CARD + 52;
    }

    private int listH() {
        return height - 24 - listTop();
    }

    private int maxScroll() {
        return Math.max(0, shown.size() * ROW - listH());
    }

    private static String pct(double x) {
        return String.format(Locale.ROOT, x >= 0.095 || x == 0 ? "%.0f%%" : "%.1f%%", x * 100);
    }

    /** Tinted greys (a little blue, never pure grey) and the bait rarity colours the server uses, common to mythical. */
    private static final int MUTED = 0xFF9AA4B2, QUIET = 0xFF5F6877, RULE = 0x22FFFFFF, SELECTED = 0x2855FF55;
    private static final int[] RARITY = {0xFFFFFF, 0x2B85C4, 0x1CD832, 0xD98103, 0xC93832};

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        int white = Foe.TEXT;

        // what feeds the numbers: label left, value right, in one column under the controls
        int y = TOP + 24 * 6 + 28;
        stat(ctx, "luck", armorLuck + " + " + s.baitLuck + " = " + luck, y, white);
        stat(ctx, "scale", armorScale + " + " + s.baitScale + " = " + scale, y + 12, white);
        stat(ctx, "level here", levelKnown ? String.valueOf(level) : "unknown", y + 24, levelKnown ? white : Foe.ACCENT);
        if (!levelKnown) {
            ctx.drawTextWithShadow(textRenderer, Foe.sentence("Open the fish compendium to read it."), LEFT, y + 38, MUTED);
        }

        // the five quests: a tint for the picked one, the casts as the number to read, a bar for how it compares
        int x0 = listX(), cw = cardW();
        double fastest = casts[top];
        for (int q = 0; q < 5; q++) {
            int x = x0 + q * (cw + 3);
            boolean hot = mouseX >= x && mouseX < x + cw && mouseY >= TOP && mouseY < TOP + CARD;
            ctx.fill(x, TOP, x + cw, TOP + CARD, q == quest ? SELECTED : hot ? 0x30FFFFFF : 0x30000000);
            ctx.drawTextWithShadow(textRenderer, Foe.text(QUEST_NAMES[q]), x + 6, TOP + 5, q == quest ? Foe.ACCENT : MUTED);
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate(x + 6, TOP + 16);
            m.scale(1.5f, 1.5f);
            ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(Math.round(casts[q]))), 0, 0, q == top ? Foe.TITLE : white);
            m.popMatrix();
            ctx.drawTextWithShadow(textRenderer, Foe.text("casts"), x + 6 + (int) (textRenderer.getWidth(String.valueOf(Math.round(casts[q]))) * 1.5f) + 4, TOP + 21, QUIET);
            ctx.drawTextWithShadow(textRenderer, Text.literal(pct(vals[q]) + " · " + need[q] + " catches"), x + 6, TOP + 33, MUTED);
            int bar = (int) ((cw - 12) * Math.max(0.05, Math.min(1, fastest / casts[q])));
            ctx.fill(x + 6, TOP + CARD - 3, x + cw - 6, TOP + CARD - 2, 0x22FFFFFF);
            ctx.fill(x + 6, TOP + CARD - 3, x + 6 + bar, TOP + CARD - 2, q == top ? Foe.TITLE : QUIET);
        }

        // the answer, as a headline and a line under it
        int ay = TOP + CARD + 8;
        int w = width - x0 - 10;
        List<Row> topRows = best.get(top);
        boolean bait = top >= RARE && vals[top] > odds[top - 1] + .0005;
        String head = top < RARE ? "Go for " + QUEST_NAMES[top] + " quests" : bait ? "Use " + topRows.get(0).bait().name() : "No bait beats the base odds";
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(x0, ay);
        m.scale(1.5f, 1.5f);
        ctx.drawTextWithShadow(textRenderer, Foe.sentence(head), 0, 0, Foe.ACCENT);
        m.popMatrix();
        String why = QUEST_NAMES[top] + " quests finish fastest, about " + Math.round(casts[top]) + " casts."
                + (bait ? " " + pct(vals[top]) + " per cast against " + pct(odds[top - 1]) + " with no bait." : top < RARE ? " Bait does not change size." : "");
        ctx.drawTextWithShadow(textRenderer, Foe.sentence(why), x0, ay + 16, MUTED);

        // the ranked baits for the picked quest
        int ly = listTop();
        if (quest < RARE) {
            ctx.drawTextWithShadow(textRenderer, Foe.sentence("A " + QUEST_NAMES[quest] + " is " + pct(vals[quest]) + " of casts at scale " + scale
                    + (s.baitBigger ? " (this size or bigger)" : "") + ". Bait does not change size, so compare this number with the other cards."), x0, ly, MUTED);
            return;
        }
        ctx.drawTextWithShadow(textRenderer, Foe.text(QUEST_NAMES[quest] + " quest · no bait " + pct(odds[quest - 1]) + " per cast"), x0, ly - 14, Foe.TITLE);
        if (shown.isEmpty()) {
            ctx.drawTextWithShadow(textRenderer, Foe.sentence("Nothing here beats the base odds by 3 points. Try a different quest or more bait tiers."), x0, ly + 2, MUTED);
            return;
        }
        double base = odds[quest - 1];
        int right = width - 14;
        ctx.enableScissor(x0, ly, width - 6, ly + listH());
        for (int i = 0; i < shown.size(); i++) {
            int ry = ly + i * ROW - scroll;
            if (ry + ROW < ly || ry > ly + listH()) {
                continue;
            }
            Row r = shown.get(i);
            var b = r.bait();
            if (i > 0) {
                ctx.fill(x0, ry - 4, right, ry - 3, RULE);
            }
            // line 1: rank, name in the baits rarity colour, the chance on the right with its bar
            ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(i + 1)), x0, ry, QUIET);
            ctx.drawTextWithShadow(textRenderer, Text.literal(b.name()), x0 + 12, ry, i == 0 ? Foe.ACCENT : white);
            ctx.drawTextWithShadow(textRenderer, Text.literal(pct(r.chance())), right - 70 - textRenderer.getWidth(pct(r.chance())) - 6, ry, Foe.TITLE);
            int bx = right - 70;
            ctx.fill(bx, ry + 3, bx + 70, ry + 6, 0x22FFFFFF);
            ctx.fill(bx, ry + 3, bx + (int) (70 * r.chance()), ry + 6, Foe.TITLE);
            ctx.fill(bx + (int) (70 * base), ry + 1, bx + (int) (70 * base) + 1, ry + 8, Foe.ACCENT);
            // line 2: what the bait is and how far it beats no bait
            String lure = b.lure() ? " · lure x" + BaitPicker.uses(b) : "";
            String facts = BaitPicker.RARITY[b.rarity()] + lure + " · " + b.likelihood() + "% likelihood · pool " + pool(r.pool())
                    + " · +" + Math.round((r.chance() - base) * 100) + " pts" + (r.same().isEmpty() ? "" : " · same pool: " + String.join(", ", r.same()));
            ctx.drawTextWithShadow(textRenderer, Text.literal(textRenderer.trimToWidth(facts, right - x0 - 12)), x0 + 12, ry + 11, MUTED);
            // line 3: the fish, each rarity letter in its colour
            int fx = x0 + 12;
            for (int q = 0; q < 5; q++) {
                if (r.fish().get(q).isEmpty() || fx > right - 40) {
                    continue;
                }
                String names = String.join(", ", r.fish().get(q));
                ctx.drawTextWithShadow(textRenderer, Text.literal(String.valueOf(Character.toUpperCase(BaitPicker.RL.charAt(q)))), fx, ry + 22, 0xFF000000 | RARITY[q]);
                String shownNames = textRenderer.trimToWidth(names, Math.max(20, right - fx - 14));
                ctx.drawTextWithShadow(textRenderer, Text.literal(shownNames), fx + 8, ry + 22, QUIET);
                fx += 8 + textRenderer.getWidth(shownNames) + 12;
            }
        }
        ctx.disableScissor();
        ctx.drawTextWithShadow(textRenderer, Foe.sentence("A roll below a bait's whole pool gets a normal fish, so bait odds are a lower bound. Hard quest sizes are fitted estimates."),
                x0, height - 14, QUIET);
    }

    private void stat(DrawContext ctx, String label, String value, int y, int color) {
        ctx.drawTextWithShadow(textRenderer, Foe.text(label), LEFT, y, MUTED);
        ctx.drawTextWithShadow(textRenderer, Text.literal(value), LEFT + LW - textRenderer.getWidth(value), y, color);
    }

    private static final String[] QUEST_NAMES = {"adult", "large", "rare", "epic", "legendary"};

    private static String pool(int[] p) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            if (p[i] > 0) {
                b.append(b.length() > 0 ? " " : "").append(p[i]).append(BaitPicker.RL.charAt(i));
            }
        }
        return b.toString();
    }

    @Override
    public void close() {
        DofiSettings.save();
        client.setScreen(parent);
    }
}
