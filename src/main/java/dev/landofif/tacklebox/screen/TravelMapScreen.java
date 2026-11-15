package dev.landofif.tacklebox.screen;

import dev.landofif.tacklebox.hud.ServerHud;
import dev.landofif.tacklebox.hud.ServerInfo;
import dev.landofif.tacklebox.menu.Locations;
import dev.landofif.tacklebox.menu.TravelMap;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.ui.Hypr;
import dev.landofif.tacklebox.ui.TackleboxScreen;
import dev.landofif.tacklebox.util.Glyphs;
import it.unimi.dsi.fastutil.ints.Int2ObjectMaps;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.ingame.GenericContainerScreen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.ClickSlotC2SPacket;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.slot.Slot;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.List;

/**
 * The server's travel menu as a world map ({@link TravelMap} has the geometry): each place is the menu's own item at its spot
 * in the world, so it shows the picture the server pack gives it and the server's tooltip. A click on a place is the player's
 * own click on that item, sent as it is: left travels, right opens its species. The wheel zooms, a drag pans, "tile menu"
 * shows the same menu as tiles ({@link FishMenuScreen}). Places the map does not know sit in a row along its bottom.
 */
public class TravelMapScreen extends TackleboxScreen {
    private static final int SEA = 0xFF0F1B26, LAND = 0xFF2F4A3C, FRAME = 0x40FFFFFF;
    /** Pins closer than this are pushed apart; a click this near a pin's middle is on it. */
    private static final int APART = 19, REACH = 9;

    /** A place of the menu on screen. */
    private record Spot(Slot slot, ItemStack stack, Locations.Location loc, int x, int y) {}

    private final GenericContainerScreen original;
    private final ScreenHandler handler;
    private final TravelMap.View view = new TravelMap.View();
    private final List<Spot> spots = new ArrayList<>();
    private int mx, my, mw, mh;
    private boolean dragging;

    public TravelMapScreen(GenericContainerScreen original) {
        super(Foe.text("travel map"));
        this.original = original;
        this.handler = original.getScreenHandler();
    }

    @Override
    protected void init() {
        mw = (int) Math.min(width - 24, (height - 64) * TravelMap.ASPECT);
        mh = (int) (mw / TravelMap.ASPECT);
        mx = (width - mw) / 2;
        my = 24 + (height - 64 - mh) / 2;
        addDrawableChild(ButtonWidget.builder(Foe.text("tile menu"), b -> client.setScreen(new FishMenuScreen(original)))
                .dimensions(mx, my + mh + 6, 70, 20).build());
    }

    /** The menu's places where they are on screen now: known ones on the map, pushed apart, the rest in a row along the bottom. */
    private void layout() {
        spots.clear();
        List<Slot> slots = new ArrayList<>();
        List<String> ids = new ArrayList<>();
        for (Slot slot : handler.slots) {
            ItemStack stack = slot.getStack();
            if (slot.inventory instanceof net.minecraft.entity.player.PlayerInventory || stack.isEmpty()) {
                continue;
            }
            Locations.Location loc = Locations.byName(Glyphs.plain(stack.getName()));
            NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
            if (loc != null || data != null && Locations.isTravelItem(data.copyNbt())) {
                slots.add(slot);
                ids.add(loc == null ? "" : loc.id());
            }
        }
        double[][] at = TravelMap.place(ids, view, mx, my, mw, mh, APART);
        int extra = 0;
        for (int i = 0; i < slots.size(); i++) {
            Locations.Location loc = Locations.byName(Glyphs.plain(slots.get(i).getStack().getName()));
            int x = at[i] != null ? (int) Math.round(at[i][0]) : mx + 12 + extra++ * 20;
            int y = at[i] != null ? (int) Math.round(at[i][1]) : my + mh - 12;
            spots.add(new Spot(slots.get(i), slots.get(i).getStack(), loc, x, y));
        }
    }

