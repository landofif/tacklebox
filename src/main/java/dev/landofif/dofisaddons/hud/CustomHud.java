package dev.landofif.dofisaddons.hud;

import java.util.ArrayList;
import java.util.List;

/** One custom HUD box: its lines of "& colours + %values%", where it sits and how it looks. */
public class CustomHud {
    public String name = "new hud";
    public boolean enabled = true;
    public List<String> lines = new ArrayList<>(List.of("&b&lmy hud", "&7fish &f%session.fish%"));
    /** Anchor point, % of the screen (0..100). */
    public double x = 2;
    public double y = 30;
    public Align align = Align.LEFT;
    /** 1.0 = normal size. */
    public double scale = 1.0;
    /** Background darkness, 0..100. */
    public int background = 85;

    public enum Align {
        LEFT, CENTER, RIGHT;

        public Align next() {
            return values()[(ordinal() + 1) % values().length];
        }
    }

    public CustomHud copy() {
        return HudStore.GSON.fromJson(HudStore.GSON.toJson(this), CustomHud.class);
    }

    public void sanitize() {
        if (name == null) name = "";
        if (lines == null) lines = new ArrayList<>();
        if (align == null) align = Align.LEFT;
        x = Math.max(0, Math.min(100, x));
        y = Math.max(0, Math.min(100, y));
        scale = Math.max(0.5, Math.min(3, scale));
        background = Math.max(0, Math.min(100, background));
    }
}
