package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.util.Glyphs;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.ScreenRect;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.text.MutableText;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The catch title (2026-11-14, luna: like foecollab's, but compact): the caught fish's picture, under it the rarity badge and
 * the name, under that the size class with weight and length. The picture is the glyph the server sends as its own catch
 * title, so it shows the moment the fish is caught; the two lines come from the fish item once it is in the inventory
 * ({@link CatchWatch}), and from the server's subtitle when it never gets there (full inventory). Display only.
 */
public final class CatchTitle {
    /** A piece of a line: its text and colour; {@code art} is a server pack picture, drawn in its own colours with no shadow. */
    public record Seg(String text, int rgb, boolean art) {}

    /** What the title says about a fish, from its custom_data and its item name. */
    public record Info(String name, String rarity, String variant, String size, float weight, float length) {}

    /** What is on screen: the picture, the fish (null until its item is read) and the server's subtitle to fall back on. */
    public record View(Text image, Info info, Text fallback) {}

    static final long FALLBACK_MS = 500; // no fish item after this long: the server's subtitle is line 1
    static final long SUBTITLE_MS = 1000; // a subtitle this close to the catch title belongs to it
    static final int IMAGE_HEIGHT = 32, IMAGE_GAP = 2, LINE = 9, LINE_GAP = 1;
    private static final Info SAMPLE = new Info("atlantic cod", "legendary", "trophy", "adult", 12.4f, 31.7f);

    private static Text image, subtitle, lastImage, lastSubtitle;
    private static Info info, lastInfo;
    private static long shownAt, subtitleAt;
    private static Text measured;
    private static float[] bounds;

    private CatchTitle() {}

    // ---------------------------------------------------------------- what it says

    public static Info read(NbtCompound data, String itemName) {
        String name = clean(itemName);
        if (name.isEmpty()) {
            name = data.getString("fish").orElse("").replace('_', ' ');
        }
        return new Info(name, data.getString("rarity").orElse(""), data.getString("variant").orElse("normal"),
                data.getString("size").orElse(""), data.getFloat("weight").orElse(0f), data.getFloat("length").orElse(0f));
    }

    /** An item name without the server's pictures, single spaced. */
    static String clean(String name) {
        StringBuilder out = new StringBuilder();
        if (name != null) {
            name.codePoints().filter(cp -> !Glyphs.isArt(cp) && !Character.isISOControl(cp)).forEach(out::appendCodePoint);
        }
        return out.toString().trim().replaceAll("\\s+", " ");
    }

    /** The server pack's rarity tag picture (research section 2), "" for a rarity we do not know. */
    public static String rarityBadge(String rarity) {
        return switch (rarity) {
            case "common" -> "";
            case "rare" -> "";
            case "epic" -> "";
            case "legendary" -> "";
            case "mythical" -> "";
            case "special" -> "";
            default -> "";
        };
    }

    /** The variant's tag picture, "" for a normal fish or a variant we do not know. */
    public static String variantBadge(String variant) {
        return switch (variant) {
            case "albino" -> "";
            case "melanistic" -> "";
            case "trophy" -> "";
            case "fabled" -> "";
            case "alternate" -> "";
            case "spooky" -> "";
            case "frozen" -> "";
            default -> "";
        };
    }

    private static final List<String> RARITIES = List.of("common", "rare", "epic", "legendary", "mythical", "special");

    /** The rarity whose badge a text holds (the server's catch subtitle, research section 2), "" when it has none. */
    public static String rarityOf(String text) {
        for (String r : RARITIES) {
            if (text.contains(rarityBadge(r))) {
                return r;
            }
        }
        return "";
    }

    /** "fabled" or "trophy" when a text holds that variant's badge, else "normal": all a big catch needs from a subtitle. */
    public static String variantOf(String text) {
        for (String v : List.of("fabled", "trophy")) {
            if (text.contains(variantBadge(v))) {
                return v;
            }
        }
        return "normal";
    }

    /** The server's colour of a size word, the text colour for one we do not know. */
    static int sizeColor(String size, int other) {
        return switch (size) {
            case "baby" -> 0x468CE7;
            case "juvenile" -> 0x22EA08;
            case "adult" -> 0x1C7DA0;
            case "large" -> 0xFF9000;
            case "gigantic" -> 0xAF3333;
            default -> other;
        };
    }

    /** Line 1: rarity badge, variant badge, name. Empty when all of them are off. */
    public static List<Seg> line1(Info f, TackleboxSettings s) {
        List<String> badges = new ArrayList<>();
        if (s.catchRarity && !rarityBadge(f.rarity()).isEmpty()) {
            badges.add(rarityBadge(f.rarity()));
        }
        if (s.catchVariant && !variantBadge(f.variant()).isEmpty()) {
            badges.add(variantBadge(f.variant()));
        }
        List<Seg> out = new ArrayList<>();
        for (String b : badges) {
            if (!out.isEmpty()) {
                out.add(new Seg(" ", 0xFFFFFF, false));
            }
            out.add(new Seg(b, 0xFFFFFF, true));
        }
        if (s.catchName && !f.name().isEmpty()) {
            int rarity = Rarity.color(f.rarity());
            String gap = out.isEmpty() ? "" : " ";
            out.add(new Seg(gap + Foe.smallCaps(f.name()), s.catchNameRarity && rarity >= 0 ? rarity : s.catchColor, false));
        }
        return out;
    }

    /** Line 2: the size class in its colour, then "12.4lb / 31.7in". Empty when all of them are off. */
    public static List<Seg> line2(Info f, TackleboxSettings s) {
        List<Seg> out = new ArrayList<>();
        if (s.catchSize && !f.size().isEmpty()) {
            out.add(new Seg(Foe.smallCaps(f.size()), sizeColor(f.size(), s.catchColor), false));
        }
        String numbers = "";
        if (s.catchWeight) {
            numbers = number(f.weight()) + "lb";
        }
        if (s.catchLength) {
            numbers += (numbers.isEmpty() ? "" : " / ") + number(f.length()) + "in";
        }
        if (!numbers.isEmpty()) {
            out.add(new Seg((out.isEmpty() ? "" : " ") + Foe.smallCaps(numbers), s.catchColor, false));
        }
        return out;
    }

    static String number(float v) {
        return String.format(Locale.ROOT, "%.1f", v);
    }

    /** Where the rows sit under each other: {picture y, line 1 y, line 2 y, whole height}; a row that is not there is -1. */
    static float[] layout(float imageHeight, boolean line1, boolean line2) {
        float y = 0;
        float[] out = {-1, -1, -1, 0};
        if (imageHeight > 0) {
            out[0] = y;
            y += imageHeight + IMAGE_GAP;
        }
        if (line1) {
            out[1] = y;
            y += LINE + LINE_GAP;
        }
        if (line2) {
            out[2] = y;
            y += LINE + LINE_GAP;
        }
        if (line1 || line2) {
            out[3] = y - LINE_GAP;
        } else {
            out[3] = Math.max(0, y - IMAGE_GAP);
        }
        return out;
    }

    // ---------------------------------------------------------------- when it shows

    private static boolean hasRarity(String text) {
        return text.chars().anyMatch(c -> (c >= 0xF033 && c <= 0xF037) || c == 0xF092);
    }

    /** The server's title. A catch title starts ours; returns true if the server's own should not show. */
    public static boolean onTitle(Text title, long now) {
        TackleboxSettings s = TackleboxSettings.get();
        if (title == null || !CatchWatch.isCatchTitle(title.getString()) || !s.catchTitle) {
            return false;
        }
        image = title;
        lastImage = title;
        info = null;
        subtitle = lastSubtitle != null && now - subtitleAt <= SUBTITLE_MS ? lastSubtitle : null; // it came first
        shownAt = now;
        return s.catchHideTitle;
    }

    /** The server's subtitle. One with a rarity badge right after a catch title is that catch's; true if it should not show. */
    public static boolean onSubtitle(Text sub, long now) {
        if (sub == null || !hasRarity(sub.getString())) {
            return false;
        }
        lastSubtitle = sub;
        subtitleAt = now;
        TackleboxSettings s = TackleboxSettings.get();
        if (!s.catchTitle || shownAt == 0 || now - shownAt > SUBTITLE_MS) {
            return false;
        }
        subtitle = sub;
        return s.catchHideTitle;
    }

    /** The caught fish turned up in the inventory ({@link CatchWatch}): the lines are its own from here on. */
    public static void onFish(NbtCompound data, String itemName, long now) {
        if (shownAt == 0 || !TackleboxSettings.get().catchTitle) {
            return;
        }
        info = read(data, itemName);
        lastInfo = info;
    }

    /** What shows {@code now}, null when nothing does. Before the fish item is read it is only the picture. */
    public static View view(long now) {
        TackleboxSettings s = TackleboxSettings.get();
        if (shownAt == 0 || !s.catchTitle || now - shownAt > s.catchSeconds * 1000) {
            return null;
        }
        return new View(image, info, info == null && now - shownAt >= FALLBACK_MS ? subtitle : null);
    }

    /** Forgets the catch on screen (tests, leaving the server). */
    public static void clear() {
        image = subtitle = lastSubtitle = null;
        info = null;
        shownAt = subtitleAt = 0;
    }

    // ---------------------------------------------------------------- drawing

    public static void renderHud(DrawContext ctx) {
        if (shownAt != 0) {
            draw(ctx, false);
        }
    }

    /**
     * Draws the title; {@code sample} (the mover) shows your last catch, or a made up fish, without the animation.
     * Returns its rectangle.
     */
    public static int[] draw(DrawContext ctx, boolean sample) {
        TackleboxSettings s = TackleboxSettings.get();
        long now = System.currentTimeMillis();
        View v = sample ? new View(lastImage != null ? lastImage : Text.literal(""), lastInfo != null ? lastInfo : SAMPLE, null) : view(now);
        if (v == null) {
            shownAt = 0;
            return new int[]{0, 0, 0, 0};
        }
        float[] a = sample ? new float[]{1, 1, 0, 0} : BobberTimer.anim(s.catchAnim, (now - shownAt) / 1000f, s.catchSeconds);
        if (a == null || a[0] <= 0.02f) {
            return new int[]{0, 0, 0, 0};
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        List<Seg> l1 = v.info() == null ? List.of() : line1(v.info(), s);
        List<Seg> l2 = v.info() == null ? List.of() : line2(v.info(), s);
        int w1 = v.fallback() != null ? tr.getWidth(v.fallback()) : width(tr, l1);
        int w2 = width(tr, l2);
        float[] b = s.catchImage && v.image() != null ? bounds(tr, v.image()) : null;
        float imgH = b == null ? 0 : IMAGE_HEIGHT * s.catchImageSize / 100f;
        float imgW = b == null ? 0 : b[2] * imgH / b[3];
        float[] rows = layout(imgH, w1 > 0, w2 > 0);
        float w = Math.max(imgW, Math.max(w1, w2)), h = rows[3];
        if (w <= 0 || h <= 0) {
            return new int[]{0, 0, 0, 0};
        }
        float size = s.catchScale / 100f;
        float cx = (float) (ctx.getScaledWindowWidth() * s.catchX / 100), top = (float) (ctx.getScaledWindowHeight() * s.catchY / 100);
        int alpha = Math.max(4, (int) (a[0] * 255)) << 24;
        var m = ctx.getMatrices();
        m.pushMatrix();
        m.translate(cx + a[2], top + a[3] + h * size / 2); // the animation grows it from its middle
        m.scale(size * a[1], size * a[1]);
        m.translate(0, -h / 2);
        if (b != null) {
            float k = imgH / b[3];
            m.pushMatrix();
            m.translate(-imgW / 2, rows[0]);
            m.scale(k, k);
            m.translate(-b[0], -b[1]);
            ctx.drawText(tr, v.image(), 0, 0, alpha | 0xFFFFFF, false);
            m.popMatrix();
        }
        if (v.fallback() != null) {
            ctx.drawText(tr, v.fallback(), -w1 / 2, (int) rows[1], alpha | 0xFFFFFF, s.catchShadow);
        } else if (w1 > 0) {
            line(ctx, tr, l1, -w1 / 2, (int) rows[1], alpha, s.catchShadow);
        }
        long shine = sample ? -1 : BigCatch.shineStart(shownAt);
        if (shine >= 0 && w1 > 0) {
            shine(ctx, tr, v.fallback() != null ? v.fallback().asOrderedText() : ordered(l1), -w1 / 2, (int) rows[1], w1, a[0], now - shine);
        }
        if (w2 > 0) {
            line(ctx, tr, l2, -w2 / 2, (int) rows[2], alpha, s.catchShadow);
        }
        m.popMatrix();
        return new int[]{Math.round(cx - w * size / 2), Math.round(top), Math.round(w * size), Math.round(h * size)};
    }

    private static int width(TextRenderer tr, List<Seg> line) {
        int w = 0;
        for (Seg seg : line) {
            w += tr.getWidth(seg.text());
        }
        return w;
    }

    private static void line(DrawContext ctx, TextRenderer tr, List<Seg> line, int x, int y, int alpha, boolean shadow) {
        for (Seg seg : line) {
            ctx.drawText(tr, Text.literal(seg.text()), x, y, alpha | seg.rgb(), shadow && !seg.art());
            x += tr.getWidth(seg.text());
        }
    }

    private static OrderedText ordered(List<Seg> line) {
        MutableText out = Text.empty();
        for (Seg seg : line) {
            out.append(seg.text());
        }
        return out.asOrderedText();
    }

    /**
     * A big catch's light band ({@link BigCatch#shine}) over line 1, {@code t} ms after it started: each letter drawn once more
     * in the band's colour, as strong as the band is on it. Badges and other pack pictures are left alone.
     */
    private static void shine(DrawContext ctx, TextRenderer tr, OrderedText line, int x, int y, int width, float alpha, long t) {
        int rgb = BigCatch.shineColor();
        int[] left = {x};
        line.accept((index, style, cp) -> {
            Text letter = Text.literal(Character.toString(cp)).setStyle(style.withColor((TextColor) null));
            int w = tr.getWidth(letter);
            int a = Math.round(BigCatch.shine((left[0] + w / 2f - x) / width, t) * alpha * 255);
            if (a > 4 && !Glyphs.isArt(cp) && !Character.isWhitespace(cp)) {
                ctx.drawText(tr, letter, left[0], y, a << 24 | rgb, false);
            }
            left[0] += w;
            return true;
        });
    }

    /**
     * Where the picture's pixels really are when its text is drawn at 0, 0: {x, y, width, height}. A pack glyph can be any
     * size and sit anywhere around the text line, so the picture is measured, then scaled to the height asked for.
     */
    private static float[] bounds(TextRenderer tr, Text picture) {
        if (picture != measured) {
            ScreenRect r = tr.prepare(picture.asOrderedText(), 0, 0, 0xFFFFFFFF, false, false, 0).getScreenRect();
            bounds = r == null || r.width() <= 0 || r.height() <= 0
                    ? new float[]{0, 0, Math.max(1, tr.getWidth(picture)), LINE}
                    : new float[]{r.getLeft(), r.getTop(), r.width(), r.height()};
            measured = picture;
        }
        return bounds;
    }
}
