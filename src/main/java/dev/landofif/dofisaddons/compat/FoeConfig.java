package dev.landofif.dofisaddons.compat;

import dev.landofif.dofisaddons.DofisAddons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.resource.language.I18n;
import net.minecraft.util.Formatting;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * foecollab's settings, read by reflection so they can show as rows in the mods own settings screen
 * (settings → foecollab). Works off foecollab's cloth config class the same way cloth builds its own
 * screen: fields in order, {@code @Category} per top level field, nested objects as sections,
 * {@code @BoundedDiscrete} as sliders, {@code @ColorPicker} as colours, {@code @Excluded} left out, and
 * foecollab's own names and tooltips from its lang file. Changes go straight into foecollab's live config
 * object and are written with its config holder.
 */
public final class FoeConfig {
    private static final String CONFIG = "io.github.foecollab.config.FOEConfig";
    private static final String KEY = "text.autoconfig.foecollab.";
    private static final String ENTRY = "me.shedaniel.autoconfig.annotation.ConfigEntry$";

    public enum Kind { BOOL, INT, SLIDER, COLOR, LONG, STRING, ENUM }

    public sealed interface Node permits Header, Option {}

    /** A category (depth 0) or a section inside one. */
    public record Header(String label, int depth) implements Node {}

    public record Option(String label, String tooltip, String prefix, int depth, Kind kind, int min, int max,
                         Object owner, Field field, Object def) implements Node {
        public Object get() {
            try {
                return field.get(owner);
            } catch (IllegalAccessException e) {
                return def;
            }
        }

        public void set(Object value) {
            try {
                field.set(owner, value);
            } catch (IllegalAccessException | IllegalArgumentException e) {
                DofisAddons.LOGGER.warn("Could not set foecollab option {}", field.getName(), e);
            }
        }

        @SuppressWarnings("unchecked")
        public Enum<?>[] constants() {
            return ((Class<? extends Enum<?>>) field.getType()).getEnumConstants();
        }
    }

    private FoeConfig() {}

    public static boolean available() {
        return FabricLoader.getInstance().isModLoaded("foecollab") && live() != null;
    }

    private static Object live() {
        try {
            return Class.forName(CONFIG).getMethod("getConfig").invoke(null);
        } catch (ReflectiveOperationException | LinkageError e) {
            return null;
        }
    }

    /** Every category, section and option, in foecollab's order. Empty if foecollab is missing. */
    public static List<Node> nodes() {
        Object config = live();
        if (config == null) {
            return List.of();
        }
        Object defaults;
        try {
            defaults = config.getClass().getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            defaults = config;
        }
        // top level fields grouped by category, categories in the order they first appear
        Map<String, List<Field>> byCategory = new LinkedHashMap<>();
        for (Field f : fields(config.getClass())) {
            Annotation category = annotation(f, "Category");
            String name = category == null ? "default" : String.valueOf(value(category, "value"));
            byCategory.computeIfAbsent(name, k -> new ArrayList<>()).add(f);
        }
        List<Node> out = new ArrayList<>();
        for (Map.Entry<String, List<Field>> e : byCategory.entrySet()) {
            out.add(new Header(label(KEY + "category." + e.getKey(), e.getKey()), 0));
            for (Field f : e.getValue()) {
                walk(out, f, config, defaults, f.getName(), 1);
            }
        }
        return out;
    }

