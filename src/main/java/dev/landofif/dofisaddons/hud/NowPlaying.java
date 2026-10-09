package dev.landofif.dofisaddons.hud;

import dev.landofif.dofisaddons.DofiSettings;
import dev.landofif.dofisaddons.DofisAddons;
import dev.landofif.dofisaddons.ui.Foe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeUnit;

/**
 * Whatever is playing on this computer: Spotify, a browser tab on YouTube, any player that speaks MPRIS. Read with `playerctl`
 * (Linux only, nothing else is installed or linked), once a second on its own thread; the hud only shows it, it never controls the
 * player. Text with a shadow and no box, like the other huds.
 */
public final class NowPlaying {
    private static final String SEP = "\u001f";
    private static final String FORMAT = "{{status}}" + SEP + "{{artist}}" + SEP + "{{title}}" + SEP + "{{mpris:length}}" + SEP + "{{position}}";
    private static final int MAX_TEXT = 150;

    /** Lengths and positions in milliseconds, 0 when the player does not say (a live stream has no length). */
    private record Track(boolean playing, String artist, String title, long length, long position) {}

    private static volatile Track track;
    private static boolean started;

    private NowPlaying() {}

    public static void start() {
        if (started) {
            return;
        }
        started = true;
        Thread t = new Thread(() -> {
            try {
                while (true) {
                    track = DofiSettings.get().nowPlaying ? poll() : null;
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
            if (f.length < 5 || f[2].isBlank()) {
                continue;
            }
            boolean playing = f[0].equals("Playing");
            if (!playing && !f[0].equals("Paused")) {
                continue;
            }
            Track t = new Track(playing, f[1], f[2], micros(f[3]), micros(f[4]));
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

    /** Draws the hud at its saved spot and returns {x, y, w, h}; with preview nothing playing still shows a line so it can be moved. */
    public static int[] draw(DrawContext ctx, boolean preview) {
        Track t = track;
        if (t == null) {
            if (!preview) {
                return null;
            }
            t = new Track(false, "", "nothing playing", 0, 0);
        }
        TextRenderer tr = MinecraftClient.getInstance().textRenderer;
        String title = tr.trimToWidth(t.title(), MAX_TEXT);
        String artist = tr.trimToWidth(t.artist(), MAX_TEXT);
        String time = t.length() <= 0 && t.position() <= 0 ? "" : ServerHud.clock(t.position()) + (t.length() > 0 ? " / " + ServerHud.clock(t.length()) : "");
        if (!t.playing() && !time.isEmpty()) {
            time = "paused " + time;
        }
        String[] lines = {title, artist, Foe.text(time).getString()};
        int w = 0, rows = 0;
        for (String l : lines) {
            if (!l.isEmpty()) {
                w = Math.max(w, tr.getWidth(l));
                rows++;
            }
        }
        DofiSettings s = DofiSettings.get();
        Foe.title(ctx, s.nowPlayingX, s.nowPlayingY, Foe.text("now playing"));
        int y = s.nowPlayingY + 20;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].isEmpty()) {
                ctx.drawTextWithShadow(tr, lines[i], s.nowPlayingX + 8, y, i == 0 ? Foe.TEXT : Foe.DIM);
                y += 11;
            }
        }
        return new int[]{s.nowPlayingX, s.nowPlayingY, w + 16, 25 + rows * 11};
    }

    public static void renderHud(DrawContext ctx) {
        if (DofiSettings.get().nowPlaying) {
            draw(ctx, false);
        }
    }
}
