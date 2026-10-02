package dev.landofif.dofisaddons.display;

import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;

import java.text.Normalizer;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.regex.Pattern;

/**
 * Recognises server-drawn hologram entities (invisible named armor stands and text displays) and
 * classifies their text, for hiding nametags and xp popups. Server text uses small caps, fullwidth
 * digits and fancy pluses, so matching goes through {@link #fold}. Ported from fishutils.
 */
public final class Holograms {
    private static final Pattern XP_SEGMENT = Pattern.compile("\\+[\\d.,]+[a-z]?xp");
    private static final Pattern XP_POPUP = Pattern.compile("(?:\\+[\\d.,]+[a-z]?xp)+");
    private static final String SMALL_CAPS = "ᴀʙᴄᴅᴇꜰɢʜɪᴊᴋʟᴍɴᴏᴘǫʀꜱᴛᴜᴠᴡxʏᴢ";
    private static final String PLUS_SIGNS = "✚✛✜➕﹢";

    private static final Map<Entity, Cached> CACHE = new WeakHashMap<>();
    private static final Set<Entity> OWN_NAMETAGS = Collections.newSetFromMap(new WeakHashMap<>());
    private static String selfNameRaw;
    private static String selfNameFolded = "";

    private Holograms() {}

    /** The text of a hologram entity, or null if it isnt one. */
    public static String text(Entity entity) {
        if (entity instanceof DisplayEntity.TextDisplayEntity display) {
            return display.getText().getString();
        }
        if (entity instanceof ArmorStandEntity stand && stand.isInvisible()) {
            Text name = stand.getCustomName();
            return name != null ? name.getString() : null;
        }
        return null;
    }

    /** Plain lowercase ascii: NFKC, small caps, fancy pluses; everything but a-z 0-9 + . , becomes a space. */
    public static String fold(String raw) {
        String normalized = Normalizer.normalize(raw, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
        StringBuilder folded = new StringBuilder(normalized.length());
        for (int i = 0; i < normalized.length(); i++) {
            char c = normalized.charAt(i);
            int smallCap = SMALL_CAPS.indexOf(c);
            if (smallCap >= 0) {
                c = (char) ('a' + smallCap);
            } else if (PLUS_SIGNS.indexOf(c) >= 0) {
                c = '+';
            }
            boolean keep = (c >= 'a' && c <= 'z') || (c >= '0' && c <= '9') || c == '+' || c == '.' || c == ',';
            folded.append(keep ? c : ' ');
        }
        return folded.toString();
    }

    /** True if the text is nothing but "+66xp" style reward segments. */
    public static boolean isXpPopup(String raw) {
        return XP_POPUP.matcher(fold(raw).replace(" ", "")).matches();
    }

    /** Folded text plus its verdicts, computed once per distinct text. */
    public static final class Cached {
        public final String raw;
        public final String folded;
        public final boolean xpPopup;
        public final boolean xpSegment;

        private Cached(String raw) {
            this.raw = raw;
            this.folded = fold(raw);
            this.xpPopup = XP_POPUP.matcher(folded.replace(" ", "")).matches();
            this.xpSegment = XP_SEGMENT.matcher(folded).find();
        }
    }

    /** Classifies a hologram (cached per entity until its text changes), or null if it isnt one. */
    public static Cached classify(Entity entity) {
        String raw = text(entity);
        if (raw == null) {
            return null;
        }
        Cached c = CACHE.get(entity);
        if (c == null || !c.raw.equals(raw)) {
            c = new Cached(raw);
            CACHE.put(entity, c);
        }
        return c;
    }

    /** Once a tag is known to be yours it stays hidden, so crowd jitter cant make it flicker. */
    public static boolean isKnownOwnNametag(Entity entity) {
        return OWN_NAMETAGS.contains(entity);
    }

    public static void markOwnNametag(Entity entity) {
        OWN_NAMETAGS.add(entity);
    }

    public static String foldedSelfName(String raw) {
        if (!raw.equals(selfNameRaw)) {
            selfNameRaw = raw;
            selfNameFolded = fold(raw);
        }
        return selfNameFolded;
    }
}