    private static void walk(List<Node> out, Field f, Object owner, Object defOwner, String path, int depth) {
        Object value = read(f, owner);
        Object def = read(f, defOwner);
        String key = KEY + "option." + path;
        String label = label(key, f.getName());
        String tooltip = tooltip(key);
        String prefix = I18n.hasTranslation(key + ".@PrefixText") ? clean(I18n.translate(key + ".@PrefixText")) : null;
        Class<?> type = f.getType();
        Kind kind = null;
        int min = 0, max = 0;
        if (type == boolean.class) {
            kind = Kind.BOOL;
        } else if (type == int.class) {
            Annotation bounded = annotation(f, "BoundedDiscrete");
            if (annotation(f, "ColorPicker") != null) {
                kind = Kind.COLOR;
            } else if (bounded != null) {
                kind = Kind.SLIDER;
                min = (int) ((Number) value(bounded, "min")).longValue();
                max = (int) ((Number) value(bounded, "max")).longValue();
            } else {
                kind = Kind.INT;
            }
        } else if (type == long.class) {
            kind = Kind.LONG;
        } else if (type == String.class) {
            kind = Kind.STRING;
        } else if (type.isEnum()) {
            kind = Kind.ENUM;
        }
        if (kind != null) {
            out.add(new Option(label, tooltip, prefix, depth, kind, min, max, owner, f, def));
            return;
        }
        if (value == null || type.isPrimitive() || type.isArray() || type.getName().startsWith("java.")) {
            return; // lists and maps have no row; foecollab edits those in its own screens
        }
        out.add(new Header(label, depth));
        for (Field child : fields(type)) {
            walk(out, child, value, def == null ? value : def, path + "." + child.getName(), depth + 1);
        }
    }

    private static List<Field> fields(Class<?> type) {
        List<Field> out = new ArrayList<>();
        for (Field f : type.getDeclaredFields()) {
            int m = f.getModifiers();
            if (Modifier.isStatic(m) || Modifier.isTransient(m) || !Modifier.isPublic(m) || annotation(f, "Gui$Excluded") != null) {
                continue;
            }
            out.add(f);
        }
        return out;
    }

    private static Object read(Field f, Object owner) {
        try {
            return f.get(owner);
        } catch (IllegalAccessException e) {
            return null;
        }
    }

    private static Annotation annotation(Field f, String name) {
        for (Annotation a : f.getAnnotations()) {
            if (a.annotationType().getName().equals(ENTRY + name)) {
                return a;
            }
        }
        return null;
    }

    private static Object value(Annotation a, String method) {
        try {
            return a.annotationType().getMethod(method).invoke(a);
        } catch (ReflectiveOperationException e) {
            return "";
        }
    }

    private static String label(String key, String fallback) {
        if (I18n.hasTranslation(key)) {
            return clean(I18n.translate(key));
        }
        // fooBarBaz → foo bar baz
        return fallback.replaceAll("([a-z0-9])([A-Z])", "$1 $2").replace('_', ' ').toLowerCase(Locale.ROOT);
    }

    private static String tooltip(String key) {
        if (I18n.hasTranslation(key + ".@Tooltip")) {
            return clean(I18n.translate(key + ".@Tooltip"));
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; I18n.hasTranslation(key + ".@Tooltip[" + i + "]"); i++) {
            if (i > 0) sb.append('\n');
            sb.append(clean(I18n.translate(key + ".@Tooltip[" + i + "]")));
        }
        return sb.isEmpty() ? null : sb.toString();
    }

    /** Lower case without formatting codes, apostrophes or capitals, like every other text in the mod. */
    static String clean(String s) {
        String plain = Formatting.strip(s);
        if (plain == null) {
            return "";
        }
        String out = plain.replace("'", "").replace("’", "").toLowerCase(Locale.ROOT).trim();
        // "show the timer." reads oddly before ": on"
        return out.endsWith(".") && !out.endsWith("..") ? out.substring(0, out.length() - 1) : out;
    }

    /** An enum value as foecollab names it, tidied the same way. */
    public static String enumLabel(Object e) {
        return e == null ? "" : clean(e.toString().replace('_', ' '));
    }

    /** Writes foecollab's config file, as its own screen does on save. */
    public static void save() {
        try {
            Class<?> autoConfig = Class.forName("me.shedaniel.autoconfig.AutoConfig");
            Object holder = autoConfig.getMethod("getConfigHolder", Class.class).invoke(null, Class.forName(CONFIG));
            holder.getClass().getMethod("save").invoke(holder);
        } catch (ReflectiveOperationException | LinkageError e) {
            DofisAddons.LOGGER.warn("Could not save the foecollab config", e);
        }
    }
}
