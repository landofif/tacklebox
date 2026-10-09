package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.display.MenuBackground;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.system.MemoryUtil;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Whatever is playing on this computer: Spotify, a browser tab on YouTube, any player that speaks MPRIS. Read with `playerctl`
 * (Linux only, nothing else is installed or linked), once a second on its own thread; the hud only shows it, it never controls the
 * player. Text with a shadow and no box, like the other huds; the heading, album art, artist, progress bar and time each have a
 * setting. The cover is `mpris:artUrl` (http, https or a file the player wrote), fetched once per track off the render thread.
 */
public final class NowPlaying {
    private static final String SEP = "\u001f";
    private static final String FORMAT = "{{status}}" + SEP + "{{artist}}" + SEP + "{{title}}" + SEP + "{{mpris:length}}" + SEP + "{{position}}" + SEP + "{{mpris:artUrl}}";
    private static final int MAX_TEXT = 150;
    /** The cover on screen, and the square it is scaled down to first (the gui can scale it up to 4x; the sampler is only bilinear). */
    private static final int ART = 32, ART_TEX = 128, ART_MAX_BYTES = 8 << 20;
    private static final Identifier ART_ID = Identifier.of("dofisaddons", "now_playing_art");

    /** Lengths and positions in milliseconds, 0 when the player does not say (a live stream has no length). */
    private record Track(boolean playing, String artist, String title, long length, long position, String art) {}

    private record Cover(String url, NativeImage image) {}

    private static volatile Track track;
    private static boolean started;

    /** The cover being wanted (poll thread), the one a worker finished (taken on the render thread), the one in the texture, and a url that would not load. */
    private static volatile String requested = "", failed = "";
    private static final AtomicReference<Cover> READY = new AtomicReference<>();
    private static String shown = "";

