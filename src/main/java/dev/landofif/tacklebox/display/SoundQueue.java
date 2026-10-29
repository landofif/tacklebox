package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.Tacklebox;
import dev.landofif.tacklebox.trigger.Trigger;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.sound.PositionedSoundInstance;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Plays trigger sounds, including repeats spaced out over time. Any sound id works, including
 * sounds from the FishOnMC server resource pack.
 */
public final class SoundQueue {
    private record Pending(SoundEvent sound, float volume, float pitch, long at) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    private SoundQueue() {}

    public static void play(Trigger.SoundSettings s) {
        Identifier id = parseId(s.soundId);
        if (id == null) {
            Tacklebox.LOGGER.warn("Not a valid sound id: '{}'", s.soundId);
            return;
        }
        SoundEvent sound = SoundEvent.of(id);
        long now = System.currentTimeMillis();
        long gap = (long) (s.secondsBetween * 1000);
        for (int i = 0; i < Math.max(1, s.plays); i++) {
            PENDING.add(new Pending(sound, (float) s.volume, (float) s.pitch, now + i * gap));
        }
        tick(); // play the first one immediately
    }

    public static void tick() {
        if (PENDING.isEmpty()) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return; // tests
        }
        long now = System.currentTimeMillis();
        Iterator<Pending> it = PENDING.iterator();
        while (it.hasNext()) {
            Pending p = it.next();
            if (p.at <= now) {
                it.remove();
                // Master category and non-positional, so it plays at the same loudness anywhere.
                client.getSoundManager().play(PositionedSoundInstance.master(p.sound, p.pitch, p.volume));
            }
        }
    }

    public static void stopAll() {
        PENDING.clear();
    }

    /** Accepts "minecraft:block.bell.use", "block.bell.use" or "block/bell/use". */
    public static Identifier parseId(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = raw.trim().toLowerCase().replace(' ', '_');
        if (!s.contains(":")) {
            s = "minecraft:" + s.replace('/', '.');
        }
        return Identifier.tryParse(s);
    }
}
