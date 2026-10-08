package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.DofiSettings;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.nio.file.Files;
import java.nio.file.Path;

/** A see-through colour and an optional picture (config/dofisaddons/menu.png, cropped to cover) behind the box-less menus. */
public final class MenuBackground {
    public static final Path IMAGE = FabricLoader.getInstance().getConfigDir().resolve("dofisaddons").resolve("menu.png");
    private static final Identifier TEXTURE = Identifier.of("dofisaddons", "menu_image");
    private static long stamp = -1;
    private static int w, h;
    private static long nextCheck;

    private MenuBackground() {}

    public static void draw(DrawContext ctx, int x, int y, int width, int height) {
        DofiSettings s = DofiSettings.get();
        if (s.menuBgOpacity > 0) {
            ctx.fill(x, y, x + width, y + height, (s.menuBgOpacity * 255 / 100) << 24 | s.menuBgColor & 0xFFFFFF);
        }
        if (s.menuImageOpacity <= 0 || System.currentTimeMillis() > nextCheck && !load() || stamp < 0) {
            return;
        }
        double k = Math.max((double) width / w, (double) height / h);
        int dw = (int) Math.round(w * k), dh = (int) Math.round(h * k);
        ctx.enableScissor(x, y, x + width, y + height);
        ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, x + (width - dw) / 2, y + (height - dh) / 2, 0f, 0f, dw, dh, w, h, w, h,
                (s.menuImageOpacity * 255 / 100) << 24 | 0xFFFFFF);
        ctx.disableScissor();
    }

    /** False when there is no usable file; checked once a second, reloads when the file changes. */
    private static boolean load() {
        nextCheck = System.currentTimeMillis() + 1000;
        try {
            long now = Files.exists(IMAGE) ? Files.getLastModifiedTime(IMAGE).toMillis() : -1;
            if (now == stamp) {
                return now >= 0;
            }
            stamp = now;
            MinecraftClient mc = MinecraftClient.getInstance();
            if (now < 0) {
                mc.getTextureManager().destroyTexture(TEXTURE);
                return false;
            }
            try (var in = Files.newInputStream(IMAGE)) {
                NativeImage img = NativeImage.read(in);
                w = img.getWidth();
                h = img.getHeight();
                mc.getTextureManager().registerTexture(TEXTURE, new NativeImageBackedTexture(() -> "dofisaddons menu image", img));
            }
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            stamp = -1;
            return false;
        }
    }
}
