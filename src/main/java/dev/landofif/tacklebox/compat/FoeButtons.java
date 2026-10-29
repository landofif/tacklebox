package dev.landofif.tacklebox.compat;

import java.lang.reflect.Field;

/** {instance} in the text of foecollab's custom inventory buttons, filled in while the button is built. All by reflection. */
public final class FoeButtons {
    private static final String[] FIELDS = {"name", "description", "icon"};
    private static String[] saved;

    private FoeButtons() {}

    public static void fill(Object button) {
        try {
            String instance = instance();
            saved = new String[FIELDS.length];
            for (int i = 0; i < FIELDS.length; i++) {
                Field f = button.getClass().getField(FIELDS[i]);
                saved[i] = (String) f.get(button);
                if (saved[i] != null && saved[i].contains("{instance}")) {
                    f.set(button, saved[i].replace("{instance}", instance));
                }
            }
        } catch (ReflectiveOperationException | LinkageError e) {
            saved = null;
        }
    }

    public static void restore(Object button) {
        if (saved == null) {
            return;
        }
        try {
            for (int i = 0; i < FIELDS.length; i++) {
                button.getClass().getField(FIELDS[i]).set(button, saved[i]);
            }
        } catch (ReflectiveOperationException e) {
            // the saved button keeps the filled in text until the next save; nothing else to do
        }
        saved = null;
    }

    /** The instance number from foecollab's tab handler ("3"), or a dash before it has read the tab list. */
    private static String instance() throws ReflectiveOperationException {
        Class<?> tab = Class.forName("io.github.foecollab.handler.TabHandler");
        Object handler = tab.getMethod("instance").invoke(null);
        String raw = String.valueOf(tab.getField("instance").get(handler)).trim();
        String digits = raw.replaceAll("\\D", "");
        return !digits.isEmpty() ? digits : raw.isEmpty() ? "-" : raw;
    }
}
