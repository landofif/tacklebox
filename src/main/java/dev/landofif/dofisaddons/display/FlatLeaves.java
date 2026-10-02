package dev.landofif.dofisaddons.display;

import dev.landofif.dofisaddons.stats.StatsTracker;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.block.BlockState;
import net.minecraft.client.render.model.SimpleBlockStateModel;
import net.minecraft.client.render.model.json.ModelVariant;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.Set;

/**
 * Flat leaves (ported from fishutils). The fishonmc pack swaps the leaf block model for a 3d "leaves on
 * every side" one, and a client pack cant override a server pack, so this replaces the loaded model of
 * every leaf block with a flat one shipped in the mod ({@code dofisaddons:block/<type>_leaves_flat}),
 * which wins whichever pack supplied the original. The flat model keeps tintindex 0 for biome colours.
 * Models are only rebuilt on a resource reload, so the setting reloads resources when it changes.
 */
public final class FlatLeaves {
    private static final Set<String> KNOWN_LEAVES = Set.of(
            "oak_leaves", "spruce_leaves", "birch_leaves", "jungle_leaves", "acacia_leaves",
            "dark_oak_leaves", "mangrove_leaves", "cherry_leaves", "pale_oak_leaves",
            "azalea_leaves", "flowering_azalea_leaves");

    private FlatLeaves() {}

    public static void register() {
        ModelLoadingPlugin.register(plugin -> plugin.modifyBlockModelOnLoad().register((model, context) -> {
            if (!StatsTracker.settings().flatLeaves) {
                return model;
            }
            // no BlockTags.LEAVES: tags arent loaded yet during the startup model bake
            BlockState state = context.state();
            String path = Registries.BLOCK.getId(state.getBlock()).getPath();
            if (!KNOWN_LEAVES.contains(path)) {
                return model;
            }
            return new SimpleBlockStateModel.Unbaked(new ModelVariant(Identifier.of("dofisaddons", "block/" + path + "_flat"))).cached();
        }));
    }
}
