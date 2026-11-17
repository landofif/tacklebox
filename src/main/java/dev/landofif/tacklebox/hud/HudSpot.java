package dev.landofif.tacklebox.hud;

import dev.landofif.tacklebox.TackleboxSettings;

/** What every movable hud saves: whether it shows, where it sits ({@link HudPos}), its size in percent and how its text lines up. */
public final class HudSpot {
    public boolean on;
    public HudPos pos;
    public int scale = 100;
    public TackleboxSettings.HudAlign align = TackleboxSettings.HudAlign.LEFT;
    /** The hud's title above its rows; off by default, like every hud heading (luna, 2026-11-17: "remove those titles by default"). */
    public boolean heading;
    /** The spot it was put at when new; right click in the mover puts it back there. */
    public transient int homeX, homeY;

    public HudSpot() {}

    public HudSpot(int left, int top) {
        pos = new HudPos(left, top);
        homeX = left;
        homeY = top;
    }

    /** After loading: a spot missing in older settings, and the home that is not saved. */
    public HudSpot fixed(int left, int top) {
        if (pos == null) {
            pos = new HudPos(left, top);
        }
        if (align == null) {
            align = TackleboxSettings.HudAlign.LEFT;
        }
        if (scale <= 0) {
            scale = 100;
        }
        homeX = left;
        homeY = top;
        return this;
    }

    public void reset() {
        scale = 100;
        pos = new HudPos(homeX, homeY);
    }

    /** Scroll in the mover: 5 percent a step, 50 to 300. */
    public void resize(int step) {
        scale = Math.max(50, Math.min(300, scale + step * 5));
    }
}
