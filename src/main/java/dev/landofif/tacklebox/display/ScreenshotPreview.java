package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.TackleboxSettings;
import dev.landofif.tacklebox.ui.Foe;
import dev.landofif.tacklebox.util.ImageClipboard;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.fabricmc.fabric.api.client.screen.v1.ScreenMouseEvents;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;

import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;
import java.util.function.LongSupplier;

/**
 * After F2 (luna 2026-10-28, like essential's): the saved screenshot goes to the clipboard on its own (util/ImageClipboard) and shows
 * in the top right corner with the copy result and an open button, which can be clicked while chat or another screen is open.
 * Setting screenshotPreview. Nothing is uploaded.
 */
public final class ScreenshotPreview {
    private static final Identifier PICTURE_ID = Identifier.of(Tacklebox.MOD_ID, "screenshot_preview");
    /** Width of the picture's texture in pixels: sharp up to gui scale 3. */
    private static final int TEX_W = 360;

    private record Picture(Path file, NativeImage image) {}

    private static final AtomicReference<ScreenshotCard> CARD = new AtomicReference<>();
    /** Made on the io thread, turned into the texture on the render thread. */
    private static final AtomicReference<Picture> READY = new AtomicReference<>();
    /** The file the registered texture shows, and its size. */
    private static Path shown;
    private static int shownW, shownH;
    /** Where the mouse was in the open screen, -1 with no screen. */
    private static double mouseX = -1, mouseY = -1;

    static Consumer<Path> opener = path -> Util.getOperatingSystem().open(path);
    static LongSupplier clock = System::currentTimeMillis;

    private ScreenshotPreview() {}

    public static void register() {
        ScreenEvents.AFTER_INIT.register((client, screen, w, h) -> {
            ScreenEvents.afterRender(screen).register((s, ctx, mx, my, delta) -> {
                mouseX = mx;
                mouseY = my;
            });
            ScreenEvents.remove(screen).register(s -> {
                mouseX = -1;
                mouseY = -1;
            });
            ScreenMouseEvents.allowMouseClick(screen).register((s, click) -> !click(click.x(), click.y(), click.button(), s.width));
        });
        HudElementRegistry.addLast(Identifier.of(Tacklebox.MOD_ID, "screenshot_preview"), (ctx, tickCounter) -> render(ctx));
    }

    /** From mixin/ScreenshotRecorderMixin on the io thread, right after the game wrote the file; the game closes the image after this. */
    public static void saved(NativeImage image, File file) {
        if (!TackleboxSettings.get().screenshotPreview) {
            return;
        }
        int w = image.getWidth(), h = image.getHeight();
        NativeImage small = new NativeImage(NativeImage.Format.RGBA, TEX_W, Math.max(1, Math.round(TEX_W * (float) h / w)), false);
        image.resizeSubRectTo(0, 0, w, h, small);
        Path path = file.toPath();
        Picture old = READY.getAndSet(new Picture(path, small));
        if (old != null) {
            old.image().close();
        }
        Util.getIoWorkerExecutor().execute(() -> show(path, w, h)); // so wl-copy doesn't hold up the game's "saved screenshot" line
    }

    /** Copies the file and puts its card up (replacing the one showing); the integration test's way in. */
    static void show(Path file, int width, int height) {
        boolean copied = ImageClipboard.copy(file);
        CARD.set(new ScreenshotCard(file, width, height, copied, clock.getAsLong()));
    }

    static ScreenshotCard card() {
        return CARD.get();
    }

    /** A click in a screen; true when it was a left click on the open button (the screen doesn't get it then). */
    static boolean click(double mx, double my, int button, int screenW) {
        ScreenshotCard c = CARD.get();
        if (c == null || button != 0 || !c.onButton(mx, my, screenW, clock.getAsLong())) {
            return false;
        }
        opener.accept(c.file());
        return true;
    }

    private static void render(DrawContext ctx) {
        ScreenshotCard c = CARD.get();
        if (c == null) {
            return;
        }
        MinecraftClient mc = MinecraftClient.getInstance();
        long now = clock.getAsLong();
        int sw = ctx.getScaledWindowWidth();
        boolean inScreen = mc.currentScreen != null;
        if (inScreen && c.onCard(mouseX, mouseY, sw, now)) {
            ScreenshotCard held = c.held(now);
            if (CARD.compareAndSet(c, held)) { // a new screenshot wins
                c = held;
            }
        }
        if (c.gone(now)) {
            CARD.compareAndSet(c, null);
            return;
        }
        Picture p = READY.getAndSet(null);
        if (p != null) {
            mc.getTextureManager().registerTexture(PICTURE_ID, MenuBackground.smooth(new NativeImageBackedTexture(() -> "tacklebox screenshot", p.image())));
            shown = p.file();
            shownW = p.image().getWidth();
            shownH = p.image().getHeight();
        }

        int x = c.x(sw, now), y = ScreenshotCard.MARGIN, h = c.pictureH();
        if (c.file().equals(shown)) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, PICTURE_ID, x, y, 0f, 0f, ScreenshotCard.PICTURE_W, h, shownW, shownH, shownW, shownH);
        }
        TextRenderer tr = mc.textRenderer;
        ctx.drawTextWithShadow(tr, Foe.text(c.copied() ? "copied" : "could not copy"), x, y + h + 4, c.copied() ? Foe.TITLE : Foe.DIM);
        int[] b = c.button(sw, now);
        Foe.button(ctx, b[0], b[1], b[2], b[3], inScreen && c.onButton(mouseX, mouseY, sw, now));
        Text open = Foe.text("open");
        ctx.drawTextWithShadow(tr, open, b[0] + (b[2] - tr.getWidth(open)) / 2, b[1] + 2, Foe.TEXT);
    }
}
