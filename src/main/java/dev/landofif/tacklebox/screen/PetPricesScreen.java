package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.menu.PetPrices;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

/**
 * The pet price table of the community's pet price sheet ({@link PetPrices}), cells exactly as the sheet writes them, with
 * the sheet credited under it. Opening it downloads the sheet when the copy on disk is older than a day.
 */
public class PetPricesScreen extends TackleboxScreen {
    private static final int ROW = 11, PAD = 12;
    private final Screen parent;

    public PetPricesScreen(Screen parent) {
        super(Foe.text("tacklebox · pet prices"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        PetPrices.refresh(System.currentTimeMillis());
        addDrawableChild(ButtonWidget.builder(Foe.text("done"), b -> close()).dimensions(width / 2 - 50, height - 26, 100, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        List<List<String>> rows = PetPrices.table();
        int top = 24, bottom = height - 54;
        if (rows.isEmpty()) {
            String state = PetPrices.busy() ? "downloading the price sheet" : PetPrices.failed() ? "could not download the price sheet. open this again in ten minutes" : "no prices yet";
            ctx.drawCenteredTextWithShadow(textRenderer, Foe.text(state), width / 2, (top + bottom) / 2, Foe.DIM);
        } else {
            int cols = rows.get(0).size();
            int[] w = new int[cols];
            int total = 0;
            for (int c = 0; c < cols; c++) {
                for (List<String> r : rows) {
                    w[c] = Math.max(w[c], textRenderer.getWidth(r.get(c)));
                }
                total += w[c] + PAD;
            }
            total -= PAD;
            int tall = rows.size() * ROW;
            float k = Math.min(1f, Math.min((width - 16f) / total, (bottom - top) / (float) tall)); // shrinks to fit, never grows
            var m = ctx.getMatrices();
            m.pushMatrix();
            m.translate((width - total * k) / 2f, top + ((bottom - top) - tall * k) / 2f);
            m.scale(k, k);
            for (int i = 0; i < rows.size(); i++) {
                int x = 0;
                if (i > 0 && i % 2 == 0) {
                    ctx.fill(-4, i * ROW - 1, total + 4, i * ROW + ROW - 1, 0x14FFFFFF);
                }
                for (int c = 0; c < cols; c++) {
                    // the sheet's own words: the header in the accent, the rating column dim, the rest white
                    ctx.drawTextWithShadow(textRenderer, Text.literal(rows.get(i).get(c)), x, i * ROW, i == 0 ? Hypr.acc2() : c == 0 ? Foe.DIM : Foe.TEXT);
                    x += w[c] + PAD;
                }
            }
            m.popMatrix();
        }
        long at = TackleboxSettings.get().petPricesAt;
        String state = PetPrices.busy() ? "downloading" : rows.isEmpty() ? "" : PetPrices.age(at, System.currentTimeMillis());
        ctx.drawCenteredTextWithShadow(textRenderer, Foe.text("prices from the fishonmc community's pet price sheet" + (state.isEmpty() ? "" : " · " + state)),
                width / 2, height - 50, Foe.DIM);
        ctx.drawCenteredTextWithShadow(textRenderer, Text.literal(PetPrices.SHEET), width / 2, height - 39, Foe.DIM);
    }

    @Override
    public void close() {
        client.setScreen(parent);
    }
}
