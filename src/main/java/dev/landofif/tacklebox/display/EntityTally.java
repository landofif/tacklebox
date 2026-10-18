package dev.landofif.tacklebox.display;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Counts entity type ids for {@code /tacklebox debug count}. */
public final class EntityTally {
    private EntityTally() {}

    /** "type n" lines, most common first, ties by name. */
    public static List<String> lines(Iterable<String> typeIds) {
        Map<String, Integer> counts = new TreeMap<>();
        for (String id : typeIds) {
            counts.merge(id, 1, Integer::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .map(e -> e.getKey() + " " + e.getValue())
                .toList();
    }
}
