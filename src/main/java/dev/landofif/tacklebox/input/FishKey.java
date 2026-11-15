package dev.landofif.tacklebox.input;

import dev.landofif.tacklebox.display.CatchWatch;
import dev.landofif.tacklebox.trigger.TriggerEngine;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.item.Items;
import net.minecraft.text.Style;
import net.minecraft.text.StyleSpriteSource;
import net.minecraft.text.Text;
import net.minecraft.util.PlayerInput;

import java.util.Optional;

/**
 * The fish key (luna 2026-11-11): one key for fishing, as a rebind. With a rod in hand a press is one right click (cast,
 * reel in, start the minigame, cast again), and while the reeling minigame is on screen the key is sneak for as long as
 * it is held. Every input is the player's own press: nothing is sent on a bite, on a timer or when the minigame ends, and
 * holding the key never clicks again (key repeat is ignored). Bound to the same button as the game's use key (right
 * mouse) the game keeps making the right clicks, and the mod only turns the button into sneak during the minigame.
 *
 * The only game state read is "the minigame is on screen": the action bar carries the minigame in the fishing_1/2/3
 * fonts (research section 2). It is over, and the key a right click again, at the first of: the catch title, an empty
 * action bar, an action bar line that is not the minigame (the +xp line), or no new frame for a short while. And the
 * key is never sneak without the bobber out: it stays out for the whole minigame and is gone the moment the fish is caught.
 */
public final class FishKey {
    /** The gap that ends the minigame before the frame rhythm is known, and its bounds once it is. */
    static final long FIRST_GAP = 500, MIN_GAP = 150, MAX_GAP = 1500;
    /** After the catch title a late frame does not start the minigame again; the next bite is further away than this. */
    static final long QUIET = 500;

    public static KeyBinding key;

    private static boolean minigame;
    private static long lastFrame, longest, quietUntil;
    private static int frames;

    private static boolean held, sneak, blockUse;

    private FishKey() {}

    /** An action bar message is a minigame frame when a part of it is in a fishing font or it holds the minigame's glyphs. */
    public static boolean isFrame(Text message) {
        if (message == null) {
            return false;
        }
        return message.visit((style, s) -> {
            if (s.isEmpty()) {
                return Optional.<Boolean>empty();
            }
            if (style.getFont() instanceof StyleSpriteSource.Font font && isFishingFont(font.id().getPath())) {
                return Optional.of(true);
            }
            return hasFrameGlyph(s) ? Optional.of(true) : Optional.<Boolean>empty();
        }, Style.EMPTY).isPresent();
    }

    static boolean isFishingFont(String path) {
        return path.substring(path.lastIndexOf('/') + 1).startsWith("fishing_");
    }