    private NowPlaying() {}

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    DofiSettings s = DofiSettings.get();
                    Track now = s.nowPlaying ? poll() : null;
                    track = now;
                    if (now != null && s.nowPlayingArt && !now.art().isEmpty() && !now.art().equals(requested)) {
                        requested = now.art();
                        loadCover(requested);
                    }
                    Thread.sleep(1000);
                }
            } catch (IOException e) {
                DofisAddons.LOGGER.info("now playing: playerctl is not available ({})", e.getMessage());
            } catch (InterruptedException ignored) {
            }
        }, "dofisaddons-nowplaying");
        t.setDaemon(true);
        t.start();
    }

    /** One line per player; the first one that is playing wins, else the first paused one. */
    private static Track poll() throws IOException, InterruptedException {
        Process p = new ProcessBuilder("playerctl", "-a", "metadata", "--format", FORMAT).redirectError(ProcessBuilder.Redirect.DISCARD).start();
        if (!p.waitFor(3, TimeUnit.SECONDS)) {
            p.destroyForcibly();
            return null;
        }
        Track paused = null;
        for (String line : new String(p.getInputStream().readAllBytes(), StandardCharsets.UTF_8).split("\n")) {
            String[] f = line.split(SEP, -1);
            if (f.length < 6 || f[2].isBlank()) {
                continue;
            }
            boolean playing = f[0].equals("Playing");
            if (!playing && !f[0].equals("Paused")) {
                continue;
            }
            Track t = new Track(playing, f[1], f[2], micros(f[3]), micros(f[4]), f[5].trim());
            if (playing) {
                return t;
            }
            if (paused == null) {
                paused = t;
            }
        }
        return paused;
    }

    private static long micros(String s) {
        try {
            return Long.parseLong(s.trim()) / 1000;
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /** NativeImage.read only takes png (it checks the signature), but covers are mostly jpeg: stb decodes both, wrapped the way vanilla does. */
    private static NativeImage decode(byte[] bytes) throws IOException {
        ByteBuffer buf = MemoryUtil.memAlloc(bytes.length).put(bytes).flip();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1), h = stack.mallocInt(1), n = stack.mallocInt(1);
            ByteBuffer px = STBImage.stbi_load_from_memory(buf, w, h, n, 4);
            if (px == null) {
                throw new IOException(STBImage.stbi_failure_reason());
            }
            return new NativeImage(NativeImage.Format.RGBA, w.get(0), h.get(0), true, MemoryUtil.memAddress(px));
        } finally {
            MemoryUtil.memFree(buf);
        }
    }

    /** Fetches, crops to the middle square (a youtube thumbnail is 16:9) and scales down; the texture itself is made on the render thread. */
    private static void loadCover(String url) {
        CompletableFuture.runAsync(() -> {
            try {
                URI uri = URI.create(url);
                if (!List.of("http", "https", "file").contains(uri.getScheme())) {
                    throw new IOException("unsupported cover url");
                }
                URLConnection c = uri.toURL().openConnection();
                c.setConnectTimeout(5000);
                c.setReadTimeout(5000);
                byte[] bytes;
                try (InputStream in = c.getInputStream()) {
                    bytes = in.readNBytes(ART_MAX_BYTES);
                }
                NativeImage out = new NativeImage(NativeImage.Format.RGBA, ART_TEX, ART_TEX, false);
                try (NativeImage src = decode(bytes)) {
                    int side = Math.min(src.getWidth(), src.getHeight());
                    src.resizeSubRectTo((src.getWidth() - side) / 2, (src.getHeight() - side) / 2, side, side, out);
                }
                if (!url.equals(requested)) { // the track already moved on
                    out.close();
                    return;
                }
                Cover old = READY.getAndSet(new Cover(url, out));
                if (old != null) {
                    old.image().close();
                }
            } catch (IOException | RuntimeException e) {
                failed = url;
                DofisAddons.LOGGER.info("now playing: no cover from {} ({})", url, e.toString());
            }
        });
    }

    /** Draws the hud at its saved spot and returns {x, y, w, h}; with preview nothing playing still shows a line so it can be moved. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        DofiSettings s = DofiSettings.get();
        return Foe.scaled(ctx, s.nowPlayingX, s.nowPlayingY, s.nowPlayingScale, c -> drawAt100(c, preview));
    }

    private static int[] drawAt100(DrawContext ctx, boolean preview) {
        Track t = track;
        if (t == null) {
            if (!preview) {
                return null;
            }
            t = new Track(false, "", "nothing playing", 0, 0, "");
        }
        DofiSettings s = DofiSettings.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        TextRenderer tr = mc.textRenderer;

        Cover cover = READY.getAndSet(null);
        if (cover != null) {
            mc.getTextureManager().registerTexture(ART_ID, MenuBackground.smooth(new NativeImageBackedTexture(() -> "dofisaddons now playing", cover.image())));
            shown = cover.url();
        }
        boolean art = s.nowPlayingArt && !t.art().isEmpty() && !t.art().equals(failed); // the room is kept while it loads

        String time = t.length() <= 0 && t.position() <= 0 ? "" : ServerHud.clock(t.position()) + (t.length() > 0 ? " / " + ServerHud.clock(t.length()) : "");
        if (!t.playing() && !time.isEmpty()) {
            time = "paused " + time;
        }
        String[] lines = {tr.trimToWidth(t.title(), MAX_TEXT), s.nowPlayingArtist ? tr.trimToWidth(t.artist(), MAX_TEXT) : "",
                s.nowPlayingTime ? Foe.text(time).getString() : ""};
        int textW = 0, rows = 0;
        for (String l : lines) {
            if (!l.isEmpty()) {
                textW = Math.max(textW, tr.getWidth(l));
                rows++;
            }
        }
        boolean bar = s.nowPlayingBar && t.length() > 0;
        int artW = art ? ART + 6 : 0;
        int contentW = Math.max(artW + textW, bar ? 100 : 0), contentH = Math.max(art ? ART : 0, rows * 11);
        int x = s.nowPlayingX + 8, top = s.nowPlayingY + (s.nowPlayingHeading ? 20 : 4);

        if (s.nowPlayingHeading) {
            Foe.title(ctx, s.nowPlayingX, s.nowPlayingY, Foe.text("now playing"));
        }
        if (art && t.art().equals(shown)) {
            ctx.drawTexture(RenderPipelines.GUI_TEXTURED, ART_ID, x, top, 0f, 0f, ART, ART, ART_TEX, ART_TEX, ART_TEX, ART_TEX);
        }
        int y = top + (contentH - rows * 11) / 2;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                ctx.drawTextWithShadow(tr, lines[i], x + artW, y, i == 0 ? Foe.TEXT : Foe.DIM);
                y += 11;
            }
        }
        if (bar) {
            int by = top + contentH + 3, filled = (int) (contentW * Math.max(0, Math.min(1, (double) t.position() / t.length())));
            ctx.fill(x, by, x + contentW, by + 2, Foe.FAINT);
            ctx.fill(x, by, x + filled, by + 2, Foe.TITLE);
        }
        return new int[]{s.nowPlayingX, s.nowPlayingY, contentW + 16, top - s.nowPlayingY + contentH + (bar ? 5 : 0) + 5};
    }

    public static void renderHud(DrawContext ctx) {
        if (DofiSettings.get().nowPlaying) {
            draw(ctx, false);
        }
    }
}