    private Spot spotAt(double x, double y) {
        if (x < mx || x >= mx + mw || y < my || y >= my + mh) {
            return null;
        }
        double[][] pts = new double[spots.size()][];
        for (int i = 0; i < pts.length; i++) {
            pts[i] = new double[]{spots.get(i).x, spots.get(i).y};
        }
        int i = TravelMap.hit(pts, x, y, REACH);
        return i < 0 ? null : spots.get(i);
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        super.render(ctx, mouseX, mouseY, delta);
        ctx.fill(mx - 1, my - 1, mx + mw + 1, my + mh + 1, FRAME);
        ctx.fill(mx, my, mx + mw, my + mh, SEA);
        ctx.enableScissor(mx, my, mx + mw, my + mh);
        for (double[] outline : TravelMap.LAND) {
            fill(ctx, outline, LAND);
        }
        for (double[] outline : TravelMap.WATER) {
            fill(ctx, outline, SEA);
        }
        layout();
        Spot hover = spotAt(mouseX, mouseY);
        String here = ServerInfo.locationId.startsWith("spawn") ? "cypress" : ServerInfo.locationId;
        List<int[]> taken = new ArrayList<>();
        for (Spot s : spots) {
            taken.add(new int[]{s.x - 9, s.y - 9, s.x + 9, s.y + 9});
        }
        for (Spot s : spots) {
            int mark = Locations.mark(s.stack.getName().getString(), ServerHud.fabledPlace());
            boolean current = s.loc != null && (s.loc.id().equals(here) || here.startsWith(s.loc.id()));
            if (mark != 0 || current || s == hover) {
                Hypr.rrect(ctx, s.x - 10, s.y - 10, 20, 20, 10, mark != 0 ? mark : current ? Hypr.acc1() : 0x66FFFFFF);
            }
            ctx.drawItem(s.stack, s.x - 8, s.y - 8);
            if (s.loc != null) { // the name under the pin, where nothing else is
                Text name = Foe.text(s.loc.name());
                int w = textRenderer.getWidth(name);
                int[] box = {s.x - w / 2, s.y + 10, s.x + w / 2 + 1, s.y + 19};
                if (taken.stream().noneMatch(t -> box[0] < t[2] && box[2] > t[0] && box[1] < t[3] && box[3] > t[1])) {
                    taken.add(box);
                    ctx.drawTextWithShadow(textRenderer, name, box[0], box[1], mark != 0 ? mark : Foe.TEXT);
                }
            }
        }
        ctx.disableScissor();
        Text hint = Foe.text("left click travels · right click species · wheel zooms · drag moves");
        ctx.drawTextWithShadow(textRenderer, hint, mx + mw - textRenderer.getWidth(hint), my + mh + 12, Foe.DIM);
        if (hover != null) {
            ctx.drawItemTooltip(textRenderer, hover.stack, mouseX, mouseY);
        }
    }

    /** One outline filled row by row, inside the map area. */
    private void fill(DrawContext ctx, double[] outline, int color) {
        double[][] p = TravelMap.project(outline, view, mx, my, mw, mh);
        double top = Double.MAX_VALUE, bottom = -Double.MAX_VALUE;
        for (double y : p[1]) {
            top = Math.min(top, y);
            bottom = Math.max(bottom, y);
        }
        for (int y = Math.max(my, (int) Math.floor(top)); y < Math.min(my + mh, (int) Math.ceil(bottom)); y++) {
            double[] xs = TravelMap.crossings(p[0], p[1], y);
            for (int i = 0; i + 1 < xs.length; i += 2) {
                int x0 = Math.max(mx, (int) Math.round(xs[i])), x1 = Math.min(mx + mw, (int) Math.round(xs[i + 1]));
                if (x1 > x0) {
                    ctx.fill(x0, y, x1, y + 1, color);
                }
            }
        }
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (super.mouseClicked(click, doubled)) {
            return true;
        }
        Spot s = spotAt(click.x(), click.y());
        if (s != null && (click.button() == 0 || click.button() == 1)) {
            // the click as the server expects it, without the client picking the item up first
            client.getNetworkHandler().sendPacket(new ClickSlotC2SPacket(handler.syncId, handler.getRevision(), (short) s.slot.id,
                    (byte) click.button(), SlotActionType.PICKUP, Int2ObjectMaps.emptyMap(), net.minecraft.screen.sync.ItemStackHash.EMPTY));
            return true;
        }
        dragging = click.button() == 0;
        return dragging;
    }

    @Override
    public boolean mouseDragged(Click click, double dx, double dy) {
        if (dragging) {
            view.drag(dx, dy, mw, mh);
            return true;
        }
        return super.mouseDragged(click, dx, dy);
    }

    @Override
    public boolean mouseReleased(Click click) {
        dragging = false;
        return super.mouseReleased(click);
    }

    @Override
    public boolean mouseScrolled(double x, double y, double horizontal, double vertical) {
        view.zoomAt(x, y, mx, my, mw, mh, vertical);
        return true;
    }

    @Override
    public boolean keyPressed(net.minecraft.client.input.KeyInput input) {
        if (client.options.inventoryKey.matchesKey(input)) { // e closes it like the chest
            close();
            return true;
        }
        return super.keyPressed(input);
    }

    @Override
    public void close() {
        client.player.closeHandledScreen();
    }
}
