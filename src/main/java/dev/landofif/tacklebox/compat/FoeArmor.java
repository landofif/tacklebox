package dev.landofif.tacklebox.compat;

/**
 * foecollabs "hide armor" ({@code fun.hideArmor}) cancels armor drawing everywhere, the player models in tacklebox's
 * screens included. A screen that draws a model switches it off while it shows one and puts it back when it closes.
 */
public final class FoeArmor {
    private boolean wasHidden;
    private boolean touched;

    public void show(boolean show) {
        if (show == touched || !FoeCollab.loaded()) {
            return;
        }
        try {
            Object cfg = Class.forName("io.github.foecollab.config.FOEConfig").getMethod("getConfig").invoke(null);
            Object fun = cfg.getClass().getField("fun").get(cfg);
            var f = fun.getClass().getField("hideArmor");
            if (show) {
                wasHidden = f.getBoolean(fun);
                f.setBoolean(fun, false);
            } else {
                f.setBoolean(fun, wasHidden);
            }
            touched = show;
        } catch (ReflectiveOperationException | LinkageError e) {
            // a foecollab without the setting, nothing hides the armor
        }
    }
}
