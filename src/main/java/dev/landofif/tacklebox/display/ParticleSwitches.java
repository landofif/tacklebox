package dev.landofif.tacklebox.display;

import dev.landofif.tacklebox.TackleboxSettings;
import net.minecraft.particle.ParticleType;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * Particle types switched off by name (settings {@code hiddenParticles}, ids like {@code minecraft:rain}), for
 * yours and everyone's particles alike. Every particle made from a type (server packets and the game's own, like
 * rain splashes) passes {@link #blocked} in mixin/ParticleManagerMixin, which also counts each type seen this
 * session so the particles screen can list what the server really sends.
 */
public final class ParticleSwitches {
    /** Always in the list, even before it rained: rain splashes are the usual one to switch off. */
    static final String RAIN = "minecraft:rain";

    private static final Map<ParticleType<?>, int[]> seen = new HashMap<>();
    private static Set<ParticleType<?>> off = Set.of();
    /** The settings list {@link #off} was built from; a new list (settings loaded) or a toggle rebuilds it. */
    private static List<String> builtFrom;

    private ParticleSwitches() {}

    /** Counts the particle; true when its type is switched off. */
    public static boolean blocked(ParticleType<?> type) {
        int[] count = seen.get(type);
        if (count == null) {
            seen.put(type, count = new int[1]);
        }
        count[0]++;
        List<String> ids = TackleboxSettings.get().hiddenParticles;
        if (ids != builtFrom) {
            Set<ParticleType<?>> next = new HashSet<>();
            for (String id : ids) {
                Identifier key = Identifier.tryParse(id);
                if (key != null) {
                    Registries.PARTICLE_TYPE.getOptionalValue(key).ifPresent(next::add);
                }
            }
            off = next;
            builtFrom = ids;
        }
        return !off.isEmpty() && off.contains(type);
    }

    /** How many of this type were made this session (0 if none). */
    public static int count(String id) {
        Identifier key = Identifier.tryParse(id);
        ParticleType<?> type = key == null ? null : Registries.PARTICLE_TYPE.getOptionalValue(key).orElse(null);
        int[] c = type == null ? null : seen.get(type);
        return c == null ? 0 : c[0];
    }

    /** The ids seen this session, for the screen. */
    public static List<String> seenIds() {
        return seen.keySet().stream().map(t -> String.valueOf(Registries.PARTICLE_TYPE.getId(t))).toList();
    }

    /** Seen, switched off and rain, each once, by name. */
    public static List<String> list(Collection<String> seenIds, Collection<String> hiddenIds) {
        Set<String> all = new TreeSet<>(seenIds);
        all.addAll(hiddenIds);
        all.add(RAIN);
        return List.copyOf(all);
    }

    /** Switches a type off, or back on. */
    public static void toggle(List<String> hiddenIds, String id) {
        if (!hiddenIds.remove(id)) {
            hiddenIds.add(id);
        }
        builtFrom = null;
    }
}
