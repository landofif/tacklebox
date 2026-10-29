package dev.landofif.tacklebox.display;

import java.nio.file.Path;

/**
 * The screenshot preview's card without the game (display/ScreenshotPreview draws it): it slides in from the right edge, stays
 * {@link #SHOW_MS}, slides out again, and holds while the mouse is on it in a screen. The picture sits in the top right corner with
 * the copy result and the open button under it.
 */
public record ScreenshotCard(Path file, int width, int height, boolean copied, long shownAt) {
    static final long SLIDE_MS = 250;
    /** Fully in this long between sliding in and out. */
    static final long SHOW_MS = 5000;
    /** After the mouse leaves the card it stays this long before sliding out. */
    static final long LINGER_MS = 1000;
    static final int PICTURE_W = 120, MARGIN = 4, BUTTON_W = 30, BUTTON_H = 12;

    /** How far in it is: 0 out of the window, 1 in place. */
    public double in(long now) {
        long age = now - shownAt;
        if (age <= 0) {
            return 0;
        }
        if (age < SLIDE_MS) {
            return ease(age / (double) SLIDE_MS);
        }
        long out = age - SLIDE_MS - SHOW_MS;
        if (out <= 0) {
            return 1;
        }
        return out >= SLIDE_MS ? 0 : 1 - ease(out / (double) SLIDE_MS);
    }

    public boolean gone(long now) {
        return now - shownAt >= SLIDE_MS * 2 + SHOW_MS;
    }

    /** The mouse is on it: the same card with its clock moved so it slides out no sooner than {@link #LINGER_MS} from now. */
    public ScreenshotCard held(long now) {
        long latest = now - SLIDE_MS - SHOW_MS + LINGER_MS;
        return latest > shownAt ? new ScreenshotCard(file, width, height, copied, latest) : this;
    }

    /** The picture's height at {@link #PICTURE_W} wide, keeping the screenshot's shape. */
    public int pictureH() {
        return Math.max(1, Math.round(PICTURE_W * (float) height / width));
    }

    /** Left edge of the card; slides past the right edge of the window while it is out. */
    public int x(int screenW, long now) {
        return screenW - MARGIN - PICTURE_W + (int) Math.round((1 - in(now)) * (PICTURE_W + MARGIN));
    }

    /** {x, y, w, h} of the open button, under the right end of the picture. */
    public int[] button(int screenW, long now) {
        return new int[]{x(screenW, now) + PICTURE_W - BUTTON_W, MARGIN + pictureH() + 2, BUTTON_W, BUTTON_H};
    }

    public boolean onButton(double mx, double my, int screenW, long now) {
        int[] b = button(screenW, now);
        return in(now) > 0 && mx >= b[0] && mx < b[0] + b[2] && my >= b[1] && my < b[1] + b[3];
    }

    /** On the picture or the row under it. */
    public boolean onCard(double mx, double my, int screenW, long now) {
        int x = x(screenW, now);
        return in(now) > 0 && mx >= x && mx < x + PICTURE_W && my >= MARGIN && my < MARGIN + pictureH() + 2 + BUTTON_H;
    }

    private static double ease(double t) {
        double u = 1 - t;
        return 1 - u * u * u;
    }
}
