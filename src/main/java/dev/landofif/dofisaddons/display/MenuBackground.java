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

/** A see-through colour and an optional picture (config/dofisaddons/menu.png, cropped to cover) behind the box-less menus, with square corners. */
public final class MenuBackground {
    public static final Path IMAGE = FabricLoader.getInstance().getConfigDir().resolve("dofisaddons").resolve("menu.png");
    private static final Identifier TEXTURE = Identifier.of("dofisaddons", "menu_image");
    private static long stamp = -1;
    private static int w, h;
    private static long nextCheck;

    private MenuBackground() {}

    /** Radius of the rounded corners in gui pixels (0 = square). */
    private static final int CORNER = 0;

    public static void draw(DrawContext ctx, int x, int y, int width, int height) {
        DofiSettings s = DofiSettings.get();
        int fill = (s.menuBgOpacity * 255 / 100) << 24 | s.menuBgColor & 0xFFFFFF;
        boolean picture = s.menuImageOpacity > 0 && (System.currentTimeMillis() <= nextCheck || load()) && stamp >= 0;
        if (s.menuBgOpacity <= 0 && !picture) {
            return;
        }
        double k = picture ? Math.max((double) width / w, (double) height / h) : 0;
        int dw = (int) Math.round(w * k), dh = (int) Math.round(h * k), px = x + (width - dw) / 2, py = y + (height - dh) / 2;
        int tint = (s.menuImageOpacity * 255 / 100) << 24 | 0xFFFFFF;
        int radius = Math.min(CORNER, Math.min(width, height) / 2);
        paint(ctx, x, y + radius, x + width, y + height - radius, s.menuBgOpacity > 0 ? fill : 0, picture, px, py, dw, dh, tint); // the middle
        for (int i = 0; i < radius; i++) { // the corners, a row at a time
            double up = radius - i - 0.5;
            int inset = radius - (int) Math.round(Math.sqrt(radius * radius - up * up));
            paint(ctx, x + inset, y + i, x + width - inset, y + i + 1, s.menuBgOpacity > 0 ? fill : 0, picture, px, py, dw, dh, tint);
            paint(ctx, x + inset, y + height - 1 - i, x + width - inset, y + height - i, s.menuBgOpacity > 0 ? fill : 0, picture, px, py, dw, dh, tint);
        }
    }

    private static void paint(DrawContext ctx, int x1, int y1, int x2, int y2, int fill, boolean picture, int px, int py, int dw, int dh, int tint) {
        if (fill != 0) {
            ctx.fill(x1, y1, x2, y2, fill);
        }
        if (picture) {
            ctx.enableScissor(x1, y1, x2, y2);
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, TEXTURE, px, py, 0f, 0f, dw, dh, w, h, w, h, tint);
            ctx.disableScissor();
        }
    }

    /** Pictures are scaled by the gui, so they get linear filtering; the default is the nearest pixel, which looks blocky. */
    public static NativeImageBackedTexture smooth(NativeImageBackedTexture tex) {
        tex.sampler = com.mojang.blaze3d.systems.RenderSystem.getSamplerCache().get(com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE,
                com.mojang.blaze3d.textures.AddressMode.CLAMP_TO_EDGE, com.mojang.blaze3d.textures.FilterMode.LINEAR, com.mojang.blaze3d.textures.FilterMode.LINEAR, false);
        return tex;
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
                mc.getTextureManager().registerTexture(TEXTURE, smooth(new NativeImageBackedTexture(() -> "dofisaddons menu image", img)));
            }
            return true;
        } catch (java.io.IOException | RuntimeException e) {
            stamp = -1;
            return false;
        }
    }
}