    /** The minigame box, the bar, or a progress glyph (p00 to p100). */
    static boolean hasFrameGlyph(String s) {
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '猪' || c == '' || (c >= '' && c <= '')) {
                return true;
            }
        }
        return false;
    }

    /** Every action bar message, from InGameHudOverlayMixin. */
    public static void onActionBar(Text message, long now) {
        onActionBar(isFrame(message), now);
    }

    /** A minigame frame keeps the minigame going; anything else in the action bar (empty, the +xp line) ends it. */
    public static void onActionBar(boolean frame, long now) {
        if (!frame) {
            minigame = false;
            return;
        }
        if (now < quietUntil) {
            return;
        }
        if (minigame(now)) {
            longest = Math.max(longest, now - lastFrame);
            frames++;
        } else {
            minigame = true;
            longest = 0;
            frames = 1;
        }
        lastFrame = now;
    }

    /** The catch title (the fish's icon) ends the minigame at once. */
    public static void onTitle(String title, long now) {
        if (CatchWatch.isCatchTitle(title)) {
            minigame = false;
            quietUntil = now + QUIET;
        }
    }

    /** Whether the minigame is on screen: a frame came and the next one is not overdue. */
    public static boolean minigame(long now) {
        if (minigame && now - lastFrame > gap()) {
            minigame = false;
        }
        return minigame;
    }

    /** The bobber is out for the whole minigame and gone when the fish is caught (luna 2026-11-13): without it, a right click. */
    public static boolean minigame(long now, boolean bobberOut) {
        if (!bobberOut) {
            minigame = false;
        }
        return minigame(now);
    }

    /** How long without a frame ends the minigame: a bit over twice the longest wait between frames seen in this one. */
    static long gap() {
        return frames < 3 ? FIRST_GAP : Math.max(MIN_GAP, Math.min(MAX_GAP, longest * 2 + 50));
    }

    /**
     * One tick of the key. {@code presses} are the press events since the last tick (a held keyboard key repeats, so they
     * only count when the key was up before), {@code down} is whether it is held now, {@code ready} whether the key applies
     * (rod in hand, no screen), {@code shared} whether it sits on the same key as the game's use key (right mouse).
     * Returns true when this tick's press is a right click the mod has to make; on a shared key the game makes it.
     */
    public static boolean step(int presses, boolean down, boolean ready, boolean inMinigame, boolean shared) {
        boolean pressed = presses > 0 && !held;
        held = down;
        sneak = down && ready && inMinigame;
        if (shared && ready && inMinigame) {
            blockUse = true; // the button is sneak now, not a right click
        } else if (!shared || !down) {
            blockUse = false; // held through the end of the minigame it stays blocked, so it takes a new press to cast
        }
        return pressed && ready && !inMinigame && !shared;
    }

    /** On a shared key: the game's own right click is dropped (MinecraftClientMixin) while the button means sneak. */
    public static boolean blockUse() {
        return blockUse;
    }

    /**
     * Holding the use button with a rod (luna 2026-11-13): the game clicks again every 4 ticks while it is held, which
     * reeled the line back in right after the cast. That repeat is dropped (MinecraftClientMixin), so only a new press
     * clicks. Whatever the fish key is bound to, or unbound.
     */
    public static boolean dropHeldUse(boolean rod, boolean fishOnMc) {
        return rod && fishOnMc;
    }

    public static boolean dropHeldUse(MinecraftClient client) {
        ClientPlayerEntity p = client.player;
        return p != null && dropHeldUse(p.getMainHandStack().isOf(Items.FISHING_ROD) || p.getOffHandStack().isOf(Items.FISHING_ROD), TriggerEngine.isOnFishOnMc());
    }

    /** The key is held in the minigame: KeyboardInputMixin adds sneak to the player's input. */
    public static boolean sneaking() {
        return sneak;
    }

    public static PlayerInput withSneak(PlayerInput in) {
        return in.sneak() ? in : new PlayerInput(in.forward(), in.backward(), in.left(), in.right(), in.jump(), true, in.sprint());
    }

    /** Start of the client tick, before the game reads its own keys and the player's input. */
    public static void tick(MinecraftClient client) {
        if (key == null) {
            return;
        }
        int presses = 0;
        while (key.wasPressed()) {
            presses++;
        }
        ClientPlayerEntity p = client.player;
        boolean ready = p != null && client.interactionManager != null && client.currentScreen == null && client.getOverlay() == null
                && (p.getMainHandStack().isOf(Items.FISHING_ROD) || p.getOffHandStack().isOf(Items.FISHING_ROD))
                && TriggerEngine.isOnFishOnMc();
        boolean shared = !key.isUnbound() && KeyBindingHelper.getBoundKeyOf(key).equals(KeyBindingHelper.getBoundKeyOf(client.options.useKey));
        if (step(presses, key.isPressed(), ready, minigame(System.currentTimeMillis(), p != null && p.fishHook != null), shared) && !p.isUsingItem()) {
            client.doItemUse(); // the same path as the use key, so the bobber timer and the bite hud see it
        }
    }

    public static void reset() {
        minigame = false;
        lastFrame = longest = quietUntil = 0;
        frames = 0;
        held = sneak = blockUse = false;
    }
}
